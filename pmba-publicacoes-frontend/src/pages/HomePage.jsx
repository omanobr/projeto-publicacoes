import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import '../App.css';

function HomePage() {
  const [publicacoes, setPublicacoes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    fetch('http://localhost:8080/api/publicacoes')
      .then(response => {
        if (!response.ok) {
          throw new Error('Falha ao buscar publicações. O servidor pode estar com problemas.');
        }
        return response.json();
      })
      .then(data => {
        setPublicacoes(data);
        setError(null);
      })
      .catch(err => {
        console.error("Erro detalhado:", err);
        setError(err.message);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  if (loading) return <p>A carregar publicações...</p>;
  if (error) return <p style={{ color: 'red' }}>Erro: {error}</p>;

  return (
    <div>
      <h2>Últimas Publicações</h2>
      <div className="lista-publicacoes">
        {publicacoes.length === 0 ? (
          <p>Nenhuma publicação encontrada.</p>
        ) : (
          <ul>
            {publicacoes.map(pub => (
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
        )}
      </div>
    </div>
  );
}

export default HomePage;
