import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import RichTextEditor from '../components/RichTextEditor';
import './AdminPage.css';

function AdminPage() {
  const navigate = useNavigate();

  // Estado unificado para o formulário, sem o título separado
  const [formData, setFormData] = useState({
    numero: '',
    tipo: 'OFICIO',
    dataPublicacao: '',
    conteudoHtml: '',
  });

  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);

  // Handler unificado para os inputs
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prevState => ({ ...prevState, [name]: value }));
  };

  // Handler para o editor
  const handleContentChange = (content) => {
    setFormData(prevState => ({ ...prevState, conteudoHtml: content }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setEnviando(true);
    setErro(null);

    // O objeto agora corresponde exatamente ao CriacaoPublicacaoDTO
    const novaPublicacao = {
        numero: formData.numero,
        tipo: formData.tipo,
        dataPublicacao: formData.dataPublicacao,
        conteudoHtml: formData.conteudoHtml,
    };

    fetch('http://localhost:8080/api/publicacoes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(novaPublicacao),
    })
    .then(response => {
        if (!response.ok) throw new Error('Falha ao criar a publicação.');
        // Não precisamos mais do response.json()
        alert('Publicação criada com sucesso!');
        navigate(`/`); // <-- NAVEGA PARA A PÁGINA INICIAL
    })
    .catch(err => setErro(err.message))
    .finally(() => setEnviando(false));
};

  return (
    <div className="admin-page">
      <Link to="/">&larr; Voltar para a lista</Link>
      <h2>Nova Publicação</h2>
      <p>Para definir o título, digite-o no editor abaixo e use o botão "H1" na barra de ferramentas.</p>

      <form onSubmit={handleSubmit} className="admin-form">
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
          />
        </div>
        {erro && <p className="error-message">{erro}</p>}
        <button type="submit" disabled={enviando}>
          {enviando ? 'Enviando...' : 'Salvar Publicação'}
        </button>
      </form>
    </div>
  );
}

export default AdminPage;