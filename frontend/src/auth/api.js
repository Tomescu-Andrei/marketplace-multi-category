export class ApiError extends Error {
  constructor(status, code, errors = []) {
    super(code);
    this.status = status;
    this.code = code;
    this.errors = errors;
  }
}

async function request(path, { signal, ...options } = {}) {
  const controller = new AbortController();
  const abort = () => controller.abort();
  signal?.addEventListener('abort', abort, { once: true });
  if (signal?.aborted) controller.abort();
  const timeout = setTimeout(abort, 15000);
  try {
    const response = await fetch(`/api/v1${path}`, {
      ...options, credentials: 'same-origin', cache: 'no-store', signal: controller.signal,
      headers: { Accept: 'application/json', ...options.headers },
    });
    if (response.status === 204) return null;
    const body = await response.json().catch(() => null);
    if (!response.ok) throw new ApiError(response.status, body?.code || 'HTTP_ERROR', body?.errors || []);
    if (body === null) throw new ApiError(502, 'INVALID_RESPONSE');
    return body;
  } catch (error) {
    if (signal?.aborted || error instanceof ApiError) throw error;
    throw new ApiError(0, 'NETWORK_ERROR');
  } finally {
    clearTimeout(timeout);
    signal?.removeEventListener('abort', abort);
  }
}

async function post(path, body) {
  // Fetch a session-bound token for every mutation, including after login/logout rotations.
  const csrf = await request('/auth/csrf');
  return request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
}

export const authApi = {
  async me(options) {
    try { return await request('/auth/me', options); }
    catch (error) { if (error.status === 401) return null; throw error; }
  },
  login: (values) => post('/auth/login', values),
  register: (values) => post('/auth/register', values),
  logout: () => post('/auth/logout'),
};

export function describeError(error) {
  if (error.status === 409) return 'Username-ul sau adresa de e-mail este deja folosită. Alege alte date sau autentifică-te.';
  if (error.status === 401) return 'Datele de autentificare nu sunt corecte.';
  if (error.code === 'INVALID_CSRF_TOKEN') return 'Sesiunea formularului a expirat. Încearcă din nou.';
  if (error.status === 400) return 'Verifică informațiile introduse și încearcă din nou.';
  return 'Nu putem comunica momentan cu serverul. Verifică conexiunea și încearcă din nou.';
}
