import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import RichTextEditor from '../components/RichTextEditor';
import RevogacaoModal from '../components/RevogacaoModal';
import AlteracaoModal from '../components/AlteracaoModal';
import VinculosPanel from '../components/VinculosPanel';
import ScrollToTopButton from '../components/ScrollToTopButton'; // VVV--- IMPORTAÇÃO ADICIONADA ---VVV
import './AdminPage.css';

function EditPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [formData, setFormData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [erro, setErro] = useState(null);
    const [enviando, setEnviando] = useState(false);
    
    const [activeTab, setActiveTab] = useState('publicacao');
    
    const [isRevogacaoModalOpen, setIsRevogacaoModalOpen] = useState(false);
    const [isAlteracaoModalOpen, setIsAlteracaoModalOpen] = useState(false);
    const [isRevogacaoTotalModalOpen, setIsRevogacaoTotalModalOpen] = useState(false);
    
    const [textoSelecionado, setTextoSelecionado] = useState('');
    const [editorInstance, setEditorInstance] = useState(null);

    const fetchData = useCallback(() => {
        setLoading(true);
        fetch(`http://localhost:8080/api/publicacoes/${id}/for-editing`) 
            .then(res => {
                if (!res.ok) throw new Error(`Publicação não encontrada (Status: ${res.status})`);
                return res.json();
            })
            .then(data => {
                const formattedData = {
                    ...data,
                    dataPublicacao: data.dataPublicacao ? data.dataPublicacao.split('T')[0] : '',
                };
                setFormData(formattedData);
            })
            .catch(err => setErro(err.message))
            .finally(() => setLoading(false));
    }, [id]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handleDeleteVinculo = useCallback((vinculoId) => {
        if (window.confirm('Tem a certeza que deseja excluir este vínculo? Esta ação não pode ser desfeita.')) {
            fetch(`http://localhost:8080/api/vinculos/${vinculoId}`, {
                method: 'DELETE',
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Falha ao excluir o vínculo.');
                }
                alert('Vínculo excluído com sucesso!');
                fetchData();
            })
            .catch(err => {
                alert(err.message);
                setErro(err.message);
            });
        }
    }, [fetchData]);

    const handleChange = useCallback((e) => {
        const { name, value } = e.target;
        setFormData(prevState => ({ ...prevState, [name]: value }));
    }, []);

    const handleContentChange = useCallback((newContent) => {
        if (!formData) return;
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = newContent;
        const titleElement = tempDiv.querySelector('span[data-meta="titulo"]');
        setFormData(prevState => ({
            ...prevState,
            conteudoHtml: newContent,
            titulo: titleElement ? titleElement.textContent : 'Título não definido',
        }));
    }, [formData]);

    const handleSubmit = useCallback((event) => {
        event.preventDefault();
        setEnviando(true);
        setErro(null);
        fetch(`http://localhost:8080/api/publicacoes/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData),
        })
        .then(response => {
            if (!response.ok) throw new Error('Falha ao atualizar a publicação.');
            return response.json();
        })
        .then(data => {
            alert('Publicação atualizada com sucesso!');
            navigate(`/publicacao/${data.id}`);
        })
        .catch(err => setErro(err.message))
        .finally(() => setEnviando(false));
    }, [formData, id, navigate]);

    const handleEditorInstance = useCallback((editor) => {
        setEditorInstance(editor);
    }, []);

    const handleRevogarTrechoClick = () => {
        if (!editorInstance) return;
        const { from, to } = editorInstance.state.selection;
        const selectedText = editorInstance.state.doc.textBetween(from, to);
        if (selectedText) {
            setTextoSelecionado(selectedText);
            setIsRevogacaoModalOpen(true);
        } else {
            alert('Por favor, selecione um trecho de texto no editor primeiro.');
        }
    };

    const handleAlterarTrechoClick = () => {
        if (!editorInstance) return;
        const { from, to } = editorInstance.state.selection;
        const selectedText = editorInstance.state.doc.textBetween(from, to);
        if (selectedText) {
            setTextoSelecionado(selectedText);
            setIsAlteracaoModalOpen(true);
        } else {
            alert('Por favor, selecione um trecho de texto no editor primeiro.');
        }
    };

    const handleConfirmRevogacaoParcial = (publicacaoOrigem, tipoVinculoEscolhido) => {
        const vinculoData = {
            publicacaoOrigemId: publicacaoOrigem.id,
            publicacaoDestinoId: parseInt(id),
            tipoVinculo: tipoVinculoEscolhido,
            textoDoTrecho: textoSelecionado,
            textoNovo: null,
        };
        fetch(`http://localhost:8080/api/vinculos`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(vinculoData),
        })
            .then(() => {
                alert('Vínculo criado com sucesso!');
                fetchData();
            })
            .catch(err => setErro(err.message));
    };

    const handleConfirmAlteracao = (publicacaoOrigem, textoNovo) => {
        const vinculoData = {
            publicacaoOrigemId: publicacaoOrigem.id,
            publicacaoDestinoId: parseInt(id),
            tipoVinculo: 'ALTERA',
            textoDoTrecho: textoSelecionado,
            textoNovo: textoNovo,
        };
        fetch(`http://localhost:8080/api/vinculos`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(vinculoData),
        })
        .then(res => {
            if (!res.ok) throw new Error('Falha ao criar o vínculo de alteração.');
            return res.json();
        })
        .then(() => {
            alert('Alteração vinculada com sucesso!');
            fetchData();
        })
        .catch(err => setErro(err.message));
    };

    const handleConfirmRevogacaoTotal = useCallback((publicacaoOrigem) => {
        const vinculoData = {
            publicacaoOrigemId: publicacaoOrigem.id,
            publicacaoDestinoId: parseInt(id),
        };
        fetch(`http://localhost:8080/api/vinculos/revogar-totalmente`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(vinculoData),
        })
        .then(res => {
            if (!res.ok) throw new Error('Falha ao revogar o documento.');
            return res.json();
        })
        .then(() => {
            alert('Documento revogado com sucesso!');
            navigate(`/publicacao/${id}`);
        })
        .catch(err => setErro(err.message));
    }, [id, navigate]);

    if (loading) return <p>Carregando...</p>;
    if (erro) return <p>Erro: {erro}</p>;
    if (!formData) return <p>Nenhum dado encontrado.</p>;

    return (
        <div className="admin-page">
            <Link to="/">&larr; Voltar para a lista</Link>
            <h2>Editar Publicação</h2>

             <div className="page-actions-top">
                <button type="submit" form="edit-form-id" disabled={enviando}>
                    {enviando ? 'Salvando...' : 'Salvar Alterações'}
                </button>
                <button 
                    type="button" 
                    className="revoke-button" 
                    onClick={() => setIsRevogacaoTotalModalOpen(true)}
                >
                    Revogar Documento...
                </button>
            </div>

            <div className="edit-page-tabs">
                <button 
                    className={`tab-button ${activeTab === 'publicacao' ? 'active' : ''}`}
                    onClick={() => setActiveTab('publicacao')}
                >
                    Publicação
                </button>
                <button 
                    className={`tab-button ${activeTab === 'vinculos' ? 'active' : ''}`}
                    onClick={() => setActiveTab('vinculos')}
                >
                    Gestão de Vínculos
                </button>
            </div>
            
            <form onSubmit={handleSubmit} className="admin-form" id="edit-form-id">
                {activeTab === 'publicacao' && (
                    <>
                        <div className="form-group">
                            <label>Título (extraído do editor)</label>
                            <input type="text" value={formData.titulo} disabled />
                        </div>
                        <div className="form-group">
                          <label htmlFor="numero">Número</label>
                          <input type="text" id="numero" name="numero" value={formData.numero} onChange={handleChange} required />
                        </div>
                        <div className="form-group">
                          <label htmlFor="bgo">BGO (Ex: 232/2025)</label>
                          <input type="text" id="bgo" name="bgo" value={formData.bgo || ''} onChange={handleChange} />
                        </div>
                        <div className="form-group">
                          <label htmlFor="tipo">Tipo</label>
                          <select id="tipo" name="tipo" value={formData.tipo} onChange={handleChange}>
                            <option value="OFICIO">Ofício</option>
                            <option value="PORTARIA">Portaria</option>
                            <option value="DESPACHO">Despacho</option>
                            <option value="BCG">BCG</option>
                          </select>
                        </div>
                        <div className="form-group">
                          <label htmlFor="dataPublicacao">Data de Publicação</label>
                          <input type="date" id="dataPublicacao" name="dataPublicacao" value={formData.dataPublicacao} onChange={handleChange} required />
                        </div>
                        <div className="form-group">
                            <label>Conteúdo</label>
                            <RichTextEditor
                                content={formData.conteudoHtml}
                                onContentChange={handleContentChange}
                                onEditorInstance={handleEditorInstance}
                                onRevogarClick={handleRevogarTrechoClick}
                                onAlterarClick={handleAlterarTrechoClick}
                            />
                        </div>
                        {erro && <p className="error-message">{erro}</p>}
                    </>
                )}
            </form>

            {activeTab === 'vinculos' && (
                 <VinculosPanel 
                    vinculosGerados={formData.vinculosGerados}
                    vinculosRecebidos={formData.vinculosRecebidos}
                    onDeleteVinculo={handleDeleteVinculo}
                />
            )}

            <RevogacaoModal
                isOpen={isRevogacaoModalOpen}
                onClose={() => setIsRevogacaoModalOpen(false)}
                onConfirm={handleConfirmRevogacaoParcial}
            />
            <AlteracaoModal
                isOpen={isAlteracaoModalOpen}
                onClose={() => setIsAlteracaoModalOpen(false)}
                onConfirm={handleConfirmAlteracao}
            />
            <RevogacaoModal
                isOpen={isRevogacaoTotalModalOpen}
                onClose={() => setIsRevogacaoTotalModalOpen(false)}
                onConfirm={handleConfirmRevogacaoTotal}
                tipoFixo="REVOGA"
            />

            {/* VVV--- BOTÃO ADICIONADO AQUI ---VVV */}
            <ScrollToTopButton />
        </div>
    );
}

export default EditPage;

