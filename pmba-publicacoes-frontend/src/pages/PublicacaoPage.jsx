import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import './PublicacaoPage.css';

function PublicacaoPage() {
  const { id } = useParams();
  const [publicacao, setPublicacao] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { isAdmin } = useAuth();

  useEffect(() => {
    setLoading(true);
    fetch(`http://localhost:8080/api/publicacoes/${id}`)
      .then(response => {
        if (!response.ok) {
          throw new Error("Publicação não encontrada");
        }
        return response.json();
      })
      .then(data => {
        setPublicacao(data);
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  }, [id]);

  const formatDateForDisplay = (dateString) => {
    if (!dateString) return '';
    // Adiciona o fuso horário UTC para evitar problemas de conversão
    const date = new Date(`${dateString}T00:00:00Z`);
    return date.toLocaleDateString('pt-BR', { timeZone: 'UTC' });
  };

  if (loading) return <p>Carregando...</p>;
  if (error) return <p>Erro: {error}</p>;
  if (!publicacao) return <p>Nenhuma publicação encontrada.</p>;

  return (
    <div className="publicacao-detalhe">
      <Link to="/">&larr; Voltar para a lista</Link>

      {isAdmin && (
        <div className="admin-actions">
          <Link to={`/admin/editar/${id}`} className="edit-button">
            Editar Publicação
          </Link>
        </div>
      )}
      
      <h2>
            {publicacao.titulo}
            {publicacao.status === 'REVOGADA' && <span className="status-tag revogado">REVOGADO</span>}
        </h2>
      <p><strong>Número:</strong> {publicacao.numero} | <strong>Data:</strong> {formatDateForDisplay(publicacao.dataPublicacao)}</p>
      <hr />
      <div
        className="conteudo-publicacao"
        dangerouslySetInnerHTML={{ __html: publicacao.conteudoHtml }}
      />
      
      {/* VVV--- LÓGICA DE EXIBIÇÃO DO BGO ---VVV */}
      {publicacao.bgo && (
        <div className="disclaimer-text">
          <p>Este texto não substitui o publicado no BGO nº {publicacao.bgo}</p>
        </div>
      )}
      {/* ^^^--- FIM DA LÓGICA ---^^^ */}

      {publicacao.publicacoesVinculadas && publicacao.publicacoesVinculadas.length > 0 && (
        <div className="vinculos-externos-panel">
          <h3>Publicações Vinculadas</h3>
          <ul>
            {publicacao.publicacoesVinculadas.map(vinculo => (
              <li key={vinculo.id}>
                <Link to={`/publicacao/${vinculo.id}`}>
                  {vinculo.titulo}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export default PublicacaoPage;

