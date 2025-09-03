import { useState } from 'react';
import { Link } from 'react-router-dom';

function BuscaAvancadaPage() {
  const [termo, setTermo] = useState('');
  const [resultados, setResultados] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [erro, setErro] = useState(null);

  const handleSearch = (event) => {
    event.preventDefault();
    if (!termo.trim()) return;

    setBuscando(true);
    setErro(null);
    setResultados([]);

    fetch(`http://localhost:8080/api/publicacoes/busca-avancada?conteudo=${encodeURIComponent(termo)}`)
      .then(res => {
        if (!res.ok) throw new Error('Falha ao realizar a busca.');
        return res.json();
      })
      .then(data => setResultados(data))
      .catch(err => setErro(err.message))
      .finally(() => setBuscando(false));
  };

  return (
    <div>
      <Link to="/">&larr; Voltar para a página inicial</Link>
      <h2>Busca Avançada no Conteúdo</h2>
      <p>Digite um trecho do texto, nome ou termo que deseja encontrar dentro das publicações.</p>

      <form onSubmit={handleSearch} style={{ display: 'flex', gap: '1rem', margin: '1rem 0' }}>
        <input
          type="text"
          value={termo}
          onChange={(e) => setTermo(e.target.value)}
          placeholder="Digite o texto para buscar..."
          style={{ flexGrow: 1, padding: '0.5rem' }}
        />
        <button type="submit" disabled={buscando}>
          {buscando ? 'Buscando...' : 'Buscar'}
        </button>
      </form>

      {erro && <p style={{ color: 'red' }}>Erro: {erro}</p>}

      <div className="lista-publicacoes">
        {resultados.length > 0 && <h4>Resultados Encontrados:</h4>}
        <ul>
          {resultados.map(pub => (
            <li key={pub.id} className={pub.status === 'REVOGADA' ? 'revogado-item' : ''}>
              <Link to={`/publicacao/${pub.id}`}>
                {pub.titulo}
                {pub.status === 'REVOGADA' && <span className="status-tag revogado">REVOGADO</span>}
              </Link>
              <div className="pub-details">
                <span>{pub.tipo} Nº {pub.numero}</span>
                <span style={{ margin: '0 8px' }}>|</span>
                <span>{new Date(pub.dataPublicacao).toLocaleDateString()}</span>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default BuscaAvancadaPage;