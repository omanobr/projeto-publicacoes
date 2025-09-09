import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

function HomePage() {
  const [publicacoes, setPublicacoes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [searchParams, setSearchParams] = useSearchParams();
  const [filtros, setFiltros] = useState({
    numero: searchParams.get('numero') || '',
    conteudo: searchParams.get('termo') || '',
    ano: searchParams.get('ano') || '',
    dataInicial: searchParams.get('dataInicial') || '',
    dataFinal: searchParams.get('dataFinal') || '',
    status: searchParams.get('status') || 'TODAS',
  });

  useEffect(() => {
    const fetchPublicacoes = () => {
      setLoading(true);
      
      const paramsParaBusca = { ...filtros };
      if (paramsParaBusca.status === 'TODAS') {
        delete paramsParaBusca.status;
      }

      const params = new URLSearchParams(paramsParaBusca);
      Object.keys(paramsParaBusca).forEach(key => {
        if (!paramsParaBusca[key]) {
          params.delete(key);
        }
      });
      
      const queryString = params.toString();
      const url = `http://localhost:8080/api/publicacoes/busca?${queryString}`;

      fetch(url)
        .then(response => {
          if (!response.ok) {
            throw new Error('Falha ao buscar publicações.');
          }
          return response.json();
        })
        .then(data => {
          setPublicacoes(data);
          setError(null);
        })
        .catch(err => {
          setError(err.message);
          setPublicacoes([]);
        })
        .finally(() => {
          setLoading(false);
        });
    };

    fetchPublicacoes();
  }, [searchParams]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFiltros(prev => ({ ...prev, [name]: value }));
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setSearchParams(filtros);
  };

  const handleClear = () => {
    const clearedFiltros = {
      numero: '',
      conteudo: '',
      ano: '',
      dataInicial: '',
      dataFinal: '',
      status: 'TODAS',
    };
    setFiltros(clearedFiltros);
    setSearchParams({});
  };

  return (
    <div className="homepage-layout">
      <div className="filter-panel">
        <h2>Filtros</h2>
        <form onSubmit={handleSearch} className="filter-form">
          <div className="form-group-filter">
            <label htmlFor="numero">Número do Ato</label>
            <input type="text" id="numero" name="numero" value={filtros.numero} onChange={handleInputChange} placeholder="Ex: 001/2025" />
          </div>

          <div className="form-group-filter">
            <label htmlFor="termo">Termo ou Título</label>
            <input type="text" id="termo" name="termo" value={filtros.termo} onChange={handleInputChange} placeholder="Busca no título e conteúdo" />
          </div>

          <div className="form-group-filter">
            <label htmlFor="ano">Ano da Publicação</label>
            <input type="number" id="ano" name="ano" value={filtros.ano} onChange={handleInputChange} placeholder="Ex: 2024" />
          </div>
          
          <div className="form-group-filter">
            <label htmlFor="status">Situação</label>
            <select id="status" name="status" value={filtros.status} onChange={handleInputChange}>
                <option value="TODAS">Todas</option>
                <option value="ATIVA">Ativas</option>
                <option value="REVOGADA">Revogadas</option>
            </select>
          </div>

          <div className="form-group-filter">
            <label>Período de Publicação</label>
            <input type="date" id="dataInicial" name="dataInicial" value={filtros.dataInicial} onChange={handleInputChange} />
            <span style={{textAlign: 'center', margin: '0.25rem 0'}}>até</span>
            <input type="date" id="dataFinal" name="dataFinal" value={filtros.dataFinal} onChange={handleInputChange} />
          </div>

          <div className="form-actions-filter">
            <button type="submit" className="filter-button">Buscar</button>
            <button type="button" onClick={handleClear} className="clear-button">Limpar Filtros</button>
          </div>
        </form>
      </div>

      <div className="lista-publicacoes">
        <h2>Publicações</h2>
        {loading && <p>A carregar publicações...</p>}
        {error && <p style={{ color: 'red' }}>Erro: {error}</p>}
        {!loading && !error && publicacoes.length === 0 && <p>Nenhuma publicação encontrada.</p>}
        
        <ul>
          {publicacoes.map(pub => (
            <li key={pub.id} className={pub.status === 'REVOGADA' ? 'revogado-item' : ''}>
              <Link to={`/publicacao/${pub.id}`}>
                {pub.titulo}
                {pub.status === 'REVOGADA' && <span className="status-tag revogado">REVOGADO</span>}
                {pub.foiAlterada && pub.status === 'ATIVA' && <span className="status-tag alterada">ALTERADA</span>}
              </Link>
              <div className="pub-details">
                <span>{pub.tipo} Nº {pub.numero}</span>
                <span style={{ margin: '0 8px' }}>|</span> 
                <span>{new Date(pub.dataPublicacao + 'T00:00:00-03:00').toLocaleDateString()}</span>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default HomePage;

