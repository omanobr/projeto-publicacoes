import { useState } from 'react';
import './PublicacaoSearch.css';

function PublicacaoSearch({ onPublicacaoSelect }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [results, setResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  const handleSearch = () => {
    if (searchTerm.trim() === '') {
      setResults([]);
      return;
    }
    setIsLoading(true);
    // Assumindo que você terá um endpoint de busca no futuro.
    // Por agora, vamos apenas buscar tudo e filtrar no frontend.
    fetch(`http://localhost:8080/api/publicacoes`)
      .then(res => res.json())
      .then(data => {
        const filteredData = data.filter(pub =>
          pub.titulo.toLowerCase().includes(searchTerm.toLowerCase())
        );
        setResults(filteredData);
        setIsLoading(false);
      });
  };

  return (
    <div className="search-container">
      <input
        type="text"
        value={searchTerm}
        onChange={e => setSearchTerm(e.target.value)}
        placeholder="Buscar título da publicação de destino..."
      />
      <button type="button" onClick={handleSearch} disabled={isLoading}>
        {isLoading ? 'Buscando...' : 'Buscar'}
      </button>
      <ul className="search-results">
        {results.map(pub => (
          <li key={pub.id} onClick={() => onPublicacaoSelect(pub)}>
            {pub.titulo}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default PublicacaoSearch;