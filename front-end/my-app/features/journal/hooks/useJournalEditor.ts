'use client';

import { useState, useEffect, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useJournal } from '../context/JournalContext';
import { useToast } from '@/lib/toast-context';
import { MoodType, AIAnalysisResult, JournalStatus } from '@/types';
import { EMOTION_TAXONOMY } from '@/constants/emotions';
import { detectCrisisKeywords } from '../utils/safety-checker';
import { JournalTipTapEditorRef } from '../components/JournalTipTapEditor';

export function useJournalEditor() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const editId = searchParams.get('id');
  const { addEntry, updateEntry, getEntryById } = useJournal();
  const { showToast } = useToast();

  const editorRef = useRef<JournalTipTapEditorRef>(null);

  const [isEditMode, setIsEditMode] = useState(false);
  const [existingDate, setExistingDate] = useState<string | null>(null);

  // Core Journal State
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [mood, setMood] = useState<MoodType>('calm-joy');
  const [moodScore, setMoodScore] = useState(8.0);
  const [hoveredMoodScore, setHoveredMoodScore] = useState<number | null>(null);
  const [stressScore, setStressScore] = useState(3.0);
  const [energyScore, setEnergyScore] = useState(7.5);
  const [sleepHours, setSleepHours] = useState(7.5);
  const [showDetailedMetrics, setShowDetailedMetrics] = useState(false);

  // Topics & Tags
  const [topics, setTopics] = useState<string[]>([]);
  const [showAddTopicModal, setShowAddTopicModal] = useState(false);

  // Insert image modal state
  const [showImageModal, setShowImageModal] = useState(false);

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
  const [lastSavedTime, setLastSavedTime] = useState<string | null>(null);

  const [aiResult, setAiResult] = useState<AIAnalysisResult | null>(null);

  const writingPrompts = [
    { category: 'Thấu cảm', prompt: 'Hôm nay điều gì đã làm tiêu tốn nhiều năng lượng tinh thần của bạn nhất?' },
    { category: 'Biết ơn', prompt: 'Một khoảnh khắc nhỏ nào trong ngày hôm nay khiến bạn mỉm cười nhẹ nhõm?' },
    { category: 'Tự vấn', prompt: 'Nếu được nhắn nhủ một câu dịu dàng với chính mình lúc này, bạn sẽ nói gì?' },
    { category: 'Giải tỏa', prompt: 'Có suy nghĩ nào đang luẩn quẩn trong đầu mà bạn muốn trút cạn lên trang giấy?' },
    { category: 'Tự hào', prompt: 'Bạn cảm thấy trân trọng nỗ lực nào của bản thân sau những ngày vừa qua?' },
  ];

  // Reset to clean new entry
  const handleResetToNewEntry = () => {
    setIsEditMode(false);
    setExistingDate(null);
    setTitle('');
    setContent('');
    setMood('calm-joy');
    setMoodScore(8.0);
    setStressScore(3.0);
    setEnergyScore(7.5);
    setSleepHours(7.5);
    setTopics([]);
    setAiResult(null);
    router.push('/journal-editor');
    showToast({
      title: 'Đã mở trang sổ mới',
      message: 'Một trang giấy trắng tinh khôi cho suy nghĩ hôm nay.',
      type: 'info',
    });
  };

  // Load existing entry for editing or draft for new entry
  useEffect(() => {
    if (editId) {
      const existing = getEntryById(editId);
      if (existing) {
        setIsEditMode(true);
        setExistingDate(existing.date);
        setTitle(existing.title);
        setContent(existing.content);
        setMood(existing.mood);
        setMoodScore(existing.moodScore);
        setStressScore(existing.stressScore);
        setEnergyScore(existing.energyScore);
        setSleepHours(existing.sleepHours);
        setTopics(existing.tags || []);
        if (existing.aiAnalysis) setAiResult(existing.aiAnalysis);
      }
    } else {
      setIsEditMode(false);
      setExistingDate(null);
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
    if (editorRef.current) {
      editorRef.current.insertContent(`<blockquote>💭 <strong>${q}</strong></blockquote><p></p>`);
      editorRef.current.focus();
    } else {
      setContent((prev) => `${prev.trim()}<br/><blockquote>💭 ${q}</blockquote><p></p>`);
    }
    showToast({
      title: 'Đã chèn câu hỏi vào sổ tay!',
      message: 'Bạn có thể tiếp tục viết dòng suy ngẫm của mình.',
      type: 'info',
    });
  };

  // Save entry
  const executeSave = () => {
    const plainText = editorRef.current ? editorRef.current.getText() : content.replace(/<[^>]*>/g, '');
    if (!plainText.trim()) {
      showToast({
        title: 'Chưa có nội dung!',
        message: 'Vui lòng viết vài dòng cảm nhận trước khi cất sổ nhé.',
        type: 'error',
      });
      return;
    }

    if (checkSafetyRisk(plainText)) return;

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

  const plainTextContent = editorRef.current ? editorRef.current.getText() : content.replace(/<[^>]*>/g, '');
  const wordCount = plainTextContent.trim() ? plainTextContent.trim().split(/\s+/).filter(Boolean).length : 0;

  return {
    editorRef,
    isEditMode,
    existingDate,
    title,
    setTitle,
    content,
    setContent,
    mood,
    setMood,
    moodScore,
    setMoodScore,
    hoveredMoodScore,
    setHoveredMoodScore,
    stressScore,
    setStressScore,
    energyScore,
    setEnergyScore,
    sleepHours,
    setSleepHours,
    showDetailedMetrics,
    setShowDetailedMetrics,
    topics,
    setTopics,
    showAddTopicModal,
    setShowAddTopicModal,
    showImageModal,
    setShowImageModal,
    showPromptModal,
    setShowPromptModal,
    isAiDrawerOpen,
    setIsAiDrawerOpen,
    isAnalyzing,
    showConfirmReflectModal,
    setShowConfirmReflectModal,
    isSafetyModalOpen,
    setIsSafetyModalOpen,
    detectedCrisisKeywords,
    lastSavedTime,
    aiResult,
    writingPrompts,
    handleResetToNewEntry,
    handleOpenAiDrawer,
    handleAnswerQuestionInJournal,
    executeSave,
    handleSaveClick,
    wordCount,
    router,
  };
}

