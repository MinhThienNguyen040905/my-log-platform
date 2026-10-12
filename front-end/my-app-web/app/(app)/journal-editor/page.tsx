import { JournalEditor } from '@/features/journal';

export const metadata = {
  title: 'Soạn thảo nhật ký - MyLog',
  description: 'Không gian viết nhật ký và ghi lại cảm xúc của bạn',
};

export default function JournalEditorPage() {
  return <JournalEditor />;
}
