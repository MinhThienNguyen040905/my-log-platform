import React from 'react';
import { Star } from 'lucide-react';

interface WashiTapeProps {
  color?: 'lime' | 'peach' | 'lavender' | 'blue';
  rotate?: number;
  className?: string;
}

export function WashiTape({ color = 'lime', rotate = -3, className = '' }: WashiTapeProps) {
  const colorMap = {
    lime: 'bg-primary-container/85 border-[#111111]',
    peach: 'bg-mood-hope-energy/85 border-[#111111]',
    lavender: 'bg-secondary-fixed/85 border-[#111111]',
    blue: 'bg-mood-sadness-reflect/85 border-[#111111]',
  };

  return (
    <div
      style={{ transform: `rotate(${rotate}deg)` }}
      className={`h-6 px-6 border-2 shadow-[2px_2px_0px_#111111] pointer-events-none select-none ${colorMap[color]} ${className}`}
    >
      <div className="w-full h-full flex items-center justify-between">
        <span className="text-[10px] font-bold tracking-widest font-mono uppercase">MYLOG</span>
        <Star className="w-2.5 h-2.5 fill-black stroke-black" />
      </div>
    </div>
  );
}

interface PolaroidCardProps {
  /** Without an image the card shows the caption on a ruled paper page, never a stock photo. */
  imageUrl?: string | null;
  caption: string;
  /** Colour strip along the top of the paper page (e.g. a mood colour). */
  accent?: string;
  date?: string;
  rotate?: number;
  className?: string;
  onClick?: () => void;
}

export function PolaroidCard({ imageUrl, caption, accent, date, rotate = 1, className = '', onClick }: PolaroidCardProps) {
  return (
    <div
      onClick={onClick}
      style={{ transform: `rotate(${rotate}deg)` }}
      className={`bg-white p-3 pb-4 border-neo shadow-neo transition-transform duration-200 hover:rotate-0 hover:scale-105 cursor-pointer ${className}`}
    >
      {imageUrl ? (
        <div className="w-full aspect-[4/3] overflow-hidden border border-black/20 bg-gray-100 relative">
          <img
            src={imageUrl}
            alt={caption}
            className="w-full h-full object-cover"
            loading="lazy"
          />
        </div>
      ) : (
        <div
          className="w-full aspect-[4/3] overflow-hidden border border-black/20 bg-paper-warm relative px-3 pt-5 pb-2"
          style={{ backgroundImage: 'repeating-linear-gradient(to bottom, transparent 0, transparent 21px, rgba(0,0,0,0.08) 21px, rgba(0,0,0,0.08) 22px)', backgroundPosition: '0 14px' }}
        >
          {accent && <span className="absolute inset-x-0 top-0 h-2 border-b border-black/20" style={{ backgroundColor: accent }} />}
          <p className="font-serif italic text-lg leading-[22px] font-semibold text-on-surface line-clamp-4">{caption}</p>
        </div>
      )}
      <div className="mt-2.5 flex items-center justify-between text-on-surface">
        {imageUrl && <p className="font-serif italic text-sm font-semibold truncate pr-2">{caption}</p>}
        {date && (
          <span className={`font-mono text-[11px] text-gray-500 shrink-0 font-bold ${imageUrl ? '' : 'ml-auto'}`}>{date}</span>
        )}
      </div>
    </div>
  );
}

interface StickerBadgeProps {
  label: string;
  icon?: string;
  variant?: 'lime' | 'yellow' | 'pink' | 'purple' | 'paper';
  rotate?: number;
  className?: string;
}

export function StickerBadge({ label, icon, variant = 'lime', rotate = 0, className = '' }: StickerBadgeProps) {
  const variantMap = {
    lime: 'bg-primary-container text-on-primary-container',
    yellow: 'bg-mood-hope-energy text-black',
    pink: 'bg-mood-anxiety-stress text-white',
    purple: 'bg-secondary-container text-white',
    paper: 'bg-paper-warm text-on-surface',
  };

  return (
    <div
      style={{ transform: `rotate(${rotate}deg)` }}
      className={`inline-flex items-center gap-1.5 px-3 py-1 font-space text-xs font-bold uppercase tracking-wider border-neo-sm shadow-neo-sm rounded-lg ${variantMap[variant]} ${className}`}
    >
      {icon && <span>{icon}</span>}
      <span>{label}</span>
    </div>
  );
}
