import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './PublicacaoPage.css';

function PublicacaoPage() {
  // O hook 'useParams' pega os parâmetros da URL, no nosso caso o ':id'.
  const { id } = useParams();
  const [publicacao, setPublicacao] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { isAdmin } = useAuth();

  useEffect(() => {
    // Buscamos os dados da publicação específica usando o ID da URL.
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
  }, [id]); // O [id] garante que a busca acontece novamente se o ID na URL mudar.

  if (loading) return <p>Carregando...</p>;
  if (error) return <p>Erro: {error}</p>;

  return (
    <div className="publicacao-detalhe">
      <Link to="/">&larr; Voltar para a lista</Link>

      {/* VVV--- ADICIONE ESTE NOVO BLOCO ---VVV */}
      {isAdmin && (
        <div className="admin-actions">
          <Link to={`/admin/editar/${id}`} className="edit-button">
            Editar Publicação
          </Link>
        </div>
      )}
      {/* ^^^--- FIM DO NOVO BLOCO ---^^^ */}
      <h2>
            {publicacao.titulo}
            {/* Se o status for REVOGADA, mostra a tag */}
            {publicacao.status === 'REVOGADA' && <span className="status-tag revogado">REVOGADO</span>}
        </h2>
      <p><strong>Número:</strong> {publicacao.numero} | <strong>Data:</strong> {publicacao.dataPublicacao}</p>
      <hr />
      <div
        className="conteudo-publicacao"
        dangerouslySetInnerHTML={{ __html: publicacao.conteudoHtml }}
      />
      {publicacao.bgo && (
        <div className="disclaimer-text">
          <p>Este texto não substitui o publicado no BGO nº {publicacao.bgo}</p>
        </div>
      )}
    </div>
  );
}

export default PublicacaoPage;