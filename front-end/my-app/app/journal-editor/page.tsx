'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useJournal } from '@/lib/journal-context';
import { MoodType, AIAnalysisResult } from '@/lib/types';
import { WashiTape, StickerBadge } from '@/components/ui/ScrapbookDecorations';
import { NeoButton } from '@/components/ui/NeoButton';

export default function JournalEditorPage() {
  const router = useRouter();
  const { addEntry } = useJournal();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [mood, setMood] = useState<MoodType>('calm-joy');
  const [moodScore, setMoodScore] = useState(8.0);
  const [stressScore, setStressScore] = useState(3.0);
  const [energyScore, setEnergyScore] = useState(7.5);
  const [sleepHours, setSleepHours] = useState(7.5);
  const [tagInput, setTagInput] = useState('');
  const [tags, setTags] = useState<string[]>(['tâm sự', 'học tập']);
  const [photoUrl, setPhotoUrl] = useState('');
  const [photoCaption, setPhotoCaption] = useState('');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [aiResult, setAiResult] = useState<AIAnalysisResult | null>(null);
  const [savedSuccess, setSavedSuccess] = useState(false);

  const moodOptions: { type: MoodType; emoji: string; label: string }[] = [
    { type: 'calm-joy', emoji: '😊', label: 'Bình an' },
    { type: 'hope-energy', emoji: '⚡', label: 'Năng lượng' },
    { type: 'anxiety-stress', emoji: '😰', label: 'Lo âu' },
    { type: 'sadness-reflect', emoji: '🌧️', label: 'Suy ngẫm' },
    { type: 'neutral', emoji: '😐', label: 'Bình thường' },
  ];

  const handleAddTag = () => {
    if (tagInput.trim() && !tags.includes(tagInput.trim())) {
      setTags([...tags, tagInput.trim()]);
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter(t => t !== tagToRemove));
  };

  const handleAnalyzeAI = () => {
    if (!content.trim()) {
      alert('Vui lòng viết vài dòng cảm nhận để AI phân tích nhé!');
      return;
    }
    setIsAnalyzing(true);
    setTimeout(() => {
      setAiResult({
        sentiment: moodScore >= 7 ? 'Tích cực & Nhẹ nhõm' : 'Trầm lắng & Tự vấn sâu',
        summary: `Bạn vừa chia sẻ câu chuyện về "${title || 'những trăn trở trong ngày'}". Các từ khoá cho thấy bạn đang chú trọng vào sự phát triển cá nhân.`,
        reflectionPrompt: 'Khi nhìn lại sự việc này trong 6 tháng nữa, bạn nghĩ chi tiết nào sẽ là bài học quý giá nhất?',
        mindfulAction: 'Hãy nhấp một ngụm trà ấm, duỗi cơ vai và thở chậm 3 nhịp trước khi chuyển sang công việc tiếp theo.',
        emotions: [
          { label: 'Thấu hiểu bản thân', percentage: 50, color: '#70E000', description: 'Nhận diện rõ ràng xúc cảm' },
          { label: 'Hy vọng tương lai', percentage: 35, color: '#FFD166', description: 'Sẵn sàng điều chỉnh' },
          { label: 'Áp lực còn sót lại', percentage: 15, color: '#FF6B6B', description: 'Cần giải phóng thêm' },
        ],
        entities: [
          { name: title || 'Trải nghiệm cá nhân', category: 'hoạt động', sentiment: 'positive' },
          { name: 'Tự phản chiếu', category: 'cảm xúc', sentiment: 'positive' }
        ]
      });
      setIsAnalyzing(false);
    }, 800);
  };

  const handleSave = () => {
    if (!title.trim() && !content.trim()) {
      alert('Vui lòng nhập tiêu đề hoặc nội dung nhật ký!');
      return;
    }

    const today = new Date().toISOString().split('T')[0];
    const now = new Date().toTimeString().slice(0, 5);

    addEntry({
      title: title.trim() || 'Nhật ký không tên',
      content,
      date: today,
      time: now,
      mood,
      moodScore,
      stressScore,
      energyScore,
      sleepHours,
      tags,
      photoUrl: photoUrl.trim() || undefined,
      photoCaption: photoCaption.trim() || undefined,
      aiAnalysis: aiResult || undefined,
    });

    setSavedSuccess(true);
    setTimeout(() => {
      router.push('/');
    }, 1000);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8">
      {/* Top Header Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b-2 border-on-background">
        <div className="flex items-center gap-3">
          <button
            onClick={() => router.back()}
            className="p-2 border-neo-sm bg-white rounded-xl shadow-neo-sm hover:bg-paper-warm transition-all"
            title="Quay lại"
          >
            <span className="material-symbols-outlined text-lg">arrow_back</span>
          </button>
          <div>
            <h1 className="font-space text-2xl sm:text-3xl font-extrabold text-on-surface">
              Trình Soạn Thảo & Phân Tích AI
            </h1>
            <p className="font-sans text-xs text-on-surface-variant">
              FR-04 Định Lượng • FR-05 Phổ Cảm Xúc • FR-07 Thực Thể
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <NeoButton
            variant="paper"
            size="sm"
            onClick={handleAnalyzeAI}
            icon={<span className="material-symbols-outlined text-base">psychology</span>}
            disabled={isAnalyzing}
          >
            {isAnalyzing ? 'Đang phân tích...' : 'Phân tích AI'}
          </NeoButton>

          <NeoButton
            variant="primary"
            size="md"
            onClick={handleSave}
            icon={<span className="material-symbols-outlined text-base">save</span>}
          >
            {savedSuccess ? 'Đã lưu ✓' : 'Lưu vào sổ tay'}
          </NeoButton>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* LEFT COLUMN: Physical Notebook Page (7 cols) */}
        <div className="lg:col-span-7 flex flex-col gap-6">
          <div className="bg-surface-card border-neo rounded-2xl p-6 sm:p-8 shadow-neo-lg relative bg-notebook-lines">
            <WashiTape color="lime" rotate={-2} className="absolute -top-3 left-12 w-32" />

            {/* Notebook Meta */}
            <div className="flex flex-wrap items-center justify-between gap-2 mb-4 pb-2 border-b border-border-soft">
              <span className="font-mono text-xs font-bold text-gray-500 uppercase tracking-wider">
                PAGE 042 • {new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
              </span>
              <span className="text-[11px] font-space font-bold bg-primary-container px-2 py-0.5 rounded border border-black">
                Bản nháp tự động lưu
              </span>
            </div>

            {/* Title Input */}
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Tiêu đề nhật ký hôm nay..."
              className="w-full font-space text-xl sm:text-2xl font-extrabold bg-transparent border-b-2 border-dashed border-on-background/40 pb-2 mb-4 focus:outline-none focus:border-on-background text-on-surface"
            />

            {/* Content Textarea */}
            <textarea
              rows={12}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Hãy trút cạn những suy nghĩ của bạn vào đây... Không phán xét, không áp lực. Hôm nay bạn đã trải qua những khoảnh khắc nào đáng nhớ?"
              className="w-full font-serif text-base sm:text-lg leading-relaxed bg-transparent resize-y focus:outline-none text-on-surface min-h-[260px]"
            />

            {/* Mood Picker */}
            <div className="mt-6 pt-4 border-t-2 border-on-background flex flex-col gap-2">
              <span className="font-space text-xs font-bold uppercase tracking-wider text-on-surface-variant">
                Tâm Trạng Chủ Đạo:
              </span>
              <div className="flex flex-wrap gap-2">
                {moodOptions.map((opt) => (
                  <button
                    key={opt.type}
                    type="button"
                    onClick={() => setMood(opt.type)}
                    className={`px-3 py-1.5 rounded-xl font-space text-xs font-bold border-neo-sm shadow-neo-sm flex items-center gap-1.5 transition-all ${
                      mood === opt.type
                        ? 'bg-primary-container text-on-primary-container translate-y-0.5 shadow-none ring-2 ring-black'
                        : 'bg-white text-on-surface hover:bg-paper-warm'
                    }`}
                  >
                    <span className="text-base">{opt.emoji}</span>
                    <span>{opt.label}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Tags section */}
            <div className="mt-4 flex flex-col gap-2">
              <span className="font-space text-xs font-bold uppercase tracking-wider text-on-surface-variant">
                Gắn Thẻ (Tags):
              </span>
              <div className="flex flex-wrap items-center gap-2">
                {tags.map((tag) => (
                  <span
                    key={tag}
                    className="bg-paper-warm text-on-surface font-space text-xs font-bold px-2.5 py-1 rounded-lg border border-black flex items-center gap-1"
                  >
                    #{tag}
                    <button
                      onClick={() => handleRemoveTag(tag)}
                      className="text-black/60 hover:text-black font-bold ml-1"
                    >
                      ×
                    </button>
                  </span>
                ))}
                <div className="flex items-center gap-1">
                  <input
                    type="text"
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddTag())}
                    placeholder="+ Thêm thẻ..."
                    className="font-space text-xs px-2.5 py-1 rounded-lg border border-dashed border-black bg-white focus:outline-none"
                  />
                  <button
                    onClick={handleAddTag}
                    className="font-space text-xs font-bold px-2 py-1 bg-black text-white rounded-lg"
                  >
                    +
                  </button>
                </div>
              </div>
            </div>

            {/* Photo Attachment URL */}
            <div className="mt-4 pt-4 border-t border-border-soft flex flex-col gap-2">
              <span className="font-space text-xs font-bold uppercase tracking-wider text-on-surface-variant">
                Dán Ảnh Lưu Niệm (Polaroid Attachment):
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <input
                  type="text"
                  value={photoUrl}
                  onChange={(e) => setPhotoUrl(e.target.value)}
                  placeholder="Dán link ảnh (Unsplash, imgur...)"
                  className="font-space text-xs p-2 rounded-lg border border-black bg-white focus:outline-none"
                />
                <input
                  type="text"
                  value={photoCaption}
                  onChange={(e) => setPhotoCaption(e.target.value)}
                  placeholder="Ghi chú dưới ảnh..."
                  className="font-space text-xs p-2 rounded-lg border border-black bg-white focus:outline-none"
                />
              </div>
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN: Quantitative Sliders (FR-04) & AI Analysis (FR-05, FR-07) (5 cols) */}
        <div className="lg:col-span-5 flex flex-col gap-6">
          {/* QUANTITATIVE SLIDERS (FR-04) */}
          <div className="bg-surface-card border-neo rounded-2xl p-6 shadow-neo">
            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-base">tune</span>
                <h2 className="font-space text-base font-bold text-on-surface">Định Lượng (FR-04)</h2>
              </div>
              <span className="text-[10px] font-mono bg-paper-warm px-2 py-0.5 rounded border border-black font-bold">
                Thang 1-10
              </span>
            </div>

            <div className="flex flex-col gap-4">
              {/* Mood Score */}
              <div>
                <div className="flex justify-between font-space text-xs font-bold mb-1">
                  <span>Tâm Trạng (Mood):</span>
                  <span className="text-primary font-extrabold">{moodScore.toFixed(1)}/10</span>
                </div>
                <input
                  type="range"
                  min="1"
                  max="10"
                  step="0.5"
                  value={moodScore}
                  onChange={(e) => setMoodScore(parseFloat(e.target.value))}
                  className="w-full accent-primary cursor-pointer"
                />
              </div>

              {/* Stress Score */}
              <div>
                <div className="flex justify-between font-space text-xs font-bold mb-1">
                  <span>Mức Độ Căng Thẳng (Stress):</span>
                  <span className="text-mood-anxiety-stress font-extrabold">{stressScore.toFixed(1)}/10</span>
                </div>
                <input
                  type="range"
                  min="1"
                  max="10"
                  step="0.5"
                  value={stressScore}
                  onChange={(e) => setStressScore(parseFloat(e.target.value))}
                  className="w-full accent-mood-anxiety-stress cursor-pointer"
                />
              </div>

              {/* Energy Score */}
              <div>
                <div className="flex justify-between font-space text-xs font-bold mb-1">
                  <span>Mức Năng Lượng (Energy):</span>
                  <span className="text-yellow-600 font-extrabold">{energyScore.toFixed(1)}/10</span>
                </div>
                <input
                  type="range"
                  min="1"
                  max="10"
                  step="0.5"
                  value={energyScore}
                  onChange={(e) => setEnergyScore(parseFloat(e.target.value))}
                  className="w-full accent-mood-hope-energy cursor-pointer"
                />
              </div>

              {/* Sleep Hours */}
              <div>
                <div className="flex justify-between font-space text-xs font-bold mb-1">
                  <span>Giấc Ngủ Đêm Qua:</span>
                  <span className="text-blue-600 font-extrabold">{sleepHours.toFixed(1)} tiếng</span>
                </div>
                <input
                  type="range"
                  min="3"
                  max="12"
                  step="0.5"
                  value={sleepHours}
                  onChange={(e) => setSleepHours(parseFloat(e.target.value))}
                  className="w-full accent-mood-sadness-reflect cursor-pointer"
                />
              </div>
            </div>
          </div>

          {/* AI REAL-TIME ANALYSIS ENGINE (FR-05, FR-07) */}
          <div className="bg-surface-card border-neo rounded-2xl p-6 shadow-neo relative">
            <WashiTape color="lavender" rotate={1} className="absolute -top-3 right-6 w-28" />

            <div className="flex items-center justify-between pb-3 border-b-2 border-on-background mb-4">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-base text-secondary">psychology</span>
                <h2 className="font-space text-base font-bold text-on-surface">AI Real-Time Engine</h2>
              </div>
              <span className="text-[10px] font-space font-bold bg-secondary-fixed px-2 py-0.5 rounded border border-black">
                FR-05 & FR-07
              </span>
            </div>

            {aiResult ? (
              <div className="flex flex-col gap-4">
                {/* Summary */}
                <div className="p-3 bg-paper-warm border-neo-sm rounded-xl">
                  <span className="font-space text-[10px] font-bold uppercase tracking-wider block text-gray-600 mb-1">
                    Cảm Nhận Trọng Tâm:
                  </span>
                  <p className="font-space text-xs font-bold text-on-surface">{aiResult.sentiment}</p>
                  <p className="font-sans text-xs text-on-surface-variant mt-1">{aiResult.summary}</p>
                </div>

                {/* Emotion Spectrum (FR-05) */}
                <div>
                  <span className="font-space text-xs font-bold uppercase tracking-wider block mb-2 text-on-surface">
                    Phổ Cảm Xúc (FR-05):
                  </span>
                  <div className="flex flex-col gap-2">
                    {aiResult.emotions.map((em, i) => (
                      <div key={i} className="flex flex-col gap-0.5">
                        <div className="flex justify-between text-xs font-space font-semibold">
                          <span>{em.label}</span>
                          <span>{em.percentage}%</span>
                        </div>
                        <div className="w-full h-2.5 bg-gray-100 rounded-full border border-black overflow-hidden">
                          <div
                            style={{ width: `${em.percentage}%`, backgroundColor: em.color }}
                            className="h-full rounded-full"
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Extracted Entities (FR-07) */}
                <div>
                  <span className="font-space text-xs font-bold uppercase tracking-wider block mb-1 text-on-surface">
                    Thực Thể & Chủ Đề (FR-07):
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {aiResult.entities.map((ent, i) => (
                      <span
                        key={i}
                        className="font-space text-[11px] font-bold px-2 py-0.5 bg-surface-container-high border border-black rounded-md"
                      >
                        {ent.name} ({ent.category})
                      </span>
                    ))}
                  </div>
                </div>

                {/* Reflection Prompt */}
                <div className="p-3.5 bg-primary-container/25 border-neo-sm rounded-xl shadow-neo-sm">
                  <span className="font-space text-[10px] uppercase font-bold tracking-wider text-on-primary-container block mb-1">
                    Câu Hỏi Gợi Mở (Self-Reflection):
                  </span>
                  <p className="font-serif italic text-xs sm:text-sm text-on-surface">
                    "{aiResult.reflectionPrompt}"
                  </p>
                </div>
              </div>
            ) : (
              <div className="py-8 flex flex-col items-center justify-center text-center gap-3">
                <div className="w-12 h-12 rounded-full bg-paper-warm border-neo-sm flex items-center justify-center text-2xl shadow-neo-sm">
                  🤖
                </div>
                <p className="font-sans text-xs text-on-surface-variant max-w-xs">
                  Sau khi viết suy nghĩ hoặc bấm <strong>"Phân tích AI"</strong>, hệ thống sẽ tự động bóc tách phổ cảm xúc và đề xuất câu hỏi gợi mở cho bạn.
                </p>
                <NeoButton variant="paper" size="sm" onClick={handleAnalyzeAI}>
                  Thử phân tích ngay
                </NeoButton>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
