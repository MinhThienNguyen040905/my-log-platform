'use client';

import React, { useState } from 'react';
import { Image as ImageIcon, Upload, X, Link as LinkIcon } from 'lucide-react';
import { NeoButton } from '@/components/ui/NeoButton';

interface InsertImageModalProps {
  isOpen: boolean;
  onClose: () => void;
  onInsert: (url: string, caption?: string) => void;
}

export const InsertImageModal: React.FC<InsertImageModalProps> = ({
  isOpen,
  onClose,
  onInsert,
}) => {
  const [activeTab, setActiveTab] = useState<'url' | 'upload'>('url');
  const [url, setUrl] = useState('');
  const [caption, setCaption] = useState('');
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!url.trim()) {
      setError('Vui lòng nhập đường dẫn ảnh hợp lệ.');
      return;
    }
    onInsert(url.trim(), caption.trim());
    setUrl('');
    setCaption('');
    setError('');
    onClose();
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setError('Tệp tải lên phải là định dạng hình ảnh.');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError('Kích thước ảnh tối đa là 5MB.');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setUrl(reader.result);
        setError('');
      }
    };
    reader.readAsDataURL(file);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in">
      <div className="relative bg-paper-warm border-2 border-black rounded-3xl p-6 shadow-neo-lg max-w-md w-full flex flex-col gap-4">
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-black pb-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-primary-container border border-black flex items-center justify-center shadow-neo-sm">
              <ImageIcon className="w-4 h-4 text-black" />
            </div>
            <h3 className="font-space font-extrabold text-base text-on-surface">
              Dán ảnh kỷ niệm vào sổ
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-7 h-7 rounded-lg bg-white border border-black flex items-center justify-center hover:bg-red-100 transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Tab switch */}
        <div className="flex gap-2 bg-white/60 p-1 rounded-xl border border-black/30 text-xs font-space font-bold">
          <button
            type="button"
            onClick={() => {
              setActiveTab('url');
              setError('');
            }}
            className={`flex-1 py-1.5 rounded-lg flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
              activeTab === 'url'
                ? 'bg-primary-container border border-black shadow-neo-sm text-black'
                : 'text-gray-600 hover:text-black'
            }`}
          >
            <LinkIcon className="w-3.5 h-3.5" />
            <span>Liên kết ảnh</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveTab('upload');
              setError('');
            }}
            className={`flex-1 py-1.5 rounded-lg flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
              activeTab === 'upload'
                ? 'bg-primary-container border border-black shadow-neo-sm text-black'
                : 'text-gray-600 hover:text-black'
            }`}
          >
            <Upload className="w-3.5 h-3.5" />
            <span>Tải ảnh từ máy</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          {activeTab === 'url' ? (
            <div>
              <label className="block text-xs font-space font-bold uppercase text-gray-700 mb-1">
                URL Hình ảnh:
              </label>
              <input
                type="url"
                value={url}
                onChange={(e) => {
                  setUrl(e.target.value);
                  setError('');
                }}
                placeholder="https://images.unsplash.com/..."
                className="w-full px-3 py-2 bg-white rounded-xl border border-black font-sans text-xs focus:outline-none shadow-neo-sm"
              />
            </div>
          ) : (
            <div>
              <label className="block text-xs font-space font-bold uppercase text-gray-700 mb-1">
                Chọn ảnh từ thiết bị:
              </label>
              <input
                type="file"
                accept="image/*"
                onChange={handleFileUpload}
                className="w-full text-xs font-space file:mr-3 file:py-1.5 file:px-3 file:rounded-xl file:border file:border-black file:text-xs file:font-bold file:bg-primary-container file:cursor-pointer cursor-pointer border border-black rounded-xl p-1 bg-white"
              />
            </div>
          )}

          {/* Preview if url exists */}
          {url && (
            <div className="relative p-2 bg-white rounded-xl border border-black shadow-neo-sm flex flex-col items-center">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={url}
                alt="Preview"
                className="max-h-36 object-contain rounded-lg border border-black/20"
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-space font-bold uppercase text-gray-700 mb-1">
              Ghi chú ảnh (tùy chọn):
            </label>
            <input
              type="text"
              value={caption}
              onChange={(e) => setCaption(e.target.value)}
              placeholder="Vd: Chiều bình yên bên hồ Tây..."
              className="w-full px-3 py-2 bg-white rounded-xl border border-black font-sans text-xs focus:outline-none shadow-neo-sm"
            />
          </div>

          {error && <p className="text-xs font-bold text-red-600 font-space">{error}</p>}

          <div className="flex justify-end gap-2 pt-2 border-t border-black/10">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-space font-bold text-gray-700 hover:text-black cursor-pointer"
            >
              Hủy
            </button>
            <NeoButton
              type="submit"
              variant="primary"
              size="sm"
              className="font-space font-extrabold text-xs shadow-neo-sm"
            >
              Dán vào trang viết
            </NeoButton>
          </div>
        </form>
      </div>
    </div>
  );
};

