import { Link } from 'react-router-dom';

/**
 * Um painel para exibir e gerir os vínculos de uma publicação.
 * Mostra duas listas: os vínculos que o documento cria e os que ele recebe.
 * Permite a exclusão de qualquer vínculo.
 */
function VinculosPanel({ vinculosGerados = [], vinculosRecebidos = [], onDeleteVinculo }) {
  
  const handleDelete = (vinculoId) => {
    // Pede confirmação ao utilizador antes de uma ação destrutiva
    if (window.confirm('Tem a certeza que deseja excluir este vínculo? Esta ação não pode ser desfeita.')) {
      onDeleteVinculo(vinculoId);
    }
  };

  return (
    <div className="vinculos-panel">
      <h3>Painel de Gestão de Vínculos</h3>
      
      {/* Secção para Vínculos Gerados */}
      <div className="vinculos-section">
        <h4>Vínculos Gerados (Esta publicação altera/revoga):</h4>
        {vinculosGerados.length > 0 ? (
          <ul className="vinculos-list">
            {vinculosGerados.map(vinculo => (
              <li key={vinculo.id}>
                <div className="vinculo-info">
                  <Link to={`/publicacao/${vinculo.publicacaoDestinoId}`}>
                    {vinculo.publicacaoDestinoTitulo}
                  </Link>
                  <span>
                    <strong>Tipo:</strong> {vinculo.tipoVinculo} | <strong>Trecho:</strong> "{vinculo.textoDoTrecho}"
                  </span>
                </div>
                <button 
                  onClick={() => handleDelete(vinculo.id)} 
                  className="delete-vinculo-btn"
                >
                  Excluir
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="no-vinculos">Nenhum vínculo gerado por este documento.</p>
        )}
      </div>

      {/* Secção para Vínculos Recebidos */}
      <div className="vinculos-section">
        <h4>Vínculos Recebidos (Esta publicação é alterada/revogada por):</h4>
        {vinculosRecebidos.length > 0 ? (
          <ul className="vinculos-list">
            {vinculosRecebidos.map(vinculo => (
              <li key={vinculo.id}>
                <div className="vinculo-info">
                  <Link to={`/publicacao/${vinculo.publicacaoOrigemId}`}>
                    {vinculo.publicacaoOrigemTitulo}
                  </Link>
                  <span>
                    <strong>Tipo:</strong> {vinculo.tipoVinculo} | <strong>Trecho afetado:</strong> "{vinculo.textoDoTrecho}"
                  </span>
                </div>
                <button 
                  onClick={() => handleDelete(vinculo.id)} 
                  className="delete-vinculo-btn"
                >
                  Excluir
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="no-vinculos">Nenhum vínculo recebido por este documento.</p>
        )}
      </div>
    </div>
  );
}

export default VinculosPanel;

