'use client';

import React, { useState, useEffect, useRef } from 'react';
import { Tag, X, Plus } from 'lucide-react';
import { NeoButton } from '@/components/ui/NeoButton';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';

interface AddTopicModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAddTopic: (topic: string) => void;
  existingTopics: string[];
}

export const AddTopicModal: React.FC<AddTopicModalProps> = ({
  isOpen,
  onClose,
  onAddTopic,
  existingTopics,
}) => {
  const [inputValue, setInputValue] = useState('');
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      const timer = setTimeout(() => {
        setInputValue('');
        setError(null);
        inputRef.current?.focus();
      }, 50);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const cleanTopic = inputValue.trim().replace(/^#+/, '').trim();

    if (!cleanTopic) {
      setError('Vui lòng nhập tag.');
      return;
    }

    if (existingTopics.some((t) => t.toLowerCase() === cleanTopic.toLowerCase())) {
      setError('Tag này đã có trong trang viết của bạn rồi.');
      return;
    }

    onAddTopic(cleanTopic);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-in fade-in">
      <div className="relative bg-paper-warm border-2.5 border-black rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-neo-lg flex flex-col gap-4">
        <WashiTape color="lime" rotate={-2} className="w-24 absolute -top-3 left-6" />

        {/* Header */}
        <div className="flex items-center justify-between pt-1">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-primary-container border border-black flex items-center justify-center shadow-neo-xs text-black">
              <Tag className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-space text-base sm:text-lg font-extrabold text-on-surface leading-tight">
                Thêm tag
              </h3>
              <p className="text-xs text-neutral-600 font-sans">
                Gắn tag để phân loại dòng suy nghĩ
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-xl bg-white border border-black flex items-center justify-center text-neutral-600 hover:text-black hover:bg-neutral-100 transition-all cursor-pointer shadow-neo-xs"
            title="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Input Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <div className="relative">
            <span className="absolute left-3.5 top-1/2 -translate-y-1/2 font-space font-bold text-neutral-400 text-sm">
              #
            </span>
            <input
              ref={inputRef}
              type="text"
              value={inputValue}
              onChange={(e) => {
                setInputValue(e.target.value);
                if (error) setError(null);
              }}
              placeholder="Nhập tag (vd: đồ án tốt nghiệp, hồ Tây...)"
              className="w-full pl-8 pr-3.5 py-2.5 bg-white border-2 border-black rounded-xl font-space text-sm text-black placeholder:text-neutral-400 focus:outline-none focus:shadow-neo-sm transition-all"
            />
          </div>

          {error && (
            <p className="text-xs text-red-600 font-space font-semibold animate-in fade-in">
              ⚠️ {error}
            </p>
          )}

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-black/15 mt-1">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-space font-bold text-neutral-600 hover:text-black hover:bg-black/5 rounded-xl transition-all cursor-pointer"
            >
              Hủy
            </button>
            <NeoButton
              type="submit"
              variant="primary"
              size="sm"
              className="font-bold text-xs shadow-neo-sm"
              icon={<Plus className="w-3.5 h-3.5" />}
            >
              Thêm tag
            </NeoButton>
          </div>
        </form>
      </div>
    </div>
  );
};

