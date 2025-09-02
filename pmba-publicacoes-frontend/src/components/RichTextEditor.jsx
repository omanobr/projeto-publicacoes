import { useEditor, EditorContent } from '@tiptap/react';
import { useEffect, useState } from 'react';
import StarterKit from '@tiptap/starter-kit';
import { MetaMark } from '../tiptap-extensions/metaMark';
import './RichTextEditor.css';

const MenuBar = ({ editor, onRevogarClick, onAlterarClick }) => {
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

      <div className="separator"></div>
      <button type="button" onClick={() => editor.chain().focus().toggleBold().run()} className={editor.isActive('bold') ? 'is-active' : ''}>Negrito</button>
      <button type="button" onClick={() => editor.chain().focus().toggleItalic().run()} className={editor.isActive('italic') ? 'is-active' : ''}>Itálico</button>
    </div>
  );
};

const RichTextEditor = ({ content, onContentChange, onEditorInstance, onRevogarClick, onAlterarClick }) => {
  const [_, setForceUpdate] = useState(0);

  const editor = useEditor({
    extensions: [StarterKit, MetaMark],
    content: content || '',
    onUpdate: ({ editor }) => { onContentChange(editor.getHTML()); },
    onSelectionUpdate: () => { setForceUpdate(val => val + 1); }
  });
  
  useEffect(() => {
    if (editor && onEditorInstance) {
      onEditorInstance(editor);
    }
  }, [editor, onEditorInstance]);

  return (
    <div className="tiptap-container">
      <MenuBar editor={editor} onRevogarClick={onRevogarClick} onAlterarClick={onAlterarClick} />
      <EditorContent editor={editor} />
    </div>
  );
};

export default RichTextEditor;
