// Arquivo: src/components/BatchEditModal.jsx (NOVO)

import { useState, useEffect, useCallback } from 'react';
import Modal from 'react-modal';
import PublicacaoSearch from './PublicacaoSearch.jsx';
import './BatchEditModal.css'; // Criaremos este arquivo a seguir

const customStyles = {
  overlay: {
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    zIndex: 1000 // z-index alto para a sobreposição escura
  },
  content: {
    inset: '5vh',
    padding: '0',
    display: 'flex',
    flexDirection: 'column',
    zIndex: 1001 // z-index ainda mais alto para o conteúdo do modal
  },
};

Modal.setAppElement('#root');

function BatchEditModal({ isOpen, onClose, onSave, publicacaoDestino }) {
  const [step, setStep] = useState(1);
  const [publicacaoOrigem, setPublicacaoOrigem] = useState(null);
  const [conteudoOrigem, setConteudoOrigem] = useState('');
  const [conteudoDestino, setConteudoDestino] = useState('');
  const [loading, setLoading] = useState(false);

  const [selecaoOrigem, setSelecaoOrigem] = useState({ texto: '', html: '' });
  const [selecaoDestino, setSelecaoDestino] = useState({ texto: '', html: '' });

  const [alteracoesPendentes, setAlteracoesPendentes] = useState([]);

  // Carrega os conteúdos dos documentos quando o modal avança para o passo 2
  useEffect(() => {
    if (step === 2 && publicacaoOrigem && publicacaoDestino) {
      setLoading(true);
      const fetchOrigem = fetch(`http://localhost:8080/api/publicacoes/${publicacaoOrigem.id}`).then(res => res.json());
      const fetchDestino = fetch(`http://localhost:8080/api/publicacoes/${publicacaoDestino.id}`).then(res => res.json());

      Promise.all([fetchOrigem, fetchDestino]).then(([dataOrigem, dataDestino]) => {
        setConteudoOrigem(dataOrigem.conteudoHtml);
        setConteudoDestino(dataDestino.conteudoHtml);
        setLoading(false);
      });
    }
  }, [step, publicacaoOrigem, publicacaoDestino]);

  // Funções para capturar a seleção de texto em cada painel
  const handleSelection = (panel, setSelecao) => {
    const selection = window.getSelection();
    if (selection.rangeCount > 0 && !selection.isCollapsed) {
      const range = selection.getRangeAt(0);
      const container = document.createElement('div');
      container.appendChild(range.cloneContents());
      setSelecao({ texto: selection.toString().trim(), html: container.innerHTML });
    } else {
      setSelecao({ texto: '', html: '' });
    }
  };
  
  // Lógica para adicionar uma nova alteração ao "cache"
  const adicionarAlteracao = (tipo) => {
    const novaAlteracao = {
      id: Date.now(), // ID temporário para a lista na UI
      tipoVinculo: tipo,
      publicacaoOrigemId: publicacaoOrigem.id,
      publicacaoDestinoId: publicacaoDestino.id,
      textoDoTrecho: tipo === 'ACRESCENTA' ? selecaoDestino.html : selecaoDestino.texto,
      textoNovo: tipo !== 'REVOGA_PARCIALMENTE' ? selecaoOrigem.html : null,
    };
    setAlteracoesPendentes(prev => [...prev, novaAlteracao]);
    // Limpa as seleções após adicionar
    setSelecaoOrigem({ texto: '', html: '' });
    setSelecaoDestino({ texto: '', html: '' });
  };
  
  const removerAlteracao = (id) => {
      setAlteracoesPendentes(prev => prev.filter(alt => alt.id !== id));
  };
  
  const handleSaveBatch = () => {
      onSave(alteracoesPendentes);
      handleClose();
  };
  
  // Reseta o estado do modal ao fechar
  const handleClose = () => {
    setStep(1);
    setPublicacaoOrigem(null);
    setAlteracoesPendentes([]);
    setSelecaoOrigem({ texto: '', html: '' });
    setSelecaoDestino({ texto: '', html: '' });
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onRequestClose={handleClose} style={customStyles}>
      {step === 1 && (
        <div className="modal-content-full">
          <h2>Alteração em Lote: Passo 1/2</h2>
          <p>Encontre a publicação que contém as alterações (a publicação de origem).</p>
          <PublicacaoSearch onPublicacaoSelect={(pub) => {
            setPublicacaoOrigem(pub);
            setStep(2);
          }} />
          <button onClick={handleClose} style={{ marginTop: '1rem', backgroundColor: '#6c757d' }}>Cancelar</button>
        </div>
      )}

      {step === 2 && (
        <div className="batch-edit-layout">
          <header className="batch-edit-header">
            <h3>Painel de Ações</h3>
            <div className="selection-panels">
                <div className="selection-box-container">
                    <strong>Trecho no Documento de Destino:</strong>
                    <div className="selection-box">{selecaoDestino.texto || "Nenhum trecho selecionado"}</div>
                </div>
                <div className="selection-box-container">
                    <strong>Trecho no Documento de Origem:</strong>
                    <div className="selection-box">{selecaoOrigem.texto || "Nenhum trecho selecionado"}</div>
                </div>
            </div>
            <div className="action-buttons">
                <button onClick={() => adicionarAlteracao('REVOGA_PARCIALMENTE')} disabled={!selecaoDestino.texto}>Revogar</button>
                <button onClick={() => adicionarAlteracao('ALTERA')} disabled={!selecaoDestino.texto || !selecaoOrigem.texto}>Alterar</button>
                <button onClick={() => adicionarAlteracao('ACRESCENTA')} disabled={!selecaoDestino.texto || !selecaoOrigem.texto}>Acrescentar Após</button>
            </div>
          </header>

          <main className="batch-edit-main">
            <div className="document-panel" onMouseUp={() => handleSelection(document.querySelector('.document-panel-destino'), setSelecaoDestino)}>
                <h4>DESTINO (Será Alterado): {publicacaoDestino.titulo}</h4>
                <div className="document-content document-panel-destino" dangerouslySetInnerHTML={{ __html: conteudoDestino }} />
            </div>
            <div className="document-panel" onMouseUp={() => handleSelection(document.querySelector('.document-panel-origem'), setSelecaoOrigem)}>
                <h4>ORIGEM (Contém as Alterações): {publicacaoOrigem.titulo}</h4>
                <div className="document-content document-panel-origem" dangerouslySetInnerHTML={{ __html: conteudoOrigem }} />
            </div>
          </main>
          
          <aside className="batch-edit-aside">
            <h4>Alterações Pendentes ({alteracoesPendentes.length})</h4>
            <ul className="pending-list">
                {alteracoesPendentes.map(alt => (
                    <li key={alt.id}>
                        <span><strong>{alt.tipoVinculo}:</strong> {alt.textoDoTrecho.substring(0, 50)}...</span>
                        <button onClick={() => removerAlteracao(alt.id)}>X</button>
                    </li>
                ))}
            </ul>
          </aside>

          <footer className="batch-edit-footer">
            <button onClick={handleClose} className="cancel-btn">Cancelar</button>
            <button onClick={handleSaveBatch} className="save-btn" disabled={alteracoesPendentes.length === 0}>
                Salvar {alteracoesPendentes.length} Alterações
            </button>
          </footer>
        </div>
      )}
    </Modal>
  );
}

export default BatchEditModal;