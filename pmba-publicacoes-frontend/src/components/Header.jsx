import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext'; // <-- Importe o hook
import './Header.css';

function Header() {
  const { isAdmin, toggleAdmin } = useAuth(); // <-- Use o contexto!

  return (
    <header className="app-header">
      <div className="logo">
        <NavLink to="/">Portal de Publicações da PMBA</NavLink>
      </div>
      <nav className="main-nav">
        <NavLink to="/">Publicações</NavLink>
      </nav>
      <div className="admin-nav">
        {/* O botão agora tem lógica de verdade! */}
        <button onClick={toggleAdmin}>
          {isAdmin ? 'Logout' : 'Login (Admin)'}
        </button>
      </div>
    </header>
  );
}

export default Header;