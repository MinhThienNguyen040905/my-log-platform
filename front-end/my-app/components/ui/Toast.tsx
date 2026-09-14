'use client';

import React, { useEffect, useState } from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastProps {
  id: string;
  title: string;
  message?: string;
  type?: ToastType;
  duration?: number;
  onClose: (id: string) => void;
}

export function ToastItem({ id, title, message, type = 'success', duration = 3500, onClose }: ToastProps) {
  const [progress, setProgress] = useState(100);

  useEffect(() => {
    const startTime = Date.now();
    const interval = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const remaining = Math.max(0, 100 - (elapsed / duration) * 100);
      setProgress(remaining);
      if (elapsed >= duration) {
        clearInterval(interval);
        onClose(id);
      }
    }, 40);

    return () => clearInterval(interval);
  }, [id, duration, onClose]);

  const typeConfig = {
    success: {
      bg: 'bg-primary-container text-black',
      border: 'border-border-hard',
      icon: <CheckCircle2 className='w-5 h-5 text-black shrink-0' />,
      bar: 'bg-black',
    },
    error: {
      bg: 'bg-mood-anxiety-stress text-white',
      border: 'border-border-hard',
      icon: <AlertCircle className='w-5 h-5 text-white shrink-0' />,
      bar: 'bg-white',
    },
    info: {
      bg: 'bg-paper-warm text-on-surface',
      border: 'border-border-hard',
      icon: <Info className='w-5 h-5 text-black shrink-0' />,
      bar: 'bg-primary-container',
    },
  };

  const config = typeConfig[type];

  return (
    <div
      className={`relative w-full sm:w-96 p-4 rounded-2xl border-neo shadow-neo flex flex-col gap-1 overflow-hidden transition-all duration-200 animate-in slide-in-from-bottom-5 ${config.bg}`}
      role='alert'
    >
      <div className='flex items-start justify-between gap-3'>
        <div className='flex items-start gap-2.5 min-w-0'>
          {config.icon}
          <div className='flex flex-col min-w-0'>
            <span className='font-space text-sm font-extrabold tracking-tight truncate'>
              {title}
            </span>
            {message && (
              <p className='font-sans text-xs opacity-90 leading-snug mt-0.5'>
                {message}
              </p>
            )}
          </div>
        </div>

        <button
          type='button'
          onClick={() => onClose(id)}
          className='p-1 rounded-lg hover:bg-black/10 transition-colors shrink-0 cursor-pointer'
          aria-label='Đóng thông báo'
        >
          <X className='w-4 h-4 stroke-[2.5]' />
        </button>
      </div>

      {/* Countdown Progress Bar */}
      <div className='absolute bottom-0 left-0 right-0 h-1 bg-black/10'>
        <div
          className={`h-full transition-all duration-75 ${config.bar}`}
          style={{ width: `${progress}%` }}
        />
      </div>
    </div>
  );
}

export function ToastContainer({ toasts, onClose }: { toasts: Omit<ToastProps, 'onClose'>[]; onClose: (id: string) => void }) {
  return (
    <div className='fixed bottom-6 right-6 z-50 flex flex-col gap-3 max-w-[calc(100vw-32px)] pointer-events-none'>
      {toasts.map((toast) => (
        <div key={toast.id} className='pointer-events-auto'>
          <ToastItem {...toast} onClose={onClose} />
        </div>
      ))}
    </div>
  );
}
