'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { ArrowLeft, ArrowRight, Eye, EyeOff, Mail, Key, User, ShieldCheck, Sparkles, CheckCircle2 } from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { useToast } from '@/lib/toast-context';
import { useJournal } from '@/lib/journal-context';

export default function RegisterPage() {
  const router = useRouter();
  const { showToast } = useToast();
  const { updateProfile } = useJournal();

  const [penName, setPenName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(true);
  const [loading, setLoading] = useState(false);

  // Calculate password strength
  const getPasswordStrength = (pwd: string) => {
    if (!pwd) return { label: 'Chưa nhập', width: 'w-0', color: 'bg-gray-200' };
    if (pwd.length < 6) return { label: 'Yếu', width: 'w-1/3', color: 'bg-red-500' };
    if (pwd.length < 10) return { label: 'Vừa phải', width: 'w-2/3', color: 'bg-mood-hope-energy' };
    return { label: 'Rất mạnh', width: 'w-full', color: 'bg-primary-container' };
  };

  const strength = getPasswordStrength(password);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (password !== confirmPassword) {
      showToast({
        title: 'Mật khẩu chưa khớp!',
        message: 'Vui lòng kiểm tra lại mật khẩu xác nhận của bạn.',
        type: 'error',
      });
      return;
    }

    setLoading(true);
    updateProfile({
      penName: penName.trim() || 'Bạn mới',
      name: penName.trim() || 'Bạn mới',
      email: email.trim(),
    });

    showToast({
      title: 'Tạo tài khoản thành công!',
      message: `Chào mừng ${penName || 'bạn'} đến với MyLog. Hãy cùng thiết lập cuốn sổ tay của bạn.`,
      type: 'success',
    });

    setTimeout(() => {
      setLoading(false);
      router.push('/onboarding');
    }, 600);
  };

  const handleGoogleSignup = () => {
    setLoading(true);
    updateProfile({
      penName: 'Minh Anh',
      name: 'Minh Anh',
      email: 'minhanh.google@gmail.com',
    });

    showToast({
      title: 'Đăng ký Google thành công!',
      message: 'Cùng cá nhân hóa không gian nhật ký của bạn...',
      type: 'success',
    });

    setTimeout(() => {
      setLoading(false);
      router.push('/onboarding');
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

      {/* Main Register Container */}
      <main className='flex-1 max-w-5xl mx-auto w-full px-4 sm:px-6 py-8 sm:py-12 flex items-center justify-center'>
        <div className='w-full grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch'>
          
          {/* Left / Feature Highlight Banner */}
          <div className='lg:col-span-5 bg-paper-warm border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg flex flex-col justify-between relative overflow-visible'>
            <WashiTape color='peach' rotate={-2} className='absolute -top-3 left-8 w-32 z-10' />

            <div className='flex flex-col gap-4'>
              <div className='inline-flex items-center gap-1.5 bg-white border border-black px-3 py-1 rounded-full text-xs font-space font-bold shadow-neo-sm w-fit'>
                <span className='w-2 h-2 rounded-full bg-brand-lime border border-black animate-pulse'></span>
                SIGN UP SANCTUARY
              </div>

              <h2 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface leading-tight'>
                Khởi tạo cuốn sổ ký ức của riêng bạn
              </h2>
              <p className='font-sans text-xs sm:text-sm text-on-surface-variant leading-relaxed'>
                Không gian an toàn tuyệt đối để ghi dấu từng cột mốc, giải tỏa âu lo và lắng nghe sự trưởng thành của chính mình mỗi ngày.
              </p>
            </div>

            {/* Benefit Checklist */}
            <div className='my-6 flex flex-col gap-3 bg-white border-neo-sm rounded-2xl p-4 shadow-neo-sm'>
              <span className='font-space text-xs font-bold uppercase text-gray-500'>Đặc quyền của bạn:</span>
              
              <div className='flex items-start gap-2 text-xs font-sans text-on-surface'>
                <CheckCircle2 className='w-4 h-4 text-primary shrink-0 mt-0.5' />
                <span>AI thấu cảm phân tích xu hướng cảm xúc tự động</span>
              </div>
              <div className='flex items-start gap-2 text-xs font-sans text-on-surface'>
                <CheckCircle2 className='w-4 h-4 text-primary shrink-0 mt-0.5' />
                <span>Kho sticker, polaroid và washi tape phong cách Scrapbook</span>
              </div>
              <div className='flex items-start gap-2 text-xs font-sans text-on-surface'>
                <CheckCircle2 className='w-4 h-4 text-primary shrink-0 mt-0.5' />
                <span>Bảo mật dữ liệu: Bạn nắm toàn quyền kiểm soát bài viết</span>
              </div>
            </div>

            {/* Trust Footer */}
            <div className='pt-4 border-t border-border-soft flex items-center gap-2 text-xs font-space font-bold text-gray-600'>
              <ShieldCheck className='w-4 h-4 text-primary shrink-0' />
              <span>Miễn phí 100% cho trải nghiệm viết nhật ký cốt lõi</span>
            </div>
          </div>

          {/* Right / Register Form */}
          <div className='lg:col-span-7 bg-surface-card border-neo rounded-3xl p-6 sm:p-10 shadow-neo-lg flex flex-col justify-between relative overflow-visible'>
            <WashiTape color='lime' rotate={2} className='absolute -top-3 right-8 w-32 z-10 hidden sm:block' />

            <div>
              <div className='flex items-center justify-between mb-6'>
                <div>
                  <h1 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface'>
                    Tạo tài khoản mới
                  </h1>
                  <p className='font-sans text-xs sm:text-sm text-on-surface-variant mt-1'>
                    Chỉ mất 30 giây để bắt đầu trang nhật ký đầu tiên
                  </p>
                </div>
                <div className='w-10 h-10 rounded-xl bg-secondary-container text-white border-neo-sm shadow-neo-sm flex items-center justify-center font-bold'>
                  <Sparkles className='w-5 h-5 text-white stroke-[2.3]' />
                </div>
              </div>

              {/* Google 1-Tap Sign Up */}
              <button
                type='button'
                onClick={handleGoogleSignup}
                disabled={loading}
                className='w-full flex items-center justify-center gap-3 py-3 px-4 bg-white border-neo rounded-xl shadow-neo font-space font-bold text-xs sm:text-sm hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-neo-lg active:translate-x-1 active:translate-y-1 active:shadow-none transition-all cursor-pointer disabled:opacity-60'
              >
                <svg className='w-4 h-4 shrink-0' viewBox='0 0 24 24'>
                  <path d='M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z' fill='#4285F4'/>
                  <path d='M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z' fill='#34A853'/>
                  <path d='M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z' fill='#FBBC05'/>
                  <path d='M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z' fill='#EA4335'/>
                </svg>
                <span>Đăng ký bằng Google</span>
              </button>

              {/* Neo-Brutalist Divider */}
              <div className='relative flex items-center justify-center my-6'>
                <div className='w-full h-0.5 bg-border-hard'></div>
                <span className='absolute bg-surface-card px-3 font-space text-[11px] font-bold uppercase tracking-widest text-on-surface-variant'>
                  Hoặc bằng email
                </span>
              </div>

              {/* Form Fields */}
              <form onSubmit={handleSubmit} className='flex flex-col gap-4'>
                {/* Pen Name */}
                <div className='flex flex-col gap-1.5'>
                  <label className='font-space text-xs font-bold uppercase text-on-surface flex items-center justify-between' htmlFor='pen-name'>
                    <span>Bút danh</span>
                    <span className='text-[11px] font-sans text-gray-500 font-normal'>Tên hiển thị trong sổ</span>
                  </label>
                  <div className='relative flex items-center'>
                    <User className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' />
                    <input
                      id='pen-name'
                      type='text'
                      required
                      value={penName}
                      onChange={(e) => setPenName(e.target.value)}
                      placeholder='Minh Anh'
                      className='w-full pl-10 pr-3 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-xs sm:text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all'
                    />
                  </div>
                </div>

                {/* Email Field */}
                <div className='flex flex-col gap-1.5'>
                  <label className='font-space text-xs font-bold uppercase text-on-surface flex items-center justify-between' htmlFor='register-email'>
                    <span>Email</span>
                    <span className='text-primary font-extrabold'>*</span>
                  </label>
                  <div className='relative flex items-center'>
                    <Mail className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' />
                    <input
                      id='register-email'
                      type='email'
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder='minhanh@gmail.com'
                      className='w-full pl-10 pr-3 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-xs sm:text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all'
                    />
                  </div>
                </div>

                {/* Password Field with Strength Meter */}
                <div className='flex flex-col gap-1.5'>
                  <label className='font-space text-xs font-bold uppercase text-on-surface' htmlFor='register-password'>
                    Mật khẩu <span className='text-primary font-extrabold'>*</span>
                  </label>
                  <div className='relative flex items-center'>
                    <Key className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' />
                    <input
                      id='register-password'
                      type={showPassword ? 'text' : 'password'}
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder='Tối thiểu 8 ký tự an toàn'
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

                  {/* Password Strength Indicator */}
                  {password && (
                    <div className='flex flex-col gap-1 mt-1'>
                      <div className='flex items-center justify-between text-[11px] font-space font-bold uppercase'>
                        <span className='text-gray-500'>Độ an toàn:</span>
                        <span className='px-1.5 py-0.5 bg-paper-warm border border-black rounded text-[10px]'>
                          {strength.label}
                        </span>
                      </div>
                      <div className='h-1.5 w-full bg-gray-200 rounded-full overflow-hidden border border-black/30'>
                        <div className={`h-full ${strength.width} ${strength.color} transition-all duration-300`}></div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Confirm Password Field */}
                <div className='flex flex-col gap-1.5'>
                  <label className='font-space text-xs font-bold uppercase text-on-surface' htmlFor='register-confirm-password'>
                    Xác nhận mật khẩu <span className='text-primary font-extrabold'>*</span>
                  </label>
                  <div className='relative flex items-center'>
                    <Key className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' />
                    <input
                      id='register-confirm-password'
                      type={showConfirmPassword ? 'text' : 'password'}
                      required
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder='Nhập lại mật khẩu vừa đặt'
                      className='w-full pl-10 pr-10 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-xs sm:text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all'
                    />
                    <button
                      type='button'
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className='absolute right-3 text-gray-500 hover:text-black transition-colors p-1'
                    >
                      {showConfirmPassword ? <EyeOff className='w-4 h-4' /> : <Eye className='w-4 h-4' />}
                    </button>
                  </div>
                </div>

                {/* Terms Checkbox */}
                <label className='flex items-start gap-2 cursor-pointer select-none mt-1'>
                  <input
                    type='checkbox'
                    checked={agreeTerms}
                    onChange={(e) => setAgreeTerms(e.target.checked)}
                    className='w-4 h-4 rounded border-2 border-black accent-black mt-0.5'
                  />
                  <span className='font-sans text-xs text-on-surface-variant font-medium leading-tight'>
                    Tôi đồng ý với điều khoản sử dụng và cam kết bảo vệ dữ liệu tâm lý của MyLog
                  </span>
                </label>

                {/* Submit CTA Button */}
                <button
                  type='submit'
                  disabled={loading}
                  className='w-full mt-2 py-3 px-4 bg-primary-container border-neo rounded-xl font-space font-extrabold text-sm uppercase tracking-wider shadow-neo hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-neo-lg active:translate-x-1 active:translate-y-1 active:shadow-none transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70'
                >
                  <span>{loading ? 'Đang mở sổ...' : 'MỞ TRANG SỔ ĐẦU TIÊN →'}</span>
                </button>
              </form>
            </div>

            {/* Bottom Redirect to Login */}
            <div className='mt-8 pt-4 border-t border-border-soft text-center font-space text-xs font-bold text-gray-600'>
              Đã có sổ nhật ký cá nhân?{' '}
              <Link href='/auth/login' className='text-black underline hover:text-primary font-extrabold'>
                Đăng nhập ngay →
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
