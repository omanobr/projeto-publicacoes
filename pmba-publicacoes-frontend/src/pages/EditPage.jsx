import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import RichTextEditor from '../components/RichTextEditor';
import RevogacaoModal from '../components/RevogacaoModal';
import './AdminPage.css';

function EditPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [formData, setFormData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [erro, setErro] = useState(null);
    const [enviando, setEnviando] = useState(false);
    
    const [isRevogacaoParcialModalOpen, setIsRevogacaoParcialModalOpen] = useState(false);
    const [isRevogacaoTotalModalOpen, setIsRevogacaoTotalModalOpen] = useState(false);
    
    const [textoSelecionado, setTextoSelecionado] = useState('');
    const [editorInstance, setEditorInstance] = useState(null);

    const fetchData = useCallback(() => {
        setLoading(true);
        fetch(`http://localhost:8080/api/publicacoes/${id}`)
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

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prevState => ({ ...prevState, [name]: value }));
    };

    const handleContentChange = (newContent) => {
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = newContent;
        const titleElement = tempDiv.querySelector('span[data-meta="titulo"]');
        setFormData(prevState => ({
            ...prevState,
            conteudoHtml: newContent,
            titulo: titleElement ? titleElement.textContent : 'Título não definido',
        }));
    };

    const handleSubmit = (event) => {
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
    };

    const handleEditorInstance = (editor) => { setEditorInstance(editor); };

    const handleRevogarTrecho = () => {
        if (!editorInstance) return;
        const { from, to } = editorInstance.state.selection;
        const selectedText = editorInstance.state.doc.textBetween(from, to);
        if (selectedText) {
            setTextoSelecionado(selectedText);
            setIsRevogacaoParcialModalOpen(true);
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
        };
        fetch(`http://localhost:8080/api/vinculos`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(vinculoData),
        })
        .then(res => {
            if (!res.ok) throw new Error('Falha ao criar o vínculo.');
            return res.json();
        })
        .then(() => {
                alert('Vínculo criado com sucesso!');
                fetchData();
            })
            .catch(err => setErro(err.message));
    };

    const handleConfirmRevogacaoTotal = (publicacaoOrigem) => {
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
    };

    if (loading) return <p>Carregando...</p>;
    if (erro) return <p>Erro: {erro}</p>;

    return (
        <div className="admin-page">
            <Link to="/">&larr; Voltar para a lista</Link>
            <h2>Editar Publicação</h2>
            <form onSubmit={handleSubmit} className="admin-form">
                <div className="form-group">
                    <label>Título (extraído do editor)</label>
                    <input type="text" value={formData.titulo} disabled />
                </div>
                <div className="form-group">
                  <label htmlFor="numero">Número</label>
                  <input type="text" id="numero" name="numero" value={formData.numero} onChange={handleChange} required />
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
                        onVincularClick={handleRevogarTrecho}
                    />
                </div>
                {erro && <p className="error-message">{erro}</p>}
                <div className="form-actions">
                    <button type="submit" disabled={enviando}>
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
            </form>

            <RevogacaoModal
                isOpen={isRevogacaoParcialModalOpen}
                onClose={() => setIsRevogacaoParcialModalOpen(false)}
                onConfirm={handleConfirmRevogacaoParcial}
            />
            <RevogacaoModal
                isOpen={isRevogacaoTotalModalOpen}
                onClose={() => setIsRevogacaoTotalModalOpen(false)}
                onConfirm={handleConfirmRevogacaoTotal}
                tipoFixo="REVOGA"
            />
        </div>
    );
}

// =================================================================
// A LINHA QUE FALTAVA
// =================================================================
export default EditPage;