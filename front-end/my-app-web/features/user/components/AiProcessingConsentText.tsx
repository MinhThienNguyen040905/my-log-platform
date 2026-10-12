export const AI_PROCESSING_CONSENT_VERSION = 'groq-v1';

export function AiProcessingConsentText() {
  return <p className="text-sm text-on-surface-variant">
    Khi bạn yêu cầu gợi ý viết hoặc lưu nhật ký để phân tích, nội dung liên quan sẽ được gửi đến Groq để tạo câu trả lời. Groq có thể lưu tạm dữ liệu theo chính sách của họ. MyLog không dùng lựa chọn này để huấn luyện mô hình. Bạn có thể thu hồi quyền bất cứ lúc nào trong Cài đặt; các yêu cầu AI tiếp theo sẽ dừng. Xem <a className="underline" href="https://console.groq.com/docs/your-data" target="_blank" rel="noreferrer">chính sách dữ liệu Groq</a>.
  </p>;
}
