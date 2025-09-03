import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

function HomePage() {
  const [publicacoes, setPublicacoes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Para a busca
  const [searchParams, setSearchParams] = useSearchParams();
  const termoDeBusca = searchParams.get('termo') || '';

  useEffect(() => {
    setLoading(true);

    const formatSearchTermForAPI = (term) => {
      // Regex para identificar o formato DD-MM-AAAA ou DD/MM/AAAA
      const dateRegex = /^(\d{2})[-/](\d{2})[-/](\d{4})$/;
      const match = term.match(dateRegex);

      if (match) {
        // Se for uma data, converte para o formato AAAA-MM-DD que a API espera
        const [, day, month, year] = match;
        return `${year}-${month}-${day}`;
      }
      // Se não for uma data, retorna o termo original
      return term;
    };
    
    const termoFormatado = termoDeBusca ? formatSearchTermForAPI(termoDeBusca) : '';

    const url = termoFormatado 
      ? `http://localhost:8080/api/publicacoes?termo=${encodeURIComponent(termoFormatado)}`
      : 'http://localhost:8080/api/publicacoes';
      
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
      })
      .finally(() => {
        setLoading(false);
      });
  }, [termoDeBusca]);

  const handleSearch = (e) => {
    e.preventDefault();
    const termo = e.target.elements.termo.value;
    setSearchParams(termo ? { termo } : {});
  };

  // VVV--- FUNÇÃO ADICIONADA PARA FORMATAR A DATA ---VVV
  const formatDateForDisplay = (dateString) => {
    // Adiciona T00:00:00 para garantir que a data seja interpretada no fuso horário local,
    // evitando que a data mude para o dia anterior.
    const date = new Date(`${dateString}T00:00:00`);
    return date.toLocaleDateString('pt-BR');
  };
  // ^^^--- FIM DA FUNÇÃO ---^^^


  if (loading) return <p>A carregar publicações...</p>;
  if (error) return <p style={{ color: 'red' }}>Erro: {error}</p>;

  return (
    <div>
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: '1rem', marginBottom: '2rem' }}>
        <input
          type="text"
          name="termo"
          defaultValue={termoDeBusca}
          placeholder="Buscar por título, número ou data (DD/MM/AAAA)..."
          style={{ flexGrow: 1, padding: '0.5rem' }}
        />
        <button type="submit">Buscar</button>
      </form>
      <Link to="/busca-avancada" style={{ marginBottom: '1rem', display: 'inline-block' }}>
        Ir para a Busca Avançada &rarr;
      </Link>
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
                  {/* VVV--- CHAMADA DA FUNÇÃO DE FORMATAÇÃO ---VVV */}
                  <span>{formatDateForDisplay(pub.dataPublicacao)}</span>
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

