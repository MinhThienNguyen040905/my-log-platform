import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { Alert, Platform, StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView, TextField } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { listSessions, revokeOtherSessions, revokeSession, type AccountSession } from '@/features/overview/api';
import { downloadExport, forgetExport, getExport, lastExportId, rememberExport, requestExport, type DataExport } from '@/features/user/export-api';
import { getConsents, revokeConsent, type Consent } from '@/features/user/api';
import { Spacing } from '@/constants/theme';

export default function SettingsScreen() {
  const session = useProtectedProfile();
  const userId = session.profile?.userId;
  const [sessions, setSessions] = useState<AccountSession[]>([]);
  const [consents, setConsents] = useState<Consent[]>([]);
  const [exportItem, setExportItem] = useState<DataExport | null>(null);
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(async () => { setError(null);
    try { const [devices, permissions] = await Promise.all([listSessions(), getConsents()]);
      setSessions(devices); setConsents(permissions); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể tải cài đặt.'); }
  }, []);
  useFocusEffect(useCallback(() => { if (userId) void load(); }, [userId, load]));
  useFocusEffect(useCallback(() => { if (!userId) return;
    void lastExportId(userId).then(async (id) => { if (id) setExportItem(await getExport(id)); }).catch(() => {});
  }, [userId]));
  const act = async (operation: () => Promise<void>) => { setBusy(true); setError(null);
    try { await operation(); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể cập nhật.'); }
    finally { setBusy(false); }
  };
  const actExport = async (operation: () => Promise<void>) => { setBusy(true); setError(null);
    try { await operation(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Không thể xuất dữ liệu.'); }
    finally { setBusy(false); }
  };
  const confirmAction = (title: string, message: string, action: () => void) => {
    if (Platform.OS === 'web') { if (window.confirm(`${title} ${message}`)) action(); return; }
    Alert.alert(title, message, [{ text: 'Hủy', style: 'cancel' }, { text: 'Tiếp tục', onPress: action }]);
  };
  return <Screen><View style={styles.stack}>
    <AppButton title="Về hồ sơ" variant="secondary" onPress={() => router.replace('/profile')} />
    <AppText variant="eyebrow">TÀI KHOẢN</AppText><AppText variant="display">Phiên và quyền dữ liệu.</AppText>
    {error ? <StatusView title="Không thể tải cài đặt" message={error} onRetry={() => void load()} /> : null}
    <PaperCard style={styles.stack}><AppText variant="heading">Quyền đồng ý</AppText>
      {consents.map((item) => <View key={item.type} style={styles.stack}>
        <AppText>{item.type}: {item.granted ? 'Đã đồng ý' : 'Chưa cấp'}</AppText>
        {item.granted && !['TERMS', 'PRIVACY'].includes(item.type)
          ? <AppButton title={`Thu hồi ${item.type}`} disabled={busy} variant="secondary"
              onPress={() => confirmAction('Thu hồi quyền?', item.type, () => void act(() => revokeConsent(item)))} /> : null}
      </View>)}</PaperCard>
    <PaperCard style={styles.stack}><AppText variant="heading">Thiết bị đã đăng nhập</AppText>
      {sessions.map((item) => <View key={item.id} style={styles.stack}>
        <AppText>{item.deviceName || 'Thiết bị'} · {item.createdAt.slice(0, 10)}</AppText>
        <AppButton title="Thu hồi phiên này" disabled={busy} variant="secondary"
          onPress={() => confirmAction('Thu hồi phiên?', 'Thiết bị này có thể bị đăng xuất.',
            () => void act(() => revokeSession(item.id)))} />
      </View>)}
      <AppButton title="Đăng xuất các thiết bị khác" disabled={busy} variant="secondary"
        onPress={() => void act(revokeOtherSessions)} />
    </PaperCard>
    <PaperCard style={styles.stack}><AppText variant="heading">Xuất dữ liệu</AppText>
      <AppText>Tạo bản CSV hoặc PDF. Bản xuất chỉ sẵn sàng trong thời hạn máy chủ trả về.</AppText>
      {!exportItem ? <View style={styles.stack}>
        <AppButton title="Tạo bản CSV" disabled={busy} variant="secondary"
          onPress={() => void actExport(async () => { const item = await requestExport('CSV');
            if (userId) await rememberExport(userId, item.id); setExportItem(item); })} />
        <AppButton title="Tạo bản PDF" disabled={busy} variant="secondary"
          onPress={() => void actExport(async () => { const item = await requestExport('PDF');
            if (userId) await rememberExport(userId, item.id); setExportItem(item); })} />
      </View> : <View style={styles.stack}>
        <AppText>{exportItem.format} · {exportItem.status}</AppText>
        {exportItem.expiresAt ? <AppText variant="caption">Hết hạn: {exportItem.expiresAt}</AppText> : null}
        <AppButton title="Cập nhật trạng thái" disabled={busy} variant="secondary"
          onPress={() => void actExport(async () => setExportItem(await getExport(exportItem.id)))} />
        {['FAILED', 'EXPIRED'].includes(exportItem.status) ? <AppButton title="Tạo bản xuất mới" disabled={busy}
          variant="secondary" onPress={() => void actExport(async () => { if (userId) await forgetExport(userId); setExportItem(null); })} /> : null}
        {exportItem.status === 'READY' ? <>
          <TextField label="Mật khẩu để tải bản xuất" secureTextEntry value={password} onChangeText={setPassword} />
          <AppButton title="Tải bản xuất" disabled={busy || !password} onPress={() => void actExport(async () => {
            const secret = password; setPassword(''); await downloadExport(exportItem, secret);
          })} />
        </> : null}
      </View>}
    </PaperCard>
  </View></Screen>;
}
const styles = StyleSheet.create({ stack: { gap: Spacing.three, paddingBottom: Spacing.four } });
