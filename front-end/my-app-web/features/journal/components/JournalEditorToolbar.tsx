'use client';

import React from 'react';
import { Editor } from '@tiptap/react';
import {
  Bold,
  Italic,
  Underline as UnderlineIcon,
  Strikethrough,
  Highlighter,
  Heading2,
  Quote,
  List,
  ListOrdered,
  Image as ImageIcon,
  Undo,
  Redo,
} from 'lucide-react';

interface JournalEditorToolbarProps {
  editor: Editor | null;
  onOpenImageModal: () => void;
}

export const JournalEditorToolbar: React.FC<JournalEditorToolbarProps> = ({
  editor,
  onOpenImageModal,
}) => {
  if (!editor) return null;

  const buttonClass = (isActive: boolean) =>
    `w-7 h-7 sm:w-8 sm:h-8 rounded-lg flex items-center justify-center transition-all cursor-pointer ${
      isActive
        ? 'bg-primary-container text-black border border-black shadow-neo-sm scale-105'
        : 'text-gray-700 hover:text-black hover:bg-black/5'
    }`;

  return (
    <div className="flex flex-wrap items-center gap-1 sm:gap-1.5 p-1.5 bg-white/80 backdrop-blur-xs border-2 border-black rounded-2xl shadow-neo-sm mb-3">
      {/* Text styles */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleBold().run()}
        disabled={!editor.can().chain().focus().toggleBold().run()}
        className={buttonClass(editor.isActive('bold'))}
        title="In đậm (Ctrl+B)"
      >
        <Bold className="w-3.5 h-3.5 sm:w-4 sm:h-4 stroke-[2.5]" />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleItalic().run()}
        disabled={!editor.can().chain().focus().toggleItalic().run()}
        className={buttonClass(editor.isActive('italic'))}
        title="In nghiêng (Ctrl+I)"
      >
        <Italic className="w-3.5 h-3.5 sm:w-4 sm:h-4 stroke-[2.5]" />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleUnderline().run()}
        disabled={!editor.can().chain().focus().toggleUnderline().run()}
        className={buttonClass(editor.isActive('underline'))}
        title="Gạch chân (Ctrl+U)"
      >
        <UnderlineIcon className="w-3.5 h-3.5 sm:w-4 sm:h-4 stroke-[2.5]" />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleStrike().run()}
        disabled={!editor.can().chain().focus().toggleStrike().run()}
        className={buttonClass(editor.isActive('strike'))}
        title="Gạch ngang"
      >
        <Strikethrough className="w-3.5 h-3.5 sm:w-4 sm:h-4 stroke-[2.5]" />
      </button>

      {/* Highlighter marker */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleHighlight({ color: '#B7FF32' }).run()}
        className={buttonClass(editor.isActive('highlight'))}
        title="Bút dạ quang (Highlight)"
      >
        <Highlighter className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-lime-600 stroke-[2.5]" />
      </button>

      <div className="w-px h-5 bg-black/20 mx-0.5" />

      {/* Structure */}
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
        className={buttonClass(editor.isActive('heading', { level: 2 }))}
        title="Tiêu đề mục nhỏ"
      >
        <Heading2 className="w-3.5 h-3.5 sm:w-4 sm:h-4 stroke-[2.5]" />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleBlockquote().run()}
        className={buttonClass(editor.isActive('blockquote'))}
        title="Trích dẫn suy ngẫm"
      >
        <Quote className="w-3.5 h-3.5 sm:w-4 sm:h-4 stroke-[2.5]" />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleBulletList().run()}
        className={buttonClass(editor.isActive('bulletList'))}
        title="Danh sách gạch đầu dòng"
      >
        <List className="w-3.5 h-3.5 sm:w-4 sm:h-4 stroke-[2.5]" />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleOrderedList().run()}
        className={buttonClass(editor.isActive('orderedList'))}
        title="Danh sách đánh số"
      >
        <ListOrdered className="w-3.5 h-3.5 sm:w-4 sm:h-4 stroke-[2.5]" />
      </button>

      <div className="w-px h-5 bg-black/20 mx-0.5" />

      {/* Insert Image */}
      <button
        type="button"
        onClick={onOpenImageModal}
        className="px-2 py-1 rounded-lg border border-black/30 hover:border-black bg-white hover:bg-lime-100 transition-all flex items-center gap-1.5 text-xs font-space font-bold cursor-pointer shadow-neo-sm"
        title="Dán ảnh kỷ niệm vào trang sổ"
      >
        <ImageIcon className="w-3.5 h-3.5 text-purple-600 stroke-[2.5]" />
        <span className="hidden sm:inline text-[11px]">Chèn ảnh</span>
      </button>

      <div className="ml-auto flex items-center gap-1">
        <button
          type="button"
          onClick={() => editor.chain().focus().undo().run()}
          disabled={!editor.can().undo()}
          className="w-7 h-7 rounded-lg flex items-center justify-center text-gray-500 hover:text-black disabled:opacity-30 cursor-pointer"
          title="Hoàn tác (Ctrl+Z)"
        >
          <Undo className="w-3.5 h-3.5" />
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().redo().run()}
          disabled={!editor.can().redo()}
          className="w-7 h-7 rounded-lg flex items-center justify-center text-gray-500 hover:text-black disabled:opacity-30 cursor-pointer"
          title="Làm lại (Ctrl+Y)"
        >
          <Redo className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};

