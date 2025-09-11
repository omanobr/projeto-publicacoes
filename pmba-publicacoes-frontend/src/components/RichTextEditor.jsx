import { useEditor, EditorContent } from '@tiptap/react';
import { useEffect, useState } from 'react';
import StarterKit from '@tiptap/starter-kit';
import { MetaMark } from '../tiptap-extensions/metaMark';
import './RichTextEditor.css';

// 1. Adicione "onAcrescentarClick" aos parâmetros do MenuBar
const MenuBar = ({ editor, onRevogarClick, onAlterarClick, onAcrescentarClick }) => {
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
      
      <div className="separator"></div>

      <button
        type="button"
        onClick={onRevogarClick}
        disabled={editor.state.selection.empty}
      >
        Revogar Trecho 🚮
      </button>
      <button
        type="button"
        onClick={onAlterarClick}
        disabled={editor.state.selection.empty}
      >
        Alterar Trecho ✍️
      </button>

      {/* VVV--- 2. BOTÃO ADICIONADO AQUI ---VVV */}
      <button
        type="button"
        onClick={onAcrescentarClick}
        disabled={editor.state.selection.empty}
      >
        Acrescentar Trecho ➕
      </button>
      {/* ^^^--- FIM DO BOTÃO ---^^^ */}

      <div className="separator"></div>
      <button type="button" onClick={() => editor.chain().focus().toggleBold().run()} className={editor.isActive('bold') ? 'is-active' : ''}>Negrito</button>
      <button type="button" onClick={() => editor.chain().focus().toggleItalic().run()} className={editor.isActive('italic') ? 'is-active' : ''}>Itálico</button>
    </div>
  );
};

// 3. Adicione "onAcrescentarClick" aos parâmetros do RichTextEditor
const RichTextEditor = ({ content, onContentChange, onEditorInstance, onRevogarClick, onAlterarClick, onAcrescentarClick }) => {
  const [_, setForceUpdate] = useState(0);

  const editor = useEditor({
    extensions: [StarterKit, MetaMark],
    content: content || '',
    onUpdate: ({ editor }) => { onContentChange(editor.getHTML()); },
    onSelectionUpdate: () => { setForceUpdate(val => val + 1); }
  });

  useEffect(() => {
    if (editor && editor.getHTML() !== content) {
      editor.commands.setContent(content, false);
    }
  }, [content, editor]);
  
  useEffect(() => {
    if (editor && onEditorInstance) {
      onEditorInstance(editor);
    }
  }, [editor, onEditorInstance]);

  return (
    <div className="tiptap-container">
      {/* 4. Passe "onAcrescentarClick" para o MenuBar */}
      <MenuBar editor={editor} onRevogarClick={onRevogarClick} onAlterarClick={onAlterarClick} onAcrescentarClick={onAcrescentarClick} />
      <EditorContent editor={editor} />
    </div>
  );
};

export default RichTextEditor;