import { useState, useEffect } from 'react';
import RichTextEditor from '../components/RichTextEditor';

function DebugEditorPage() {
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Busca uma publicação específica e real do seu backend
    fetch(`http://localhost:8080/api/publicacoes/1`)
      .then(res => res.json())
      .then(data => {
        setContent(data.conteudoHtml); // Define o conteúdo com o HTML recebido
        setLoading(false);
      })
      .catch(error => {
        console.error("Falha ao buscar dados de depuração:", error);
        setLoading(false);
      });
  }, []); // Roda apenas uma vez

  if (loading) {
    return <h1>Carregando editor...</h1>;
  }

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Página de Teste do Editor</h1>
      <p>Se o editor aparecer abaixo com conteúdo, o problema está na complexidade da EditPage. Se a página quebrar, o problema é na biblioteca.</p>
      <hr />
      <RichTextEditor
        value={content}
        onChange={setContent}
      />
    </div>
  );
}

export default DebugEditorPage;