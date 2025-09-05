import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

function HomePage() {
  const [publicacoes, setPublicacoes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [searchParams, setSearchParams] = useSearchParams();

  // Inicializa o estado dos filtros a partir dos parâmetros da URL
  const [filtros, setFiltros] = useState({
    numero: searchParams.get('numero') || '',
    conteudo: searchParams.get('conteudo') || '',
    ano: searchParams.get('ano') || '',
    dataInicial: searchParams.get('dataInicial') || '',
    dataFinal: searchParams.get('dataFinal') || '',
  });

  useEffect(() => {
    const fetchPublicacoes = () => {
      setLoading(true);
      // Usa os searchParams diretamente para construir a URL, garantindo consistência
      const url = `http://localhost:8080/api/publicacoes/busca?${searchParams.toString()}`;

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

  const handleFormSubmit = (e) => {
    e.preventDefault();
    const newSearchParams = new URLSearchParams();
    Object.entries(filtros).forEach(([key, value]) => {
      if (value) {
        newSearchParams.set(key, value);
      }
    });
    setSearchParams(newSearchParams);
  };
  
  const handleClearFilters = () => {
    setFiltros({
      numero: '',
      conteudo: '',
      ano: '',
      dataInicial: '',
      dataFinal: '',
    });
    setSearchParams({});
  };


  return (
    <div>
      <h2>Consulta de Publicações</h2>

      {/* VVV--- NOVO CONTAINER PARA O LAYOUT DE DUAS COLUNAS ---VVV */}
      <div className="home-container">
        <div className="filter-panel">
          <form onSubmit={handleFormSubmit} className="filter-form">
            <div className="form-group-filter">
              <label htmlFor="numero">Número do Ato</label>
              <input type="text" name="numero" id="numero" value={filtros.numero} onChange={handleInputChange} />
            </div>
            <div className="form-group-filter">
              <label htmlFor="conteudo">Termo de Busca</label>
              <input type="text" name="conteudo" id="conteudo" value={filtros.conteudo} onChange={handleInputChange} />
            </div>
            <div className="form-group-filter">
              <label htmlFor="ano">Ano de Publicação</label>
              <input type="number" name="ano" id="ano" value={filtros.ano} onChange={handleInputChange} />
            </div>
            <div className="form-row">
              <div className="form-group-filter">
                <label htmlFor="dataInicial">Período - De</label>
                <input type="date" name="dataInicial" id="dataInicial" value={filtros.dataInicial} onChange={handleInputChange} />
              </div>
              <div className="form-group-filter">
                <label htmlFor="dataFinal">Até</label>
                <input type="date" name="dataFinal" id="dataFinal" value={filtros.dataFinal} onChange={handleInputChange} />
              </div>
            </div>
            <div className="form-actions-filter">
              <button type="submit" className="filter-button">Buscar</button>
              <button type="button" className="clear-button" onClick={handleClearFilters}>Limpar Filtros</button>
            </div>
          </form>
        </div>

        <div className="lista-publicacoes">
          {loading && <p>A carregar publicações...</p>}
          {error && <p style={{ color: 'red' }}>Erro: {error}</p>}
          {!loading && !error && publicacoes.length === 0 && (
            <p>Nenhuma publicação encontrada para os filtros aplicados.</p>
          )}
          {!loading && !error && publicacoes.length > 0 && (
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
                    <span>
                      {new Date(pub.dataPublicacao + 'T00:00:00-03:00').toLocaleDateString('pt-BR')}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

export default HomePage;

