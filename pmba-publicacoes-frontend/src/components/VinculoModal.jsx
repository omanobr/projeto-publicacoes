import { useState, useEffect } from 'react';
import Modal from 'react-modal';

// Estilos customizados para o Modal (podem ser movidos para um CSS depois)
const customStyles = {
  content: {
    top: '50%',
    left: '50%',
    right: 'auto',
    bottom: 'auto',
    marginRight: '-50%',
    transform: 'translate(-50%, -50%)',
    width: '80%',
    maxWidth: '800px',
    maxHeight: '80vh',
    overflowY: 'auto',
  },
};

// Diz ao react-modal qual é o elemento raiz da sua aplicação (importante para acessibilidade)
Modal.setAppElement('#root');

function VinculoModal({ isOpen, onRequestClose, publicacaoDestino, onTrechoSelect }) {
  const [conteudo, setConteudo] = useState('');
  const [loading, setLoading] = useState(true);

  // Busca o conteúdo completo da publicação de destino sempre que o modal for aberto
  useEffect(() => {
    if (isOpen && publicacaoDestino) {
      setLoading(true);
      fetch(`http://localhost:8080/api/publicacoes/${publicacaoDestino.id}`)
        .then(res => res.json())
        .then(data => {
          setConteudo(data.conteudoHtml);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [isOpen, publicacaoDestino]);

  // A MÁGICA DA SELEÇÃO DE TEXTO
  const handleMouseUp = () => {
    const selection = window.getSelection().toString().trim();
    if (selection) {
      onTrechoSelect(selection); // Envia o texto selecionado de volta para a EditPage
      onRequestClose(); // Fecha o modal
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onRequestClose={onRequestClose}
      style={customStyles}
      contentLabel="Selecionar Trecho da Publicação"
    >
      {publicacaoDestino && <h2>Selecione o trecho em: {publicacaoDestino.titulo}</h2>}
      <hr />
      <div className="modal-content" onMouseUp={handleMouseUp}>
        {loading ? (
          <p>Carregando conteúdo...</p>
        ) : (
          // Renderizamos o HTML da publicação de destino aqui
          <div dangerouslySetInnerHTML={{ __html: conteudo }} />
        )}
      </div>
      <button onClick={onRequestClose} style={{ marginTop: '1rem' }}>Cancelar</button>
    </Modal>
  );
}

export default VinculoModal;