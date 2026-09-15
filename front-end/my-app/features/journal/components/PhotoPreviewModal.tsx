'use client';

import React from 'react';
import { PolaroidCard } from '@/components/ui/ScrapbookDecorations';

interface PhotoPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  photoUrl: string | null;
  caption?: string;
}

export const PhotoPreviewModal: React.FC<PhotoPreviewModalProps> = ({
  isOpen,
  onClose,
  photoUrl,
  caption,
}) => {
  if (!isOpen || !photoUrl) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in">
      <div className="relative bg-paper-warm border-2 border-black rounded-3xl p-6 shadow-neo-lg max-w-sm flex flex-col items-center gap-4">
        <PolaroidCard
          imageUrl={photoUrl}
          caption={caption || 'Kỷ niệm hôm nay'}
          date={new Date().toLocaleDateString('vi-VN')}
          rotate={-1}
          className="w-64"
        />
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-1.5 bg-black text-white font-space text-xs font-bold rounded-xl shadow-neo-sm hover:bg-neutral-800 cursor-pointer"
        >
          Đóng xem ảnh
        </button>
      </div>
    </div>
  );
};

