'use client';

import { useRef, useState, type KeyboardEvent, type TouchEvent } from 'react';
import Image from 'next/image';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';

const PHOTO_URLS = [
  'https://images.unsplash.com/photo-1517842645767-c639042777db?w=600&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1455390582262-044cdead277a?w=600&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=600&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=600&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=600&auto=format&fit=crop&q=80',
];

const SWIPE_THRESHOLD_PX = 40;

export function LoginPhotoCarousel() {
  const { t } = useTranslation();
  const [index, setIndex] = useState(0);
  const touchStartX = useRef<number | null>(null);
  const total = PHOTO_URLS.length;

  const goTo = (next: number) => setIndex((next + total) % total);
  const prev = () => goTo(index - 1);
  const next = () => goTo(index + 1);

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'ArrowLeft') { event.preventDefault(); prev(); }
    if (event.key === 'ArrowRight') { event.preventDefault(); next(); }
  };

  const onTouchStart = (event: TouchEvent<HTMLDivElement>) => {
    touchStartX.current = event.touches[0]?.clientX ?? null;
  };

  const onTouchEnd = (event: TouchEvent<HTMLDivElement>) => {
    const start = touchStartX.current;
    touchStartX.current = null;
    const end = event.changedTouches[0]?.clientX;
    if (start === null || end === undefined) return;
    const delta = end - start;
    if (Math.abs(delta) < SWIPE_THRESHOLD_PX) return;
    if (delta < 0) next(); else prev();
  };

  const navButtonClass =
    'absolute top-1/2 -translate-y-1/2 z-10 w-8 h-8 rounded-full bg-white border-neo-sm shadow-neo-sm flex items-center justify-center hover:bg-primary-container active:shadow-none transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black';

  return (
    <div
      role='region'
      aria-roledescription='carousel'
      aria-label={t('auth.photoGallery')}
      tabIndex={0}
      onKeyDown={onKeyDown}
      className='mt-4 bg-white border-neo rounded-2xl p-3 shadow-neo rotate-[1.5deg] relative focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-black'
    >
      <WashiTape color='peach' rotate={2} className='absolute -top-2.5 right-6 w-24 z-20' />

      <div
        className='w-full aspect-[4/3] rounded-xl overflow-hidden border border-black/20 relative bg-gray-100 touch-pan-y'
        onTouchStart={onTouchStart}
        onTouchEnd={onTouchEnd}
      >
        {PHOTO_URLS.map((src, i) => (
          <Image
            key={src}
            src={src}
            alt={t(`auth.slides.${i}.alt`)}
            fill
            sizes='(min-width: 1024px) 380px, 90vw'
            priority={i === 0}
            aria-hidden={i !== index}
            className={`object-cover transition-opacity duration-300 motion-reduce:transition-none ${i === index ? 'opacity-100' : 'opacity-0'}`}
          />
        ))}

        <button type='button' onClick={prev} aria-label={t('auth.photoPrev')} className={`${navButtonClass} left-2`}>
          <ChevronLeft className='w-4 h-4' />
        </button>
        <button type='button' onClick={next} aria-label={t('auth.photoNext')} className={`${navButtonClass} right-2`}>
          <ChevronRight className='w-4 h-4' />
        </button>
      </div>

      <div className='mt-2.5 flex items-start justify-between gap-3'>
        <div className='flex flex-col min-w-0' aria-live='polite'>
          <span className='sr-only'>{t('auth.photoPosition', { current: index + 1, total })}</span>
          <p className='font-serif italic text-xs sm:text-sm font-semibold text-on-surface'>
            &ldquo;{t(`auth.slides.${index}.quote`)}&rdquo;
          </p>
          <span className='font-mono text-[11px] text-gray-500 font-bold mt-0.5'>
            {t(`auth.slides.${index}.caption`)}
          </span>
        </div>

        <div className='flex items-center gap-1.5 shrink-0 pt-1'>
          {PHOTO_URLS.map((src, i) => (
            <button
              key={src}
              type='button'
              onClick={() => goTo(i)}
              aria-label={t('auth.photoGoTo', { index: i + 1 })}
              aria-current={i === index}
              className={`h-2.5 rounded-full border border-black transition-all cursor-pointer ${i === index ? 'w-5 bg-primary-container' : 'w-2.5 bg-white hover:bg-gray-200'}`}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
