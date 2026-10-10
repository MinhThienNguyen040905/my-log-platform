'use client';

import React, { useEffect, useState } from 'react';
import { HeartHandshake, ShieldAlert, X, Heart } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { ApprovedSafetyResource, loadApprovedSafetyResources } from '../api/safety-resources';

export interface SafetyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onNavigateHome?: () => void;
  detectedKeywords?: string[];
}

export function SafetyModal({
  isOpen,
  onClose,
  onNavigateHome,
}: SafetyModalProps) {
  const [resources, setResources] = useState<ApprovedSafetyResource[]>([]);

  useEffect(() => {
    if (!isOpen) return;
    const controller = new AbortController();
    loadApprovedSafetyResources(controller.signal).then(setResources).catch(() => {
      if (!controller.signal.aborted) setResources([]);
    });
    return () => controller.abort();
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200"
      role="dialog"
      aria-modal="true"
      aria-labelledby="safety-modal-title"
    >
      <div className="relative w-full max-w-xl bg-paper-warm border-[2.5px] border-on-background rounded-3xl p-6 sm:p-8 shadow-neo-lg flex flex-col gap-5 overflow-visible">
        {/* Scrapbook WashiTape on top */}
        <WashiTape color="peach" rotate={-2} className="absolute -top-3.5 left-10 w-36 z-20" />

        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-xl hover:bg-black/10 transition-colors cursor-pointer border border-transparent hover:border-black"
          aria-label="Đóng cửa sổ an toàn"
        >
          <X className="w-5 h-5 text-black" />
        </button>

        {/* Header with Empathetic Shield */}
        <div className="flex items-start gap-4 mt-2">
          <div className="w-14 h-14 rounded-2xl bg-mood-anxiety-stress/20 border-2 border-black flex items-center justify-center shrink-0 shadow-neo-sm">
            <HeartHandshake className="w-7 h-7 text-mood-anxiety-stress stroke-[2.3]" />
          </div>
          <div className="flex-1">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-mood-anxiety-stress text-white font-space text-[10px] font-extrabold uppercase tracking-wider mb-1">
              <ShieldAlert className="w-3.5 h-3.5" />
              Hỗ trợ an toàn
            </div>
            <h2 id="safety-modal-title" className="font-space text-xl sm:text-2xl font-extrabold text-on-surface leading-tight">
              Bạn không phải đơn độc lúc này
            </h2>
            <p className="font-sans text-xs sm:text-sm text-on-surface-variant mt-1 leading-relaxed">
              Nếu bạn đang lo lắng về sự an toàn của mình, hãy tìm sự hỗ trợ từ một người bạn tin cậy hoặc dịch vụ khẩn cấp tại nơi bạn sống.
            </p>
          </div>
        </div>

        {/* Reassurance Notice */}
        <div className="bg-surface-card p-4 rounded-2xl border-2 border-black shadow-neo-sm flex flex-col gap-2">
          <div className="flex items-center gap-2">
            <Heart className="w-4 h-4 text-red-500 fill-red-500" />
            <span className="font-space text-xs font-bold uppercase tracking-wider text-black">
              Lời nhắc nhở nhẹ nhàng:
            </span>
          </div>
          <p className="font-sans text-xs text-on-surface leading-relaxed">
            AI không thể thay thế sự hỗ trợ trực tiếp từ con người trong tình huống khẩn cấp. Bạn có thể liên hệ một người bạn tin cậy hoặc dịch vụ khẩn cấp tại nơi bạn sống.
          </p>

        </div>

        <div className="bg-surface-card p-4 rounded-2xl border-2 border-black shadow-neo-sm">
          <p className="font-sans text-xs text-on-surface leading-relaxed">
            Nếu bạn đang gặp nguy hiểm ngay lúc này, hãy liên hệ dịch vụ khẩn cấp tại nơi bạn sống
            hoặc một người bạn tin cậy. Thông tin liên hệ cụ thể sẽ được hiển thị khi đã được xác minh.
          </p>
        </div>

        {resources.length > 0 && (
          <div className="flex flex-col gap-2.5">
            <h3 className="font-space text-xs font-extrabold uppercase tracking-wider text-on-surface">
              Nguồn hỗ trợ đã được xác minh
            </h3>
            {resources.map((resource) => (
              <div key={resource.id} className="rounded-xl border-2 border-black bg-white p-3 text-xs">
                <p className="font-space font-bold">{resource.name}</p>
                {resource.description && <p className="mt-1">{resource.description}</p>}
                {resource.contactValue && <p className="mt-1 font-semibold">{resource.contactValue}</p>}
                <a href={resource.sourceUrl} target="_blank" rel="noopener noreferrer"
                  className="mt-1 inline-block underline">Nguồn xác minh</a>
              </div>
            ))}
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex items-center justify-between gap-3 pt-3 border-t-2 border-black/10">
          {onNavigateHome && (
            <button
              type="button"
              onClick={() => {
                onClose();
                onNavigateHome();
              }}
              className="text-xs font-space font-bold text-gray-700 hover:text-black underline cursor-pointer"
            >
              ← Rời khỏi và về Dashboard
            </button>
          )}

          <NeoButton
            variant="primary"
            size="sm"
            onClick={onClose}
            className="text-xs font-space font-bold ml-auto"
          >
            Tiếp tục viết sổ
          </NeoButton>
        </div>
      </div>
    </div>
  );
}

