import { useEffect, useState } from 'react';
import { Linking, Modal, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { AppButton, AppText, PaperCard } from '@/components/ui';
import { Palette, Spacing } from '@/constants/theme';
import { loadApprovedSafetyResources, phoneLink, type ApprovedSafetyResource } from './api';

type Props = { visible: boolean; onClose: () => void; onGoHome?: () => void };

/**
 * Màn hỗ trợ an toàn, cùng nội dung với `SafetyModal` của web. Chỉ hiển thị nguồn hỗ trợ backend đã
 * duyệt; khi chưa có nguồn nào thì chỉ có lời nhắc chung, không có số điện thoại nào.
 */
export function SafetySupportModal({ visible, onClose, onGoHome }: Props) {
  const [resources, setResources] = useState<ApprovedSafetyResource[]>([]);
  useEffect(() => {
    if (!visible) return;
    let active = true;
    void loadApprovedSafetyResources().then((items) => { if (active) setResources(items); })
      .catch(() => { if (active) setResources([]); });
    return () => { active = false; };
  }, [visible]);

  return <Modal visible={visible} animationType="slide" presentationStyle="fullScreen" onRequestClose={onClose}>
    <ScrollView contentContainerStyle={styles.container} accessibilityViewIsModal>
      <View style={styles.header}>
        <AppText variant="eyebrow">HỖ TRỢ AN TOÀN</AppText>
        <AppText variant="display" accessibilityRole="header">Bạn không phải đơn độc lúc này.</AppText>
        <AppText>
          Nếu bạn đang lo lắng về sự an toàn của mình, hãy tìm sự hỗ trợ từ một người bạn tin cậy hoặc dịch vụ khẩn cấp tại nơi bạn sống.
        </AppText>
      </View>
      <PaperCard style={styles.card}>
        <AppText variant="heading">Lời nhắc nhẹ nhàng</AppText>
        <AppText>
          AI không thể thay thế sự hỗ trợ trực tiếp từ con người trong tình huống khẩn cấp. Nếu bạn đang gặp nguy hiểm ngay lúc này,
          hãy liên hệ dịch vụ khẩn cấp tại nơi bạn sống hoặc một người bạn tin cậy. Thông tin liên hệ cụ thể sẽ được hiển thị khi đã được xác minh.
        </AppText>
      </PaperCard>
      {resources.length > 0 ? <View style={styles.list}>
        <AppText variant="heading">Nguồn hỗ trợ đã được xác minh</AppText>
        {resources.map((resource) => {
          const tel = phoneLink(resource.contactValue);
          return <PaperCard key={resource.id} style={styles.card}>
            <AppText variant="heading">{resource.name}</AppText>
            {resource.description ? <AppText>{resource.description}</AppText> : null}
            {resource.contactValue ? tel
              ? <Pressable accessibilityRole="button" accessibilityLabel={`Gọi ${resource.name}: ${resource.contactValue}`}
                  onPress={() => void Linking.openURL(tel)} style={styles.contact}>
                  <AppText style={styles.contactText}>{resource.contactValue}</AppText>
                </Pressable>
              : <AppText>{resource.contactValue}</AppText> : null}
            <Pressable accessibilityRole="link" onPress={() => void Linking.openURL(resource.sourceUrl)}>
              <AppText style={styles.link}>Nguồn xác minh</AppText>
            </Pressable>
          </PaperCard>;
        })}
      </View> : null}
      <View style={styles.actions}>
        <AppButton title="Tiếp tục" onPress={onClose} />
        {onGoHome ? <AppButton title="Về trang chính" variant="secondary" onPress={() => { onClose(); onGoHome(); }} /> : null}
      </View>
    </ScrollView>
  </Modal>;
}

const styles = StyleSheet.create({
  container: { padding: Spacing.four, paddingTop: Spacing.six, gap: Spacing.four, backgroundColor: Palette.canvas, flexGrow: 1 },
  header: { gap: Spacing.three },
  card: { gap: Spacing.two },
  list: { gap: Spacing.three },
  contact: { minHeight: 44, justifyContent: 'center' },
  contactText: { fontWeight: '700', textDecorationLine: 'underline' },
  link: { textDecorationLine: 'underline' },
  actions: { gap: Spacing.two, marginTop: Spacing.two },
});
