import { journalAssetUploadProxy } from '@/features/journal/api/proxy';

export function POST(request: Request) {
  return journalAssetUploadProxy(request);
}

