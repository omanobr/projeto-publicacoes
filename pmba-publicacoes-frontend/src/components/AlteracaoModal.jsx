import { useState, useEffect } from 'react';
import Modal from 'react-modal';
import PublicacaoSearch from './PublicacaoSearch';

const customStyles = { /* ... cole os estilos do seu RevogacaoModal.jsx ... */ };
Modal.setAppElement('#root');

function AlteracaoModal({ isOpen, onClose, onConfirm }) {
  const [step, setStep] = useState(1); // 1: Buscar Origem, 2: Selecionar Novo Texto
  const [publicacaoOrigem, setPublicacaoOrigem] = useState(null);
  const [conteudoOrigem, setConteudoOrigem] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Busca o conteúdo da publicação de origem quando ela é selecionada
    if (publicacaoOrigem) {
      setLoading(true);
      fetch(`http://localhost:8080/api/publicacoes/${publicacaoOrigem.id}`)
        .then(res => res.json())
        .then(data => {
          setConteudoOrigem(data.conteudoHtml);
          setStep(2); // Avança para o próximo passo
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [publicacaoOrigem]);

  const handleSelectNovoTexto = () => {
    const selection = window.getSelection().toString().trim();
    if (selection) {
      onConfirm(publicacaoOrigem, selection); // Envia a origem e o texto novo
      handleClose();
    } else {
      alert('Por favor, selecione o novo texto.');
    }
  };
  
  const handleClose = () => {
    setStep(1);
    setPublicacaoOrigem(null);
    setConteudoOrigem('');
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
          <p>Agora, selecione o <strong>novo texto</strong> no documento abaixo:</p>
          <div className="modal-content" onMouseUp={handleSelectNovoTexto}>
            <div dangerouslySetInnerHTML={{ __html: conteudoOrigem }} />
          </div>
        </>
      )}
      <button onClick={handleClose} style={{ marginTop: '1rem', backgroundColor: '#6c757d' }}>Cancelar</button>
    </Modal>
  );
}

export default AlteracaoModal;
