import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Alert, Platform, Pressable, StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView, TextField } from '@/components/ui';
import { Palette, Spacing } from '@/constants/theme';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { ApiError } from '@/lib/api/client';
import { createEntry, documentText, getEntry, textDocument, updateEntry, type JournalEntry } from '@/features/journal/api';
import { readDraft, removeDraft, writeDraft, type JournalDraft } from '@/features/journal/drafts';

const moods = [{ code: 'calm', label: 'Bình yên' }, { code: 'happy', label: 'Vui' },
  { code: 'sad', label: 'Buồn' }, { code: 'anxious', label: 'Lo lắng' }];
function newKey() { return `mobile-${Date.now()}-${Math.random().toString(36).slice(2)}`; }

export default function JournalEditorScreen() {
  const session = useProtectedProfile();
  const userId = session.profile?.userId;
  const { id } = useLocalSearchParams<{ id?: string }>();
  const draftId = id || 'new';
  const [entry, setEntry] = useState<JournalEntry | null>(null);
  const [draft, setDraft] = useState<JournalDraft>({ title: '', body: '', moodCode: '', key: newKey() });
  const [ready, setReady] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const skipSave = useRef(false);
  useEffect(() => { if (!userId) return; let active = true;
    void (async () => {
      try {
        const local = await readDraft(userId, draftId);
        const remote = id ? await getEntry(id) : null;
        if (!active) return;
        setEntry(remote);
        if (local) {
          setDraft(local);
          if (remote && local.version !== remote.rowVersion) setConflict(true);
        } else if (remote) setDraft({ title: remote.title ?? '', body: documentText(remote.contentJson),
          moodCode: remote.moodCode ?? '', key: newKey(), version: remote.rowVersion });
      } catch (cause) { if (active) setError(cause instanceof Error ? cause.message : 'Không thể mở trang.'); }
      finally { if (active) setReady(true); }
    })();
    return () => { active = false; };
  }, [userId, draftId, id]);
  useEffect(() => { if (!ready || !userId || skipSave.current) return;
    const timer = setTimeout(() => { if (skipSave.current) return; void writeDraft(userId, draftId, draft).catch(() =>
      setError('Không thể lưu bản nháp trên thiết bị.')); }, 500);
    return () => clearTimeout(timer);
  }, [draft, draftId, ready, userId]);
  const change = (values: Partial<JournalDraft>) => setDraft((current) => ({ ...current, ...values }));
  const save = async () => {
    if (!userId || !ready || busy) return;
    if (!draft.body.trim()) { setError('Hãy viết nội dung trước khi lưu.'); return; }
    if (draft.body.length > 20000) { setError('Nội dung tối đa 20.000 ký tự.'); return; }
    setBusy(true); setError(null);
    try {
      const input = { title: draft.title.trim(), contentJson: textDocument(draft.body),
        timezone: session.profile?.timezone || 'Asia/Ho_Chi_Minh',
        ...(draft.moodCode ? { moodCode: draft.moodCode } : {}) };
      const saved = entry ? await updateEntry(entry.id, entry.rowVersion, input) : await createEntry(input, draft.key);
      skipSave.current = true;
      await removeDraft(userId, draftId);
      router.replace({ pathname: '/journal-entry/[id]', params: { id: saved.id } });
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 409) setConflict(true);
      setError(cause instanceof Error ? cause.message : 'Không thể lưu trang. Bản nháp vẫn còn trên thiết bị.');
    } finally { setBusy(false); }
  };
  const reloadRemote = async () => {
    if (!id || !userId) return; setBusy(true);
    try { const latest = await getEntry(id); setEntry(latest); setConflict(false);
      setDraft({ title: latest.title ?? '', body: documentText(latest.contentJson),
        moodCode: latest.moodCode ?? '', key: newKey(), version: latest.rowVersion });
      await removeDraft(userId, draftId); setError(null);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể tải phiên bản mới.'); }
    finally { setBusy(false); }
  };
  const confirmReload = () => {
    if (Platform.OS === 'web') { if (window.confirm('Bỏ bản nháp và tải phiên bản trên máy chủ?')) void reloadRemote(); return; }
    Alert.alert('Bỏ bản nháp?', 'Nội dung đang viết sẽ được thay bằng phiên bản trên máy chủ.',
      [{ text: 'Giữ bản nháp', style: 'cancel' }, { text: 'Tải phiên bản mới', onPress: () => void reloadRemote() }]);
  };
  if (!ready) return <Screen><StatusView title="Đang mở trang viết" message={error ?? undefined} /></Screen>;
  return <Screen>
    <View style={styles.header}><AppButton title="Quay lại" variant="secondary" onPress={() => router.back()} />
      <AppText variant="eyebrow">{entry ? 'CHỈNH SỬA TRANG' : 'TRANG MỚI'}</AppText>
      <AppText variant="display">Viết điều bạn muốn nhớ.</AppText>
      <AppText variant="caption">Bản nháp được lưu riêng trên thiết bị này cho tài khoản của bạn.</AppText></View>
    <PaperCard style={styles.paper}>
      <TextField label="Tiêu đề" value={draft.title} onChangeText={(value) => change({ title: value })} maxLength={200} />
      <TextField label="Nội dung" value={draft.body} onChangeText={(value) => change({ body: value })}
        multiline textAlignVertical="top" style={styles.body} />
      <AppText variant="heading">Tâm trạng</AppText>
      <View style={styles.moods}>{moods.map((mood) => <Pressable key={mood.code} accessibilityRole="radio"
        accessibilityState={{ selected: draft.moodCode === mood.code }} onPress={() => change({ moodCode: mood.code })}
        style={[styles.mood, draft.moodCode === mood.code && styles.selected]}><AppText>{mood.label}</AppText></Pressable>)}</View>
      {conflict ? <View style={styles.conflict}><AppText accessibilityRole="alert">Trang trên máy chủ đã thay đổi. Bản nháp của bạn vẫn được giữ.</AppText>
        <AppButton title="Tải phiên bản mới" variant="secondary" onPress={confirmReload} /></View> : null}
      {error ? <AppText accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
      <AppButton title="Lưu nhật ký" loading={busy} disabled={conflict} onPress={() => void save()} />
    </PaperCard>
  </Screen>;
}
const styles = StyleSheet.create({ header: { gap: Spacing.three, paddingVertical: Spacing.four },
  paper: { gap: Spacing.three, backgroundColor: Palette.paper, marginBottom: Spacing.four },
  body: { minHeight: 280, backgroundColor: Palette.surface }, moods: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.two },
  mood: { minHeight: 44, justifyContent: 'center', paddingHorizontal: Spacing.three, borderWidth: 2,
    borderColor: Palette.ink, borderRadius: 12, backgroundColor: Palette.surface }, selected: { backgroundColor: Palette.accent },
  conflict: { gap: Spacing.two }, error: { color: Palette.danger } });
