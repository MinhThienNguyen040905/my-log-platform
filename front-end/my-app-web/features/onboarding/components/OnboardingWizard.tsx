'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import {
  Sparkles,
  ArrowRight,
  Heart,
  Brain,
  Smile,
  Compass,
  CheckCircle2,
  Calendar,
  PenTool,
  Clock,
  Coffee,
  Cat,
  Sprout,
  Palette,
} from 'lucide-react';
import { WashiTape, StickerBadge } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { useJournal } from '@/features/journal';
import { useToast } from '@/lib/toast-context';

const AVATAR_OPTIONS = [
  { id: 'sprout', label: 'Mầm Xanh', icon: Sprout, color: 'bg-primary-container text-black', url: '/avatar.png' },
  { id: 'coffee', label: 'Ly Cà Phê', icon: Coffee, color: 'bg-secondary-container text-black', url: '/avatar.png' },
  { id: 'cat', label: 'Mèo Cam', icon: Cat, color: 'bg-paper-warm text-black', url: '/avatar.png' },
  { id: 'palette', label: 'Bút Vẽ', icon: Palette, color: 'bg-tertiary-container text-black', url: '/avatar.png' },
];

const GOAL_OPTIONS = [
  {
    id: 'stress_relief',
    title: 'Giải tỏa căng thẳng',
    desc: 'Trút cạn áp lực học tập, thi cử và công việc mỗi ngày',
    icon: Smile,
  },
  {
    id: 'inner_peace',
    title: 'Thấu hiểu cảm xúc & Bình an',
    desc: 'Gọi tên cảm xúc, nhận diện khuôn mẫu suy nghĩ cùng AI',
    icon: Heart,
  },
  {
    id: 'habit_building',
    title: 'Xây dựng thói quen viết bền bỉ',
    desc: 'Theo dõi chỉ số kiên cường và chuỗi ngày streak',
    icon: Brain,
  },
  {
    id: 'scrapbook_memory',
    title: 'Lưu giữ kỷ niệm độc bản',
    desc: 'Dán ảnh polaroid, sticker và viết nhật ký như sổ giấy thật',
    icon: Palette,
  },
];

const TIME_OPTIONS = [
  {
    id: 'evening',
    title: 'Mỗi tối trước khi ngủ',
    desc: 'Khoảng lặng êm đềm để khép lại một ngày dài',
    badge: 'Khuyên dùng 🌙',
  },
  {
    id: 'morning',
    title: 'Mỗi sáng sớm thức dậy',
    desc: 'Đặt tâm thế tích cực và định hình ngày mới ☀️',
    badge: 'Năng lượng ☕',
  },
  {
    id: 'anytime',
    title: 'Bất cứ khi nào cần trút cạn',
    desc: 'Viết tự do mỗi khi có cảm xúc dâng trào 🍃',
    badge: 'Linh hoạt ⚡',
  },
];

export function OnboardingWizard() {
  const router = useRouter();
  const { userProfile, updateProfile } = useJournal();
  const { showToast } = useToast();

  const [selectedAvatar, setSelectedAvatar] = useState('sprout');
  const [selectedGoals, setSelectedGoals] = useState<string[]>(['stress_relief', 'inner_peace']);
  const [selectedTime, setSelectedTime] = useState<string>('evening');
  const [submitting, setSubmitting] = useState(false);

  const toggleGoal = (id: string) => {
    setSelectedGoals(prev =>
      prev.includes(id) ? (prev.length > 1 ? prev.filter(g => g !== id) : prev) : [...prev, id]
    );
  };

  const handleFinish = (target: '/journal-editor' | '/dashboard') => {
    setSubmitting(true);
    updateProfile({
      isOnboarded: true,
      journalingGoals: selectedGoals,
      preferredTime: selectedTime,
    });

    showToast({
      title: 'Thiết lập hoàn tất!',
      message: `Cuốn sổ của ${userProfile.penName || 'bạn'} đã sẵn sàng mở ra trang đầu tiên.`,
      type: 'success',
    });

    setTimeout(() => {
      setSubmitting(false);
      router.push(target);
    }, 400);
  };

  return (
    <div className='min-h-screen bg-bg-canvas flex flex-col justify-between selection:bg-brand-lime selection:text-black overflow-x-hidden'>
      {/* Header Bar */}
      <header className='w-full border-b-2 border-on-background bg-bg-canvas/90 backdrop-blur-md px-4 sm:px-8 py-4 flex items-center justify-between sticky top-0 z-40'>
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
        <button
          type='button'
          onClick={() => handleFinish('/dashboard')}
          className='font-space text-xs sm:text-sm font-bold text-gray-600 hover:text-black hover:underline cursor-pointer px-3 py-1.5'
        >
          Để sau
        </button>
      </header>

      {/* Main Container */}
      <main className='flex-1 max-w-4xl mx-auto w-full px-4 sm:px-6 py-8 sm:py-12 flex flex-col gap-8'>
        {/* Top Welcome Title */}
        <div className='text-center max-w-2xl mx-auto flex flex-col items-center gap-3 relative'>
          <WashiTape color='lime' rotate={-2} className='w-32 absolute -top-4 -left-6 hidden sm:block' />
          <StickerBadge label='BƯỚC ĐẦU ĐỒNG HÀNH' variant='pink' rotate={1.5} />
          
          <h1 className='font-space text-3xl sm:text-4xl font-extrabold text-on-surface tracking-tight mt-1'>
            Chào mừng {userProfile.penName || 'bạn'}, hãy mở cuốn sổ của riêng mình
          </h1>
          <p className='font-sans text-sm sm:text-base text-on-surface-variant max-w-xl leading-relaxed'>
            Dành 30 giây để MyLog hiểu nhịp điệu cảm xúc và chuẩn bị không gian phù hợp nhất cho bạn.
          </p>
        </div>

        {/* Section 1: Chọn Avatar Linh Vật */}
        <div className='bg-surface-card border-neo rounded-3xl p-6 sm:p-8 shadow-neo flex flex-col gap-4 relative'>
          <WashiTape color='peach' rotate={2} className='w-24 absolute -top-3 right-8' />
          
          <div className='flex items-center gap-2'>
            <span className='w-7 h-7 rounded-lg bg-primary-container border-neo-sm flex items-center justify-center font-space font-extrabold text-xs shadow-neo-sm'>
              1
            </span>
            <h2 className='font-space text-lg sm:text-xl font-bold text-on-surface'>
              Chọn linh vật đồng hành cùng bạn
            </h2>
          </div>

          <div className='grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 mt-2'>
            {AVATAR_OPTIONS.map(avatar => {
              const Icon = avatar.icon;
              const isSelected = selectedAvatar === avatar.id;
              return (
                <button
                  key={avatar.id}
                  type='button'
                  onClick={() => setSelectedAvatar(avatar.id)}
                  className={`flex flex-col items-center justify-center gap-2 p-4 rounded-2xl border-2 transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-paper-warm border-black shadow-neo -translate-y-1'
                      : 'bg-white border-black/30 hover:border-black hover:bg-paper-warm/50'
                  }`}
                >
                  <div className={`w-12 h-12 rounded-xl border-neo-sm flex items-center justify-center shadow-neo-sm ${avatar.color}`}>
                    <Icon className='w-6 h-6 stroke-[2.2]' />
                  </div>
                  <span className='font-space text-xs sm:text-sm font-bold text-on-surface mt-1'>
                    {avatar.label}
                  </span>
                  {isSelected && (
                    <span className='text-[10px] font-space font-extrabold text-primary uppercase'>
                      ✓ Đang chọn
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* Section 2: Mục đích viết nhật ký */}
        <div className='bg-surface-card border-neo rounded-3xl p-6 sm:p-8 shadow-neo flex flex-col gap-4 relative'>
          <WashiTape color='blue' rotate={-1.5} className='w-28 absolute -top-3 left-10' />

          <div className='flex items-center justify-between'>
            <div className='flex items-center gap-2'>
              <span className='w-7 h-7 rounded-lg bg-secondary-container border-neo-sm flex items-center justify-center font-space font-extrabold text-xs shadow-neo-sm'>
                2
              </span>
              <h2 className='font-space text-lg sm:text-xl font-bold text-on-surface'>
                Điều gì đưa bạn đến với việc viết nhật ký?
              </h2>
            </div>
            <span className='text-xs font-space font-bold text-gray-500 hidden sm:inline-block'>
              (Có thể chọn nhiều)
            </span>
          </div>

          <div className='grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 mt-2'>
            {GOAL_OPTIONS.map(goal => {
              const Icon = goal.icon;
              const isSelected = selectedGoals.includes(goal.id);
              return (
                <button
                  key={goal.id}
                  type='button'
                  onClick={() => toggleGoal(goal.id)}
                  className={`flex items-start gap-3.5 p-4 rounded-2xl border-2 text-left transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-paper-warm border-black shadow-neo-sm -translate-y-0.5'
                      : 'bg-white border-black/30 hover:border-black hover:bg-paper-warm/40'
                  }`}
                >
                  <div className={`w-9 h-9 rounded-xl border-neo-sm flex items-center justify-center shrink-0 mt-0.5 ${
                    isSelected ? 'bg-primary-container text-black' : 'bg-gray-100 text-gray-600'
                  }`}>
                    <Icon className='w-5 h-5 stroke-[2.2]' />
                  </div>
                  <div className='flex-1'>
                    <div className='flex items-center justify-between'>
                      <h3 className='font-space text-sm font-bold text-on-surface'>
                        {goal.title}
                      </h3>
                      {isSelected && <CheckCircle2 className='w-4 h-4 text-green-700 shrink-0 ml-1' />}
                    </div>
                    <p className='font-sans text-xs text-on-surface-variant mt-1 leading-relaxed'>
                      {goal.desc}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Section 3: Thời điểm viết yêu thích */}
        <div className='bg-surface-card border-neo rounded-3xl p-6 sm:p-8 shadow-neo flex flex-col gap-4 relative'>
          <WashiTape color='lime' rotate={1.5} className='w-24 absolute -top-3 right-12' />

          <div className='flex items-center gap-2'>
            <span className='w-7 h-7 rounded-lg bg-tertiary-container border-neo-sm flex items-center justify-center font-space font-extrabold text-xs shadow-neo-sm'>
              3
            </span>
            <h2 className='font-space text-lg sm:text-xl font-bold text-on-surface'>
              Khoảnh khắc bạn muốn dành riêng cho bản thân
            </h2>
          </div>

          <div className='grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4 mt-2'>
            {TIME_OPTIONS.map(time => {
              const isSelected = selectedTime === time.id;
              return (
                <button
                  key={time.id}
                  type='button'
                  onClick={() => setSelectedTime(time.id)}
                  className={`flex flex-col justify-between p-4 rounded-2xl border-2 text-left transition-all cursor-pointer min-h-[110px] ${
                    isSelected
                      ? 'bg-paper-warm border-black shadow-neo-sm -translate-y-0.5'
                      : 'bg-white border-black/30 hover:border-black hover:bg-paper-warm/40'
                  }`}
                >
                  <div>
                    <span className='inline-block px-2 py-0.5 bg-white border border-black/40 rounded text-[10px] font-space font-bold mb-2'>
                      {time.badge}
                    </span>
                    <h3 className='font-space text-sm font-bold text-on-surface'>
                      {time.title}
                    </h3>
                  </div>
                  <p className='font-sans text-xs text-on-surface-variant mt-2 leading-relaxed'>
                    {time.desc}
                  </p>
                </button>
              );
            })}
          </div>
        </div>

        {/* Bottom CTA Action Bar */}
        <div className='bg-primary-container border-neo rounded-3xl p-6 sm:p-8 shadow-neo-lg flex flex-col sm:flex-row items-center justify-between gap-4 mt-2'>
          <div>
            <h3 className='font-space text-lg sm:text-xl font-extrabold text-black'>
              Sổ tay của bạn đã sẵn sàng!
            </h3>
            <p className='font-sans text-xs sm:text-sm text-black/80 mt-0.5'>
              Bạn có thể bắt đầu viết ngay trang đầu tiên hoặc khám phá bàn làm việc tổng quan.
            </p>
          </div>

          <div className='flex flex-wrap items-center gap-3 w-full sm:w-auto'>
            <NeoButton
              variant='paper'
              size='md'
              onClick={() => handleFinish('/dashboard')}
              disabled={submitting}
              className='font-bold text-xs sm:text-sm flex-1 sm:flex-initial'
            >
              Vào Dashboard
            </NeoButton>

            <NeoButton
              variant='dark'
              size='lg'
              onClick={() => handleFinish('/journal-editor')}
              disabled={submitting}
              className='font-extrabold text-sm sm:text-base flex-1 sm:flex-initial shadow-neo'
              icon={<PenTool className='w-4 h-4' />}
            >
              <span>{submitting ? 'Đang chuẩn bị...' : 'BẮT ĐẦU VIẾT NGAY →'}</span>
            </NeoButton>
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

