'use client';

import React from 'react';
import { NodeViewWrapper, ReactNodeViewRenderer, type NodeViewProps } from '@tiptap/react';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';
import { X } from 'lucide-react';
import { JournalImageBase } from './journal-image-base';

/**
 * Transaction meta that allows removing images: set by the image's X button and by code that replaces the
 * editor content on purpose (loading an entry). Any other change may not remove an image.
 */
export const IMAGE_DELETE_META = 'journalImageDelete';

const IMAGE_CLASS =
  'max-h-96 w-auto mx-auto rounded-2xl border-2 border-black shadow-neo-sm p-1.5 bg-white object-contain';

function countImages(doc: ProseMirrorNode): number {
  let count = 0;
  doc.descendants((node) => {
    if (node.type.name === 'image') count++;
  });
  return count;
}

function JournalImageView({ node, editor, getPos, selected }: NodeViewProps) {
  const remove = () => {
    const pos = getPos();
    if (typeof pos !== 'number') return;
    editor.chain().focus().command(({ tr }) => {
      tr.setMeta(IMAGE_DELETE_META, true).delete(pos, pos + node.nodeSize);
      return true;
    }).run();
  };
  const alt = typeof node.attrs.alt === 'string' && node.attrs.alt ? node.attrs.alt : 'Ảnh trong bài viết';
  return (
    <NodeViewWrapper as="figure" className="my-4 flex justify-center" data-drag-handle="">
      {/* The frame shrinks to the image so the X button always sits on the image's own corner. */}
      <span className="group relative inline-block max-w-full">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={node.attrs.src}
          alt={alt}
          title={node.attrs.title ?? undefined}
          data-asset-id={node.attrs['data-asset-id'] ?? undefined}
          className={`${IMAGE_CLASS} ${selected ? 'ring-4 ring-primary-container' : ''}`}
        />
        {editor.isEditable && (
          <button
            type="button"
            contentEditable={false}
            onMouseDown={(event) => event.preventDefault()}
            onClick={remove}
            aria-label="Xóa ảnh"
            title="Xóa ảnh khỏi bài viết"
            className="absolute -top-3 -right-3 flex h-8 w-8 items-center justify-center rounded-full border-2 border-black bg-white shadow-neo-sm cursor-pointer hover:bg-red-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black opacity-100 md:opacity-0 md:group-hover:opacity-100 md:focus-visible:opacity-100"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </button>
        )}
      </span>
    </NodeViewWrapper>
  );
}

/**
 * Journal image: keeps the uploaded asset ID, shows an X button to remove the image, and refuses any other
 * edit that would drop an image (Backspace/Delete, typing over or cutting a selection that contains one).
 */
export const JournalImage = JournalImageBase.extend({
  addNodeView() {
    return ReactNodeViewRenderer(JournalImageView);
  },

  addProseMirrorPlugins() {
    return [
      ...(this.parent?.() ?? []),
      new Plugin({
        key: new PluginKey('journalImageGuard'),
        filterTransaction: (tr, state) => {
          // Select-all + Backspace also replaces the whole document, so it is blocked like any key deletion.
          if (!tr.docChanged || tr.getMeta(IMAGE_DELETE_META)) return true;
          return countImages(tr.doc) >= countImages(state.doc);
        },
      }),
    ];
  },
});
