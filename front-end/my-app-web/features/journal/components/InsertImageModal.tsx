'use client';

import React, { useState } from 'react';
import { Image as ImageIcon, Upload, X, Loader2 } from 'lucide-react';
import { NeoButton } from '@/components/ui/NeoButton';
import { uploadJournalAsset, type UploadedAsset } from '../api/client';

interface InsertImageModalProps {
  isOpen: boolean;
  onClose: () => void;
  onInsert: (url: string, assetId: string, caption?: string) => void;
}

export const InsertImageModal: React.FC<InsertImageModalProps> = ({
  isOpen,
  onClose,
  onInsert,
}) => {
  const [caption, setCaption] = useState('');
  const [error, setError] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [uploadedAsset, setUploadedAsset] = useState<UploadedAsset | null>(null);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadedAsset) {
      setError('Vui lòng chọn và tải ảnh lên trước khi dán vào bài viết.');
      return;
    }
    onInsert(uploadedAsset.url, uploadedAsset.id, caption.trim());
    setUploadedAsset(null);
    setCaption('');
    setError('');
    onClose();
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setError('Tệp tải lên phải là hình ảnh (JPEG, PNG, WebP).');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError('Kích thước ảnh tối đa là 5MB.');
      return;
    }

    try {
      setIsUploading(true);
      setError('');
      const asset = await uploadJournalAsset(file);
      setUploadedAsset(asset);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Tải ảnh thất bại. Vui lòng thử lại.';
      setError(message);
    } finally {
      setIsUploading(false);
    }
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

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <div>
            <label className="block text-xs font-space font-bold uppercase text-gray-700 mb-1">
              Chọn ảnh từ thiết bị (tối đa 5MB):
            </label>
            <div className="relative">
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                disabled={isUploading}
                onChange={handleFileUpload}
                className="w-full text-xs font-space file:mr-3 file:py-1.5 file:px-3 file:rounded-xl file:border file:border-black file:text-xs file:font-bold file:bg-primary-container file:cursor-pointer cursor-pointer border border-black rounded-xl p-1 bg-white disabled:opacity-50"
              />
              {isUploading && (
                <div className="absolute inset-0 bg-white/80 rounded-xl flex items-center justify-center gap-2 text-xs font-space font-bold">
                  <Loader2 className="w-4 h-4 animate-spin text-black" />
                  <span>Đang tải ảnh an toàn lên đám mây...</span>
                </div>
              )}
            </div>
          </div>

          {/* Preview if uploaded */}
          {uploadedAsset && (
            <div className="relative p-2 bg-white rounded-xl border border-black shadow-neo-sm flex flex-col items-center">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={uploadedAsset.url}
                alt="Preview"
                className="max-h-36 object-contain rounded-lg border border-black/20"
              />
              <span className="text-[10px] text-gray-500 font-space mt-1">
                {uploadedAsset.width}x{uploadedAsset.height} px • {(uploadedAsset.sizeBytes / 1024).toFixed(1)} KB
              </span>
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
              disabled={isUploading}
              className="px-4 py-2 text-xs font-space font-bold text-gray-700 hover:text-black cursor-pointer disabled:opacity-50"
            >
              Hủy
            </button>
            <NeoButton
              type="submit"
              variant="primary"
              size="sm"
              disabled={isUploading || !uploadedAsset}
              className="font-space font-extrabold text-xs shadow-neo-sm disabled:opacity-50"
            >
              <Upload className="w-3.5 h-3.5 mr-1" />
              Dán vào trang viết
            </NeoButton>
          </div>
        </form>
      </div>
    </div>
  );
};
