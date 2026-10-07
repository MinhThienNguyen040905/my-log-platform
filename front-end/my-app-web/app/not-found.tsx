import React from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { LayoutDashboard, PenLine, Home, ArrowLeft } from 'lucide-react';
import { NeoButton } from '@/components/ui/NeoButton';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';

export const metadata = {
  title: '404 - Trang không tồn tại | MyLog',
  description: 'Trang nhật ký này dường như đã bị thất lạc hoặc chưa được viết.',
};

export default function NotFound() {
  return (
    <div className="min-h-screen bg-bg-canvas flex flex-col items-center justify-center p-4 sm:p-8 relative selection:bg-primary-container selection:text-black overflow-hidden">
      {/* Decorative Washi Tapes in background corners */}
      <WashiTape color="lime" rotate={-5} className="absolute top-8 left-8 w-36 hidden sm:block pointer-events-none" />
      <WashiTape color="peach" rotate={6} className="absolute bottom-10 right-10 w-40 hidden sm:block pointer-events-none" />

      {/* Main 404 Scrapbook Card */}
      <main className="w-full max-w-2xl bg-paper-warm border-[2.5px] border-black rounded-3xl p-6 sm:p-10 shadow-neo-lg relative flex flex-col items-center text-center gap-6 animate-in fade-in zoom-in-95 duration-300">
        
        {/* Top Floating Washi Tape Sticker */}
        <WashiTape color="lavender" rotate={-2} className="absolute -top-3.5 left-12 w-36 z-10" />

        {/* The Exact Scrapbook 404 Illustration from user */}
        <div className="relative w-full max-w-sm sm:max-w-md aspect-[3/2] flex items-center justify-center transition-transform hover:scale-[1.02] duration-200">
          <Image
            src="/image-removebg-preview.png"
            alt="404 - Trang nhật ký bị xé hoặc thất lạc"
            width={480}
            height={320}
            priority
            className="w-full h-auto object-contain drop-shadow-[4px_6px_0px_rgba(0,0,0,0.15)]"
          />
        </div>

        {/* Message block */}
        <div className="flex flex-col gap-2.5 max-w-lg">
          <h1 className="font-space text-2xl sm:text-3xl font-extrabold text-on-surface tracking-tight">
            Trang nhật ký này dường như đã bị thất lạc...
          </h1>
          <p className="font-sans text-sm sm:text-base text-on-surface-variant leading-relaxed">
            Đường dẫn bạn vừa truy cập không tồn tại hoặc đã được di chuyển sang một góc ký ức khác. Đừng lo, những suy tư và mục tiêu của bạn vẫn vẹn nguyên!
          </p>
        </div>

        {/* Secondary Back to Home Link */}
        <div className="pt-2 border-t border-black/15 w-full flex items-center justify-center">
          <Link
            href="/"
            className="font-space text-xs font-bold text-gray-600 hover:text-black hover:underline inline-flex items-center gap-1.5 transition-colors"
          >
            <Home className="w-3.5 h-3.5" />
            <span>Quay lại trang chủ MyLog</span>
          </Link>
        </div>
      </main>
    </div>
  );
}

