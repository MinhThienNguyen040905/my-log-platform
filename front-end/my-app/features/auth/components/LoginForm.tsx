'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { ArrowLeft, ArrowRight, Eye, EyeOff, Mail, Key, Sparkles, ShieldCheck, Heart, BookOpen } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { useToast } from '@/lib/toast-context';

export function LoginForm() {
  const router = useRouter();
  const { showToast } = useToast();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    showToast({
      title: 'Đăng nhập thành công!',
      message: 'Chào mừng bạn trở lại với không gian nhật ký.',
      type: 'success',
    });
    setTimeout(() => {
      setLoading(false);
      router.push('/dashboard');
    }, 600);
  };

  const handleGoogleLogin = () => {
    setLoading(true);
    showToast({
      title: 'Đăng nhập Google thành công!',
      message: 'Đang mở cuốn sổ tay cá nhân của bạn...',
      type: 'success',
    });
    setTimeout(() => {
      setLoading(false);
      router.push('/dashboard');
    }, 600);
  };

  return (
    <div className='min-h-screen bg-bg-canvas flex flex-col justify-between selection:bg-brand-lime selection:text-black overflow-x-hidden'>
      {/* Header Bar */}
      <header className='w-full border-b-2 border-on-background bg-bg-canvas/90 backdrop-blur-md px-4 sm:px-8 py-4 flex items-center justify-between'>
        <Link href='/' className='flex items-center gap-2 group'>
          <div className='relative flex items-center justify-center h-9 sm:h-10 w-auto max-w-[130px] overflow-hidden'>
            <Image
              src='/logo.png'
              alt='MyLog Logo'
              width={130}
              height={40}
              priority
              className='h-9 sm:h-10 w-auto object-contain transition-transform duration-150 group-hover:scale-105'
            />
          </div>
        </Link>
        <Link href='/'>
          <NeoButton variant='paper' size='sm' className='font-space font-bold gap-1.5 text-xs'>
            <ArrowLeft className='w-4 h-4' />
          </NeoButton>
        </Link>
      </header>

      {/* Main Login Container */}
      <main className='flex-1 max-w-5xl mx-auto w-full px-4 sm:px-6 py-8 sm:py-12 flex items-center justify-center'>
        <div className='w-full grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch'>
          
          {/* Left / Top Sanctuary Polaroid Banner (Scrapbook Cozy Desk) */}
          <div className='lg:col-span-5 bg-paper-warm border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg flex flex-col justify-between relative overflow-visible'>
            <WashiTape color='lime' rotate={-3} className='absolute -top-3 left-8 w-32 z-10' />
            
            <div className='flex flex-col gap-4'>
              <div className='inline-flex items-center gap-1.5 bg-white border border-black px-3 py-1 rounded-full text-xs font-space font-bold shadow-neo-sm w-fit'>
                <span className='w-2 h-2 rounded-full bg-brand-lime border border-black animate-pulse'></span>
                LOGIN SANCTUARY
              </div>

              <h2 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface leading-tight'>
                Chào mừng bạn trở lại với cuốn sổ riêng
              </h2>
              <p className='font-sans text-xs sm:text-sm text-on-surface-variant leading-relaxed'>
                Nơi những suy tư không bị phán xét, nơi bạn dành ít phút lắng lại sau những ồn ào giảng đường và công việc.
              </p>
            </div>

            {/* Cozy Desk Polaroid Micro Card */}
            <div className='mt-6 bg-white border-neo rounded-2xl p-3 shadow-neo rotate-[1.5deg] relative'>
              <WashiTape color='peach' rotate={2} className='absolute -top-2.5 right-6 w-24 z-10' />
              <div className='w-full aspect-[4/3] rounded-xl overflow-hidden border border-black/20 relative bg-gray-100'>
                <img
                  src='https://images.unsplash.com/photo-1517842645767-c639042777db?w=600&auto=format&fit=crop&q=80'
                  alt='Cozy Desk and Journal'
                  className='w-full h-full object-cover'
                />
              </div>
              <div className='mt-3 flex flex-col'>
                <p className='font-serif italic text-xs sm:text-sm font-semibold text-on-surface'>
                  &ldquo;Hôm nay của bạn có êm đềm không?&rdquo;
                </p>
                <span className='font-mono text-[11px] text-gray-500 font-bold mt-1'>
                  17:45 • Giờ tan lớp • Bình yên
                </span>
              </div>
            </div>

            {/* Privacy Promise Badge */}
            <div className='mt-6 pt-4 border-t border-border-soft flex items-center gap-2 text-xs font-space font-bold text-gray-600'>
              <ShieldCheck className='w-4 h-4 text-primary shrink-0' />
              <span>Dữ liệu cảm xúc được mã hoá an toàn nội bộ</span>
            </div>
          </div>

          {/* Right / Form Box */}
          <div className='lg:col-span-7 bg-surface-card border-neo rounded-3xl p-6 sm:p-10 shadow-neo-lg flex flex-col justify-between relative overflow-visible'>
            <WashiTape color='blue' rotate={2} className='absolute -top-3 right-8 w-32 z-10 hidden sm:block' />

            <div>
              <div className='flex items-center justify-between mb-6'>
                <div>
                  <h1 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface'>
                    Đăng nhập tài khoản
                  </h1>
                  <p className='font-sans text-xs sm:text-sm text-on-surface-variant mt-1'>
                    Truy cập lại kho tàng ký ức và chuỗi streak 14 ngày của bạn
                  </p>
                </div>
                <div className='w-10 h-10 rounded-xl bg-primary-container border-neo-sm shadow-neo-sm flex items-center justify-center font-bold'>
                  <BookOpen className='w-5 h-5 text-black stroke-[2.3]' />
                </div>
              </div>

              {/* Google 1-Tap Login */}
              <button
                type='button'
                onClick={handleGoogleLogin}
                className='w-full flex items-center justify-center gap-3 py-3 px-4 bg-white border-neo rounded-xl shadow-neo font-space font-bold text-xs sm:text-sm hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-neo-lg active:translate-x-1 active:translate-y-1 active:shadow-none transition-all cursor-pointer'
              >
                <svg className='w-4 h-4 shrink-0' viewBox='0 0 24 24'>
                  <path d='M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z' fill='#4285F4'/>
                  <path d='M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z' fill='#34A853'/>
                  <path d='M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z' fill='#FBBC05'/>
                  <path d='M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z' fill='#EA4335'/>
                </svg>
                <span>Đăng nhập bằng Google</span>
              </button>

              {/* Neo-Brutalist Divider */}
              <div className='relative flex items-center justify-center my-6'>
                <div className='w-full h-0.5 bg-border-hard'></div>
                <span className='absolute bg-surface-card px-3 font-space text-[11px] font-bold uppercase tracking-widest text-on-surface-variant'>
                  Hoặc bằng email
                </span>
              </div>

              {/* Form Input Fields */}
              <form onSubmit={handleSubmit} className='flex flex-col gap-4'>
                {/* Email Field */}
                <div className='flex flex-col gap-1.5'>
                  <label className='font-space text-xs font-bold uppercase text-on-surface flex items-center justify-between' htmlFor='login-email'>
                    <span>Email</span>
                    <span className='text-primary font-extrabold'>*</span>
                  </label>
                  <div className='relative flex items-center'>
                    <Mail className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' />
                    <input
                      id='login-email'
                      type='email'
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder='minhanh@gmail.com'
                      className='w-full pl-10 pr-3 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-xs sm:text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all'
                    />
                  </div>
                </div>

                {/* Password Field */}
                <div className='flex flex-col gap-1.5'>
                  <div className='flex items-center justify-between'>
                    <label className='font-space text-xs font-bold uppercase text-on-surface' htmlFor='login-password'>
                      Mật khẩu
                    </label>
                    <button type='button' className='font-space text-xs font-bold text-primary hover:underline'>
                      Quên mật khẩu?
                    </button>
                  </div>
                  <div className='relative flex items-center'>
                    <Key className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' />
                    <input
                      id='login-password'
                      type={showPassword ? 'text' : 'password'}
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder='••••••••••••'
                      className='w-full pl-10 pr-10 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-xs sm:text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all'
                    />
                    <button
                      type='button'
                      onClick={() => setShowPassword(!showPassword)}
                      className='absolute right-3 text-gray-500 hover:text-black transition-colors p-1'
                    >
                      {showPassword ? <EyeOff className='w-4 h-4' /> : <Eye className='w-4 h-4' />}
                    </button>
                  </div>
                </div>

                {/* Remember Me Checkbox */}
                <label className='flex items-center gap-2 cursor-pointer select-none mt-1'>
                  <input
                    type='checkbox'
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className='w-4 h-4 rounded border-2 border-black accent-black'
                  />
                  <span className='font-sans text-xs text-on-surface-variant font-medium'>
                    Ghi nhớ đăng nhập trên máy này
                  </span>
                </label>

                {/* Submit CTA Button */}
                <button
                  type='submit'
                  disabled={loading}
                  className='w-full mt-2 py-3 px-4 bg-primary-container border-neo rounded-xl font-space font-extrabold text-sm uppercase tracking-wider shadow-neo hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-neo-lg active:translate-x-1 active:translate-y-1 active:shadow-none transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70'
                >
                  <span>{loading ? 'Đang mở khóa...' : 'VÀO KHÔNG GIAN VIẾT →'}</span>
                </button>
              </form>
            </div>

            {/* Bottom Redirect to Register */}
            <div className='mt-8 pt-4 border-t border-border-soft text-center font-space text-xs font-bold text-gray-600'>
              Chưa có sổ nhật ký cá nhân?{' '}
              <Link href='/auth/register' className='text-black underline hover:text-primary font-extrabold'>
                Đăng ký mở sổ mới →
              </Link>
            </div>
          </div>
        </div>
      </main>

      {/* Mini Footer */}
      <footer className='py-4 px-4 border-t-2 border-on-background bg-bg-canvas text-center font-space text-xs text-gray-500'>
        © 2026 MyLog. Đồng hành cùng sức khỏe tâm trí người trẻ.
      </footer>
    </div>
  );
}
