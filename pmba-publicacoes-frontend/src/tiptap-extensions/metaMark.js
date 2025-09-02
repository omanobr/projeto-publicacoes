import { Mark } from '@tiptap/core';

export const MetaMark = Mark.create({
  name: 'metaMark',

  addAttributes() {
    return {
      'data-meta': {
        default: null,
        // Adicionamos 'parseHTML' para garantir que o atributo seja lido corretamente
        parseHTML: element => element.getAttribute('data-meta'),
      },
    };
  },

  parseHTML() {
    return [{ tag: 'span[data-meta]' }];
  },

  renderHTML({ HTMLAttributes }) {
    // O '0' aqui significa "renderize o conteúdo do texto aqui dentro"
    return ['span', HTMLAttributes, 0];
  },

  addCommands() {
    return {
      toggleMeta: (attributes) => ({ commands }) => {
        // Usamos toggleMark para aplicar/remover a marcação com o atributo
        return commands.toggleMark(this.name, attributes);
      },
    };
  },
});