import { useState, useEffect } from 'react';
import Modal from 'react-modal';
import PublicacaoSearch from './PublicacaoSearch';

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
    maxHeight: '80vh',
    overflowY: 'auto',
    border: '1px solid #ccc',
    borderRadius: '8px',
    padding: '2rem',
  },
};

Modal.setAppElement('#root');

function AlteracaoModal({ isOpen, onClose, onConfirm }) {
  const [step, setStep] = useState(1); // 1: Buscar Origem, 2: Selecionar Novo Texto
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
  
  // VVV--- ESTA FUNÇÃO FOI TOTALMENTE ALTERADA ---VVV
  const handleSelectionChange = () => {
    const selection = window.getSelection();
    if (!selection.rangeCount || selection.isCollapsed) {
        setNovoTextoSelecionado('');
        return;
    }

    const range = selection.getRangeAt(0);
    const container = document.createElement('div');
    container.appendChild(range.cloneContents());

    // Agora, em vez de texto puro, guardamos o HTML selecionado.
    setNovoTextoSelecionado(container.innerHTML);
  };

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
        <>
          <h2>Alterar Trecho: Passo 1/2</h2>
          <p>Encontre a publicação que contém o novo texto (a publicação de origem).</p>
          <PublicacaoSearch onPublicacaoSelect={setPublicacaoOrigem} />
          {loading && <p>A carregar...</p>}
        </>
      )}
      {step === 2 && (
        <>
          <h2>Alterar Trecho: Passo 2/2</h2>
          <p>Agora, selecione o <strong>novo texto</strong> no documento abaixo. O texto aparecerá na área de revisão para confirmação.</p>
          
          <div className="modal-content" onMouseUp={handleSelectionChange}>
            <div dangerouslySetInnerHTML={{ __html: conteudoOrigem }} />
          </div>

          <div className="selection-review">
            <h4>Texto Selecionado para Revisão:</h4>
            {/* VVV--- A ÁREA DE REVISÃO AGORA RENDERIZA O HTML ---VVV */}
            <div 
              className="selection-box"
              dangerouslySetInnerHTML={{ __html: novoTextoSelecionado || '<em>Nenhum texto selecionado.</em>' }}
            />
          </div>
          <button 
            onClick={handleConfirmarClick} 
            disabled={!novoTextoSelecionado}
            style={{ marginTop: '1rem' }}
          >
            Confirmar Alteração
          </button>
        </>
      )}
      <button onClick={handleClose} style={{ marginTop: '1rem', marginLeft: '0.5rem', backgroundColor: '#6c757d' }}>Cancelar</button>
    </Modal>
  );
}

export default AlteracaoModal;