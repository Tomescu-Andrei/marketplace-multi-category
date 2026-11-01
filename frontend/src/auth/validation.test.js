import { describe, expect, it } from 'vitest';
import { validate } from './validation.js';

const values = { username: 'seller', email: 'seller@example.test', displayName: 'Seller', password: 'A-test-password-2026!', confirmPassword: 'A-test-password-2026!' };

describe('registration validation', () => {
  it('accepts valid matching passwords', () => expect(validate(values, true)).toEqual({}));
  it('rejects a mismatched confirmation', () => expect(validate({ ...values, confirmPassword: 'different' }, true)).toHaveProperty('confirmPassword'));
  it('respects the backend UTF-8 byte limit for non-ASCII passwords', () => {
    const password = 'é'.repeat(40);
    expect(validate({ ...values, password, confirmPassword: password }, true)).toHaveProperty('password');
  });
  it('accepts exactly 72 UTF-8 bytes', () => {
    const password = 'é'.repeat(36);
    expect(validate({ ...values, password, confirmPassword: password }, true)).toEqual({});
  });
  it('requires a display name, email and minimum password length', () => {
    expect(validate({ ...values, email: 'invalid', displayName: ' ', password: 'short' }, true)).toMatchObject({ email: expect.any(String), displayName: expect.any(String), password: expect.any(String) });
  });
  it('does not enforce registration length rules when logging in', () => {
    expect(validate({ identifier: 'admin', password: 'admin' }, false)).toEqual({});
  });
  it('rejects email-shaped usernames at registration', () => {
    expect(validate({ ...values, username: values.email }, true)).toHaveProperty('username');
  });
});
