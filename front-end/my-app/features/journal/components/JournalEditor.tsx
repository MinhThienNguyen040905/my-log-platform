'use client';

import React, { useState, useEffect, useRef, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { useJournal } from '@/lib/journal-context';
import { useToast } from '@/lib/toast-context';
import { MoodType, AIAnalysisResult, EMOTION_TAXONOMY, JournalStatus } from '@/lib/types';
import { detectCrisisKeywords } from '@/lib/safety-checker';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';
import { SafetyModal } from '@/components/ui/SafetyModal';
import { AiReflectionDrawer } from './AiReflectionDrawer';
import { ConfirmReflectModal } from './ConfirmReflectModal';
import { PhotoPreviewModal } from './PhotoPreviewModal';
import {
  ArrowLeft,
  Sparkles,
  Save,
  Sliders,
  Check,
  Calendar,
  MapPin,
  Image as ImageIcon,
  CheckCircle2,
  Lightbulb,
  ChevronDown,
  Paperclip,
  Zap,
  BatteryCharging,
  Moon,
  Plus,
  Frown,
  Annoyed,
  Meh,
  Smile,
  Laugh,
} from 'lucide-react';

export function JournalEditor() {
  return (
    <Suspense
      fallback={
        <div className="p-12 text-center font-space text-sm font-bold">
          Đang mở trang sổ tay của bạn...
        </div>
      }
    >
      <JournalEditorContent />
    </Suspense>
  );
}

const MOOD_ICONS: {
  score: number;
  mood: MoodType;
  icon: React.ElementType;
  label: string;
  bg: string;
}[] = [
  { score: 2.0, mood: 'anxiety-stress', icon: Frown, label: 'Mệt mỏi / Trầm', bg: 'hover:bg-red-100' },
  { score: 4.5, mood: 'anxiety-stress', icon: Annoyed, label: 'Áp lực / Bối rối', bg: 'hover:bg-amber-100' },
  { score: 6.0, mood: 'neutral', icon: Meh, label: 'Bình thường', bg: 'hover:bg-gray-100' },
  { score: 8.0, mood: 'calm-joy', icon: Smile, label: 'Thư thái / Tốt', bg: 'hover:bg-lime-100' },
  { score: 9.5, mood: 'hope-energy', icon: Laugh, label: 'Hào hứng / Tuyệt vời', bg: 'hover:bg-yellow-100' },
];

function JournalEditorContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const editId = searchParams.get('id');
  const { addEntry, updateEntry, getEntryById } = useJournal();
  const { showToast } = useToast();

  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const [isEditMode, setIsEditMode] = useState(false);

  // Core Journal State
  const [title, setTitle] = useState('Một buổi chiều hoàn thành đề cương đồ án');
  const [content, setContent] = useState(
    'Hôm nay mình bảo vệ xong đề cương đồ án tốt nghiệp. Lúc đầu rất hồi hộp khi trình bày pipeline RAG & Safety, nhưng thầy đánh giá cao định vị self-reflection. Thở phào nhẹ nhõm! Tối nay thưởng ly cà phê muối bên hồ Tây...'
  );
  const [mood, setMood] = useState<MoodType>('calm-joy');
  const [moodScore, setMoodScore] = useState(8.0);
  const [stressScore, setStressScore] = useState(3.0);
  const [energyScore, setEnergyScore] = useState(7.5);
  const [sleepHours, setSleepHours] = useState(7.5);
  const [showDetailedMetrics, setShowDetailedMetrics] = useState(false);

  // Topics & Tags
  const [topics, setTopics] = useState<string[]>(['đồ án tốt nghiệp', 'thầy hướng dẫn', 'hồ Tây', 'cà phê muối']);

  // Photo attachment (Compact while writing)
  const [photoUrl, setPhotoUrl] = useState(
    'https://images.unsplash.com/photo-1517842645767-c639042777db?w=600&auto=format&fit=crop&q=80'
  );
  const [photoCaption, setPhotoCaption] = useState('Góc quán quen bên hồ Tây chiều thu');
  const [showPhotoUpload, setShowPhotoUpload] = useState(false);
  const [showPhotoPreview, setShowPhotoPreview] = useState(false);

  // Writing Prompts
  const [showPromptModal, setShowPromptModal] = useState(false);

  // AI Drawer & State
  const [isAiDrawerOpen, setIsAiDrawerOpen] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  // Prompt to reflect before saving modal
  const [showConfirmReflectModal, setShowConfirmReflectModal] = useState(false);

  // Safety Crisis Modal
  const [isSafetyModalOpen, setIsSafetyModalOpen] = useState(false);
  const [detectedCrisisKeywords, setDetectedCrisisKeywords] = useState<string[]>([]);

  // Autosave timestamp
  const [lastSavedTime, setLastSavedTime] = useState('16:42');

  const [aiResult, setAiResult] = useState<AIAnalysisResult | null>({
    sentiment: 'Tích cực & Nhẹ nhõm',
    summary: 'Có vẻ hôm nay bạn vừa nhẹ nhõm vừa tự hào sau khi vượt qua một cột mốc quan trọng.',
    reflectionPrompt: 'Khi nhìn lại sự chuẩn bị trong 2 tuần qua, điều gì bạn thấy mình đã vượt qua tốt hơn mong đợi?',
    reflectionQuestions: [
      'Khoảnh khắc nào trong buổi bảo vệ khiến bạn cảm thấy tự tin và vững vàng nhất?',
      'Sau khi buông bỏ được áp lực deadline này, bạn muốn dành cuối tuần để tái tạo năng lượng thế nào?',
      'Bạn muốn ghi nhận và cảm ơn bản thân về điều gì nhất trong ngày hôm nay?',
    ],
    emotions: [
      { label: 'Bình an & Thư thái', percentage: 55, color: '#70E000', description: 'Cảm giác giải tỏa áp lực', emotionType: 'CALM' },
      { label: 'Tự hào & Hy vọng', percentage: 35, color: '#FFD166', description: 'Niềm vui sau cột mốc tốt nghiệp', emotionType: 'HOPE' },
      { label: 'Hồi hộp còn sót lại', percentage: 10, color: '#FF6B6B', description: 'Dư âm căng thẳng trước phòng thi', emotionType: 'ANXIETY' },
    ],
    entities: [
      { name: 'đề cương tốt nghiệp', category: 'công việc', sentiment: 'positive' },
      { name: 'hồ Tây', category: 'địa điểm', sentiment: 'positive' },
    ],
    mindfulAction: 'Tối nay, hãy tự thưởng cho bản thân 1 tiếng thư thái bên hồ Tây trọn vẹn mà không nghĩ về việc tốt nghiệp nữa nhé.',
    topics: ['đồ án', 'hồ Tây', 'tự hào'],
    riskLevel: 'NORMAL',
    userCorrected: false,
    status: 'ANALYZED',
  });

  const writingPrompts = [
    { category: 'Thấu cảm', prompt: 'Hôm nay điều gì đã làm tiêu tốn nhiều năng lượng tinh thần của bạn nhất?' },
    { category: 'Biết ơn', prompt: 'Một khoảnh khắc nhỏ nào trong ngày hôm nay khiến bạn mỉm cười nhẹ nhõm?' },
    { category: 'Tự vấn', prompt: 'Nếu được nhắn nhủ một câu dịu dàng với chính mình lúc này, bạn sẽ nói gì?' },
    { category: 'Giải tỏa', prompt: 'Có suy nghĩ nào đang luẩn quẩn trong đầu mà bạn muốn trút cạn lên trang giấy?' },
    { category: 'Tự hào', prompt: 'Bạn cảm thấy trân trọng nỗ lực nào của bản thân sau những ngày vừa qua?' },
  ];

  // Load existing entry for editing
  useEffect(() => {
    if (editId) {
      const existing = getEntryById(editId);
      if (existing) {
        setIsEditMode(true);
        setTitle(existing.title);
        setContent(existing.content);
        setMood(existing.mood);
        setMoodScore(existing.moodScore);
        setStressScore(existing.stressScore);
        setEnergyScore(existing.energyScore);
        setSleepHours(existing.sleepHours);
        setTopics(existing.tags || []);
        if (existing.photoUrl) setPhotoUrl(existing.photoUrl);
        if (existing.photoCaption) setPhotoCaption(existing.photoCaption);
        if (existing.aiAnalysis) setAiResult(existing.aiAnalysis);
      }
    } else {
      try {
        const draft = localStorage.getItem('mylog_draft_journal');
        if (draft) {
          const parsed = JSON.parse(draft);
          if (parsed.title) setTitle(parsed.title);
          if (parsed.content) setContent(parsed.content);
          if (parsed.moodScore) setMoodScore(parsed.moodScore);
          if (parsed.stressScore) setStressScore(parsed.stressScore);
          if (parsed.energyScore) setEnergyScore(parsed.energyScore);
          if (parsed.sleepHours) setSleepHours(parsed.sleepHours);
          if (parsed.topics) setTopics(parsed.topics);
        }
      } catch (e) {
        console.error('Draft load error:', e);
      }
    }
  }, [editId, getEntryById]);

  // Local Autosave (quiet)
  useEffect(() => {
    if (isEditMode) return;
    const timer = setTimeout(() => {
      try {
        localStorage.setItem(
          'mylog_draft_journal',
          JSON.stringify({ title, content, moodScore, stressScore, energyScore, sleepHours, topics })
        );
        const now = new Date();
        setLastSavedTime(
          `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
        );
      } catch (e) {
        console.error('Draft save error:', e);
      }
    }, 1500);

    return () => clearTimeout(timer);
  }, [title, content, moodScore, stressScore, energyScore, sleepHours, topics, isEditMode]);

  // Check crisis patterns using safety-checker
  const checkSafetyRisk = (text: string) => {
    const found = detectCrisisKeywords(text);
    if (found.length > 0) {
      setDetectedCrisisKeywords(found);
      setIsSafetyModalOpen(true);
      return true;
    }
    return false;
  };

  // Trigger AI reflection on-demand
  const handleOpenAiDrawer = () => {
    if (!content.trim()) {
      showToast({
        title: 'Trang sổ còn trống!',
        message: 'Hãy viết vài dòng tâm sự trước khi để MyLog cùng phản chiếu nhé.',
        type: 'info',
      });
      return;
    }

    if (checkSafetyRisk(content)) return;

    setIsAiDrawerOpen(true);
    setIsAnalyzing(true);

    setTimeout(() => {
      setIsAnalyzing(false);
      setAiResult({
        sentiment: moodScore >= 7 ? 'Tích cực & Nhẹ nhõm' : 'Suy tư & Tự vấn sâu',
        summary: `Có vẻ hôm nay bạn đang cảm thấy ${
          moodScore >= 8 ? 'nhẹ nhõm, tự hào và tràn đầy hy vọng' : 'cần một khoảng lặng để cân bằng lại nội tâm'
        }.`,
        reflectionPrompt: 'Khi nhìn lại ngày hôm nay, điều gì khiến bạn thấy trân quý nhất?',
        reflectionQuestions: [
          'Khoảnh khắc nào trong ngày khiến bạn cảm thấy tự tin và nhẹ nhõm nhất?',
          'Sau khi trải qua những giờ bận rộn, bạn muốn dành cho bản thân sự chăm sóc nào tối nay?',
          'Bạn muốn nhắn gửi một lời động viên gì đến chính mình lúc này?',
        ],
        emotions: [
          { label: 'Bình an & Thư thái', percentage: Math.min(85, Math.round(moodScore * 9)), color: EMOTION_TAXONOMY.CALM.color, description: 'Cảm giác giải tỏa áp lực', emotionType: 'CALM' },
          { label: 'Hy vọng & Tự hào', percentage: Math.min(60, Math.round(energyScore * 8)), color: EMOTION_TAXONOMY.HOPE.color, description: 'Sẵn sàng hướng về phía trước', emotionType: 'HOPE' },
          { label: 'Áp lực còn sót lại', percentage: Math.min(50, Math.round(stressScore * 8)), color: EMOTION_TAXONOMY.ANXIETY.color, description: 'Dư âm công việc', emotionType: 'ANXIETY' },
        ],
        entities: [{ name: topics[0] || 'mục tiêu', category: 'công việc', sentiment: 'positive' }],
        mindfulAction: 'Thử dành 15 phút nghe một bản nhạc êm dịu, thả lỏng bờ vai và không nhìn màn hình.',
        suggestedAction: 'Dành 20 phút đi dạo thư thái quanh quán quen và ngủ một giấc thật sâu không đặt báo thức.',
        topics: topics,
        riskLevel: 'NORMAL',
        userCorrected: false,
        status: 'ANALYZED',
      });
    }, 650);
  };

  // Append question to editor to continue writing
  const handleAnswerQuestionInJournal = (q: string) => {
    setContent((prev) => `${prev.trim()}\n\n💭 ${q}\n`);
    showToast({
      title: 'Đã chèn câu hỏi vào sổ tay!',
      message: 'Bạn có thể tiếp tục viết dòng suy ngẫm của mình.',
      type: 'info',
    });
    setTimeout(() => {
      textareaRef.current?.focus();
    }, 100);
  };

  // Save entry
  const executeSave = () => {
    if (!content.trim()) {
      showToast({
        title: 'Chưa có nội dung!',
        message: 'Vui lòng viết vài dòng cảm nhận trước khi cất sổ nhé.',
        type: 'error',
      });
      return;
    }

    if (checkSafetyRisk(content)) return;

    const todayStr = new Date().toISOString().split('T')[0];
    const nowTimeStr = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    const journalStatus: JournalStatus = isEditMode
      ? 'ANALYSIS_OUTDATED'
      : (aiResult ? 'ANALYZED' : 'SAVED');

    if (isEditMode && editId) {
      updateEntry(editId, {
        title: title.trim() || 'Trang nhật ký không tên',
        content: content.trim(),
        mood: moodScore >= 7 ? 'calm-joy' : stressScore >= 6 ? 'anxiety-stress' : 'neutral',
        moodScore,
        stressScore,
        energyScore,
        sleepHours,
        tags: topics,
        photoUrl: photoUrl || undefined,
        photoCaption: photoCaption || undefined,
        status: journalStatus,
        aiAnalysis: aiResult ? { ...aiResult, status: journalStatus } : undefined,
      });

      showToast({
        title: 'Đã cập nhật bài viết thành công!',
        message: 'Trang nhật ký đã được cất vào Lịch ký ức.',
        type: 'success',
      });
    } else {
      addEntry({
        title: title.trim() || 'Trang nhật ký không tên',
        content: content.trim(),
        date: todayStr,
        time: nowTimeStr,
        mood: moodScore >= 7 ? 'calm-joy' : stressScore >= 6 ? 'anxiety-stress' : 'neutral',
        moodScore,
        stressScore,
        energyScore,
        sleepHours,
        tags: topics,
        photoUrl: photoUrl || undefined,
        photoCaption: photoCaption || undefined,
        location: 'Hà Nội, Việt Nam',
        status: journalStatus,
        aiAnalysis: aiResult ? { ...aiResult, status: journalStatus } : undefined,
      });

      showToast({
        title: 'Đã lưu trang nhật ký thành công!',
        message: 'Những dòng suy tư của bạn đã được lưu giữ an toàn.',
        type: 'success',
      });

      localStorage.removeItem('mylog_draft_journal');
    }

    setTimeout(() => {
      router.push('/history-calendar');
    }, 600);
  };

  const handleSaveClick = () => {
    if (!isAiDrawerOpen && !isEditMode) {
      setShowConfirmReflectModal(true);
    } else {
      executeSave();
    }
  };

  const wordCount = content.trim() ? content.trim().split(/\s+/).length : 0;

  return (
    <div className="w-full flex flex-col bg-bg-canvas min-h-[calc(100vh-80px)] pb-16 selection:bg-brand-lime selection:text-black overflow-x-hidden relative">
      {/* Top Quiet Meta Bar */}
      <section className="w-full max-w-4xl mx-auto px-4 sm:px-6 pt-4 pb-2 flex items-center justify-between text-xs font-space font-bold text-gray-600">
        <div className="flex items-center gap-2">
          <Link
            href="/history-calendar"
            className="inline-flex items-center gap-1 text-gray-700 hover:text-black hover:underline"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Xem lịch ký ức</span>
          </Link>
          {isEditMode && (
            <span className="px-2 py-0.5 bg-amber-200 border border-black rounded text-[11px] font-extrabold uppercase">
              Chế độ sửa bài
            </span>
          )}
        </div>

        <div className="flex items-center gap-3">
          <span className="text-gray-500">
            {wordCount} từ
          </span>
          <span className="inline-flex items-center gap-1 text-green-700 font-bold bg-white px-2.5 py-1 rounded-full border border-black/30 shadow-neo-sm">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Đã lưu lúc {lastSavedTime}
          </span>
        </div>
      </section>

      {/* Main Screen Layout Container */}
      <main className="w-full max-w-7xl mx-auto px-4 sm:px-6 py-2 flex items-start justify-center transition-all duration-300">
        {/* PHYSICAL NOTEBOOK DESK CANVAS (Max-w 850px centered) */}
        <div
          className={`w-full transition-all duration-300 ${
            isAiDrawerOpen ? 'lg:max-w-3xl lg:mr-[420px]' : 'max-w-[850px]'
          }`}
        >
          <article className="relative bg-paper-warm border-[2.5px] border-black rounded-3xl p-6 sm:p-10 shadow-neo-lg transition-all overflow-visible flex flex-col gap-6">
            {/* Washi Tape and Page Number Stamp */}
            <WashiTape color="lime" rotate={-2} className="absolute -top-3.5 left-12 w-32 z-10" />
            <div className="absolute -top-3 right-10 z-10 bg-white border border-black px-2.5 py-0.5 rounded-full font-space text-[10px] font-extrabold shadow-neo-sm uppercase">
              Trang riêng tư
            </div>

            {/* Notebook Header */}
            <header className="flex flex-col gap-3 pb-4 border-b-2 border-black/15">
              <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-space font-bold text-gray-700">
                <span className="inline-flex items-center gap-1.5 bg-white px-3 py-1 rounded-xl border border-black shadow-neo-sm">
                  <Calendar className="w-3.5 h-3.5 text-black" />
                  Thứ Tư, 15 Tháng 10, 2026
                </span>
                <span className="inline-flex items-center gap-1.5 bg-white px-3 py-1 rounded-xl border border-black/40">
                  <MapPin className="w-3.5 h-3.5 text-gray-500" />
                  Hà Nội, 24°C
                </span>
              </div>

              {/* Title Input */}
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Đặt một tựa đề cho hôm nay..."
                className="w-full bg-transparent font-space text-xl sm:text-2xl font-bold text-on-surface focus:outline-none placeholder:text-gray-400 py-1 border-b border-transparent focus:border-black/30 transition-all"
              />

              {/* Tags / Topics */}
              <div className="flex flex-wrap items-center gap-2 pt-1">
                {topics.map((tag, idx) => (
                  <span
                    key={idx}
                    className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-white rounded-lg border border-black text-xs font-space font-bold shadow-neo-sm"
                  >
                    #{tag}
                    <button
                      type="button"
                      onClick={() => setTopics(topics.filter((_, i) => i !== idx))}
                      className="text-gray-400 hover:text-black ml-1 text-xs"
                    >
                      ×
                    </button>
                  </span>
                ))}
                <button
                  type="button"
                  onClick={() => {
                    const nextTag = prompt('Nhập chủ đề hoặc từ khóa mới:');
                    if (nextTag && nextTag.trim() && !topics.includes(nextTag.trim())) {
                      setTopics([...topics, nextTag.trim()]);
                    }
                  }}
                  className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-brand-lime/30 hover:bg-brand-lime rounded-lg border border-black text-xs font-space font-bold transition-all cursor-pointer"
                >
                  <Plus className="w-3 h-3" />
                  <span>Chủ đề</span>
                </button>
              </div>
            </header>

            {/* Prompt Helper Bar */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setShowPromptModal(!showPromptModal)}
                className="inline-flex items-center gap-2 text-xs font-space font-bold text-gray-700 bg-white px-3 py-1.5 rounded-xl border border-black shadow-neo-sm hover:bg-lime-100 transition-all cursor-pointer"
              >
                <Lightbulb className="w-4 h-4 text-amber-500 fill-amber-300" />
                <span>💡 Gợi ý chủ đề viết hôm nay</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform ${showPromptModal ? 'rotate-180' : ''}`} />
              </button>

              {showPromptModal && (
                <div className="absolute top-10 left-0 z-30 w-full sm:w-[420px] bg-white border-2 border-black rounded-2xl p-3 shadow-neo-lg animate-in fade-in">
                  <div className="flex items-center justify-between pb-2 mb-2 border-b border-black/10">
                    <span className="font-space text-xs font-bold text-gray-800">
                      Chọn câu hỏi để bắt đầu dòng chảy:
                    </span>
                    <button
                      type="button"
                      onClick={() => setShowPromptModal(false)}
                      className="text-xs font-bold text-gray-500 hover:text-black"
                    >
                      Đóng
                    </button>
                  </div>
                  <div className="flex flex-col gap-1.5 max-h-56 overflow-y-auto">
                    {writingPrompts.map((item, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => {
                          setContent((prev) => (prev ? `${prev}\n\n[${item.prompt}]\n` : `${item.prompt}\n\n`));
                          setShowPromptModal(false);
                          showToast({ title: 'Đã thêm gợi ý vào trang viết!', type: 'info' });
                        }}
                        className="text-left p-2 rounded-xl hover:bg-paper-warm border border-transparent hover:border-black transition-all cursor-pointer flex flex-col gap-0.5"
                      >
                        <span className="font-space text-[10px] font-extrabold uppercase text-purple-700">
                          {item.category}
                        </span>
                        <span className="font-serif text-xs text-gray-800">
                          {item.prompt}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Writing Area: Tactile Lined Paper */}
            <div className="relative">
              <textarea
                ref={textareaRef}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={12}
                placeholder="Hôm nay điều gì làm bạn bận tâm hay mỉm cười? Hãy trút cạn suy nghĩ lên trang giấy này..."
                className="w-full bg-transparent font-serif text-base sm:text-lg text-on-surface focus:outline-none resize-y leading-relaxed tracking-wide placeholder:text-gray-400 placeholder:italic p-1"
              />
            </div>

            {/* Bottom Toolbar: Quiet & Friendly */}
            <div className="pt-4 border-t-2 border-black/15 flex flex-col gap-4">
              {/* Mood 5-point Selector */}
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span className="font-space text-xs font-extrabold uppercase text-gray-600">
                    Hôm nay bạn thấy thế nào?
                  </span>
                  <div className="flex items-center gap-2 bg-white py-1 px-3 rounded-2xl border border-black shadow-neo-sm">
                    {MOOD_ICONS.map((item) => {
                      const isSelected = Math.abs(moodScore - item.score) < 1.0;
                      const IconComponent = item.icon;
                      return (
                        <button
                          key={item.score}
                          type="button"
                          onClick={() => {
                            setMoodScore(item.score);
                            setMood(item.mood);
                          }}
                          className={`w-8 h-8 sm:w-9 sm:h-9 rounded-xl flex items-center justify-center transition-all cursor-pointer ${
                            isSelected
                              ? 'bg-primary-container border border-black shadow-neo-sm scale-110 text-black'
                              : 'text-gray-600 opacity-75 hover:opacity-100 hover:text-black hover:bg-gray-100'
                          }`}
                          title={item.label}
                        >
                          <IconComponent className="w-5 h-5 stroke-[2.2]" />
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Optional Detailed Metrics & Photo toggles */}
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setShowDetailedMetrics(!showDetailedMetrics)}
                    className="text-xs font-space font-bold text-gray-700 hover:text-black underline flex items-center gap-1 cursor-pointer"
                  >
                    <Sliders className="w-3.5 h-3.5" />
                    <span>{showDetailedMetrics ? 'Ẩn chỉ số' : '+ Thêm chỉ số'}</span>
                  </button>

                  {/* Photo Attachment Tiny Badge */}
                  {photoUrl ? (
                    <button
                      type="button"
                      onClick={() => setShowPhotoPreview(true)}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-white hover:bg-lime-100 border border-black rounded-xl text-xs font-space font-bold shadow-neo-sm cursor-pointer"
                    >
                      <Paperclip className="w-3.5 h-3.5 text-purple-600" />
                      <span>1 ảnh kẹp</span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setShowPhotoUpload(!showPhotoUpload)}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-white hover:bg-gray-100 border border-black rounded-xl text-xs font-space font-bold shadow-neo-sm cursor-pointer"
                    >
                      <ImageIcon className="w-3.5 h-3.5 text-gray-600" />
                      <span>Kẹp ảnh</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Expandable Health Metrics (Quiet Sliders) */}
              {showDetailedMetrics && (
                <div className="p-4 bg-white rounded-2xl border border-black shadow-neo-sm grid grid-cols-1 sm:grid-cols-3 gap-4 animate-in fade-in">
                  <div>
                    <div className="flex justify-between text-xs font-space font-bold mb-1">
                      <span className="flex items-center gap-1 text-gray-700">
                        <Zap className="w-3.5 h-3.5 text-amber-500" /> Áp lực (Stress):
                      </span>
                      <span>{stressScore}/10</span>
                    </div>
                    <input
                      type="range"
                      min="1"
                      max="10"
                      step="0.5"
                      value={stressScore}
                      onChange={(e) => setStressScore(parseFloat(e.target.value))}
                      className="w-full accent-amber-500 cursor-pointer"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-xs font-space font-bold mb-1">
                      <span className="flex items-center gap-1 text-gray-700">
                        <BatteryCharging className="w-3.5 h-3.5 text-green-600" /> Năng lượng:
                      </span>
                      <span>{energyScore}/10</span>
                    </div>
                    <input
                      type="range"
                      min="1"
                      max="10"
                      step="0.5"
                      value={energyScore}
                      onChange={(e) => setEnergyScore(parseFloat(e.target.value))}
                      className="w-full accent-green-600 cursor-pointer"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-xs font-space font-bold mb-1">
                      <span className="flex items-center gap-1 text-gray-700">
                        <Moon className="w-3.5 h-3.5 text-blue-500" /> Giấc ngủ:
                      </span>
                      <span>{sleepHours}h</span>
                    </div>
                    <input
                      type="range"
                      min="3"
                      max="12"
                      step="0.5"
                      value={sleepHours}
                      onChange={(e) => setSleepHours(parseFloat(e.target.value))}
                      className="w-full accent-blue-500 cursor-pointer"
                    />
                  </div>
                </div>
              )}

              {/* Expandable Compact Photo Upload Field */}
              {showPhotoUpload && (
                <div className="p-3 bg-white rounded-2xl border border-black flex flex-col gap-2 animate-in fade-in">
                  <div className="flex items-center justify-between">
                    <span className="font-space text-xs font-bold uppercase">Đính kèm liên kết ảnh:</span>
                    {photoUrl && (
                      <button
                        type="button"
                        onClick={() => {
                          setPhotoUrl('');
                          setPhotoCaption('');
                          setShowPhotoUpload(false);
                        }}
                        className="text-xs text-red-600 font-bold hover:underline"
                      >
                        Gỡ ảnh
                      </button>
                    )}
                  </div>
                  <input
                    type="text"
                    value={photoUrl}
                    onChange={(e) => setPhotoUrl(e.target.value)}
                    placeholder="https://images.unsplash.com/photo-..."
                    className="w-full p-2 bg-paper-warm rounded-lg border border-black font-mono text-xs focus:outline-none"
                  />
                  <input
                    type="text"
                    value={photoCaption}
                    onChange={(e) => setPhotoCaption(e.target.value)}
                    placeholder="Ghi chú ảnh (vd: Góc quán quen chiều thu)"
                    className="w-full p-2 bg-paper-warm rounded-lg border border-black font-sans text-xs focus:outline-none"
                  />
                </div>
              )}

              {/* Action Buttons: 2 Clear Actions */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => router.back()}
                  className="px-4 py-2 bg-white text-black font-space text-xs font-bold border border-black rounded-xl shadow-neo-sm hover:bg-gray-100 transition-all cursor-pointer"
                >
                  Đóng sổ
                </button>

                <div className="flex items-center gap-3">
                  <NeoButton
                    variant="paper"
                    size="md"
                    onClick={handleOpenAiDrawer}
                    className="font-space font-bold text-xs sm:text-sm border-2 border-black shadow-neo hover:bg-lime-100"
                    icon={<Sparkles className="w-4 h-4 text-purple-700" />}
                  >
                    <span>{isAiDrawerOpen ? 'Đang mở phản chiếu' : 'Phản chiếu cùng MyLog'}</span>
                  </NeoButton>

                  <NeoButton
                    variant="primary"
                    size="md"
                    onClick={handleSaveClick}
                    className="font-space font-extrabold text-xs sm:text-sm shadow-neo"
                    icon={<Save className="w-4 h-4 stroke-[2.5]" />}
                  >
                    <span>{isEditMode ? 'Cập nhật trang nhật ký' : 'Lưu vào sổ tay'}</span>
                  </NeoButton>
                </div>
              </div>
            </div>
          </article>
        </div>

        {/* AI REFLECTION DRAWER (ON-DEMAND) */}
        <AiReflectionDrawer
          isOpen={isAiDrawerOpen}
          onClose={() => setIsAiDrawerOpen(false)}
          isAnalyzing={isAnalyzing}
          aiResult={aiResult}
          topics={topics}
          onInsertQuestionToJournal={handleAnswerQuestionInJournal}
          onSave={executeSave}
        />
      </main>

      {/* CONFIRM REFLECT MODAL (Appears on save if user hasn't clicked Reflect) */}
      <ConfirmReflectModal
        isOpen={showConfirmReflectModal}
        onConfirmReflect={() => {
          setShowConfirmReflectModal(false);
          handleOpenAiDrawer();
        }}
        onDirectSave={() => {
          setShowConfirmReflectModal(false);
          executeSave();
        }}
      />

      {/* PHOTO PREVIEW MODAL */}
      <PhotoPreviewModal
        isOpen={showPhotoPreview}
        onClose={() => setShowPhotoPreview(false)}
        photoUrl={photoUrl}
        caption={photoCaption}
      />

      {/* SAFETY CRISIS MODAL */}
      <SafetyModal
        isOpen={isSafetyModalOpen}
        onClose={() => setIsSafetyModalOpen(false)}
        onNavigateHome={() => router.push('/dashboard')}
        detectedKeywords={detectedCrisisKeywords}
      />
    </div>
  );
}
