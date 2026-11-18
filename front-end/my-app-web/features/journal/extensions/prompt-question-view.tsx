'use client';

import React from 'react';
import { NodeViewContent, NodeViewWrapper, ReactNodeViewRenderer, type NodeViewProps } from '@tiptap/react';
import { X } from 'lucide-react';
import { PromptQuestion } from './prompt-question';

function PromptQuestionView({ node, editor, getPos }: NodeViewProps) {
  const remove = () => {
    const pos = getPos();
    if (typeof pos !== 'number') return;
    editor.chain().focus().deleteRange({ from: pos, to: pos + node.nodeSize }).run();
  };
  return (
    <NodeViewWrapper className="prompt-question group" data-prompt-question="">
      <NodeViewContent as="div" />
      {editor.isEditable && (
        <button
          type="button"
          contentEditable={false}
          onMouseDown={(event) => event.preventDefault()}
          onClick={remove}
          aria-label="Xóa gợi ý viết"
          title="Xóa gợi ý viết khỏi bài"
          className="absolute -top-3 -right-3 flex h-8 w-8 items-center justify-center rounded-full border-2 border-black bg-white shadow-neo-sm cursor-pointer hover:bg-red-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black opacity-100 md:opacity-0 md:group-hover:opacity-100 md:focus-visible:opacity-100"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      )}
    </NodeViewWrapper>
  );
}

/**
 * Thẻ gợi ý trong editor: như `PromptQuestion` nhưng có nút X để xóa cả thẻ. Chỉ dùng trong editor;
 * `generateHTML`/import vẫn dùng `PromptQuestion` gốc nên nội dung lưu không đổi.
 */
export const PromptQuestionWithRemove = PromptQuestion.extend({
  addNodeView() {
    return ReactNodeViewRenderer(PromptQuestionView);
  },
});
