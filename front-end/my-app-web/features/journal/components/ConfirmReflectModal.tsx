'use client';

import React from 'react';
import { Sparkles } from 'lucide-react';
import { NeoButton } from '@/components/ui/NeoButton';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';

interface ConfirmReflectModalProps {
  isOpen: boolean;
  onConfirmReflect: () => void;
  onDirectSave: () => void;
}

export const ConfirmReflectModal: React.FC<ConfirmReflectModalProps> = ({
  isOpen,
  onConfirmReflect,
  onDirectSave,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in">
      <div className="relative bg-paper-warm border-2 border-black rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-neo-lg flex flex-col gap-5">
        <WashiTape color="lime" rotate={-2} className="w-28 absolute -top-3 left-8" />

        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-primary-container border border-black flex items-center justify-center shadow-neo-sm">
            <Sparkles className="w-5 h-5 text-black" />
          </div>
          <h3 className="font-space text-lg font-extrabold text-on-surface">
            Cùng MyLog phản chiếu nhé?
          </h3>
        </div>

        <p className="font-sans text-sm text-on-surface-variant leading-relaxed">
          Bạn có muốn dành 30 giây để MyLog gợi ý một góc nhìn thấu cảm và câu hỏi tự vấn trước khi cất sổ hôm nay không?
        </p>

        <div className="flex flex-col sm:flex-row items-center gap-3 pt-2">
          <NeoButton
            variant="primary"
            size="md"
            onClick={onConfirmReflect}
            className="w-full sm:flex-1 font-bold text-xs shadow-neo"
            icon={<Sparkles className="w-4 h-4" />}
          >
            Có, cùng phản chiếu
          </NeoButton>

          <button
            type="button"
            onClick={onDirectSave}
            className="w-full sm:w-auto px-4 py-2 text-xs font-space font-bold text-gray-700 hover:text-black hover:underline cursor-pointer"
          >
            Lưu và xem lịch ngay →
          </button>
        </div>
      </div>
    </div>
  );
};

