import { Outlet } from 'react-router-dom';
import Header from './components/Header'; // <-- Importe o novo componente
import './App.css';

function App() {
  return (
    <div className="app-container">
      {/* Substitua o <header> antigo pelo novo componente */}
      <Header /> 
      
      <main>
        <Outlet />
      </main>
    </div>
  );
}

export default App;