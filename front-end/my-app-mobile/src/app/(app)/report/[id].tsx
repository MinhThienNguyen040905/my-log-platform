import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard, Screen, StatusView } from '@/components/ui';
import { useProtectedProfile } from '@/features/auth/use-protected-profile';
import { getReport, type Report } from '@/features/overview/api';
import { Spacing } from '@/constants/theme';

export default function ReportDetailScreen() {
  useProtectedProfile();
  const { id } = useLocalSearchParams<{ id: string }>();
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { if (!id) return; let active = true;
    void getReport(id).then((value) => { if (active) setReport(value); })
      .catch((cause) => { if (active) setError(cause instanceof Error ? cause.message : 'Không thể tải báo cáo.'); });
    return () => { active = false; };
  }, [id]);
  return <Screen><View style={styles.stack}>
    <AppButton title="Về góc nhìn" variant="secondary" onPress={() => router.back()} />
    <AppText variant="eyebrow">BÁO CÁO</AppText>
    {!report ? <StatusView title={error || 'Đang tải báo cáo'} onRetry={() => id && getReport(id).then(setReport).catch(() => {})} /> : <>
      <AppText variant="display">{report.type}</AppText>
      <AppText>{report.from} – {report.to} · {report.status}</AppText>
      <AppText variant="caption">Cỡ mẫu: {report.sampleSize}</AppText>
      {report.narrative ? <PaperCard><AppText>{report.narrative}</AppText></PaperCard> : null}
      {Object.entries(report.metrics || {}).map(([name, value]) => <PaperCard key={name}>
        <AppText variant="heading">{name}</AppText><AppText>{typeof value === 'object' ? JSON.stringify(value) : String(value)}</AppText>
      </PaperCard>)}
    </>}
  </View></Screen>;
}
const styles = StyleSheet.create({ stack: { gap: Spacing.three, paddingBottom: Spacing.four } });
