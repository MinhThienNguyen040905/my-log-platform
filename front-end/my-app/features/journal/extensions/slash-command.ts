import { Extension } from '@tiptap/core';
import Suggestion, { SuggestionOptions } from '@tiptap/suggestion';
import { ReactRenderer } from '@tiptap/react';
import tippy, { Instance as TippyInstance } from 'tippy.js';
import {
  Heading2,
  Heading3,
  Quote,
  List,
  ListOrdered,
  ImagePlus,
  Type,
  Highlighter,
} from 'lucide-react';
import { SlashCommandMenu, CommandItem, SlashCommandMenuRef } from '../components/SlashCommandMenu';

export interface SlashCommandsOptions {
  suggestion: Omit<SuggestionOptions, 'editor'>;
  onOpenImageModal?: () => void;
}

export const getSuggestionItems = (onOpenImageModal?: () => void): CommandItem[] => [
  {
    title: 'Văn bản thường',
    subtitle: 'Đoạn văn tự do, ghi chép nhật ký',
    icon: Type,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).setParagraph().run();
    },
  },
  {
    title: 'Tiêu đề lớn (H2)',
    subtitle: 'Tiêu đề cho sự kiện hoặc chủ đề chính',
    icon: Heading2,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).setHeading({ level: 2 }).run();
    },
  },
  {
    title: 'Tiêu đề nhỏ (H3)',
    subtitle: 'Tiêu đề phân mục con, cảm nghĩ ngắn',
    icon: Heading3,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).setHeading({ level: 3 }).run();
    },
  },
  {
    title: 'Trích dẫn suy ngẫm',
    subtitle: 'Khung trích dẫn cảm xúc hoặc câu nói hay',
    icon: Quote,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).setBlockquote().run();
    },
  },
  {
    title: 'Danh sách gạch đầu dòng',
    subtitle: 'Liệt kê các điều biết ơn, việc đã qua',
    icon: List,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).toggleBulletList().run();
    },
  },
  {
    title: 'Danh sách đánh số',
    subtitle: 'Thứ tự ưu tiên, các bài học hôm nay',
    icon: ListOrdered,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).toggleOrderedList().run();
    },
  },
  {
    title: 'Bút dạ quang',
    subtitle: 'Tô sáng câu chữ bằng màu vàng chanh',
    icon: Highlighter,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).toggleHighlight({ color: '#B7FF32' }).run();
    },
  },
  {
    title: 'Dán ảnh kỷ niệm',
    subtitle: 'Chèn ảnh chụp hoặc ảnh URL vào trang sổ',
    icon: ImagePlus,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).run();
      if (onOpenImageModal) {
        onOpenImageModal();
      }
    },
  },
];

export const createSlashCommandExtension = (onOpenImageModal?: () => void) => {
  return Extension.create({
    name: 'slashCommand',

    addOptions() {
      return {
        suggestion: {
          char: '/',
          command: ({ editor, range, props }: any) => {
            props.command({ editor, range });
          },
        },
      };
    },

    addProseMirrorPlugins() {
      return [
        Suggestion({
          editor: this.editor,
          ...this.options.suggestion,
          items: ({ query }: { query: string }) => {
            const allItems = getSuggestionItems(onOpenImageModal);
            if (!query) return allItems;
            const normalizedQuery = query.toLowerCase().trim();
            return allItems.filter(
              (item) =>
                item.title.toLowerCase().includes(normalizedQuery) ||
                item.subtitle.toLowerCase().includes(normalizedQuery)
            );
          },
          render: () => {
            let component: ReactRenderer<SlashCommandMenuRef>;
            let popup: TippyInstance[];

            return {
              onStart: (props: any) => {
                component = new ReactRenderer(SlashCommandMenu, {
                  props,
                  editor: props.editor,
                });

                if (!props.clientRect) {
                  return;
                }

                popup = tippy('body', {
                  getReferenceClientRect: props.clientRect,
                  appendTo: () => document.body,
                  content: component.element,
                  showOnCreate: true,
                  interactive: true,
                  trigger: 'manual',
                  placement: 'bottom-start',
                });
              },

              onUpdate(props: any) {
                component?.updateProps(props);

                if (!props.clientRect) {
                  return;
                }

                popup?.[0]?.setProps({
                  getReferenceClientRect: props.clientRect,
                });
              },

              onKeyDown(props: any) {
                if (props.event.key === 'Escape') {
                  popup?.[0]?.hide();
                  return true;
                }

                return component?.ref?.onKeyDown(props) ?? false;
              },

              onExit() {
                popup?.[0]?.destroy();
                component?.destroy();
              },
            };
          },
        }),
      ];
    },
  });
};

