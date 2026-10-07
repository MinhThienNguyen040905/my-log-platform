'use client';

import React from 'react';
import { AlertTriangle, PartyPopper, Heart, Info, X } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';

export interface NotificationModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  description: string;
  type?: 'danger' | 'success' | 'info';
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm?: () => void;
  washiColor?: 'lime' | 'peach' | 'lavender' | 'blue';
}

export function NotificationModal({
  isOpen,
  onClose,
  title,
  description,
  type = 'info',
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Hủy bỏ',
  onConfirm,
  washiColor = 'peach',
}: NotificationModalProps) {
  if (!isOpen) return null;

  const icons = {
    danger: <AlertTriangle className='w-6 h-6 text-mood-anxiety-stress stroke-[2.2]' />,
    success: <PartyPopper className='w-6 h-6 text-primary stroke-[2.2]' />,
    info: <Heart className='w-6 h-6 text-secondary stroke-[2.2]' />,
  };

  const handleConfirm = () => {
    if (onConfirm) onConfirm();
    onClose();
  };

  return (
    <div className='fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200'>
      <div
        className='relative w-full max-w-md bg-paper-warm border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg overflow-visible flex flex-col gap-5'
        role='dialog'
        aria-modal='true'
      >
        {/* Scrapbook WashiTape on Top Border */}
        <WashiTape color={washiColor} rotate={-2} className='absolute -top-3 left-10 w-32 z-10' />

        {/* Close button */}
        <button
          type='button'
          onClick={onClose}
          className='absolute top-4 right-4 p-1 rounded-xl hover:bg-black/10 transition-colors cursor-pointer border border-transparent hover:border-black'
          aria-label='Đóng modal'
        >
          <X className='w-5 h-5 text-black' />
        </button>

        {/* Modal Header */}
        <div className='flex items-start gap-3 mt-1'>
          <div className='w-12 h-12 rounded-2xl bg-white border-neo-sm shadow-neo-sm flex items-center justify-center shrink-0'>
            {icons[type]}
          </div>
          <div>
            <h3 className='font-space text-xl font-extrabold text-on-surface leading-snug'>
              {title}
            </h3>
            <p className='font-sans text-xs sm:text-sm text-on-surface-variant mt-1 leading-relaxed'>
              {description}
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className='flex items-center justify-end gap-3 pt-4 border-t border-border-soft'>
          <button
            type='button'
            onClick={onClose}
            className='px-4 py-2 bg-white text-on-surface font-space font-bold text-xs uppercase border-neo-sm rounded-xl shadow-neo-sm hover:bg-gray-100 transition-all cursor-pointer'
          >
            {cancelLabel}
          </button>
          
          <NeoButton
            variant={type === 'danger' ? 'danger' : 'primary'}
            size='sm'
            onClick={handleConfirm}
            className='text-xs font-space font-bold'
          >
            {confirmLabel}
          </NeoButton>
        </div>
      </div>
    </div>
  );
}
