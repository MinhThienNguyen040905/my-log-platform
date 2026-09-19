'use client';

import React from 'react';
import { Editor } from '@tiptap/react';
import { BubbleMenu } from '@tiptap/react/menus';
import {
  Bold,
  Italic,
  Underline as UnderlineIcon,
  Strikethrough,
  Highlighter,
  Heading2,
  Quote,
} from 'lucide-react';

interface JournalBubbleMenuProps {
  editor: Editor | null;
}

export const JournalBubbleMenu: React.FC<JournalBubbleMenuProps> = ({ editor }) => {
  if (!editor) return null;

  return (
    <BubbleMenu
      editor={editor}
      options={{
        placement: 'top',
        offset: 8,
      }}
      className="flex items-center gap-1 p-1 bg-white border-2 border-black rounded-xl shadow-neo-sm transition-all duration-150 z-40"
    >
      {/* Bold */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleBold().run()}
        className={`p-1.5 rounded-lg border border-transparent transition-all flex items-center justify-center ${
          editor.isActive('bold')
            ? 'bg-primary-container text-black font-bold border-black shadow-neo-xs'
            : 'text-neutral-700 hover:bg-neutral-100 hover:text-black'
        }`}
        title="In đậm (Ctrl+B)"
      >
        <Bold className="w-4 h-4" />
      </button>

      {/* Italic */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleItalic().run()}
        className={`p-1.5 rounded-lg border border-transparent transition-all flex items-center justify-center ${
          editor.isActive('italic')
            ? 'bg-primary-container text-black font-bold border-black shadow-neo-xs'
            : 'text-neutral-700 hover:bg-neutral-100 hover:text-black'
        }`}
        title="In nghiêng (Ctrl+I)"
      >
        <Italic className="w-4 h-4" />
      </button>

      {/* Underline */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleUnderline().run()}
        className={`p-1.5 rounded-lg border border-transparent transition-all flex items-center justify-center ${
          editor.isActive('underline')
            ? 'bg-primary-container text-black font-bold border-black shadow-neo-xs'
            : 'text-neutral-700 hover:bg-neutral-100 hover:text-black'
        }`}
        title="Gạch chân (Ctrl+U)"
      >
        <UnderlineIcon className="w-4 h-4" />
      </button>

      {/* Strikethrough */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleStrike().run()}
        className={`p-1.5 rounded-lg border border-transparent transition-all flex items-center justify-center ${
          editor.isActive('strike')
            ? 'bg-primary-container text-black font-bold border-black shadow-neo-xs'
            : 'text-neutral-700 hover:bg-neutral-100 hover:text-black'
        }`}
        title="Gạch ngang"
      >
        <Strikethrough className="w-4 h-4" />
      </button>

      {/* Divider */}
      <div className="w-[1.5px] h-4 bg-black/20 mx-0.5" />

      {/* Highlight */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleHighlight({ color: '#B7FF32' }).run()}
        className={`p-1.5 rounded-lg border border-transparent transition-all flex items-center justify-center ${
          editor.isActive('highlight')
            ? 'bg-primary-container text-black font-bold border-black shadow-neo-xs'
            : 'text-neutral-700 hover:bg-neutral-100 hover:text-black'
        }`}
        title="Bút dạ quang (Vàng chanh)"
      >
        <Highlighter className="w-4 h-4 text-lime-600" />
      </button>

      {/* Heading 2 */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
        className={`p-1.5 rounded-lg border border-transparent transition-all flex items-center justify-center ${
          editor.isActive('heading', { level: 2 })
            ? 'bg-primary-container text-black font-bold border-black shadow-neo-xs'
            : 'text-neutral-700 hover:bg-neutral-100 hover:text-black'
        }`}
        title="Tiêu đề (H2)"
      >
        <Heading2 className="w-4 h-4" />
      </button>

      {/* Quote */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleBlockquote().run()}
        className={`p-1.5 rounded-lg border border-transparent transition-all flex items-center justify-center ${
          editor.isActive('blockquote')
            ? 'bg-primary-container text-black font-bold border-black shadow-neo-xs'
            : 'text-neutral-700 hover:bg-neutral-100 hover:text-black'
        }`}
        title="Trích dẫn suy ngẫm"
      >
        <Quote className="w-4 h-4" />
      </button>
    </BubbleMenu>
  );
};

