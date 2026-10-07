'use client';

import { useState, useEffect, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useJournal } from '../context/JournalContext';
import { useToast } from '@/lib/toast-context';
import { MoodType, JournalStatus } from '@/types';
import { JournalTipTapEditorRef } from '../components/JournalTipTapEditor';

export function useJournalEditor() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const editId = searchParams.get('id');
  const { addEntry, updateEntry, getEntryById, streakCount } = useJournal();
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

  // Autosave timestamp
  const [lastSavedTime, setLastSavedTime] = useState<string | null>(null);

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
    router.push('/journal-editor');
    showToast({
      title: 'Đã mở trang sổ mới',
      message: 'Một trang giấy trắng tinh khôi cho suy nghĩ hôm nay.',
      type: 'info',
    });
  };

  // Load existing entry for editing or draft for new entry
  useEffect(() => {
    const timer = setTimeout(() => {
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
    }, 0);
    return () => clearTimeout(timer);
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

    const todayStr = new Date().toISOString().split('T')[0];
    const nowTimeStr = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    const journalStatus: JournalStatus = 'SAVED';

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
        aiAnalysis: undefined,
      });

      showToast({
        title: 'Đã cập nhật bản demo',
        message: 'Bài viết chỉ được lưu trong trình duyệt này.',
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
        aiAnalysis: undefined,
      });

      showToast({
        title: 'Đã lưu bản demo',
        message: 'Bài viết chỉ được lưu trong trình duyệt này.',
        type: 'success',
      });

      localStorage.removeItem('mylog_draft_journal');
    }

    setTimeout(() => {
      router.push('/history-calendar');
    }, 600);
  };

  const handleSaveClick = executeSave;

  const plainTextContent = content.replace(/<[^>]*>/g, '');
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
    lastSavedTime,
    handleResetToNewEntry,
    handleSaveClick,
    wordCount,
    router,
    streakCount,
  };
}

