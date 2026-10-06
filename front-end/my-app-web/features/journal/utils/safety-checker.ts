export const CRISIS_KEYWORDS = [
  'tuyệt vọng',
  'bế tắc',
  'không muốn sống',
  'tự hại',
  'muốn chết',
  'chấm dứt tất cả',
  'buông xuôi',
];

/**
 * Kiểm tra các từ khóa khủng hoảng tâm lý / tự hại trong văn bản
 * @param text Chuỗi văn bản cần kiểm tra
 * @returns Danh sách các từ khóa nguy cơ được phát hiện
 */
export function detectCrisisKeywords(text: string): string[] {
  if (!text) return [];
  const lower = text.toLowerCase();
  return CRISIS_KEYWORDS.filter((kw) => lower.includes(kw));
}

