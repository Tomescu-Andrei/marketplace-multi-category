import { useId, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

export default function Field({ label, name, error, hint, type = 'text', ...props }) {
  const id = useId();
  const [visible, setVisible] = useState(false);
  const password = type === 'password';
  const passwordLabel = name === 'confirmPassword' ? 'confirmarea parolei' : 'parola';
  return <div className="field">
    <label htmlFor={id}>{label}</label>
    <div className="input-wrap">
      <input {...props} id={id} name={name} type={password && visible ? 'text' : type}
        className={password ? 'password-input' : ''} aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined} />
      {password && <button type="button" className="reveal" aria-label={visible ? `Ascunde ${passwordLabel}` : `Arată ${passwordLabel}`}
        aria-pressed={visible} onClick={() => setVisible(!visible)}>{visible ? <EyeOff size={18} /> : <Eye size={18} />}</button>}
    </div>
    {error ? <p id={`${id}-error`} className="field-error">{error}</p> : hint && <p id={`${id}-hint`} className="field-hint">{hint}</p>}
  </div>;
}
