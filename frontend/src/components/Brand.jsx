import { Link } from 'react-router-dom';

export default function Brand() {
  return <Link to="/" className="brand" aria-label="Piața — pagina principală"><span className="brand-mark" aria-hidden="true"><i /><i /><i /><i /></span><span>Piața<span className="brand-dot">.</span></span></Link>;
}
