import Link from 'next/link';
import Image from 'next/image';
import { ArrowLeft } from 'lucide-react';

interface AuthHeaderProps {
  exitLabel: string;
}

export function AuthHeader({ exitLabel }: AuthHeaderProps) {
  return (
    <div className='w-full px-4 sm:px-8 pt-4 flex items-center justify-between'>
      <Link href='/' className='flex items-center group' aria-label='MyLog'>
        <Image
          src='/logo.png'
          alt='MyLog Logo'
          width={130}
          height={40}
          priority
          className='h-8 sm:h-9 w-auto max-w-[110px] sm:max-w-[130px] object-contain transition-transform duration-150 group-hover:scale-105'
        />
      </Link>

      <Link
        href='/'
        aria-label={exitLabel}
        title={exitLabel}
        className='inline-flex items-center justify-center w-9 h-9 bg-paper-warm border-neo rounded-xl shadow-neo-sm hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-neo active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all'
      >
        <ArrowLeft className='w-4 h-4' />
      </Link>
    </div>
  );
}
