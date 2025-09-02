import { Outlet } from 'react-router-dom';
import './App.css'; // <--- Verifique se esta linha existe

function App() {
  return (
    // VVV--- A CLASSE PRECISA SER EXATAMENTE ESTA ---VVV
    <div className="app-container"> 
      <header>
        <h1>Portal de Publicações da PMBA</h1>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}

export default App;