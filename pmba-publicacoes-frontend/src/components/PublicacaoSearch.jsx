import { useState } from 'react';
import './PublicacaoSearch.css';

function PublicacaoSearch({ onPublicacaoSelect }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [results, setResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const handleSearch = () => {
    if (searchTerm.trim() === '') {
      setResults([]);
      return;
    }
    setIsLoading(true);
    setHasSearched(true);

    // VVV--- CORREÇÃO DO ENDPOINT E DO PARÂMETRO ---VVV
    const url = `http://localhost:8080/api/publicacoes/busca?termo=${encodeURIComponent(searchTerm)}`;
    
    fetch(url)
      .then(res => {
        if (!res.ok) {
            throw new Error('Falha na busca.');
        }
        return res.json();
      })
      .then(data => {
        setResults(data);
      })
      .catch(() => {
        setResults([]);
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  return (
    <div className="search-wrapper">
      <div className="search-container">
        <input
          type="text"
          value={searchTerm}
          onChange={e => setSearchTerm(e.target.value)}
          placeholder="Buscar por título ou conteúdo..."
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
        />
        <button type="button" onClick={handleSearch} disabled={isLoading}>
          {isLoading ? 'Buscando...' : 'Buscar'}
        </button>
      </div>
      {hasSearched && (
        <ul className="search-results">
          {isLoading ? (
            <li>A carregar...</li>
          ) : results.length > 0 ? (
            results.map(pub => (
              <li key={pub.id} onClick={() => onPublicacaoSelect(pub)}>
                {pub.titulo} ({pub.tipo} Nº {pub.numero})
              </li>
            ))
          ) : (
            <li>Nenhum resultado encontrado.</li>
          )}
        </ul>
      )}
    </div>
  );
}

export default PublicacaoSearch;
