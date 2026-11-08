'use client';

import { Send } from 'lucide-react';
import type { CompanionChipOption } from '../../types/companion.types';

interface CompanionComposerProps {
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  disabled: boolean;
  chips: CompanionChipOption[];
  onChip: (chip: CompanionChipOption) => void;
}

export function CompanionComposer({ value, onChange, onSend, disabled, chips, onChip }: CompanionComposerProps) {
  return (
    <div className="flex flex-col gap-1.5 border-t border-black/15 p-3 pt-2">
      {chips.length > 0 && (
        <div className="flex flex-wrap gap-1.5 pb-1">
          {chips.map((chip) => (
            <button
              key={chip.intent}
              type="button"
              onClick={() => onChip(chip)}
              className="rounded-xl border border-black bg-white px-2.5 py-1 text-left font-space text-[11px] font-bold text-black transition-all hover:-translate-y-0.5 hover:bg-lime-100 shadow-neo-xs"
            >
              {chip.label}
            </button>
          ))}
        </div>
      )}
      <form
        onSubmit={(event) => {
          event.preventDefault();
          onSend();
        }}
        className="flex items-center gap-1.5"
      >
        <input
          type="text"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder="Nhắn vài chữ với mình..."
          maxLength={300}
          disabled={disabled}
          aria-label="Tin nhắn cho trợ lý"
          className="flex-1 rounded-xl border border-black bg-white px-3 py-2 text-xs text-black placeholder:text-gray-400 focus:outline-2 focus:outline-black disabled:opacity-60"
        />
        <button
          type="submit"
          disabled={disabled || !value.trim()}
          aria-label="Gửi tin nhắn"
          className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-xl border border-black bg-primary-container text-black shadow-neo-xs transition-all hover:bg-lime-200 disabled:opacity-40"
        >
          <Send className="h-3.5 w-3.5" />
        </button>
      </form>
    </div>
  );
}
