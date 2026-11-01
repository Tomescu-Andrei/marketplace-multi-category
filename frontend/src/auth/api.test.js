import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, authApi } from './api.js';

const response = (status, body) => ({ status, ok: status >= 200 && status < 300, json: async () => body });
afterEach(() => vi.unstubAllGlobals());

describe('session API', () => {
  it('renews the CSRF token for login and for the next logout mutation', async () => {
    const fetch = vi.fn()
      .mockResolvedValueOnce(response(200, { headerName: 'X-CSRF-TOKEN', token: 'anonymous-token' }))
      .mockResolvedValueOnce(response(200, { id: 2, email: 'seller@example.test' }))
      .mockResolvedValueOnce(response(200, { headerName: 'X-CSRF-TOKEN', token: 'authenticated-token' }))
      .mockResolvedValueOnce(response(204));
    vi.stubGlobal('fetch', fetch);
    await authApi.login({ email: 'seller@example.test', password: 'Test-password-2026!' });
    await authApi.logout();
    expect(fetch.mock.calls.map(([path]) => path)).toEqual(['/api/v1/auth/csrf', '/api/v1/auth/login', '/api/v1/auth/csrf', '/api/v1/auth/logout']);
    expect(fetch.mock.calls[1][1]).toMatchObject({ credentials: 'same-origin', headers: { 'X-CSRF-TOKEN': 'anonymous-token' } });
    expect(fetch.mock.calls[3][1]).toMatchObject({ headers: { 'X-CSRF-TOKEN': 'authenticated-token' } });
  });
  it('treats a missing session as anonymous but surfaces server failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response(401, { code: 'UNAUTHENTICATED' })).mockResolvedValueOnce(response(503, { code: 'HTTP_ERROR' })));
    await expect(authApi.me()).resolves.toBeNull();
    await expect(authApi.me()).rejects.toMatchObject({ status: 503 });
  });
  it('preserves structured server validation errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response(200, { headerName: 'X-CSRF-TOKEN', token: 'token' })).mockResolvedValueOnce(response(400, { code: 'VALIDATION_FAILED', errors: [{ field: 'email', message: 'Invalid email' }] })));
    await expect(authApi.register({})).rejects.toMatchObject({ status: 400, errors: [{ field: 'email', message: 'Invalid email' }] });
  });
  it('reports an unreachable backend without pretending the session is logged out', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    await expect(authApi.me()).rejects.toMatchObject({ code: 'NETWORK_ERROR' });
  });
  it('rejects successful HTML responses from a broken proxy', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ status: 200, ok: true, json: async () => { throw new SyntaxError(); } }));
    await expect(authApi.me()).rejects.toBeInstanceOf(ApiError);
  });
});
