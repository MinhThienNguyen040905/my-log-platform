'use client';

import React, { useEffect, useImperativeHandle, forwardRef, useMemo } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Placeholder from '@tiptap/extension-placeholder';
import Highlight from '@tiptap/extension-highlight';
import Underline from '@tiptap/extension-underline';
import Image from '@tiptap/extension-image';
import { JournalBubbleMenu } from './JournalBubbleMenu';
import { createSlashCommandExtension } from '../extensions/slash-command';

export interface JournalTipTapEditorRef {
  insertContent: (content: string) => void;
  insertImage: (url: string, alt?: string) => void;
  focus: () => void;
  getText: () => string;
  getHTML: () => string;
}

interface JournalTipTapEditorProps {
  initialContent: string;
  onChange: (html: string, text: string) => void;
  onOpenImageModal: () => void;
  editorRef?: React.Ref<JournalTipTapEditorRef>;
}

export const JournalTipTapEditor = forwardRef<JournalTipTapEditorRef, JournalTipTapEditorProps>(
  ({ initialContent, onChange, onOpenImageModal }, ref) => {
    // Memoize custom slash command extension with dynamic onOpenImageModal trigger
    const slashCommandExtension = useMemo(() => {
      return createSlashCommandExtension(onOpenImageModal);
    }, [onOpenImageModal]);

    const editor = useEditor({
      extensions: [
        StarterKit.configure({
          heading: {
            levels: [2, 3],
          },
          blockquote: {
            HTMLAttributes: {
              class: 'border-l-4 border-black pl-4 py-1.5 my-3 italic bg-black/5 rounded-r-xl font-serif text-on-surface',
            },
          },
          bulletList: {
            HTMLAttributes: {
              class: 'list-disc list-outside ml-6 my-2 space-y-1',
            },
          },
          orderedList: {
            HTMLAttributes: {
              class: 'list-decimal list-outside ml-6 my-2 space-y-1',
            },
          },
        }),
        Placeholder.configure({
          placeholder:
            "Hôm nay điều gì làm bạn bận tâm hay mỉm cười? Gõ '/' để chọn kiểu khối hoặc chèn ảnh...",
          emptyEditorClass: 'is-editor-empty',
        }),
        Highlight.configure({
          multicolor: true,
          HTMLAttributes: {
            class: 'bg-primary-container text-black px-1 rounded font-semibold',
          },
        }),
        Underline,
        Image.configure({
          inline: false,
          allowBase64: true,
          HTMLAttributes: {
            class:
              'max-h-96 w-auto mx-auto rounded-2xl border-2 border-black shadow-neo-sm p-1.5 bg-white my-4 object-contain transition-transform hover:scale-[1.01]',
          },
        }),
        slashCommandExtension,
      ],
      content: initialContent || '',
      immediatelyRender: false,
      editorProps: {
        attributes: {
          class:
            'w-full min-h-[380px] bg-transparent font-serif text-base sm:text-lg text-on-surface focus:outline-none leading-relaxed tracking-wide p-1 cursor-text select-text',
        },
      },
      onUpdate: ({ editor: ed }) => {
        onChange(ed.getHTML(), ed.getText());
      },
    });

    // Synchronize initialContent when loaded asynchronously (e.g. from localStorage/editId)
    useEffect(() => {
      if (editor && initialContent && editor.isEmpty) {
        editor.commands.setContent(initialContent);
      }
    }, [editor, initialContent]);

    useImperativeHandle(ref, () => ({
      insertContent: (content: string) => {
        if (!editor) return;
        editor.chain().focus().insertContent(content).run();
      },
      insertImage: (url: string, alt?: string) => {
        if (!editor) return;
        editor
          .chain()
          .focus()
          .setImage({ src: url, alt: alt || 'Kỷ niệm nhật ký' })
          .run();
      },
      focus: () => {
        editor?.commands.focus();
      },
      getText: () => editor?.getText() || '',
      getHTML: () => editor?.getHTML() || '',
    }));

    return (
      <div className="relative flex flex-col">
        {/* Floating Bubble Menu when text is selected (bôi đen chữ) */}
        <JournalBubbleMenu editor={editor} />

        {/* Editor Writing Area */}
        <div className="relative">
          <EditorContent editor={editor} />
        </div>
      </div>
    );
  }
);

JournalTipTapEditor.displayName = 'JournalTipTapEditor';
