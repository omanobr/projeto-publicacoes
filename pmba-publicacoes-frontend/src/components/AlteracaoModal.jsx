import { useState, useEffect } from 'react';
import Modal from 'react-modal';
import PublicacaoSearch from './PublicacaoSearch.jsx';
import '../pages/AdminPage.css';

// Estilos para o modal
const customStyles = {
  content: {
    top: '50%',
    left: '50%',
    right: 'auto',
    bottom: 'auto',
    marginRight: '-50%',
    transform: 'translate(-50%, -50%)',
    width: '80%',
    maxWidth: '700px',
    height: '90vh',
    display: 'flex',
    flexDirection: 'column',
    padding: '0',
  },
};

Modal.setAppElement('#root');

function AlteracaoModal({ isOpen, onClose, onConfirm }) {
  const [step, setStep] = useState(1);
  const [publicacaoOrigem, setPublicacaoOrigem] = useState(null);
  const [conteudoOrigem, setConteudoOrigem] = useState('');
  const [loading, setLoading] = useState(false);
  const [novoTextoSelecionado, setNovoTextoSelecionado] = useState('');

  useEffect(() => {
    if (publicacaoOrigem) {
      setLoading(true);
      fetch(`http://localhost:8080/api/publicacoes/${publicacaoOrigem.id}`)
        .then(res => res.json())
        .then(data => {
          setConteudoOrigem(data.conteudoHtml);
          setStep(2);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [publicacaoOrigem]);
  
  // VVV--- FUNÇÃO DE SELEÇÃO CORRIGIDA ---VVV
  const handleSelectionChange = () => {
    const selection = window.getSelection();
    // Se não houver seleção, limpa o estado
    if (!selection.rangeCount || selection.isCollapsed) {
        setNovoTextoSelecionado('');
        return;
    }

    // Pega o conteúdo de texto puro da seleção, ignorando o HTML
    const plainText = selection.toString().trim();
    setNovoTextoSelecionado(plainText);
  };
  // ^^^--- FIM DA CORREÇÃO ---^^^

  const handleConfirmarClick = () => {
    if (novoTextoSelecionado) {
      onConfirm(publicacaoOrigem, novoTextoSelecionado);
      handleClose();
    } else {
      alert('Por favor, selecione o novo texto.');
    }
  };
  
  const handleClose = () => {
    setStep(1);
    setPublicacaoOrigem(null);
    setConteudoOrigem('');
    setNovoTextoSelecionado('');
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onRequestClose={handleClose} style={customStyles}>
      {step === 1 && (
        <div className="modal-content-full">
          <h2>Alterar Trecho: Passo 1/2</h2>
          <p>Encontre a publicação que contém o novo texto (a publicação de origem).</p>
          <PublicacaoSearch onPublicacaoSelect={setPublicacaoOrigem} />
          {loading && <p>A carregar...</p>}
          <button onClick={handleClose} style={{ marginTop: '1rem', backgroundColor: '#6c757d' }}>Cancelar</button>
        </div>
      )}
      {step === 2 && (
        <div className="modal-layout">
          <div className="modal-header">
            <h2>Alterar Trecho: Passo 2/2</h2>
            <p>Agora, selecione o <strong>novo texto</strong> no documento abaixo. O texto aparecerá na área de revisão para confirmação.</p>
          </div>
          
          <div className="modal-scrollable-content" onMouseUp={handleSelectionChange}>
            <div dangerouslySetInnerHTML={{ __html: conteudoOrigem }} />
          </div>

          <div className="modal-footer">
            <div className="selection-review">
              <h4>Texto Selecionado para Revisão:</h4>
              {/* VVV--- RENDERIZAÇÃO CORRIGIDA PARA TEXTO PURO ---VVV */}
              <div className="selection-box">
                {novoTextoSelecionado ? (
                  <p style={{ margin: 0 }}>{novoTextoSelecionado}</p>
                ) : (
                  <em>Nenhum texto selecionado.</em>
                )}
              </div>
              {/* ^^^--- FIM DA CORREÇÃO ---^^^ */}
            </div>
            <div className="modal-actions">
                <button 
                    onClick={handleConfirmarClick} 
                    disabled={!novoTextoSelecionado}
                >
                    Confirmar Alteração
                </button>
                <button onClick={handleClose} style={{ backgroundColor: '#6c757d' }}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </Modal>
  );
}

export default AlteracaoModal;

