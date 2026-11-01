import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App.jsx';
import { AuthProvider } from './auth/AuthContext.jsx';
import { ApiError, authApi } from './auth/api.js';

vi.mock('./auth/api.js', async (original) => ({ ...await original(), authApi: { me: vi.fn(), login: vi.fn(), register: vi.fn(), logout: vi.fn() } }));
const account = { id: 2, username: 'ana.demo', email: 'seller@example.test', displayName: 'Ana', createdAt: '2026-11-01T10:00:00Z' };
const mount = (path = '/login') => render(<MemoryRouter initialEntries={[path]}><AuthProvider><App /></AuthProvider></MemoryRouter>);

beforeEach(() => { vi.resetAllMocks(); authApi.me.mockResolvedValue(null); authApi.logout.mockResolvedValue(null); });

describe('authentication screens', () => {
  it('focuses the first invalid field in visual order', async () => {
    const user = userEvent.setup(); mount('/register');
    await user.click(await screen.findByRole('button', { name: 'Creează contul' }));
    expect(screen.getByLabelText('Nume afișat')).toHaveFocus();
    expect(authApi.register).not.toHaveBeenCalled();
  });
  it('allows revealing and hiding a password without clearing it', async () => {
    const user = userEvent.setup(); mount();
    const password = await screen.findByLabelText('Parolă');
    await user.type(password, 'Test-password-2026!');
    await user.click(screen.getByRole('button', { name: 'Arată parola' }));
    expect(password).toHaveAttribute('type', 'text');
    await user.click(screen.getByRole('button', { name: 'Ascunde parola' }));
    expect(password).toHaveAttribute('type', 'password');
    expect(password).toHaveValue('Test-password-2026!');
  });
  it('redirects an anonymous visitor away from the account page', async () => {
    mount('/cont');
    expect(await screen.findByRole('heading', { name: 'Bine ai revenit.' })).toBeVisible();
  });
  it('registers without sending confirmation, then pre-fills the login username', async () => {
    authApi.register.mockResolvedValue(account);
    const user = userEvent.setup(); mount('/register');
    await user.type(await screen.findByLabelText('Nume afișat'), 'Ana');
    await user.type(screen.getByLabelText('Username', { exact: true }), account.username);
    await user.type(screen.getByLabelText('Adresă de e-mail'), account.email);
    await user.type(screen.getByLabelText('Parolă', { exact: true }), 'Test-password-2026!');
    await user.type(screen.getByLabelText('Confirmă parola'), 'Test-password-2026!');
    await user.click(screen.getByRole('button', { name: 'Creează contul' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Contul tău este pregătit');
    expect(screen.getByLabelText('Username sau e-mail')).toHaveValue(account.username);
    expect(screen.getByLabelText('Parolă', { exact: true })).toHaveValue('');
    expect(authApi.register).toHaveBeenCalledWith({ displayName: 'Ana', username: account.username, email: account.email, password: 'Test-password-2026!' });
  });
  it('keeps mismatched passwords out of the API and focuses the invalid field', async () => {
    const user = userEvent.setup(); mount('/register');
    await user.type(await screen.findByLabelText('Nume afișat'), 'Ana');
    await user.type(screen.getByLabelText('Username', { exact: true }), account.username);
    await user.type(screen.getByLabelText('Adresă de e-mail'), account.email);
    await user.type(screen.getByLabelText('Parolă', { exact: true }), 'Test-password-2026!');
    await user.type(screen.getByLabelText('Confirmă parola'), 'Different-password!');
    await user.click(screen.getByRole('button', { name: 'Creează contul' }));
    expect(screen.getByText('Parolele nu coincid.')).toBeVisible();
    expect(screen.getByLabelText('Confirmă parola')).toHaveFocus();
    expect(authApi.register).not.toHaveBeenCalled();
  });
  it('logs in, shows the account and logs out', async () => {
    authApi.login.mockResolvedValue(account);
    const user = userEvent.setup(); mount();
    await user.type(await screen.findByLabelText('Username sau e-mail'), account.username);
    await user.type(screen.getByLabelText('Parolă'), 'Test-password-2026!');
    await user.click(screen.getByRole('button', { name: 'Intră în cont' }));
    expect(await screen.findByRole('heading', { name: 'Salut, Ana.' })).toBeVisible();
    expect(authApi.login).toHaveBeenCalledWith({ identifier: account.username, password: 'Test-password-2026!' });
    expect(screen.getByText(account.email)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Deconectare' }));
    expect(await screen.findByRole('heading', { name: 'Bine ai revenit.' })).toBeVisible();
    expect(screen.getByRole('status')).toHaveTextContent('Te-ai deconectat');
    expect(authApi.logout).toHaveBeenCalledTimes(1);
  });
  it('restores an existing session when loading a protected URL', async () => {
    authApi.me.mockResolvedValue(account); mount('/cont');
    expect(await screen.findByRole('heading', { name: 'Salut, Ana.' })).toBeVisible();
    expect(screen.queryByLabelText('Parolă')).not.toBeInTheDocument();
  });
  it('shows a useful login error without navigating to the account page', async () => {
    authApi.login.mockRejectedValue(new ApiError(401, 'UNAUTHENTICATED'));
    const user = userEvent.setup(); mount();
    await user.type(await screen.findByLabelText('Username sau e-mail'), account.email);
    await user.type(screen.getByLabelText('Parolă'), 'Wrong-password!');
    await user.click(screen.getByRole('button', { name: 'Intră în cont' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Datele de autentificare nu sunt corecte.');
    expect(screen.getByRole('button', { name: 'Intră în cont' })).toBeEnabled();
  });
  it('supports retry after an initial backend outage', async () => {
    authApi.me.mockRejectedValueOnce(new ApiError(0, 'NETWORK_ERROR')).mockResolvedValueOnce(null);
    const user = userEvent.setup(); mount();
    expect(await screen.findByRole('alert')).toHaveTextContent('Nu putem comunica');
    await user.click(screen.getByRole('button', { name: 'Încearcă din nou' }));
    expect(await screen.findByLabelText('Username sau e-mail')).toBeVisible();
  });
  it('does not restore an old session from a delayed refresh after logout', async () => {
    let resolveRefresh;
    authApi.me.mockResolvedValueOnce(account).mockImplementationOnce(() => new Promise((resolve) => { resolveRefresh = resolve; }));
    const user = userEvent.setup(); mount('/cont');
    await screen.findByRole('heading', { name: 'Salut, Ana.' });
    fireEvent(window, new Event('focus'));
    await user.click(screen.getByRole('button', { name: 'Deconectare' }));
    await screen.findByRole('heading', { name: 'Bine ai revenit.' });
    await act(async () => resolveRefresh(account));
    expect(screen.queryByRole('heading', { name: 'Salut, Ana.' })).not.toBeInTheDocument();
  });
});
