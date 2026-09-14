'use client';

import React from 'react';
import { HeartHandshake, PhoneCall, ShieldAlert, X, Heart, ExternalLink } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';

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
  detectedKeywords = [],
}: SafetyModalProps) {
  if (!isOpen) return null;

  const hotlines = [
    {
      name: 'Đường Dây Nóng Ngày Mai (Hỗ trợ tâm lý & trầm cảm)',
      phone: '096 306 1414',
      hours: '13:00 - 20:30 hàng ngày',
      badge: 'Miễn phí & Bảo mật',
      bg: 'bg-primary-container',
    },
    {
      name: 'Viện Sức Khỏe Tâm Thần - Bệnh Viện Bạch Mai',
      phone: '024 3869 3731',
      hours: '24/7 Cấp cứu y tế',
      badge: 'Chuyên môn y khoa',
      bg: 'bg-mood-hope-energy',
    },
    {
      name: 'Tổng Đài Quốc Gia Bảo Vệ Trẻ Em & Thanh Thiếu Niên',
      phone: '111',
      hours: '24/7 Toàn quốc',
      badge: 'Khẩn cấp 24/7',
      bg: 'bg-mood-sadness-reflect text-white',
    },
  ];

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
              Lưới An Toàn Cảm Xúc (FR-SAFETY-04)
            </div>
            <h2 id="safety-modal-title" className="font-space text-xl sm:text-2xl font-extrabold text-on-surface leading-tight">
              Bạn không phải đơn độc lúc này
            </h2>
            <p className="font-sans text-xs sm:text-sm text-on-surface-variant mt-1 leading-relaxed">
              MyLog nhận thấy những xúc cảm dồn nén và áp lực bạn vừa viết ra. Chúng tôi ở đây để đồng hành và nâng đỡ bạn, không hề phán xét.
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
            AI không thể thay thế sự hỗ trợ từ con người trong những thời điểm khủng hoảng. Hãy hít một hơi thở thật sâu, uống một ngụm nước ấm và cân nhắc trò chuyện với chuyên gia hoặc người bạn thân thiết nhất của mình.
          </p>
          {detectedKeywords.length > 0 && (
            <div className="flex items-center gap-1.5 flex-wrap pt-2 border-t border-dashed border-gray-300">
              <span className="text-[11px] font-space text-gray-500 font-bold">Từ khóa nhận diện:</span>
              {detectedKeywords.map((kw, idx) => (
                <span key={idx} className="text-[10px] font-space font-bold bg-gray-100 border border-black px-2 py-0.5 rounded">
                  {kw}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Emergency Hotlines List */}
        <div className="flex flex-col gap-2.5">
          <span className="font-space text-xs font-extrabold uppercase tracking-wider text-on-surface">
            Đường dây nóng hỗ trợ tâm lý miễn phí tại Việt Nam:
          </span>
          <div className="grid grid-cols-1 gap-2.5">
            {hotlines.map((h, i) => (
              <a
                key={i}
                href={`tel:${h.phone.replace(/\s+/g, '')}`}
                className="flex items-center justify-between p-3 bg-surface-container-lowest hover:bg-white rounded-xl border-2 border-black shadow-neo-sm transition-all hover:-translate-x-0.5 hover:-translate-y-0.5 cursor-pointer group"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-lg bg-black text-white flex items-center justify-center shrink-0">
                    <PhoneCall className="w-4 h-4" />
                  </div>
                  <div className="flex flex-col">
                    <span className="font-space text-xs font-bold text-black group-hover:text-primary transition-colors">
                      {h.name}
                    </span>
                    <span className="font-sans text-[11px] text-gray-600">
                      {h.hours} • <strong className="text-black">{h.phone}</strong>
                    </span>
                  </div>
                </div>
                <span className={`text-[10px] font-space font-bold px-2 py-0.5 rounded border border-black shrink-0 ${h.bg}`}>
                  {h.badge}
                </span>
              </a>
            ))}
          </div>
        </div>

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
            Tôi đã bình tâm, tiếp tục viết sổ
          </NeoButton>
        </div>
      </div>
    </div>
  );
}

