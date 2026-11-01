import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, ArrowUpRight, Check, CheckCircle2, LoaderCircle, LockKeyhole, LogOut, Mail, Package2, ShieldCheck, UserRound } from 'lucide-react';
import Brand from './components/Brand.jsx';
import Field from './components/Field.jsx';
import ThemeToggle from './components/ThemeToggle.jsx';
import { useAuth } from './auth/AuthContext.jsx';
import { describeError } from './auth/api.js';
import { serverFieldErrors, validate } from './auth/validation.js';

function Story() {
  return <aside className="story"><div className="story-top"><span className="eyebrow"><span /> MAI MULT DECÂT UN MARKETPLACE</span>
    <h1>Lucruri bune.<br />Povești <em>noi.</em></h1><p>Un loc pentru ce îți dorești.<br />Un nou început pentru ce dai mai departe.</p></div>
    <div className="story-art" aria-hidden="true"><div className="art-orbit" /><div className="art-tag"><span className="tag-dot" /> O nouă poveste începe aici</div>
      <div className="art-card card-back"><div className="art-book"><span /><span /><span /></div><span>O idee bună merită continuată.</span></div>
      <div className="art-card card-front"><div className="art-laptop"><div /><span /></div><div className="art-card-caption"><span>Mai departe, cu bucurie.</span><ArrowUpRight size={17} /></div></div>
      <div className="art-seal"><Package2 size={23} strokeWidth={1.4} /></div></div>
    <div className="story-bottom"><ShieldCheck size={21} /><p>Contul tău. Toate categoriile.<br /><span>Un singur loc de unde să începi.</span></p></div></aside>;
}

function Layout({ children }) {
  const { user } = useAuth();
  const { pathname } = useLocation();
  const registering = pathname === '/register';
  return <div className="app-shell"><header className="site-header"><Brand /><div className="header-actions"><div className="header-note">{user ? <span className="flex items-center gap-2"><UserRound size={16} /> Contul meu</span> : <>{registering ? 'Ai deja un cont?' : 'Ești pentru prima dată aici?'} <Link to={registering ? '/login' : '/register'}>{registering ? 'Autentifică-te' : 'Creează un cont'} <ArrowUpRight size={15} /></Link></>}</div><ThemeToggle /></div></header>
    <main className="auth-layout"><Story /><section className="form-side">{children}</section></main>
    <footer className="site-footer"><span>© {new Date().getFullYear()} Piața. Lucruri bune, mai departe.</span><span className="flex items-center gap-2"><LockKeyhole size={13} /> Parola ta rămâne privată</span></footer></div>;
}

function AuthForm({ registering = false }) {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const formRef = useRef(null);
  const titleRef = useRef(null);
  const [values, setValues] = useState({ displayName: '', username: '', email: '', identifier: location.state?.identifier || location.state?.email || '', password: '', confirmPassword: '' });
  const [errors, setErrors] = useState({});
  const [failure, setFailure] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { document.title = `${registering ? 'Creează un cont' : 'Autentificare'} · Piața`; titleRef.current?.focus(); }, [registering]);
  if (auth.user) return <Navigate to="/cont" replace />;

  const change = (event) => {
    const { name, value } = event.target;
    setValues((previous) => ({ ...previous, [name]: value }));
    setErrors((previous) => ({ ...previous, [name]: undefined }));
    setFailure('');
  };
  const submit = async (event) => {
    event.preventDefault();
    if (busy) return;
    const validation = validate(values, registering);
    setErrors(validation);
    setFailure('');
    if (Object.keys(validation).length) {
      Array.from(formRef.current.elements).find((element) => validation[element.name])?.focus();
      return;
    }
    setBusy(true);
    const payload = registering
      ? { username: values.username.trim(), email: values.email.trim(), password: values.password }
      : { identifier: values.identifier.trim(), password: values.password };
    try {
      if (registering) {
        await auth.register({ ...payload, displayName: values.displayName.trim() });
        navigate('/login', { replace: true, state: { registered: true, identifier: payload.username } });
      } else {
        await auth.login(payload);
        navigate('/cont', { replace: true });
      }
    } catch (error) {
      const fields = serverFieldErrors(error);
      setErrors(fields);
      setFailure(describeError(error));
      if (Object.keys(fields).length) formRef.current?.elements.namedItem(Object.keys(fields)[0])?.focus();
    } finally { setBusy(false); }
  };

  return <div className="form-content"><div className="form-kicker"><span className="little-line" /> CONTUL TĂU, ÎNTR-UN SINGUR LOC</div>
    <h2 ref={titleRef} tabIndex={-1}>{registering ? 'Bine ai venit în Piața.' : 'Bine ai revenit.'}</h2>
    <p className="form-intro">{registering ? 'Câteva detalii și ești parte din comunitate.' : 'Intră în cont și continuă de unde ai rămas.'}</p>
    {!registering && location.state?.registered && <div className="notice success" role="status"><CheckCircle2 size={18} /><span>Contul tău este pregătit. Autentifică-te pentru a continua.</span></div>}
    {!registering && auth.notice && !location.state?.registered && <div className="notice" role="status">{auth.notice}</div>}
    {failure && <div className="notice error" role="alert">{failure}</div>}
    <form ref={formRef} onSubmit={submit} noValidate aria-busy={busy}>
      {registering && <Field label="Nume afișat" name="displayName" value={values.displayName} onChange={change} error={errors.displayName} autoComplete="nickname" maxLength={100} placeholder="Cum te numești?" disabled={busy} required />}
      {registering && <Field label="Username" name="username" value={values.username} onChange={change} error={errors.username} autoComplete="username" maxLength={32} placeholder="ex: ana.popescu" hint="3–32 caractere: litere, cifre, punct, _ sau -." disabled={busy} required />}
      {registering ? <Field label="Adresă de e-mail" name="email" type="email" value={values.email} onChange={change} error={errors.email} autoComplete="email" maxLength={254} placeholder="nume@exemplu.ro" disabled={busy} required />
        : <Field label="Username sau e-mail" name="identifier" value={values.identifier} onChange={change} error={errors.identifier} autoComplete="username" maxLength={254} placeholder="username sau nume@exemplu.ro" disabled={busy} required />}
      <Field label="Parolă" name="password" type="password" value={values.password} onChange={change} error={errors.password} autoComplete={registering ? 'new-password' : 'current-password'} placeholder={registering ? 'Alege o parolă sigură' : 'Introdu parola ta'}
        hint={registering ? 'Folosește cel puțin 12 caractere.' : undefined} disabled={busy} required />
      {registering && <Field label="Confirmă parola" name="confirmPassword" type="password" value={values.confirmPassword} onChange={change} error={errors.confirmPassword} autoComplete="new-password" placeholder="Introdu parola încă o dată" disabled={busy} required />}
      <button type="submit" className="primary-button" disabled={busy}>{busy ? <><LoaderCircle className="spin" size={18} /> {registering ? 'Se creează contul…' : 'Te conectăm…'}</> : <>{registering ? 'Creează contul' : 'Intră în cont'}<ArrowRight size={19} /></>}</button>
    </form>
    <p className="switch-form">{registering ? 'Ai deja un cont?' : 'Nu ai încă un cont?'} <Link to={registering ? '/login' : '/register'}>{registering ? 'Autentifică-te' : 'Înregistrează-te'}</Link></p>
    <div className="form-assurance"><ShieldCheck size={16} /><span>Parola este protejată și nu este afișată altor utilizatori.</span></div>
  </div>;
}

function Account() {
  const { user, logout, refresh, error } = useAuth();
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState('');
  useEffect(() => { document.title = 'Contul meu · Piața'; }, []);
  if (!user) return <Navigate to="/login" replace />;
  const leave = async () => {
    setBusy(true); setFailure('');
    try { await logout(); } catch (problem) { setFailure(describeError(problem)); } finally { setBusy(false); }
  };
  return <div className="form-content account-content"><div className="account-check"><Check size={27} /></div><div className="form-kicker">EȘTI ÎN LOCUL POTRIVIT</div>
    <h2>Salut, {user.displayName}.</h2><p className="form-intro">Bine ai venit în contul tău.</p>
    {(failure || error) && <div className="notice error" role="alert">{failure || error}</div>}
    <dl className="account-details"><div><dt><UserRound size={17} /> Nume afișat</dt><dd>{user.displayName}</dd></div><div><dt>Username</dt><dd>{user.username}</dd></div><div><dt><Mail size={17} /> E-mail</dt><dd>{user.email}</dd></div><div><dt>Membru din</dt><dd>{new Intl.DateTimeFormat('ro-RO', { dateStyle: 'long' }).format(new Date(user.createdAt))}</dd></div></dl>
    <div className="notice success"><ShieldCheck size={18} /><span>Te-ai autentificat cu succes.</span></div>
    <button type="button" className="primary-button" onClick={leave} disabled={busy}>{busy ? <LoaderCircle className="spin" size={18} /> : <LogOut size={18} />}{busy ? 'Te deconectăm…' : 'Deconectare'}</button>
    <button className="text-button" onClick={() => refresh()} type="button" disabled={busy}>Reîncarcă datele contului</button>
  </div>;
}

export default function App() {
  const { status, error, refresh, user } = useAuth();
  return <Layout>{status === 'loading' ? <div className="loading-state" role="status"><LoaderCircle className="spin" size={26} /><p>Pregătim contul tău…</p></div>
    : status === 'error' ? <div className="form-content"><h2>Revenim imediat.</h2><p className="form-intro" role="alert">{error}</p><button className="primary-button" onClick={() => refresh()}>Încearcă din nou <ArrowRight size={18} /></button></div>
      : <Routes><Route path="/" element={<Navigate to={user ? '/cont' : '/login'} replace />} /><Route path="/login" element={<AuthForm key="login" />} /><Route path="/register" element={<AuthForm key="register" registering />} /><Route path="/cont" element={<Account />} /><Route path="*" element={<div className="form-content"><h2>Pagina nu a fost găsită.</h2><p className="form-intro">Te ajutăm să revii la contul tău.</p><Link className="primary-button" to="/">Înapoi la început <ArrowRight size={18} /></Link></div>} /></Routes>}</Layout>;
}
