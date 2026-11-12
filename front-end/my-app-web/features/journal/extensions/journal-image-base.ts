import Image from '@tiptap/extension-image';

/**
 * Image node with the uploaded asset ID (`data-asset-id`), which the backend needs to keep the image on save.
 * Shared by the editor (`JournalImage`, adds the X button and the delete guard) and by `generateHTML` when an
 * entry is turned into HTML for viewing or editing, so reopening an entry keeps its image IDs.
 */
export const JournalImageBase = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      'data-asset-id': {
        default: null,
        parseHTML: (element) => element.getAttribute('data-asset-id'),
        renderHTML: (attributes) => {
          if (!attributes['data-asset-id']) return {};
          return { 'data-asset-id': attributes['data-asset-id'] };
        },
      },
    };
  },
});
