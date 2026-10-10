'use client';

import { useState, useEffect, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useJournal } from '../context/JournalContext';
import { useToast } from '@/lib/toast-context';
import { MoodType, JournalEmotion } from '@/types';
import { normalizeJournalEmotion } from '../utils/journal-emotions';
import { JournalTipTapEditorRef } from '../components/JournalTipTapEditor';
import { prepareContent } from '../api/client';
import { journalEditorFieldsSchema } from '../schemas/journal-editor-fields';

export function useJournalEditor() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const editId = searchParams.get('id');
  const { addEntry, updateEntry, loadEntry, streakCount, draftStorageKey, userProfile, registerJournalLeaveCheck } = useJournal();
  const { showToast } = useToast();

  const editorRef = useRef<JournalTipTapEditorRef>(null);

  const [isEditMode, setIsEditMode] = useState(false);
  const [existingDate, setExistingDate] = useState<string | null>(null);
  const [rowVersion, setRowVersion] = useState<number | null>(null);
  const [contentVersion, setContentVersion] = useState<number | null>(null);
  const [loadedEntryId, setLoadedEntryId] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [createKey, setCreateKey] = useState(() => crypto.randomUUID());

  // Core Journal State
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [mood, setMood] = useState<MoodType>('calm-joy');
  const [selectedEmotion, setSelectedEmotion] = useState<JournalEmotion | null>('happy');
  const [moodScore, setMoodScore] = useState(8.0);
  const [hoveredMoodScore, setHoveredMoodScore] = useState<number | null>(null);
  const [stressScore, setStressScore] = useState(3.0);
  const [energyScore, setEnergyScore] = useState(7.5);
  const [sleepHours, setSleepHours] = useState(7.5);
  const [showDetailedMetrics, setShowDetailedMetrics] = useState(true);

  // Topics & Tags
  const [topics, setTopics] = useState<string[]>([]);
  const baselineRef = useRef(JSON.stringify(['', '', 'calm-joy', 'happy', 8, 3, 7.5, 7.5, []]));
  const currentSnapshot = JSON.stringify([title, content, mood, selectedEmotion, moodScore, stressScore, energyScore, sleepHours, topics]);

  useEffect(() => {
    registerJournalLeaveCheck(() => currentSnapshot !== baselineRef.current);
    return () => registerJournalLeaveCheck(null);
  }, [currentSnapshot, registerJournalLeaveCheck]);
  const [showAddTopicModal, setShowAddTopicModal] = useState(false);

  // Insert image modal state
  const [showImageModal, setShowImageModal] = useState(false);

  // Writing Prompts
  const [showPromptModal, setShowPromptModal] = useState(false);

  // AI Drawer & State
  const [isAiDrawerOpen, setIsAiDrawerOpen] = useState(false);

  // Safety Crisis Modal
  const [isSafetyModalOpen, setIsSafetyModalOpen] = useState(false);
  const [safetySaved, setSafetySaved] = useState(false);
  const [detectedCrisisKeywords, setDetectedCrisisKeywords] = useState<string[]>([]);


  const writingPrompts = [
    { category: 'Thấu cảm', prompt: 'Hôm nay điều gì đã làm tiêu tốn nhiều năng lượng tinh thần của bạn nhất?' },
    { category: 'Biết ơn', prompt: 'Một khoảnh khắc nhỏ nào trong ngày hôm nay khiến bạn mỉm cười nhẹ nhõm?' },
    { category: 'Tự vấn', prompt: 'Nếu được nhắn nhủ một câu dịu dàng với chính mình lúc này, bạn sẽ nói gì?' },
    { category: 'Giải tỏa', prompt: 'Có suy nghĩ nào đang luẩn quẩn trong đầu mà bạn muốn trút cạn lên trang giấy?' },
    { category: 'Tự hào', prompt: 'Bạn cảm thấy trân trọng nỗ lực nào của bản thân sau những ngày vừa qua?' },
  ];

  // Reset to clean new entry
  const handleResetToNewEntry = () => {
    baselineRef.current = JSON.stringify(['', '', 'calm-joy', 'happy', 8, 3, 7.5, 7.5, []]);
    setIsEditMode(false);
    setLoadedEntryId(null);
    setRowVersion(null);
    setContentVersion(null);
    setExistingDate(null);
    setTitle('');
    setContent('');
    setMood('calm-joy');
    setSelectedEmotion('happy');
    setMoodScore(8.0);
    setStressScore(3.0);
    setEnergyScore(7.5);
    setSleepHours(7.5);
    setTopics([]);
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
      let active = true;
      void loadEntry(editId).then((existing) => {
        if (!active) return;
        setIsEditMode(true);
        setRowVersion(existing.rowVersion ?? null);
        setContentVersion(existing.contentVersion ?? null);
        setLoadedEntryId(editId);
        setExistingDate(existing.date);
        baselineRef.current = JSON.stringify([
          existing.title, existing.content, existing.mood, existing.emotion ?? null,
          existing.moodScore ?? 8, existing.stressScore ?? 3,
          existing.energyScore ?? 7.5, existing.sleepHours ?? 7.5, existing.tags || [],
        ]);
        setTitle(existing.title);
        setContent(existing.content);
        setMood(existing.mood);
        setSelectedEmotion(existing.emotion ?? null);
        setMoodScore(existing.moodScore ?? 8);
        setStressScore(existing.stressScore ?? 3);
        setEnergyScore(existing.energyScore ?? 7.5);
        setSleepHours(existing.sleepHours ?? 7.5);
        setTopics(existing.tags || []);
      }).catch((error) => {
        if (active) showToast({ title: 'Không thể mở bài viết', message: error instanceof Error ? error.message : 'Vui lòng thử lại.', type: 'error' });
      });
      return () => { active = false; };
    } else {
      const timer = setTimeout(() => {
      setIsEditMode(false);
      setLoadedEntryId(null);
      setRowVersion(null);
      setContentVersion(null);
      setExistingDate(null);
      baselineRef.current = JSON.stringify(['', '', 'calm-joy', 'happy', 8, 3, 7.5, 7.5, []]);
      setTitle('');
      setContent('');
      setMood('calm-joy');
      setSelectedEmotion('happy');
      setMoodScore(8);
      setStressScore(3);
      setEnergyScore(7.5);
      setSleepHours(7.5);
      setTopics([]);
      try {
        const draft = localStorage.getItem(draftStorageKey);
        if (draft) {
          const parsed = JSON.parse(draft);
          if (parsed.title) setTitle(parsed.title);
          if (parsed.content) setContent(parsed.content);
          if (typeof parsed.moodScore === 'number') {
            setMoodScore(parsed.moodScore);
            setMood(parsed.mood === 'neutral' || parsed.mood === 'calm-joy'
              || parsed.mood === 'anxiety-stress' || parsed.mood === 'sadness-reflect'
              || parsed.mood === 'hope-energy'
              ? parsed.mood
              : parsed.moodScore >= 7 ? 'calm-joy'
                : parsed.moodScore >= 5.5 ? 'neutral'
                  : parsed.moodScore >= 4 ? 'anxiety-stress' : 'sadness-reflect');
          }
          const restoredEmotion = normalizeJournalEmotion(parsed.emotion);
          if (restoredEmotion) {
            setSelectedEmotion(restoredEmotion);
          } else if (typeof parsed.moodScore === 'number') {
            setSelectedEmotion(null);
          }
          if (parsed.stressScore) setStressScore(parsed.stressScore);
          if (parsed.energyScore) setEnergyScore(parsed.energyScore);
          if (parsed.sleepHours) setSleepHours(parsed.sleepHours);
          if (parsed.topics) setTopics(parsed.topics);
        }
      } catch (e) {
        console.error('Draft load error:', e);
      }
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [editId, loadEntry, draftStorageKey, showToast]);

  // Local Autosave (quiet)
  useEffect(() => {
    if (isEditMode) return;
    const timer = setTimeout(() => {
      try {
        localStorage.setItem(
          draftStorageKey,
          JSON.stringify({ title, content, mood, moodScore, emotion: selectedEmotion, stressScore, energyScore, sleepHours, topics })
        );
      } catch (e) {
        console.error('Draft save error:', e);
      }
    }, 1500);

    return () => clearTimeout(timer);
  }, [title, content, mood, moodScore, selectedEmotion, stressScore, energyScore, sleepHours, topics, isEditMode, draftStorageKey]);

  const handleOpenAiDrawer = () => {
    if (!editId || loadedEntryId !== editId || contentVersion === null) {
      showToast({ title: 'Hãy lưu bài viết trước', message: 'Phân tích chỉ dành cho bài viết đã lưu.', type: 'info' });
      return;
    }
    if (currentSnapshot !== baselineRef.current) {
      showToast({ title: 'Bài viết có thay đổi chưa lưu', message: 'Hãy lưu thay đổi trước khi xem phân tích.', type: 'info' });
      return;
    }
    setIsAiDrawerOpen(true);
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
      title: 'Đã đưa câu hỏi vào bài viết.',
      message: 'Bạn có thể tiếp tục viết dòng suy ngẫm của mình.',
      type: 'info',
    });
  };

  // Save entry
  const executeSave = async () => {
    if (isSaving) return;
    if (editId && loadedEntryId !== editId) return;
    const plainText = editorRef.current ? editorRef.current.getText() : content.replace(/<[^>]*>/g, '');
    if (!plainText.trim()) {
      showToast({
        title: 'Chưa có nội dung!',
        message: 'Vui lòng viết vài dòng cảm nhận trước khi cất sổ nhé.',
        type: 'error',
      });
      return;
    }

    const editorContent = editorRef.current?.getJSON();
    if (!editorContent) return;
    let contentJson;
    try { contentJson = prepareContent(editorContent); }
    catch (error) {
      showToast({ title: 'Chưa thể lưu nội dung này', message: error instanceof Error ? error.message : 'Kiểu nội dung chưa được hỗ trợ.', type: 'error' });
      return;
    }
    if (!journalEditorFieldsSchema.safeParse({ title, topics }).success) {
      showToast({ title: 'Thông tin quá dài', message: 'Tiêu đề tối đa 160 ký tự; mỗi bài tối đa 20 chủ đề, mỗi chủ đề tối đa 40 ký tự.', type: 'error' });
      return;
    }

    const body = {
      title: title.trim() || 'Trang nhật ký không tên', contentJson,
      timezone: userProfile.timezone, moodCode: selectedEmotion === 'very_bad' ? 'very-bad' : selectedEmotion ?? 'neutral',
      moodScore, stressScore, energyScore, sleepMinutes: Math.round(sleepHours * 60),
    };
    if (topics.some((topic) => topic.trim())) {
      showToast({ title: 'Chủ đề chưa khả dụng', message: 'Chưa thể lưu chủ đề theo bài viết. Vui lòng bỏ chủ đề trước khi lưu.', type: 'error' });
      return;
    }
    setIsSaving(true);
    try {
      let saved;
      if (isEditMode && editId) {
        if (rowVersion === null) throw new Error('Chưa tải phiên bản bài viết. Vui lòng mở lại bài.');
        saved = await updateEntry(editId, body, rowVersion);
        setRowVersion(saved.rowVersion ?? null);
        setContentVersion(saved.contentVersion ?? null);
      } else {
        saved = await addEntry(body, createKey);
      }
      baselineRef.current = currentSnapshot;
      if (!isEditMode) {
        try { localStorage.removeItem(draftStorageKey); } catch { /* The entry is already saved on the server. */ }
        setCreateKey(crypto.randomUUID());
      }
      try {
        await loadEntry(saved.id);
      } catch {
        showToast({ title: 'Đã lưu bài viết', message: 'Bài đã được lưu trên máy chủ nhưng chưa tải lại được. Bạn có thể mở lại từ lịch sử.', type: 'info' });
      }
      if (saved.riskLevel === 'HIGH' || saved.riskLevel === 'CRITICAL') {
        setDetectedCrisisKeywords([]);
        setSafetySaved(true);
        setIsSafetyModalOpen(true);
      }
      showToast({ title: 'Đã lưu trang nhật ký', message: 'Bài viết đã được lưu trên máy chủ.', type: 'success' });
      if (saved.riskLevel !== 'HIGH' && saved.riskLevel !== 'CRITICAL') router.push('/history-calendar');
    } catch (error) {
      showToast({ title: 'Chưa thể lưu bài viết', message: error instanceof Error ? error.message : 'Vui lòng thử lại.', type: 'error' });
    } finally { setIsSaving(false); }
  };

  const plainTextContent = content.replace(/<[^>]*>/g, '');
  const wordCount = plainTextContent.trim() ? plainTextContent.trim().split(/\s+/).filter(Boolean).length : 0;

  return {
    editorRef,
    isEditMode,
    editId,
    rowVersion,
    contentVersion,
    loadedEntryId,
    isSaving,
    existingDate,
    title,
    setTitle,
    content,
    setContent,
    mood,
    setMood,
    selectedEmotion,
    setSelectedEmotion,
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
    isSafetyModalOpen,
    safetySaved,
    setIsSafetyModalOpen,
    detectedCrisisKeywords,
    writingPrompts,
    handleResetToNewEntry,
    handleOpenAiDrawer,
    handleAnswerQuestionInJournal,
    executeSave,
    wordCount,
    router,
    streakCount,
  };
}

