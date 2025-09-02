import { useEditor, EditorContent } from '@tiptap/react';
import { useEffect, useState } from 'react'; // <-- 1. Importe o useState
import StarterKit from '@tiptap/starter-kit';
import { MetaMark } from '../tiptap-extensions/metaMark';
import './RichTextEditor.css';

const MenuBar = ({ editor, onVincularClick }) => {
  if (!editor) return null;

  const setAsTitle = () => {
    editor.chain().focus().toggleMeta({ 'data-meta': 'titulo' }).run();
  };

  return (
    <div className="tiptap-menu">
      <button
        type="button"
        onClick={setAsTitle}
        className={editor.isActive('metaMark', { 'data-meta': 'titulo' }) ? 'is-active' : ''}
      >
        Título
      </button>
      
      <button
        type="button"
        onClick={onVincularClick}
        // AGORA A LÓGICA ESTÁ MAIS PRECISA
        disabled={editor.state.selection.empty}
      >
        Vincular 🔗
      </button>

      <div className="separator"></div>
      <button type="button" onClick={() => editor.chain().focus().toggleBold().run()} className={editor.isActive('bold') ? 'is-active' : ''}>Negrito</button>
      <button type="button" onClick={() => editor.chain().focus().toggleItalic().run()} className={editor.isActive('italic') ? 'is-active' : ''}>Itálico</button>
    </div>
  );
};

const RichTextEditor = ({ content, onContentChange, onEditorInstance, onVincularClick }) => {
  
  // 2. ADICIONAMOS UM ESTADO PARA FORÇAR A ATUALIZAÇÃO DA INTERFACE
  const [_, setForceUpdate] = useState(0);

  const editor = useEditor({
    extensions: [StarterKit, MetaMark],
    content: content || '',
    onUpdate: ({ editor }) => {
      onContentChange(editor.getHTML());
    },
    // 3. OUVINTE DE SELEÇÃO: A CADA MUDANÇA NA SELEÇÃO, FORÇAMOS UMA RE-RENDERIZAÇÃO
    onSelectionUpdate: () => {
      setForceUpdate(val => val + 1); // Apenas muda o estado para forçar o refresh
    }
  });
  
  useEffect(() => {
    if (editor && onEditorInstance) {
      onEditorInstance(editor);
    }
  }, [editor, onEditorInstance]);

  return (
    <div className="tiptap-container">
      <MenuBar editor={editor} onVincularClick={onVincularClick} />
      <EditorContent editor={editor} />
    </div>
  );
};

export default RichTextEditor;