import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom'; // <-- 1. ADICIONE ESTE IMPORT
import '../App.css';

function HomePage() {
  const [publicacoes, setPublicacoes] = useState([]); // <-- Garanta que começa com []

 useEffect(() => {
  fetch('http://localhost:8080/api/publicacoes')
    .then(response => response.json())
    .then(data => {
      if (Array.isArray(data)) {
        setPublicacoes(data);
      } else if (data && Array.isArray(data.content)) {
        setPublicacoes(data.content); // caso backend esteja paginando
      } else {
        console.error("Formato inesperado da resposta:", data);
        setPublicacoes([]);
      }
    })
    .catch(error => {
      console.error("Erro ao buscar dados:", error);
      setPublicacoes([]);
    });
}, []);

  return (
    <div>
      <h2>Últimas Publicações</h2>
      <div className="lista-publicacoes">
        {publicacoes.length === 0 ? (
          <p>Carregando publicações...</p>
        ) : (
          <ul>
            {publicacoes.map(pub => (
              // 2. MODIFIQUE O 'li' PARA USAR O COMPONENTE LINK
              <li key={pub.id} className={pub.status === 'REVOGADA' ? 'revogado-item' : ''}>
                <Link to={`/publicacao/${pub.id}`}>
                  {pub.titulo}
                  {/* Se o status for REVOGADA, mostra a tag */}
                  {pub.status === 'REVOGADA' && <span className="status-tag revogado">REVOGADO</span>}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default HomePage; // Exporte HomePage