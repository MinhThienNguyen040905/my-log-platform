'use client';

import React, { forwardRef, useEffect, useImperativeHandle, useState } from 'react';
import {
  Heading2,
  Heading3,
  Quote,
  List,
  ListOrdered,
  ImagePlus,
  Type,
  Highlighter,
} from 'lucide-react';

export interface CommandItem {
  title: string;
  subtitle: string;
  icon: React.ComponentType<{ className?: string }>;
  command: (params: { editor: any; range: any }) => void;
}

export interface SlashCommandMenuRef {
  onKeyDown: (props: { event: KeyboardEvent }) => boolean;
}

interface SlashCommandMenuProps {
  items: CommandItem[];
  command: (item: CommandItem) => void;
}

export const SlashCommandMenu = forwardRef<SlashCommandMenuRef, SlashCommandMenuProps>(
  ({ items, command }, ref) => {
    const [selectedIndex, setSelectedIndex] = useState(0);

    useEffect(() => {
      setSelectedIndex(0);
    }, [items]);

    useImperativeHandle(ref, () => ({
      onKeyDown: ({ event }: { event: KeyboardEvent }) => {
        if (event.key === 'ArrowUp') {
          setSelectedIndex((prev) => (prev + items.length - 1) % items.length);
          return true;
        }

        if (event.key === 'ArrowDown') {
          setSelectedIndex((prev) => (prev + 1) % items.length);
          return true;
        }

        if (event.key === 'Enter') {
          if (items[selectedIndex]) {
            command(items[selectedIndex]);
            return true;
          }
        }

        return false;
      },
    }));

    if (items.length === 0) {
      return (
        <div className="bg-white border-2 border-black rounded-xl p-3 shadow-neo-md text-xs text-neutral-500 font-sans">
          Không tìm thấy lệnh phù hợp
        </div>
      );
    }

    return (
      <div className="bg-white border-2 border-black rounded-2xl shadow-neo-lg p-1.5 w-64 max-h-72 overflow-y-auto z-50 flex flex-col gap-1">
        <div className="px-2.5 py-1 text-[11px] font-bold tracking-wider uppercase text-neutral-400 font-mono">
          Khối & Định dạng
        </div>
        {items.map((item, index) => {
          const isSelected = index === selectedIndex;
          const Icon = item.icon;
          return (
            <button
              key={item.title}
              type="button"
              onClick={() => command(item)}
              onMouseEnter={() => setSelectedIndex(index)}
              className={`w-full flex items-center gap-2.5 px-2.5 py-2 rounded-xl text-left transition-all ${
                isSelected
                  ? 'bg-primary-container text-black font-semibold border-2 border-black shadow-neo-xs'
                  : 'text-neutral-700 hover:bg-neutral-100 border-2 border-transparent'
              }`}
            >
              <div
                className={`p-1.5 rounded-lg border border-black/20 ${
                  isSelected ? 'bg-white text-black' : 'bg-neutral-100 text-neutral-600'
                }`}
              >
                <Icon className="w-4 h-4" />
              </div>
              <div className="flex flex-col min-w-0">
                <span className="text-sm leading-snug truncate">{item.title}</span>
                <span className="text-[11px] text-neutral-500 truncate leading-none">
                  {item.subtitle}
                </span>
              </div>
            </button>
          );
        })}
      </div>
    );
  }
);

SlashCommandMenu.displayName = 'SlashCommandMenu';

