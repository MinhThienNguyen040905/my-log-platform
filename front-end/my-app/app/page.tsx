'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { WashiTape, PolaroidCard, StickerBadge } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { BentoCard } from '@/components/ui/BentoCard';
import {
  Sparkles,
  ArrowRight,
  PenTool,
  CheckCircle2,
  Brain,
  Zap,
  Heart,
  TrendingUp,
  Lightbulb,
  ShieldCheck,
  Sliders,
  Compass,
  CalendarDays,
  Lock,
  Flame,
  Check,
  EyeOff,
} from 'lucide-react';

export default function LandingPage() {
  return (
    <div className='w-full flex flex-col bg-bg-canvas overflow-x-hidden selection:bg-brand-lime selection:text-black'>
      {/* 0. STICKY LANDING HEADER */}
      <header className='sticky top-0 left-0 w-full bg-bg-canvas/90 backdrop-blur-md border-b-2 border-on-background z-50 transition-all'>
        <div className='h-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between'>
          <Link href='/' className='flex items-center gap-3 group'>
            <div className='relative flex items-center justify-center h-10 sm:h-11 w-auto max-w-[140px] overflow-hidden'>
              <Image
                src='/logo.png'
                alt='MyLog Logo'
                width={140}
                height={44}
                priority
                className='h-10 sm:h-11 w-auto object-contain transition-transform duration-150 group-hover:scale-105'
              />
            </div>
          </Link>

          <nav className='hidden md:flex items-center gap-2 lg:gap-4 font-space text-sm font-bold'>
            <a
              href='#how-it-works'
              className='px-3 py-1.5 rounded-lg hover:bg-paper-warm hover:text-black transition-colors'
            >
              Cách hoạt động
            </a>
            <a
              href='#why-mylog'
              className='px-3 py-1.5 rounded-lg hover:bg-paper-warm hover:text-black transition-colors'
            >
              Tại sao là MyLog
            </a>
            <a
              href='#features'
              className='px-3 py-1.5 rounded-lg hover:bg-paper-warm hover:text-black transition-colors'
            >
              Tính năng
            </a>
            <a
              href='#privacy'
              className='px-3 py-1.5 rounded-lg hover:bg-paper-warm hover:text-black transition-colors'
            >
              Riêng tư & An toàn
            </a>
            <a
              href='#reviews'
              className='px-3 py-1.5 rounded-lg hover:bg-paper-warm hover:text-black transition-colors'
            >
              Cảm nhận
            </a>
          </nav>

          <div className='flex items-center gap-3'>
            <Link
              href='/auth/login'
              className='hidden sm:block font-space text-sm font-bold text-on-surface hover:text-primary px-3 py-2 transition-colors'
            >
              Đăng nhập
            </Link>
            <Link href='/journal-editor'>
              <NeoButton variant='primary' size='sm' className='font-space font-bold text-xs sm:text-sm shadow-neo-sm'>
                <span className="inline-flex items-center gap-1.5">
                  Bắt đầu viết
                  <ArrowRight className="w-4 h-4 shrink-0" />
                </span>
              </NeoButton>
            </Link>
          </div>
        </div>
      </header>

      {/* 1. HERO SECTION: 5-Second Value Proposition */}
      <section className='relative w-full min-h-[82vh] flex flex-col items-center justify-center pt-8 pb-16 px-4 sm:px-6 lg:px-8 border-b-2 border-on-background bg-scrapbook-dots'>
        {/* Decorative Floating Scrapbook Elements */}
        <WashiTape color='lime' rotate={-5} className='absolute top-6 left-6 sm:left-16 w-36 sm:w-44 z-10 hidden sm:block' />
        <WashiTape color='peach' rotate={4} className='absolute top-12 right-6 sm:right-20 w-32 sm:w-40 z-10 hidden sm:block' />

        {/* Polaroid 1 floating left */}
        <div className='absolute left-4 lg:left-12 top-1/3 hidden xl:block z-10'>
          <PolaroidCard
            imageUrl='https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=500&auto=format&fit=crop&q=80'
            caption='Bảo vệ đề cương thành công'
            date='15.10.2026'
            rotate={-6}
            className='w-56'
          />
        </div>

        {/* Polaroid 2 floating right */}
        <div className='absolute right-4 lg:right-12 top-1/3 hidden xl:block z-10'>
          <PolaroidCard
            imageUrl='https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=500&auto=format&fit=crop&q=80'
            caption='5km hồ Tây và latte yến mạch'
            date='14.10.2026'
            rotate={5}
            className='w-56'
          />
        </div>

        {/* Main Hero Container */}
        <div className='max-w-4xl mx-auto text-center flex flex-col items-center gap-6 relative z-20'>
          {/* Brand Tagline Badge */}
          <div className='inline-flex items-center gap-2 bg-paper-warm px-4 py-1.5 border-neo-sm rounded-full shadow-neo-sm rotate-[-1deg]'>
            <span className='w-2.5 h-2.5 rounded-full bg-primary-container border border-black animate-pulse'></span>
            <span className='font-space text-xs sm:text-sm font-extrabold uppercase tracking-wider text-on-surface'>
              Sổ Tay Cảm Xúc Neo-Brutalist & Trí Tuệ Nhân Tạo
            </span>
          </div>

          {/* Core Iconic Headline */}
          <div className='flex flex-col pt-4 items-center font-space font-extrabold text-on-surface tracking-tighter leading-[0.95] select-none'>
            <span className='text-4xl sm:text-6xl md:text-7xl lg:text-8xl'>
              WRITE IT.
            </span>
            <span className='text-4xl sm:text-6xl md:text-7xl lg:text-8xl text-transparent bg-clip-text bg-gradient-to-r from-primary to-[#8433C4]'>
              KEEP IT.
            </span>
            <span className='text-4xl sm:text-6xl md:text-7xl lg:text-8xl underline decoration-primary-container decoration-[6px] sm:decoration-8 underline-offset-8'>
              REMEMBER IT.
            </span>
          </div>

          {/* Concrete Description: Understandable in 5 seconds */}
          <p className='font-sans text-base sm:text-lg md:text-xl text-on-surface-variant max-w-2xl mt-3 leading-relaxed'>
            Viết điều bạn đang trải qua. MyLog giúp bạn nhận diện cảm xúc, tìm ra những khuôn mẫu lặp lại và gợi ý bước nhỏ để hiểu bản thân hơn mỗi ngày.
          </p>

          {/* Action CTAs: Clear Dominant Hierarchy */}
          <div className='flex flex-wrap items-center justify-center gap-4 mt-3'>
            <Link href='/journal-editor'>
              <NeoButton
                size='lg'
                className='text-base sm:text-lg px-8 sm:px-10 py-4 bg-primary-container text-black font-extrabold shadow-neo hover:translate-x-0.5 hover:translate-y-0.5 transition-all'
                icon={<PenTool className='w-5 h-5 stroke-[2.5]' />}
              >
                BẮT ĐẦU VIẾT →
              </NeoButton>
            </Link>

            <a href='#how-it-works'>
              <NeoButton
                variant='paper'
                size='lg'
                className='text-base sm:text-lg px-6 py-4 font-bold'
                icon={<Compass className='w-5 h-5 stroke-[2.2]' />}
              >
                XEM MYLOG HOẠT ĐỘNG
              </NeoButton>
            </a>
          </div>

          {/* Micro Trust Badges */}
          <div className='flex flex-wrap items-center justify-center gap-6 mt-4 pt-4 border-t border-border-soft text-xs sm:text-sm font-space font-bold text-gray-700'>
            <span className='flex items-center gap-1.5'>
              <CheckCircle2 className='w-4 h-4 text-green-700 shrink-0' /> Hoàn toàn riêng tư
            </span>
            <span className='flex items-center gap-1.5'>
              <CheckCircle2 className='w-4 h-4 text-green-700 shrink-0' /> AI thấu cảm không phán xét
            </span>
            <span className='flex items-center gap-1.5'>
              <CheckCircle2 className='w-4 h-4 text-green-700 shrink-0' /> Không cần đắn đo cấu trúc
            </span>
          </div>
        </div>
      </section>

      {/* 2. PRODUCT LOOP: 3 STEPS (BROUGHT UP EARLY) */}
      <section id='how-it-works' className='w-full py-16 sm:py-20 px-4 sm:px-6 lg:px-8 bg-paper-warm border-b-2 border-on-background scroll-mt-20'>
        <div className='max-w-6xl mx-auto flex flex-col gap-10 text-center'>
          <div className='flex flex-col items-center'>
            <StickerBadge label='VÒNG LẶP SẢN PHẨM' variant='lime' rotate={2} />
            <h2 className='font-space text-3xl sm:text-4xl font-extrabold text-on-surface mt-3'>
              3 bước đơn giản nuôi dưỡng sự tự phản tư
            </h2>
            <p className='font-sans text-base text-on-surface-variant mt-2 max-w-xl'>
              Quy trình tự nhiên giúp bạn giải tỏa cảm xúc ngay trong hiện tại và tích lũy insight lâu dài.
            </p>
          </div>

          <div className='grid grid-cols-1 md:grid-cols-3 gap-6 sm:gap-8 text-left'>
            {/* Step 1 */}
            <div className='bg-surface-card border-neo rounded-2xl p-6 sm:p-7 shadow-neo flex flex-col gap-4 relative hover:-translate-y-1 transition-transform'>
              <div className='flex items-center justify-between'>
                <div className='w-11 h-11 rounded-xl bg-primary-container border-neo-sm flex items-center justify-center font-space font-extrabold text-lg shadow-neo-sm'>
                  1
                </div>
                <span className='font-space text-xs font-bold text-gray-500 uppercase tracking-wider'>Bước 1</span>
              </div>
              <h3 className='font-space text-xl font-bold text-on-surface'>Viết Tự Do Không Áp Lực</h3>
              <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                Trút cạn những gì bạn đang nghĩ mà không cần trau chuốt câu từ. Bạn có thể đính kèm ảnh polaroid hoặc dán các nhãn cảm xúc trực quan.
              </p>
            </div>

            {/* Step 2 */}
            <div className='bg-surface-card border-neo rounded-2xl p-6 sm:p-7 shadow-neo flex flex-col gap-4 relative hover:-translate-y-1 transition-transform'>
              <div className='flex items-center justify-between'>
                <div className='w-11 h-11 rounded-xl bg-secondary-container border-neo-sm flex items-center justify-center font-space font-extrabold text-lg shadow-neo-sm'>
                  2
                </div>
                <span className='font-space text-xs font-bold text-gray-500 uppercase tracking-wider'>Bước 2</span>
              </div>
              <h3 className='font-space text-xl font-bold text-on-surface'>AI Thấu Cảm Phân Tích</h3>
              <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                Mô hình AI riêng tư tự động nhận diện sắc thái cảm xúc, mức độ năng lượng và bóc tách những chủ đề bạn thường bận tâm nhất.
              </p>
            </div>

            {/* Step 3 */}
            <div className='bg-surface-card border-neo rounded-2xl p-6 sm:p-7 shadow-neo flex flex-col gap-4 relative hover:-translate-y-1 transition-transform'>
              <div className='flex items-center justify-between'>
                <div className='w-11 h-11 rounded-xl bg-tertiary-container border-neo-sm flex items-center justify-center font-space font-extrabold text-lg shadow-neo-sm'>
                  3
                </div>
                <span className='font-space text-xs font-bold text-gray-500 uppercase tracking-wider'>Bước 3</span>
              </div>
              <h3 className='font-space text-xl font-bold text-on-surface'>Nhận Diện Mẫu Hình & Hành Động</h3>
              <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                Nhìn ra xu hướng cảm xúc qua các tuần, nhận các gợi ý bước nhỏ thực tế (micro-steps) để cân bằng và giải tỏa áp lực.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 3. VALUE PROPOSITION: CONDENSED (1 IDEA PER CARD) */}
      <section id='why-mylog' className='w-full py-16 sm:py-20 px-4 sm:px-6 lg:px-8 bg-bg-canvas border-b-2 border-on-background scroll-mt-20'>
        <div className='max-w-7xl mx-auto flex flex-col gap-10'>
          <div className='text-center max-w-3xl mx-auto'>
            <StickerBadge label='TẠI SAO LÀ MYLOG?' variant='purple' rotate={-2} />
            <h2 className='font-space text-3xl sm:text-4xl font-extrabold text-on-surface mt-3'>
              Không chỉ là ghi chú, đây là không gian chăm sóc nội tâm
            </h2>
            <p className='font-sans text-base text-on-surface-variant mt-2'>
              Sự kết hợp tinh tế giữa xúc cảm sổ dán ảnh mộc mạc và công nghệ nhận diện tâm lý hành vi.
            </p>
          </div>

          <div className='grid grid-cols-1 md:grid-cols-3 gap-6 sm:gap-8'>
            {/* Value Card 1 */}
            <div className='bg-paper-warm border-2 border-black/80 rounded-2xl p-6 sm:p-7 shadow-neo-sm flex flex-col gap-3.5 relative'>
              <WashiTape color='lime' rotate={-2} className='absolute -top-3 left-8 w-24' />
              <div className='w-11 h-11 rounded-xl bg-primary-container border-neo-sm flex items-center justify-center shadow-neo-sm mt-1'>
                <Heart className='w-5 h-5 text-black stroke-[2.2]' />
              </div>
              <div>
                <span className='font-space text-xs font-extrabold uppercase text-primary tracking-wider'>Trải nghiệm</span>
                <h3 className='font-space text-xl font-bold text-on-surface mt-0.5'>Sổ Tay Scrapbook Độc Bản</h3>
              </div>
              <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                Gợi nhớ xúc cảm tự nhiên của trang giấy thật với băng dán washi, sticker và ảnh polaroid — mang lại sự ấm áp thay vì các ứng dụng khô cứng.
              </p>
            </div>

            {/* Value Card 2 */}
            <div className='bg-paper-warm border-2 border-black/80 rounded-2xl p-6 sm:p-7 shadow-neo-sm flex flex-col gap-3.5 relative'>
              <WashiTape color='peach' rotate={3} className='absolute -top-3 right-8 w-24' />
              <div className='w-11 h-11 rounded-xl bg-secondary-container border-neo-sm flex items-center justify-center shadow-neo-sm mt-1'>
                <Brain className='w-5 h-5 text-black stroke-[2.2]' />
              </div>
              <div>
                <span className='font-space text-xs font-extrabold uppercase text-primary tracking-wider'>Đồng hành</span>
                <h3 className='font-space text-xl font-bold text-on-surface mt-0.5'>AI Phản Tư Không Phán Xét</h3>
              </div>
              <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                Lắng nghe trọn vẹn và phản hồi dịu dàng. AI giúp bạn gọi tên cảm xúc và làm sáng tỏ dòng suy nghĩ mà bạn đang mắc kẹt.
              </p>
            </div>

            {/* Value Card 3 */}
            <div className='bg-paper-warm border-2 border-black/80 rounded-2xl p-6 sm:p-7 shadow-neo-sm flex flex-col gap-3.5 relative'>
              <WashiTape color='blue' rotate={-1} className='absolute -top-3 left-12 w-24' />
              <div className='w-11 h-11 rounded-xl bg-tertiary-container border-neo-sm flex items-center justify-center shadow-neo-sm mt-1'>
                <TrendingUp className='w-5 h-5 text-black stroke-[2.2]' />
              </div>
              <div>
                <span className='font-space text-xs font-extrabold uppercase text-primary tracking-wider'>Nhận thức</span>
                <h3 className='font-space text-xl font-bold text-on-surface mt-0.5'>Nhịp Điệu & Khuôn Mẫu Cảm Xúc</h3>
              </div>
              <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                Dòng thời gian và bản đồ ký ức cho bạn thấy bức tranh toàn cảnh: điều gì đem lại năng lượng tích cực và nguyên nhân nào gây stress lặp lại.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 4. BENEFIT-DRIVEN BENTO GRID (FEATURES AS OUTCOMES) */}
      <section id='features' className='w-full py-16 sm:py-24 px-4 sm:px-6 lg:px-8 bg-paper-warm border-b-2 border-on-background scroll-mt-20'>
        <div className='max-w-7xl mx-auto flex flex-col gap-10'>
          <div className='flex flex-col sm:flex-row sm:items-end justify-between gap-4'>
            <div>
              <span className='font-space text-xs font-extrabold uppercase tracking-widest text-primary'>Bộ Công Cụ Toàn Diện</span>
              <h2 className='font-space text-3xl sm:text-4xl font-extrabold text-on-surface mt-1'>
                Những kết quả cụ thể bạn nhận được mỗi ngày
              </h2>
            </div>
            <Link href='/journal-editor'>
              <NeoButton variant='paper' size='sm' className='font-space font-bold'>
                Trải nghiệm ngay →
              </NeoButton>
            </Link>
          </div>

          <div className='grid grid-cols-1 lg:grid-cols-12 gap-6'>
            {/* Bento Item 1: Viết dễ hơn (Span 7) */}
            <BentoCard
              className='lg:col-span-7'
              washiTape={{ color: 'lime', position: 'top-right', rotate: -2 }}
            >
              <div>
                <div className='flex items-center gap-2 mb-3'>
                  <span className='px-3 py-1 bg-primary-container font-space text-xs font-extrabold border-neo-sm rounded-md shadow-neo-sm'>
                    TRÌNH SOẠN THẢO KHÔNG RÀO CẢN
                  </span>
                </div>
                <h3 className='font-space text-2xl font-bold text-on-surface mb-2'>
                  Viết dễ hơn ngay cả khi bạn không biết bắt đầu từ đâu
                </h3>
                <p className='font-sans text-base text-on-surface-variant max-w-xl leading-relaxed'>
                  Không còn cảm giác sợ trang giấy trắng. Bạn chỉ cần gõ vài dòng ngắn, AI sẽ tự động phân tích phổ cảm xúc và giúp bạn mở khóa mạch suy nghĩ.
                </p>
              </div>

              <div className='mt-6 bg-white border-neo-sm rounded-2xl p-4 shadow-neo-sm'>
                <div className='flex items-center justify-between pb-3 border-b border-border-soft'>
                  <span className='font-space text-xs font-bold text-gray-500'>Trích đoạn nhật ký mẫu</span>
                  <span className='font-space text-xs font-bold text-primary'>Chỉ số tích cực 92%</span>
                </div>
                <p className='font-sans text-sm text-on-surface mt-3 italic leading-relaxed'>
                  &ldquo;Chiều nay sau khi hoàn thành buổi báo cáo, mình đi dạo một vòng quanh bờ hồ. Cảm giác nhẹ nhõm như vừa gỡ bỏ một tảng đá trong lòng...&rdquo;
                </p>
                <div className='flex flex-wrap gap-2 mt-4'>
                  <span className='text-xs font-space font-bold bg-paper-warm px-2.5 py-1 border border-black rounded-md'>#NhẹNhõm</span>
                  <span className='text-xs font-space font-bold bg-paper-warm px-2.5 py-1 border border-black rounded-md'>#HoànThànhMụcTiêu</span>
                </div>
              </div>
            </BentoCard>

            {/* Bento Item 2: Xây dựng thói quen & Sức bật (Span 5) */}
            <BentoCard
              className='lg:col-span-5'
              bgVariant='purple'
              washiTape={{ color: 'peach', position: 'top-left', rotate: 3 }}
            >
              <div>
                <span className='px-3 py-1 bg-white font-space text-xs font-extrabold border-neo-sm rounded-md shadow-neo-sm inline-block mb-3'>
                  THEO DÕI SỰ BỀN BỈ
                </span>
                <h3 className='font-space text-2xl font-bold text-black mb-2'>
                  Thấy rõ sức bật tinh thần qua từng tuần
                </h3>
                <p className='font-sans text-base text-black/85 leading-relaxed'>
                  Nuôi dưỡng thói quen tự quan sát với chỉ số kiên cường và chuỗi streak đều đặn mà không tạo áp lực.
                </p>
              </div>

              <div className='mt-6 bg-white border-neo-sm rounded-2xl p-4 shadow-neo-sm flex items-center justify-around text-center'>
                <div>
                  <div className='font-space text-3xl font-extrabold text-black'>14</div>
                  <div className='font-space text-xs font-bold text-gray-600 flex items-center justify-center gap-1 mt-0.5'>
                    <span>Ngày Streak</span>
                    <Flame className='w-3.5 h-3.5 text-amber-500 fill-amber-400 stroke-[2.3]' />
                  </div>
                </div>
                <div className='w-px h-10 bg-black/20'></div>
                <div>
                  <div className='font-space text-3xl font-extrabold text-black'>28</div>
                  <div className='font-space text-xs font-bold text-gray-600 mt-0.5'>Trang Đã Viết</div>
                </div>
                <div className='w-px h-10 bg-black/20'></div>
                <div>
                  <div className='font-space text-3xl font-extrabold text-black'>85%</div>
                  <div className='font-space text-xs font-bold text-gray-600 mt-0.5'>Tích Cực</div>
                </div>
              </div>
            </BentoCard>

            {/* Bento Item 3: Nhìn thấy cảm xúc thay đổi theo thời gian (Span 5) */}
            <BentoCard
              className='lg:col-span-5'
              bgVariant='yellow'
            >
              <div>
                <span className='px-3 py-1 bg-white font-space text-xs font-extrabold border-neo-sm rounded-md shadow-neo-sm inline-block mb-3'>
                  DÒNG THỜI GIAN KÝ ỨC
                </span>
                <h3 className='font-space text-2xl font-bold text-black mb-2'>
                  Nhìn thấy cảm xúc biến chuyển qua thời gian
                </h3>
                <p className='font-sans text-base text-black/85 leading-relaxed'>
                  Không chỉ là danh sách bài viết khô khan. Lưới lịch trực quan cho bạn biết tâm trạng đã vượt qua những nốt trầm như thế nào.
                </p>
              </div>
              <div className='mt-6 pt-4 border-t-2 border-black/10 flex items-center justify-between text-sm font-space font-bold'>
                <span>Lọc nhanh theo 5 sắc thái tâm trạng</span>
                <Link href='/history-calendar' className='underline hover:text-black'>Xem lịch mẫu →</Link>
              </div>
            </BentoCard>

            {/* Bento Item 4: Nhận ra khuôn mẫu lặp lại (Span 7) */}
            <BentoCard
              className='lg:col-span-7'
              washiTape={{ color: 'blue', position: 'top-left', rotate: -1 }}
            >
              <div>
                <div className='flex items-center gap-2 mb-3'>
                  <span className='px-3 py-1 bg-paper-warm font-space text-xs font-extrabold border-neo-sm rounded-md shadow-neo-sm'>
                    AI PSYCHOLOGICAL INSIGHTS
                  </span>
                </div>
                <h3 className='font-space text-2xl font-bold text-on-surface mb-2'>
                  Nhận ra khuôn mẫu lặp lại trong suy nghĩ
                </h3>
                <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                  Tìm ra những yếu tố vô hình kích hoạt năng lượng tốt hoặc kéo tụt tinh thần của bạn, kèm gợi ý hành động chăm sóc bản thân cụ thể.
                </p>
              </div>

              <div className='mt-6 bg-paper-warm/90 border-neo-sm rounded-2xl p-4 flex items-start sm:items-center gap-4 shadow-neo-sm'>
                <div className='w-10 h-10 rounded-xl bg-primary-container border-neo-sm flex items-center justify-center shrink-0 mt-0.5 sm:mt-0'>
                  <Lightbulb className='w-5 h-5 text-amber-700 fill-amber-300 stroke-[2.3]' />
                </div>
                <p className='font-sans text-sm sm:text-base text-on-surface leading-relaxed'>
                  <strong>Gợi ý tuần này:</strong> Mức năng lượng của bạn tăng 25% vào những ngày có vận động ngoài trời. Hãy duy trì đi dạo 15 phút mỗi chiều!
                </p>
              </div>
            </BentoCard>
          </div>
        </div>
      </section>

      {/* 5. NEW SECTION: PRIVACY & TRUST (QUYỀN RIÊNG TƯ & SỰ AN TÂM) */}
      <section id='privacy' className='w-full py-16 sm:py-20 px-4 sm:px-6 lg:px-8 bg-bg-canvas border-b-2 border-on-background scroll-mt-20'>
        <div className='max-w-6xl mx-auto flex flex-col gap-10'>
          <div className='text-center max-w-3xl mx-auto'>
            <StickerBadge label='AN TOÀN & MINH BẠCH' variant='lime' rotate={-1} />
            <h2 className='font-space text-3xl sm:text-4xl font-extrabold text-on-surface mt-3'>
              Quyền riêng tư là nền tảng số 1 của chúng tôi
            </h2>
            <p className='font-sans text-base text-on-surface-variant mt-2'>
              Nhật ký là nơi riêng tư nhất của tâm hồn. MyLog được xây dựng dựa trên sự tôn trọng tuyệt đối dữ liệu của bạn.
            </p>
          </div>

          <div className='grid grid-cols-1 md:grid-cols-3 gap-6 sm:gap-8'>
            {/* Pillar 1 */}
            <div className='bg-surface-card border-neo rounded-2xl p-6 sm:p-7 shadow-neo flex flex-col gap-4'>
              <div className='w-11 h-11 rounded-xl bg-secondary-container border-neo-sm flex items-center justify-center shadow-neo-sm'>
                <Lock className='w-5 h-5 text-black stroke-[2.2]' />
              </div>
              <h3 className='font-space text-xl font-bold text-on-surface'>Nhật Ký Là Của Riêng Bạn</h3>
              <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                Toàn bộ dữ liệu bài viết được bảo vệ an toàn. Bạn hoàn toàn làm chủ dữ liệu của mình và có thể xem, chỉnh sửa hoặc xóa vĩnh viễn bất kỳ lúc nào.
              </p>
            </div>

            {/* Pillar 2 */}
            <div className='bg-surface-card border-neo rounded-2xl p-6 sm:p-7 shadow-neo flex flex-col gap-4'>
              <div className='w-11 h-11 rounded-xl bg-primary-container border-neo-sm flex items-center justify-center shadow-neo-sm'>
                <Sliders className='w-5 h-5 text-black stroke-[2.2]' />
              </div>
              <h3 className='font-space text-xl font-bold text-on-surface'>Bạn Toàn Quyền Quyết Định</h3>
              <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                AI chỉ đóng vai trò gợi ý. Nếu AI nhận diện chưa đúng cảm xúc hay chủ đề, bạn luôn có quyền chỉnh sửa lại phổ cảm xúc và tag theo ý mình.
              </p>
            </div>

            {/* Pillar 3 */}
            <div className='bg-surface-card border-neo rounded-2xl p-6 sm:p-7 shadow-neo flex flex-col gap-4'>
              <div className='w-11 h-11 rounded-xl bg-tertiary-container border-neo-sm flex items-center justify-center shadow-neo-sm'>
                <ShieldCheck className='w-5 h-5 text-black stroke-[2.2]' />
              </div>
              <h3 className='font-space text-xl font-bold text-on-surface'>Tự Phản Tư, Không Y Khoa</h3>
              <p className='font-sans text-base text-on-surface-variant leading-relaxed'>
                MyLog là người bạn đồng hành hỗ trợ tự nhận thức, không thay thế cho các liệu pháp hay chẩn đoán y khoa từ chuyên gia tâm lý có chứng chỉ.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 6. TESTIMONIALS (IMPROVED AUTHENTICITY & READABILITY) */}
      <section id='reviews' className='w-full py-16 sm:py-20 px-4 sm:px-6 lg:px-8 bg-paper-warm border-b-2 border-on-background scroll-mt-20'>
        <div className='max-w-6xl mx-auto flex flex-col gap-10'>
          <div className='text-center'>
            <StickerBadge label='TIẾNG NÓI ĐỒNG ĐIỆU' variant='pink' rotate={-2} />
            <h2 className='font-space text-3xl sm:text-4xl font-extrabold text-on-surface mt-3'>
              Được tin tưởng bởi những người muốn hiểu rõ bản thân
            </h2>
          </div>

          <div className='grid grid-cols-1 md:grid-cols-3 gap-6'>
            {/* Note 1 */}
            <div className='bg-surface-card border-neo rounded-2xl p-6 sm:p-7 shadow-neo rotate-[-1deg] relative flex flex-col justify-between hover:rotate-0 transition-transform'>
              <WashiTape color='lime' rotate={-3} className='absolute -top-3 left-8 w-24' />
              <p className='font-sans text-base text-on-surface italic leading-relaxed pt-2'>
                &ldquo;Trước đây mình hay bỏ dở việc viết nhật ký vì sợ phải viết dài. Với MyLog, mình chỉ cần gõ 3 dòng trước khi ngủ, AI phản hồi rất ấm áp và tinh tế.&rdquo;
              </p>
              <div className='mt-6 pt-4 border-t border-border-soft flex items-center gap-3'>
                <div className='w-9 h-9 rounded-full bg-primary-container border-neo-sm flex items-center justify-center font-space font-bold text-xs'>
                  MA
                </div>
                <div>
                  <div className='font-space text-sm font-bold text-on-surface'>Minh Anh</div>
                  <div className='font-sans text-xs text-gray-600'>Sinh viên Kiến Trúc • 14 ngày streak</div>
                </div>
              </div>
            </div>

            {/* Note 2 */}
            <div className='bg-surface-card border-neo rounded-2xl p-6 sm:p-7 shadow-neo rotate-[1.5deg] relative flex flex-col justify-between hover:rotate-0 transition-transform'>
              <WashiTape color='peach' rotate={2} className='absolute -top-3 right-8 w-24' />
              <p className='font-sans text-base text-on-surface italic leading-relaxed pt-2'>
                &ldquo;Nhờ bảng tổng hợp cảm xúc tuần, mình mới giật mình nhận ra cứ hôm nào làm việc qua 9h tối là ngày hôm sau tâm trạng xuống dốc. Nhận thức này đã giúp mình biết cách thiết lập ranh giới tốt hơn.&rdquo;
              </p>
              <div className='mt-6 pt-4 border-t border-border-soft flex items-center gap-3'>
                <div className='w-9 h-9 rounded-full bg-secondary-container border-neo-sm flex items-center justify-center font-space font-bold text-xs'>
                  HL
                </div>
                <div>
                  <div className='font-space text-sm font-bold text-on-surface'>Hoàng Long</div>
                  <div className='font-sans text-xs text-gray-600'>Product Designer • 28 ngày streak</div>
                </div>
              </div>
            </div>

            {/* Note 3 */}
            <div className='bg-surface-card border-neo rounded-2xl p-6 sm:p-7 shadow-neo rotate-[-1.5deg] relative flex flex-col justify-between hover:rotate-0 transition-transform'>
              <WashiTape color='blue' rotate={-2} className='absolute -top-3 left-10 w-24' />
              <p className='font-sans text-base text-on-surface italic leading-relaxed pt-2'>
                &ldquo;Giao diện scrapbook cực kỳ có hồn! Cảm giác dán ảnh polaroid và chọn sticker làm mình thấy như đang ngồi trước cuốn sổ thật sự sau một ngày bận rộn trên máy tính.&rdquo;
              </p>
              <div className='mt-6 pt-4 border-t border-border-soft flex items-center gap-3'>
                <div className='w-9 h-9 rounded-full bg-tertiary-container border-neo-sm flex items-center justify-center font-space font-bold text-xs'>
                  TH
                </div>
                <div>
                  <div className='font-space text-sm font-bold text-on-surface'>Thu Hương</div>
                  <div className='font-sans text-xs text-gray-600'>Content Creator • 35 ngày streak</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 7. FINAL ACTION CTA (LIME DOMINANT & REASSURING) */}
      <section className='w-full py-16 sm:py-24 px-4 sm:px-6 lg:px-8 bg-bg-canvas border-t-2 border-on-background'>
        <div className='max-w-4xl mx-auto bg-primary-container/70 border-neo rounded-3xl p-8 sm:p-14 shadow-neo-lg text-center flex flex-col items-center gap-6 relative overflow-visible'>
          <WashiTape color='lavender' rotate={-3} className='absolute -top-3 left-16 w-36 z-10' />

          <h2 className='font-space text-3xl sm:text-5xl font-extrabold text-black tracking-tight leading-tight max-w-2xl'>
            Bắt đầu trang nhật ký đầu tiên của bạn hôm nay
          </h2>
          <p className='font-sans text-base sm:text-lg text-black/85 max-w-xl leading-relaxed'>
            Không cần biết phải viết gì — MyLog sẽ giúp bạn bắt đầu từ những dòng suy nghĩ mộc mạc nhất.
          </p>

          <Link href='/journal-editor'>
            <NeoButton
              variant='dark'
              size='lg'
              className='font-extrabold text-base sm:text-lg px-8 sm:px-10 py-4 shadow-neo'
            >
              BẮT ĐẦU VIẾT NGAY
            </NeoButton>
          </Link>
        </div>
      </section>

      {/* 8. NEO-BRUTALIST FOOTER */}
      <footer className='w-full bg-surface-card border-t-2 border-on-background py-12 px-4 sm:px-6 lg:px-8'>
        <div className='max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-6'>
          <div className='flex items-center gap-3'>
            <Link href='/' className='flex items-center gap-3 group'>
              <div className='relative flex items-center justify-center h-10 sm:h-11 w-auto max-w-[140px] overflow-hidden'>
                <Image
                  src='/logo.png'
                  alt='MyLog Logo'
                  width={140}
                  height={44}
                  priority
                  className='h-10 sm:h-11 w-auto object-contain transition-transform duration-150 group-hover:scale-105'
                />
              </div>
            </Link>
            <span className='text-xs sm:text-sm text-gray-500 font-space ml-2'>
              © 2026 MyLog. All memories reserved.
            </span>
          </div>

          <div className='flex items-center gap-6 font-space text-xs sm:text-sm font-bold text-gray-600'>
            <a href='#how-it-works' className='hover:text-black transition-colors'>Cách hoạt động</a>
            <a href='#features' className='hover:text-black transition-colors'>Tính năng</a>
            <a href='#privacy' className='hover:text-black transition-colors'>Riêng tư</a>
            <Link href='/journal-editor' className='text-primary hover:underline'>Mở sổ tay</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
