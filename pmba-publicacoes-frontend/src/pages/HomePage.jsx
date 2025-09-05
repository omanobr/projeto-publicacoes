import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

function HomePage() {
  const [publicacoes, setPublicacoes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [searchParams, setSearchParams] = useSearchParams();

  // Estado para todos os campos do filtro
  const [filters, setFilters] = useState({
    numero: searchParams.get('numero') || '',
    conteudo: searchParams.get('conteudo') || '',
    ano: searchParams.get('ano') || '',
    dataInicial: searchParams.get('dataInicial') || '',
    dataFinal: searchParams.get('dataFinal') || '',
  });

  useEffect(() => {
    const fetchPublicacoes = () => {
      setLoading(true);
      
      // Constrói a URL com base nos searchParams atuais
      const params = new URLSearchParams(searchParams);
      const url = `http://localhost:8080/api/publicacoes?${params.toString()}`;
      
      fetch(url)
        .then(response => {
          if (!response.ok) throw new Error('Falha ao buscar publicações.');
          return response.json();
        })
        .then(data => {
          setPublicacoes(data);
          setError(null);
        })
        .catch(err => setError(err.message))
        .finally(() => setLoading(false));
    };

    fetchPublicacoes();
  }, [searchParams]); // Re-executa a busca sempre que os parâmetros da URL mudam

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({ ...prev, [name]: value }));
  };

  const handleSearch = (e) => {
    e.preventDefault();
    const newParams = new URLSearchParams();
    // Adiciona apenas os filtros que têm valor
    Object.entries(filters).forEach(([key, value]) => {
      if (value) {
        newParams.set(key, value);
      }
    });
    setSearchParams(newParams);
  };

  const clearFilters = () => {
    setFilters({ numero: '', conteudo: '', ano: '', dataInicial: '', dataFinal: '' });
    setSearchParams({});
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    // Adiciona o fuso horário para corrigir a exibição
    return new Date(date.getTime() + date.getTimezoneOffset() * 60000).toLocaleDateString('pt-BR');
  };

  return (
    <div>
      <div className="filter-panel">
        <form onSubmit={handleSearch} className="filter-form">
          <div className="form-row">
            <div className="form-group-filter">
              <label htmlFor="numero">Número do Ato</label>
              <input type="text" id="numero" name="numero" value={filters.numero} onChange={handleFilterChange} placeholder="Ex: 001/2025"/>
            </div>
            <div className="form-group-filter">
              <label htmlFor="conteudo">Termo para busca</label>
              <input type="text" id="conteudo" name="conteudo" value={filters.conteudo} onChange={handleFilterChange} placeholder="Busca no conteúdo do texto"/>
            </div>
            <div className="form-group-filter">
              <label htmlFor="ano">Ano</label>
              <input type="number" id="ano" name="ano" value={filters.ano} onChange={handleFilterChange} placeholder="Ex: 2024"/>
            </div>
          </div>
          <div className="form-row">
            <div className="form-group-filter">
              <label htmlFor="dataInicial">Período de Publicação (Início)</label>
              <input type="date" id="dataInicial" name="dataInicial" value={filters.dataInicial} onChange={handleFilterChange} />
            </div>
            <div className="form-group-filter">
              <label htmlFor="dataFinal">Período de Publicação (Fim)</label>
              <input type="date" id="dataFinal" name="dataFinal" value={filters.dataFinal} onChange={handleFilterChange} />
            </div>
          </div>
          <div className="form-actions-filter">
            <button type="submit" className="filter-button">Filtrar</button>
            <button type="button" className="clear-button" onClick={clearFilters}>Limpar Filtros</button>
          </div>
        </form>
      </div>

      <h2>Publicações</h2>
      {loading && <p>A carregar publicações...</p>}
      {error && <p style={{ color: 'red' }}>Erro: {error}</p>}
      {!loading && !error && (
        <div className="lista-publicacoes">
          {publicacoes.length === 0 ? (
            <p>Nenhuma publicação encontrada para os filtros aplicados.</p>
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
                    <span>{formatDate(pub.dataPublicacao)}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

export default HomePage;

