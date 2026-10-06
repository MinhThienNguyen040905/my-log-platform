'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  ArrowLeft,
  ArrowRight,
  Mail,
  Key,
  Eye,
  EyeOff,
  Check,
  CheckCircle2,
  RotateCcw,
  ShieldCheck,
  BookOpen,
} from 'lucide-react';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { useForgotPassword } from '../hooks/useForgotPassword';

export function ForgotPasswordForm() {
  const {
    step,
    email,
    setEmail,
    otp,
    otpInputRefs,
    newPassword,
    setNewPassword,
    confirmPassword,
    setConfirmPassword,
    showPassword,
    setShowPassword,
    showConfirmPassword,
    setShowConfirmPassword,
    loading,
    countdown,
    canResend,
    passwordStrength,
    maskedEmail,
    handleSendOtp,
    handleOtpChange,
    handleOtpKeyDown,
    handleOtpPaste,
    handleVerifyOtp,
    handleResendOtp,
    handleBackToEmail,
    handleResetPassword,
    handleGoToLogin,
  } = useForgotPassword();

  const stepsList = [
    { id: 'EMAIL', label: '1. Email' },
    { id: 'OTP', label: '2. Mã OTP' },
    { id: 'PASSWORD', label: '3. Mật khẩu mới' },
    { id: 'SUCCESS', label: '4. Hoàn tất' },
  ];

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
        <Link href='/auth/login'>
          <NeoButton variant='paper' size='sm' className='font-space font-bold gap-1.5 text-xs'>
            <ArrowLeft className='w-4 h-4' />
            <span className='hidden sm:inline'>Quay lại Đăng nhập</span>
          </NeoButton>
        </Link>
      </header>

      {/* Main Container */}
      <main className='flex-1 max-w-5xl mx-auto w-full px-4 sm:px-6 py-8 sm:py-12 flex items-center justify-center'>
        <div className='w-full grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch'>
          {/* Left Desk Sanctuary Polaroid Banner */}
          <div className='lg:col-span-5 bg-paper-warm border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg flex flex-col justify-between relative overflow-visible'>
            <WashiTape color='lime' rotate={-3} className='absolute -top-3 left-8 w-32 z-10' />

            <div className='flex flex-col gap-4'>
              <div className='inline-flex items-center gap-1.5 bg-white border border-black px-3 py-1 rounded-full text-xs font-space font-bold shadow-neo-sm w-fit'>
                <span className='w-2 h-2 rounded-full bg-brand-lime border border-black animate-pulse'></span>
                ACCOUNT RECOVERY
              </div>

              <h2 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface leading-tight'>
                Khôi phục chìa khóa sổ tay
              </h2>
              <p className='font-sans text-xs sm:text-sm text-on-surface-variant leading-relaxed'>
                Đừng lo lắng, chúng tôi luôn bảo vệ những trang nhật ký riêng tư của bạn an toàn và sẵn sàng mở lại bất cứ khi nào.
              </p>
            </div>

            {/* Cozy Desk Polaroid Card */}
            <div className='mt-6 bg-white border-neo rounded-2xl p-3 shadow-neo rotate-[1.5deg] relative'>
              <WashiTape color='peach' rotate={2} className='absolute -top-2.5 right-6 w-24 z-10' />
              <div className='w-full aspect-[4/3] rounded-xl overflow-hidden border border-black/20 relative bg-gray-100'>
                <img
                  src='https://images.unsplash.com/photo-1455390582262-044cdead277a?w=600&auto=format&fit=crop&q=80'
                  alt='Cozy Notebook and Key'
                  className='w-full h-full object-cover'
                />
              </div>
              <div className='mt-3 flex flex-col'>
                <p className='font-serif italic text-xs sm:text-sm font-semibold text-on-surface'>
                  &ldquo;Chỉ cần một hơi thở chậm, mọi dòng ký ức vẫn đợi bạn.&rdquo;
                </p>
                <span className='font-mono text-[11px] text-gray-500 font-bold mt-1'>
                  Khôi phục an toàn • 4 bước tinh gọn
                </span>
              </div>
            </div>

            {/* Privacy Promise Badge */}
            <div className='mt-6 pt-4 border-t border-border-soft flex items-center gap-2 text-xs font-space font-bold text-gray-600'>
              <ShieldCheck className='w-4 h-4 text-primary shrink-0' />
              <span>Xác thực OTP 2 lớp bảo vệ riêng tư</span>
            </div>
          </div>

          {/* Right / Form Box */}
          <div className='lg:col-span-7 bg-surface-card border-neo rounded-3xl p-6 sm:p-10 shadow-neo-lg flex flex-col justify-between relative overflow-visible'>
            <WashiTape color='blue' rotate={2} className='absolute -top-3 right-8 w-32 z-10 hidden sm:block' />

            <div>
              {/* Stepper Header (Scrapbook badges) */}
              <div className='flex items-center gap-2 flex-wrap mb-6 pb-4 border-b border-border-soft'>
                {stepsList.map((s, idx) => {
                  const isActive = step === s.id;
                  const isCompleted =
                    (step === 'OTP' && s.id === 'EMAIL') ||
                    (step === 'PASSWORD' && (s.id === 'EMAIL' || s.id === 'OTP')) ||
                    (step === 'SUCCESS' && s.id !== 'SUCCESS');

                  return (
                    <div
                      key={s.id}
                      className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-space font-bold transition-all ${
                        isActive
                          ? 'bg-primary-container text-black border border-black shadow-neo-xs'
                          : isCompleted
                          ? 'bg-white text-gray-800 border border-black/40'
                          : 'bg-paper-warm/60 text-gray-400 border border-dashed border-gray-300'
                      }`}
                    >
                      {isCompleted && <Check className='w-3 h-3 text-green-700 stroke-[3]' />}
                      <span>{s.label}</span>
                    </div>
                  );
                })}
              </div>

              {/* STEP 1: ENTER EMAIL */}
              {step === 'EMAIL' && (
                <div className='flex flex-col gap-6 animate-in fade-in duration-200'>
                  <div>
                    <div className='flex items-center justify-between mb-2'>
                      <h1 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface'>
                        Quên mật khẩu?
                      </h1>
                      <div className='w-10 h-10 rounded-xl bg-primary-container border-neo-sm shadow-neo-sm flex items-center justify-center font-bold'>
                        <Mail className='w-5 h-5 text-black stroke-[2.3]' />
                      </div>
                    </div>
                    <p className='font-sans text-xs sm:text-sm text-on-surface-variant'>
                      Nhập email đã đăng ký để nhận mã xác thực OTP 6 chữ số khôi phục tài khoản.
                    </p>
                  </div>

                  <form onSubmit={handleSendOtp} className='flex flex-col gap-5'>
                    <div className='flex flex-col gap-1.5'>
                      <label className='font-space text-xs font-bold uppercase text-on-surface flex items-center justify-between' htmlFor='recover-email'>
                        <span>Email tài khoản</span>
                        <span className='text-primary font-extrabold'>*</span>
                      </label>
                      <div className='relative flex items-center'>
                        <Mail className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' />
                        <input
                          id='recover-email'
                          type='email'
                          required
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                          placeholder='minhanh.journal@gmail.com'
                          className='w-full pl-10 pr-3 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-xs sm:text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all'
                        />
                      </div>
                    </div>

                    <NeoButton
                      type='submit'
                      variant='primary'
                      size='md'
                      disabled={loading}
                      className='w-full justify-center font-space font-extrabold text-xs sm:text-sm gap-2 mt-2 shadow-neo'
                      icon={<ArrowRight className='w-4 h-4' />}
                    >
                      {loading ? 'Đang gửi mã...' : 'Gửi mã xác thực OTP'}
                    </NeoButton>
                  </form>
                </div>
              )}

              {/* STEP 2: ENTER OTP */}
              {step === 'OTP' && (
                <div className='flex flex-col gap-6 animate-in fade-in duration-200'>
                  <div>
                    <div className='flex items-center justify-between mb-2'>
                      <h1 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface'>
                        Nhập mã OTP 6 số
                      </h1>
                      <div className='w-10 h-10 rounded-xl bg-primary-container border-neo-sm shadow-neo-sm flex items-center justify-center font-bold'>
                        <Key className='w-5 h-5 text-black stroke-[2.3]' />
                      </div>
                    </div>
                    <p className='font-sans text-xs sm:text-sm text-on-surface-variant'>
                      Mã bảo mật đã được gửi đến <strong className='text-black font-space'>{maskedEmail}</strong>.
                    </p>
                  </div>

                  <form onSubmit={handleVerifyOtp} className='flex flex-col gap-6'>
                    {/* Segmented 6 OTP Boxes */}
                    <div className='flex items-center justify-between gap-2 sm:gap-3 py-2'>
                      {otp.map((digit, idx) => (
                        <input
                          key={idx}
                          ref={(el) => {
                            otpInputRefs.current[idx] = el;
                          }}
                          type='text'
                          inputMode='numeric'
                          pattern='[0-9]*'
                          maxLength={1}
                          value={digit}
                          onChange={(e) => handleOtpChange(idx, e.target.value)}
                          onKeyDown={(e) => handleOtpKeyDown(idx, e)}
                          onPaste={handleOtpPaste}
                          className='w-11 h-14 sm:w-12 sm:h-14 bg-paper-warm border-2 border-black rounded-xl font-mono text-xl sm:text-2xl font-extrabold text-center shadow-neo-sm focus:outline-none focus:bg-white focus:border-black focus:shadow-neo transition-all'
                        />
                      ))}
                    </div>

                    {/* Resend Cooldown & Change Email */}
                    <div className='flex items-center justify-between flex-wrap gap-2 text-xs font-space'>
                      <button
                        type='button'
                        onClick={handleBackToEmail}
                        className='text-gray-600 hover:text-black underline font-bold cursor-pointer'
                      >
                        ← Đổi địa chỉ email khác
                      </button>

                      {canResend ? (
                        <button
                          type='button'
                          onClick={handleResendOtp}
                          disabled={loading}
                          className='inline-flex items-center gap-1.5 text-primary font-extrabold hover:underline cursor-pointer'
                        >
                          <RotateCcw className='w-3.5 h-3.5' />
                          <span>Gửi lại mã OTP</span>
                        </button>
                      ) : (
                        <span className='text-gray-500 font-bold'>
                          Gửi lại mã sau <strong className='text-black'>{countdown}s</strong>
                        </span>
                      )}
                    </div>

                    <NeoButton
                      type='submit'
                      variant='primary'
                      size='md'
                      disabled={loading || otp.join('').length < 6}
                      className='w-full justify-center font-space font-extrabold text-xs sm:text-sm gap-2 shadow-neo'
                      icon={<ArrowRight className='w-4 h-4' />}
                    >
                      {loading ? 'Đang xác thực...' : 'Xác thực mã OTP'}
                    </NeoButton>
                  </form>
                </div>
              )}

              {/* STEP 3: RESET PASSWORD */}
              {step === 'PASSWORD' && (
                <div className='flex flex-col gap-6 animate-in fade-in duration-200'>
                  <div>
                    <div className='flex items-center justify-between mb-2'>
                      <h1 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface'>
                        Tạo mật khẩu mới
                      </h1>
                      <div className='w-10 h-10 rounded-xl bg-primary-container border-neo-sm shadow-neo-sm flex items-center justify-center font-bold'>
                        <Key className='w-5 h-5 text-black stroke-[2.3]' />
                      </div>
                    </div>
                    <p className='font-sans text-xs sm:text-sm text-on-surface-variant'>
                      Mật khẩu nên chứa tối thiểu 6 ký tự để bảo vệ cuốn sổ an toàn.
                    </p>
                  </div>

                  <form onSubmit={handleResetPassword} className='flex flex-col gap-4'>
                    {/* New Password */}
                    <div className='flex flex-col gap-1.5'>
                      <label className='font-space text-xs font-bold uppercase text-on-surface' htmlFor='new-password'>
                        Mật khẩu mới
                      </label>
                      <div className='relative flex items-center'>
                        <Key className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' />
                        <input
                          id='new-password'
                          type={showPassword ? 'text' : 'password'}
                          required
                          value={newPassword}
                          onChange={(e) => setNewPassword(e.target.value)}
                          placeholder='Nhập mật khẩu mới...'
                          className='w-full pl-10 pr-10 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-xs sm:text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all'
                        />
                        <button
                          type='button'
                          onClick={() => setShowPassword(!showPassword)}
                          className='absolute right-3 text-gray-500 hover:text-black cursor-pointer'
                          aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                        >
                          {showPassword ? <EyeOff className='w-4 h-4' /> : <Eye className='w-4 h-4' />}
                        </button>
                      </div>

                      {/* Password strength meter */}
                      {newPassword && (
                        <div className='flex flex-col gap-1 mt-1'>
                          <div className='w-full h-1.5 bg-gray-200 rounded-full overflow-hidden border border-black/20'>
                            <div className={`h-full transition-all duration-300 ${passwordStrength.width} ${passwordStrength.color}`} />
                          </div>
                          <span className='font-space text-[10px] text-gray-600 font-bold'>
                            Độ mạnh: <strong className='text-black'>{passwordStrength.label}</strong>
                          </span>
                        </div>
                      )}
                    </div>

                    {/* Confirm Password */}
                    <div className='flex flex-col gap-1.5'>
                      <label className='font-space text-xs font-bold uppercase text-on-surface' htmlFor='confirm-password'>
                        Xác nhận mật khẩu mới
                      </label>
                      <div className='relative flex items-center'>
                        <Key className='w-4 h-4 text-gray-500 absolute left-3.5 pointer-events-none' />
                        <input
                          id='confirm-password'
                          type={showConfirmPassword ? 'text' : 'password'}
                          required
                          value={confirmPassword}
                          onChange={(e) => setConfirmPassword(e.target.value)}
                          placeholder='Nhập lại mật khẩu mới...'
                          className='w-full pl-10 pr-10 py-2.5 bg-paper-warm border-neo-sm rounded-xl font-sans text-xs sm:text-sm text-on-surface placeholder:text-gray-400 focus:outline-none focus:bg-white focus:shadow-neo-sm transition-all'
                        />
                        <button
                          type='button'
                          onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                          className='absolute right-3 text-gray-500 hover:text-black cursor-pointer'
                          aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                        >
                          {showConfirmPassword ? <EyeOff className='w-4 h-4' /> : <Eye className='w-4 h-4' />}
                        </button>
                      </div>
                    </div>

                    <NeoButton
                      type='submit'
                      variant='primary'
                      size='md'
                      disabled={loading || !newPassword || !confirmPassword}
                      className='w-full justify-center font-space font-extrabold text-xs sm:text-sm gap-2 mt-2 shadow-neo'
                      icon={<Check className='w-4 h-4 stroke-[2.5]' />}
                    >
                      {loading ? 'Đang lưu...' : 'Lưu mật khẩu mới'}
                    </NeoButton>
                  </form>
                </div>
              )}

              {/* STEP 4: SUCCESS */}
              {step === 'SUCCESS' && (
                <div className='flex flex-col items-center text-center gap-6 py-4 animate-in fade-in duration-200'>
                  <div className='w-16 h-16 rounded-2xl bg-primary-container border-2 border-black flex items-center justify-center shadow-neo-sm'>
                    <CheckCircle2 className='w-8 h-8 text-black stroke-[2.5]' />
                  </div>

                  <div className='flex flex-col gap-2'>
                    <h1 className='font-space text-2xl sm:text-3xl font-extrabold text-on-surface'>
                      Khôi phục thành công!
                    </h1>
                    <p className='font-sans text-xs sm:text-sm text-on-surface-variant max-w-sm leading-relaxed'>
                      Chìa khóa sổ tay của bạn đã được cập nhật an toàn. Giờ bạn có thể mở lại cuốn sổ quen thuộc với mật khẩu mới.
                    </p>
                  </div>

                  <div className='w-full pt-2'>
                    <NeoButton
                      type='button'
                      variant='primary'
                      size='md'
                      onClick={handleGoToLogin}
                      className='w-full justify-center font-space font-extrabold text-xs sm:text-sm gap-2 shadow-neo'
                      icon={<ArrowRight className='w-4 h-4' />}
                    >
                      Đăng nhập ngay
                    </NeoButton>
                  </div>
                </div>
              )}
            </div>

            {/* Bottom Switch Note */}
            <div className='mt-8 pt-4 border-t border-border-soft flex items-center justify-between text-xs font-space'>
              <span className='text-on-surface-variant'>Nhớ lại mật khẩu cũ?</span>
              <Link href='/auth/login' className='font-bold text-black hover:text-primary hover:underline flex items-center gap-1'>
                <span>Đăng nhập</span>
                <ArrowRight className='w-3.5 h-3.5' />
              </Link>
            </div>
          </div>
        </div>
      </main>

      {/* Footer Meta */}
      <footer className='w-full border-t border-border-soft py-4 px-4 text-center'>
        <p className='font-space text-[11px] text-gray-500 font-bold'>
          MyLog © 2026 • Cuốn sổ tay số với cảm giác trang giấy thật
        </p>
      </footer>
    </div>
  );
}

