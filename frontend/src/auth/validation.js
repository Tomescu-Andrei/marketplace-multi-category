export function validate(values, registering) {
  const errors = {};
  if (registering) {
    if (!values.email.trim()) errors.email = 'Introdu adresa de e-mail.';
    else if (values.email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) errors.email = 'Introdu o adresă de e-mail validă.';
    if (!/^[a-zA-Z0-9][a-zA-Z0-9_.-]{2,31}$/.test(values.username?.trim() || '')) errors.username = 'Alege un username de 3–32 caractere, fără spații sau @.';
  } else if (!values.identifier?.trim()) errors.identifier = 'Introdu username-ul sau adresa de e-mail.';
  else if (values.identifier.trim().length > 254) errors.identifier = 'Introdu cel mult 254 de caractere.';
  if (!values.password.trim()) errors.password = 'Introdu parola.';
  if (registering) {
    if (!values.displayName.trim()) errors.displayName = 'Introdu numele afișat.';
    else if (values.displayName.trim().length > 100) errors.displayName = 'Numele poate avea cel mult 100 de caractere.';
    if (values.password.length < 12) errors.password = 'Parola trebuie să aibă cel puțin 12 caractere.';
    else if (new TextEncoder().encode(values.password).length > 72) errors.password = 'Parola este prea lungă. Folosește mai puține caractere.';
    if (values.confirmPassword !== values.password) errors.confirmPassword = 'Parolele nu coincid.';
  }
  return errors;
}

export function serverFieldErrors(error) {
  const messages = { username: 'Alege un username de 3–32 caractere, fără spații sau @.', identifier: 'Introdu username-ul sau adresa de e-mail.', email: 'Verifică adresa de e-mail.', displayName: 'Introdu un nume valid, de cel mult 100 de caractere.', password: 'Folosește o parolă de minimum 12 caractere și maximum 72 de octeți UTF-8.' };
  return Object.fromEntries((error.errors || []).filter(({ field }) => messages[field]).map(({ field }) => [field, messages[field]]));
}
