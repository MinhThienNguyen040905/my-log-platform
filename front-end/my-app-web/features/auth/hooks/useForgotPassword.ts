'use client';

import { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useToast } from '@/lib/toast-context';

export type ForgotPasswordStep = 'EMAIL' | 'OTP' | 'PASSWORD' | 'SUCCESS';

export function useForgotPassword() {
  const router = useRouter();
  const { showToast } = useToast();

  const [step, setStep] = useState<ForgotPasswordStep>('EMAIL');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState<string[]>(['', '', '', '', '', '']);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  // OTP Resend Cooldown Timer
  const [countdown, setCountdown] = useState(60);
  const [canResend, setCanResend] = useState(false);

  // Refs for 6 OTP input boxes
  const otpInputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (step === 'OTP' && countdown > 0) {
      timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            setCanResend(true);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [step, countdown]);

  // Calculate password strength
  const getPasswordStrength = (pwd: string) => {
    if (!pwd) return { label: 'Chưa nhập', width: 'w-0', color: 'bg-gray-200' };
    if (pwd.length < 6) return { label: 'Yếu', width: 'w-1/3', color: 'bg-red-500' };
    if (pwd.length < 10) return { label: 'Vừa phải', width: 'w-2/3', color: 'bg-amber-300' };
    return { label: 'Rất mạnh', width: 'w-full', color: 'bg-primary-container' };
  };

  // Mask email for display: minhanh@gmail.com -> m***h@gmail.com
  const getMaskedEmail = (rawEmail: string) => {
    if (!rawEmail || !rawEmail.includes('@')) return rawEmail;
    const [name, domain] = rawEmail.split('@');
    if (name.length <= 2) return `${name[0]}*@${domain}`;
    return `${name[0]}${'*'.repeat(Math.min(name.length - 2, 4))}${name[name.length - 1]}@${domain}`;
  };

  // 1. Submit Email
  const handleSendOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !email.includes('@')) {
      showToast({
        title: 'Email không hợp lệ!',
        message: 'Vui lòng kiểm tra lại địa chỉ email của bạn.',
        type: 'error',
      });
      return;
    }

    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setStep('OTP');
      setCountdown(60);
      setCanResend(false);
      setOtp(['', '', '', '', '', '']);

      showToast({
        title: 'Đã gửi mã xác thực!',
        message: `Mã OTP thử nghiệm là 123456 (đã gửi đến ${email}).`,
        type: 'info',
      });

      // Focus first OTP input
      setTimeout(() => {
        otpInputRefs.current[0]?.focus();
      }, 100);
    }, 500);
  };

  // 2. Handle OTP input change
  const handleOtpChange = (index: number, value: string) => {
    // Only accept numeric digit
    const cleanValue = value.replace(/\D/g, '').slice(-1);
    const newOtp = [...otp];
    newOtp[index] = cleanValue;
    setOtp(newOtp);

    // Auto advance to next input if filled
    if (cleanValue && index < 5) {
      otpInputRefs.current[index + 1]?.focus();
    }
  };

  // Handle Backspace navigation in OTP
  const handleOtpKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      otpInputRefs.current[index - 1]?.focus();
    }
  };

  // Handle paste 6-digit code
  const handleOtpPaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pastedData) return;

    const newOtp = [...otp];
    for (let i = 0; i < 6; i++) {
      newOtp[i] = pastedData[i] || '';
    }
    setOtp(newOtp);

    // Focus last filled or next input
    const nextIndex = Math.min(pastedData.length, 5);
    otpInputRefs.current[nextIndex]?.focus();
  };

  // Submit OTP Verification
  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    const fullOtp = otp.join('');
    if (fullOtp.length < 6) {
      showToast({
        title: 'Chưa đủ 6 số!',
        message: 'Vui lòng nhập đầy đủ mã OTP 6 chữ số.',
        type: 'error',
      });
      return;
    }

    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setStep('PASSWORD');
      showToast({
        title: 'Mã xác thực chính xác!',
        message: 'Vui lòng thiết lập mật khẩu mới cho cuốn sổ của bạn.',
        type: 'success',
      });
    }, 450);
  };

  // Resend OTP
  const handleResendOtp = () => {
    if (!canResend) return;
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setCountdown(60);
      setCanResend(false);
      setOtp(['', '', '', '', '', '']);
      showToast({
        title: 'Đã gửi lại mã mới!',
        message: 'Mã OTP mới là 123456.',
        type: 'info',
      });
      otpInputRefs.current[0]?.focus();
    }, 400);
  };

  // Back to Email Step
  const handleBackToEmail = () => {
    setStep('EMAIL');
  };

  // 3. Submit New Password
  const handleResetPassword = (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < 6) {
      showToast({
        title: 'Mật khẩu quá ngắn!',
        message: 'Mật khẩu cần tối thiểu 6 ký tự.',
        type: 'error',
      });
      return;
    }

    if (newPassword !== confirmPassword) {
      showToast({
        title: 'Mật khẩu xác nhận không khớp!',
        message: 'Vui lòng kiểm tra lại mật khẩu xác nhận của bạn.',
        type: 'error',
      });
      return;
    }

    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setStep('SUCCESS');
      showToast({
        title: 'Đặt lại mật khẩu thành công!',
        message: 'Chìa khóa sổ tay của bạn đã được cập nhật.',
        type: 'success',
      });
    }, 500);
  };

  const handleGoToLogin = () => {
    router.push('/auth/login');
  };

  return {
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
    passwordStrength: getPasswordStrength(newPassword),
    maskedEmail: getMaskedEmail(email),
    handleSendOtp,
    handleOtpChange,
    handleOtpKeyDown,
    handleOtpPaste,
    handleVerifyOtp,
    handleResendOtp,
    handleBackToEmail,
    handleResetPassword,
    handleGoToLogin,
  };
}

