import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, Platform, StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView, TextField } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { createTag, deleteEntry, documentText, favoriteEntry, getEntry, listTags, setEntryTag,
  type JournalEntry, type JournalTag } from '@/features/journal/api';
import { Palette, Spacing } from '@/constants/theme';

export default function JournalDetailScreen() {
  useProtectedProfile();
  const { id } = useLocalSearchParams<{ id: string }>();
  const [entry, setEntry] = useState<JournalEntry | null>(null);
  const [tags, setTags] = useState<JournalTag[]>([]);
  const [tagName, setTagName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { if (!id) return; let active = true;
    void getEntry(id).then((value) => { if (active) setEntry(value); })
      .catch((cause) => { if (active) setError(cause instanceof Error ? cause.message : 'Không thể tải trang.'); });
    void listTags().then((value) => { if (active) setTags(value); }).catch(() => {});
    return () => { active = false; };
  }, [id]);
  const toggleFavorite = async () => { if (!entry) return; setBusy(true); setError(null);
    try { setEntry(await favoriteEntry(entry.id, !entry.favorite)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể cập nhật yêu thích.'); }
    finally { setBusy(false); } };
  const remove = async () => { if (!entry) return; setBusy(true); setError(null);
    try { await deleteEntry(entry.id, entry.rowVersion); router.replace('/journal'); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể xóa trang.'); setBusy(false); } };
  const confirmDelete = () => {
    if (Platform.OS === 'web') { if (window.confirm('Xóa trang nhật ký? Thao tác này không thể hoàn tác.')) void remove(); return; }
    Alert.alert('Xóa trang nhật ký?', 'Thao tác này không thể hoàn tác.',
      [{ text: 'Giữ lại', style: 'cancel' }, { text: 'Xóa', style: 'destructive', onPress: () => void remove() }]);
  };
  if (!entry) return <Screen><StatusView title={error || 'Đang tải trang'} onRetry={() => id && getEntry(id).then(setEntry).catch(() => {})} /></Screen>;
  return <Screen>
    <View style={styles.header}><AppButton title="Về danh sách" variant="secondary" onPress={() => router.replace('/journal')} />
      <AppText variant="eyebrow">{entry.localDate}</AppText><AppText variant="display">{entry.title || 'Trang không tiêu đề'}</AppText></View>
    <PaperCard style={styles.paper}><AppText>{documentText(entry.contentJson)}</AppText></PaperCard>
    {entry.moodCode ? <AppText>Tâm trạng: {entry.moodCode}</AppText> : null}
    {entry.analysisStatus ? <AppText variant="caption">Phân tích: {entry.analysisStatus}</AppText> : null}
    {entry.riskLevel && entry.riskLevel !== 'NONE' ? <AppText variant="caption">Trạng thái an toàn: {entry.riskLevel}</AppText> : null}
    {error ? <AppText accessibilityRole="alert" style={styles.error}>{error}</AppText> : null}
    <View style={styles.actions}>
      <AppButton title="Chỉnh sửa" disabled={busy} onPress={() => router.push({ pathname: '/journal-editor', params: { id: entry.id } })} />
      <AppButton title={entry.favorite ? 'Bỏ yêu thích' : 'Yêu thích'} variant="secondary" disabled={busy} onPress={() => void toggleFavorite()} />
      <AppButton title="Xóa trang" variant="secondary" disabled={busy} onPress={confirmDelete} />
    </View>
    <PaperCard style={styles.actions}><AppText variant="heading">Nhãn</AppText>
      <TextField label="Tên nhãn mới" value={tagName} onChangeText={setTagName} maxLength={60} />
      <AppButton title="Tạo và gắn nhãn" variant="secondary" disabled={busy || !tagName.trim()}
        onPress={() => { setBusy(true); void createTag(tagName.trim()).then(async (tag) => {
          await setEntryTag(entry.id, tag.id, true); setTags((current) => [...current, tag]); setTagName(''); setError(null);
        }).catch((cause) => setError(cause instanceof Error ? cause.message : 'Không thể tạo nhãn.'))
          .finally(() => setBusy(false)); }} />
      {tags.map((tag) => <View key={tag.id} style={styles.tag}>
        <AppText>{tag.name}</AppText>
        <AppButton title={`Gắn nhãn ${tag.name}`} variant="secondary" disabled={busy}
          onPress={() => { setBusy(true); void setEntryTag(entry.id, tag.id, true).then(() => setError(null))
            .catch((cause) => setError(cause instanceof Error ? cause.message : 'Không thể gắn nhãn.'))
            .finally(() => setBusy(false)); }} />
        <AppButton title={`Bỏ nhãn ${tag.name}`} variant="secondary" disabled={busy}
          onPress={() => { setBusy(true); void setEntryTag(entry.id, tag.id, false).then(() => setError(null))
            .catch((cause) => setError(cause instanceof Error ? cause.message : 'Không thể bỏ nhãn.'))
            .finally(() => setBusy(false)); }} />
      </View>)}</PaperCard>
  </Screen>;
}
const styles = StyleSheet.create({ header: { gap: Spacing.three, paddingVertical: Spacing.four },
  paper: { backgroundColor: Palette.paper, minHeight: 220, marginBottom: Spacing.four },
  actions: { gap: Spacing.two, marginBottom: Spacing.four }, tag: { gap: Spacing.two }, error: { color: Palette.danger } });
