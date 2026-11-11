import type { ReactNode } from 'react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { AuthHeader } from './AuthHeader';

interface AuthCardShellProps {
  title: string;
  description: string;
  exitLabel: string;
  children: ReactNode;
}

/** Khung một cột cho các trang xác thực phụ (quên mật khẩu, đặt lại mật khẩu). */
export function AuthCardShell({ title, description, exitLabel, children }: AuthCardShellProps) {
  return (
    <div className='min-h-dvh bg-bg-canvas flex flex-col selection:bg-primary-container selection:text-black overflow-x-hidden'>
      <AuthHeader exitLabel={exitLabel} />
      <main className='flex-1 w-full px-4 py-6 flex items-center justify-center'>
        <section className='relative w-full max-w-md bg-surface-card border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg'>
          <WashiTape color='lime' rotate={2} className='absolute -top-3 right-8 w-28 z-10' />
          <h1 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface'>{title}</h1>
          <p className='font-sans text-xs sm:text-sm text-on-surface-variant mt-1 mb-5 leading-relaxed'>{description}</p>
          {children}
        </section>
      </main>
    </div>
  );
}
