// Arquivo: src/components/AcrescentoModal.jsx (SUBSTITUA TODO O CONTEÚDO)

import { useState, useEffect } from 'react';
import Modal from 'react-modal';
import PublicacaoSearch from './PublicacaoSearch.jsx';
import '../pages/AdminPage.css'; // Reutilizamos os estilos

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

function AcrescentoModal({ isOpen, onClose, onConfirm }) {
  const [step, setStep] = useState(1);
  const [publicacaoOrigem, setPublicacaoOrigem] = useState(null);
  const [conteudoOrigem, setConteudoOrigem] = useState('');
  const [loading, setLoading] = useState(false);
  const [novoTextoSelecionado, setNovoTextoSelecionado] = useState('');

  // Efeito para buscar o conteúdo da publicação de origem quando ela for selecionada
  useEffect(() => {
    if (publicacaoOrigem) {
      setLoading(true);
      fetch(`http://localhost:8080/api/publicacoes/${publicacaoOrigem.id}`)
        .then(res => res.json())
        .then(data => {
          setConteudoOrigem(data.conteudoHtml);
          setStep(2); // Avança para o passo 2
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [publicacaoOrigem]);
  
  // Captura o HTML do texto que o usuário seleciona na tela
  const handleSelectionChange = () => {
    const selection = window.getSelection();
    if (!selection.rangeCount || selection.isCollapsed) {
        setNovoTextoSelecionado('');
        return;
    }

    const range = selection.getRangeAt(0);
    const container = document.createElement('div');
    container.appendChild(range.cloneContents());
    setNovoTextoSelecionado(container.innerHTML);
  };

  // Confirma a operação e envia os dados para a EditPage
  const handleConfirmarClick = () => {
    if (novoTextoSelecionado) {
      onConfirm(publicacaoOrigem, novoTextoSelecionado);
      handleClose();
    } else {
      alert('Por favor, selecione o novo texto a ser acrescentado.');
    }
  };
  
  // Reseta todos os estados ao fechar o modal
  const handleClose = () => {
    setStep(1);
    setPublicacaoOrigem(null);
    setConteudoOrigem('');
    setNovoTextoSelecionado('');
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onRequestClose={handleClose} style={customStyles}>
      {/* PASSO 1: Buscar a publicação de origem */}
      {step === 1 && (
        <div className="modal-content-full">
          <h2>Acrescentar Trecho: Passo 1/2</h2>
          <p>Encontre a publicação que contém o texto a ser acrescentado (a publicação de origem).</p>
          <PublicacaoSearch onPublicacaoSelect={setPublicacaoOrigem} />
          {loading && <p>A carregar...</p>}
          <button onClick={handleClose} style={{ marginTop: '1rem', backgroundColor: '#6c757d' }}>Cancelar</button>
        </div>
      )}

      {/* PASSO 2: Selecionar o texto na publicação de origem */}
      {step === 2 && (
        <div className="modal-layout">
          <div className="modal-header">
            <h2>Acrescentar Trecho: Passo 2/2</h2>
            <p>Agora, selecione o <strong>texto a ser acrescentado</strong> no documento abaixo.</p>
          </div>
          
          <div className="modal-scrollable-content" onMouseUp={handleSelectionChange}>
            <div dangerouslySetInnerHTML={{ __html: conteudoOrigem }} />
          </div>

          <div className="modal-footer">
            <div className="selection-review">
              <h4>Texto Selecionado para Acréscimo:</h4>
              <div 
                className="selection-box"
                dangerouslySetInnerHTML={{ __html: novoTextoSelecionado || '<em>Nenhum texto selecionado.</em>' }}
              />
            </div>
            <div className="modal-actions">
                <button 
                    onClick={handleConfirmarClick} 
                    disabled={!novoTextoSelecionado}
                >
                    Confirmar Acréscimo
                </button>
                <button onClick={handleClose} style={{ backgroundColor: '#6c757d' }}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </Modal>
  );
}

export default AcrescentoModal;