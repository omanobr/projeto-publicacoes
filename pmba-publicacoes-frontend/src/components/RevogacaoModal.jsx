import Modal from 'react-modal';
import { useState } from 'react';
import PublicacaoSearch from './PublicacaoSearch';

// Estilos para o modal
const customStyles = {
  overlay: {
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    zIndex: 1000
  },
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
    zIndex: 1001
  },
};

Modal.setAppElement('#root');

function RevogacaoModal({ isOpen, onClose, onConfirm, tipoFixo }) {
  const [publicacaoOrigem, setPublicacaoOrigem] = useState(null);
  // Se um tipoFixo for passado, ele é o padrão. Senão, 'REVOGA_PARCIALMENTE'
  const [tipoVinculo, setTipoVinculo] = useState(tipoFixo || 'REVOGA_PARCIALMENTE');

  const handleConfirm = () => {
    if (publicacaoOrigem) {
      onConfirm(publicacaoOrigem, tipoVinculo);
      onClose();
    } else {
      alert('Por favor, selecione uma publicação de origem.');
    }
  };

  // Limpa a seleção ao fechar
  const handleClose = () => {
    setPublicacaoOrigem(null);
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onRequestClose={handleClose} style={customStyles} contentLabel="Vincular Revogação">
      <h2>Vincular Revogação/Alteração</h2>
      <p>Selecione a publicação que está revogando/alterando o trecho selecionado no editor.</p>

      <div className="form-group">
        <label>1. Encontre a publicação de ORIGEM (a que revoga)</label>
        <PublicacaoSearch onPublicacaoSelect={setPublicacaoOrigem} />
        {publicacaoOrigem && (
          <p className="selection-feedback">Selecionado: <strong>{publicacaoOrigem.titulo}</strong></p>
        )}
      </div>

      {/* VVV--- LÓGICA CONDICIONAL AQUI ---VVV */}
      {/* O seletor só aparece se um 'tipoFixo' NÃO for definido */}
      {!tipoFixo && (
        <div className="form-group">
          <label htmlFor="modalTipoVinculo">2. Escolha o tipo de vínculo</label>
          <select id="modalTipoVinculo" value={tipoVinculo} onChange={e => setTipoVinculo(e.target.value)}>
            <option value="ALTERA">Altera</option>
            <option value="REVOGA_PARCIALMENTE">Revoga Parcialmente</option>
            <option value="REVOGA">Revoga</option>
            <option value="CORRELACIONA">Correlaciona</option>
          </select>
        </div>
      )}

      <div style={{ marginTop: '1.5rem', display: 'flex', gap: '0.5rem' }}>
        <button onClick={handleConfirm} disabled={!publicacaoOrigem}>Confirmar Vínculo</button>
        <button onClick={handleClose} style={{ backgroundColor: '#6c757d' }}>Cancelar</button>
      </div>
    </Modal>
  );
}

export default RevogacaoModal;