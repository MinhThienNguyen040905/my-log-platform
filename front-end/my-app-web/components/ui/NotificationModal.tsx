'use client';

import React, { useEffect, useRef, useState } from 'react';
import { AlertTriangle, PartyPopper, Heart, X } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { AnimatePresence, motion } from 'motion/react';

export interface NotificationModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  description: string;
  type?: 'danger' | 'success' | 'info';
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm?: () => void | boolean | Promise<void | boolean>;
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
  const dialogRef = useRef<HTMLDivElement>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    dialogRef.current?.focus();
    return () => previousFocus?.focus();
  }, [isOpen]);

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape' && !busy) {
      event.stopPropagation();
      onClose();
      return;
    }
    if (event.key !== 'Tab') return;
    const controls = dialogRef.current?.querySelectorAll<HTMLElement>('button, a[href], input, select, textarea');
    if (!controls?.length) return;
    const first = controls[0];
    const last = controls[controls.length - 1];
    if (event.shiftKey && (document.activeElement === first || document.activeElement === dialogRef.current)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

  const icons = {
    danger: <AlertTriangle className='w-6 h-6 text-mood-anxiety-stress stroke-[2.2]' />,
    success: <PartyPopper className='w-6 h-6 text-primary stroke-[2.2]' />,
    info: <Heart className='w-6 h-6 text-secondary stroke-[2.2]' />,
  };

  const handleConfirm = async () => {
    if (busy) return;
    setBusy(true);
    try {
      const result = await onConfirm?.();
      if (result !== false) onClose();
    } finally { setBusy(false); }
  };

  return (
    <AnimatePresence>
    {isOpen && <motion.div
      key='notification-backdrop'
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.16 }}
      className='fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm'
    >
      <motion.div
        initial={{ opacity: 0, y: 12, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 8, scale: 0.98 }}
        transition={{ duration: 0.2, ease: 'easeOut' }}
        ref={dialogRef}
        tabIndex={-1}
        onKeyDown={handleKeyDown}
        className='relative w-full max-w-md bg-paper-warm border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg overflow-visible flex flex-col gap-5'
        role='dialog'
        aria-modal='true'
        aria-labelledby='notification-modal-title'
      >
        {/* Scrapbook WashiTape on Top Border */}
        <WashiTape color={washiColor} rotate={-2} className='absolute -top-3 left-10 w-32 z-10' />

        {/* Close button */}
        <button
          type='button'
          onClick={onClose}
          disabled={busy}
          className='absolute top-4 right-4 w-8 h-8 flex items-center justify-center rounded-xl hover:bg-black/10 transition-colors cursor-pointer border border-transparent hover:border-black'
          aria-label='Đóng hộp thoại'
        >
          <X className='w-5 h-5 text-black' />
        </button>

        {/* Modal Header */}
        <div className='flex items-start gap-3 mt-1'>
          <div className='w-12 h-12 rounded-2xl bg-white border-neo-sm shadow-neo-sm flex items-center justify-center shrink-0'>
            {icons[type]}
          </div>
          <div>
            <h3 id='notification-modal-title' className='font-space text-xl font-extrabold text-on-surface leading-snug'>
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
            disabled={busy}
            className='px-4 py-2 bg-white text-on-surface font-space font-bold text-xs uppercase border-neo-sm rounded-xl shadow-neo-sm hover:bg-gray-100 transition-all cursor-pointer'
          >
            {cancelLabel}
          </button>
          
          <NeoButton
            variant={type === 'danger' ? 'danger' : 'primary'}
            size='sm'
            onClick={handleConfirm}
            disabled={busy}
            className='text-xs font-space font-bold'
          >
            {confirmLabel}
          </NeoButton>
        </div>
      </motion.div>
    </motion.div>}
    </AnimatePresence>
  );
}
