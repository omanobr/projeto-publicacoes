import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import RichTextEditor from '../components/RichTextEditor';
import ScrollToTopButton from '../components/ScrollToTopButton'; // VVV--- IMPORTAÇÃO ADICIONADA ---VVV
import './AdminPage.css';

function AdminPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    numero: '',
    tipo: 'OFICIO',
    dataPublicacao: '',
    conteudoHtml: '',
    bgo: '',
  });
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prevState => ({ ...prevState, [name]: value }));
  };

  const handleContentChange = (content) => {
    setFormData(prevState => ({ ...prevState, conteudoHtml: content }));
  };
  
  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    setEnviando(true); 

    fetch('http://localhost:8080/api/publicacoes/upload-extract-text', {
        method: 'POST',
        body: formData,
    })
    .then(response => {
        if (!response.ok) throw new Error('Falha ao extrair texto do arquivo.');
        return response.text();
    })
    .then(htmlContent => {
        handleContentChange(htmlContent); 
    })
    .catch(err => setErro(err.message))
    .finally(() => setEnviando(false));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setEnviando(true);
    setErro(null);
  
    const novaPublicacao = {
        numero: formData.numero,
        tipo: formData.tipo,
        dataPublicacao: formData.dataPublicacao,
        conteudoHtml: formData.conteudoHtml,
        bgo: formData.bgo,
    };

    fetch('http://localhost:8080/api/publicacoes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(novaPublicacao),
    })
    .then(response => {
        if (!response.ok) throw new Error('Falha ao criar a publicação.');
        alert('Publicação criada com sucesso!');
        navigate(`/`);
    })
    .catch(err => setErro(err.message))
    .finally(() => setEnviando(false));
  };

  return (
    <div className="admin-page">
      <Link to="/">&larr; Voltar para a lista</Link>
      <h2>Nova Publicação</h2>

      <div className="page-actions-top">
        <button type="submit" form="admin-form-id" disabled={enviando}>
          {enviando ? 'Enviando...' : 'Salvar Publicação'}
        </button>
      </div>
      
      <p>Para definir o título, digite-o no editor abaixo e use o botão "Título".</p>
      
      <form onSubmit={handleSubmit} className="admin-form" id="admin-form-id">
        <div className="form-group">
          <label htmlFor="file-upload">Importe o texto de um arquivo (PDF, DOCX)</label>
          <input type="file" id="file-upload" onChange={handleFileChange} accept=".pdf,.docx" />
        </div>
        <div className="form-group">
          <label htmlFor="numero">Número</label>
          <input type="text" id="numero" name="numero" value={formData.numero} onChange={handleChange} required />
        </div>
        <div className="form-group">
          <label htmlFor="bgo">BGO (Ex: 232/2025)</label>
          <input type="text" id="bgo" name="bgo" value={formData.bgo} onChange={handleChange} />
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
          />
        </div>
        {erro && <p className="error-message">{erro}</p>}
      </form>
      
      {/* VVV--- BOTÃO ADICIONADO AQUI ---VVV */}
      <ScrollToTopButton />
    </div>
  );
}

export default AdminPage;

