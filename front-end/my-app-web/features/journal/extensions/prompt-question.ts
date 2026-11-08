import { Node, mergeAttributes } from '@tiptap/core';

/**
 * Câu hỏi gợi ý của trợ lý mà người dùng chọn chèn vào bài. Là một khối riêng (không phải
 * blockquote) để trông như một thẻ gợi ý và không lẫn với trích dẫn người dùng tự gõ. Không có
 * thuộc tính — backend `TipTapContent` chỉ chấp nhận `{ type: "promptQuestion", content: [...] }`.
 * Nhãn "Gợi ý viết" vẽ bằng CSS (`::before`), không nằm trong nội dung lưu.
 */
export const PromptQuestion = Node.create({
  name: 'promptQuestion',
  group: 'block',
  content: 'text*',
  defining: true,

  parseHTML() {
    return [{ tag: 'div[data-prompt-question]' }];
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-prompt-question': '', class: 'prompt-question' }), 0];
  },
});
