import React from 'react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';

/**
 * Skeleton mô phỏng khung phân tích cảm xúc AI
 */
export function AiAnalysisSkeleton() {
  return (
    <div className='bg-paper-warm border-neo rounded-2xl p-5 shadow-neo relative overflow-visible animate-pulse'>
      <WashiTape color='lime' rotate={-2} className='absolute -top-3 right-6 w-24 z-10' />
      
      <div className='flex items-center gap-2 mb-4'>
        <div className='w-6 h-6 rounded-full bg-neutral-300 border border-black/30' />
        <div className='h-4 bg-neutral-300 rounded w-44' />
      </div>

      <div className='space-y-2 mb-4'>
        <div className='h-3.5 bg-neutral-300/80 rounded w-full' />
        <div className='h-3.5 bg-neutral-300/80 rounded w-5/6' />
        <div className='h-3.5 bg-neutral-300/80 rounded w-4/6' />
      </div>

      <div className='pt-3 border-t border-black/10 flex items-center justify-between'>
        <div className='h-6 bg-neutral-300 rounded-lg w-28' />
        <div className='h-6 bg-neutral-300 rounded-lg w-20' />
      </div>
    </div>
  );
}

/**
 * Skeleton mô phỏng thẻ nhật ký dạng Scrapbook
 */
export function JournalEntrySkeleton() {
  return (
    <div className='bg-surface-card border-neo rounded-2xl p-5 shadow-neo relative overflow-visible animate-pulse flex flex-col gap-3'>
      <WashiTape color='peach' rotate={1.5} className='absolute -top-3 left-6 w-24 z-10' />
      
      <div className='flex items-center justify-between mt-1'>
        <div className='h-5 bg-neutral-300 rounded-md w-32' />
        <div className='h-4 bg-neutral-200 rounded w-20' />
      </div>

      <div className='space-y-2 my-2'>
        <div className='h-3.5 bg-neutral-200 rounded w-full' />
        <div className='h-3.5 bg-neutral-200 rounded w-11/12' />
        <div className='h-3.5 bg-neutral-200 rounded w-3/4' />
      </div>

      <div className='flex items-center gap-2 pt-2 border-t border-border-soft'>
        <div className='h-5 bg-neutral-200 rounded-md w-16' />
        <div className='h-5 bg-neutral-200 rounded-md w-20' />
      </div>
    </div>
  );
}

/**
 * Skeleton toàn trang dùng cho Next.js loading.tsx
 */
export function PageSkeleton() {
  return (
    <div className='max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8 animate-pulse'>
      {/* Header Skeleton */}
      <div className='flex flex-wrap items-center justify-between gap-4 pb-4 border-b-2 border-on-background'>
        <div className='space-y-2'>
          <div className='h-8 bg-neutral-300 rounded-lg w-64 sm:w-80' />
          <div className='h-4 bg-neutral-200 rounded w-48 sm:w-60' />
        </div>
        <div className='flex gap-2'>
          <div className='h-10 bg-neutral-300 rounded-xl w-28 border-neo-sm' />
          <div className='h-10 bg-neutral-300 rounded-xl w-32 border-neo-sm' />
        </div>
      </div>

      {/* Hero Banner Skeleton */}
      <div className='bg-paper-warm border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg relative overflow-visible'>
        <WashiTape color='lime' rotate={-2} className='absolute -top-3 left-10 w-32 z-10' />
        <div className='space-y-4'>
          <div className='h-6 bg-neutral-300 rounded-md w-40' />
          <div className='h-10 bg-neutral-300 rounded-lg w-3/4 max-w-xl' />
          <div className='h-4 bg-neutral-200 rounded w-full max-w-lg' />
          <div className='flex gap-3 pt-2'>
            <div className='h-10 bg-neutral-300 rounded-xl w-36 border-neo-sm' />
            <div className='h-10 bg-neutral-200 rounded-xl w-36 border-neo-sm' />
          </div>
        </div>
      </div>

      {/* Cards Grid Skeleton */}
      <div className='grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6'>
        <JournalEntrySkeleton />
        <JournalEntrySkeleton />
        <JournalEntrySkeleton />
      </div>
    </div>
  );
}
