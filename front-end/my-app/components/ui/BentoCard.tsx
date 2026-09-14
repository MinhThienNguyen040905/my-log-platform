import React from 'react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';

export interface BentoCardProps {
  children: React.ReactNode;
  className?: string;
  bgVariant?: 'white' | 'paper' | 'purple' | 'lime' | 'yellow';
  washiTape?: {
    color?: 'lime' | 'peach' | 'lavender' | 'blue';
    position?: 'top-left' | 'top-right' | 'top-center';
    rotate?: number;
    className?: string;
  };
}

export function BentoCard({
  children,
  className = '',
  bgVariant = 'white',
  washiTape,
}: BentoCardProps) {
  const bgStyles = {
    white: 'bg-surface-card',
    paper: 'bg-paper-warm',
    purple: 'bg-secondary-container',
    lime: 'bg-primary-container',
    yellow: 'bg-tertiary-container',
  };

  const tapePositions = {
    'top-left': 'left-8 sm:left-14',
    'top-right': 'right-8 sm:right-14',
    'top-center': 'left-1/2 -translate-x-1/2',
  };

  const defaultRotates = {
    'top-left': -2,
    'top-right': 2,
    'top-center': -1,
  };

  const tapePos = washiTape?.position || 'top-right';
  const tapeRotate = washiTape?.rotate ?? defaultRotates[tapePos];

  return (
    <div
      className={`relative border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg overflow-visible flex flex-col justify-between ${bgStyles[bgVariant]} ${className}`}
    >
      {washiTape && (
        <WashiTape
          color={washiTape.color || 'lime'}
          rotate={tapeRotate}
          className={`absolute -top-3 ${tapePositions[tapePos]} w-28 sm:w-32 z-10 ${washiTape.className || ''}`}
        />
      )}
      {children}
    </div>
  );
}
