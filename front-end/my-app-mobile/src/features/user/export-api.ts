import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { apiRequest, jsonBody } from '@/lib/api/client';

export type DataExport = { id: string; format: 'CSV' | 'PDF'; status: string;
  createdAt: string; completedAt: string | null; expiresAt: string | null };
type DownloadGrant = { url: string; expiresAt: string };
const webIds = new Map<string, string>();
function exportKey(userId: string) { return `mylog.export.${userId}`; }
export async function rememberExport(userId: string, id: string) {
  if (Platform.OS === 'web') webIds.set(userId, id);
  else await SecureStore.setItemAsync(exportKey(userId), id);
}
export async function lastExportId(userId: string): Promise<string | null> {
  return Platform.OS === 'web' ? webIds.get(userId) ?? null : SecureStore.getItemAsync(exportKey(userId));
}
export async function forgetExport(userId: string) {
  if (Platform.OS === 'web') webIds.delete(userId);
  else await SecureStore.deleteItemAsync(exportKey(userId));
}

export async function requestExport(format: 'CSV' | 'PDF'): Promise<DataExport> {
  return (await apiRequest('/exports', { method: 'POST', ...jsonBody({ format }) })).json() as Promise<DataExport>;
}
export async function getExport(id: string): Promise<DataExport> {
  return (await apiRequest(`/exports/${encodeURIComponent(id)}`)).json() as Promise<DataExport>;
}
export async function downloadExport(item: DataExport, password: string) {
  const grant = await (await apiRequest(`/exports/${encodeURIComponent(item.id)}:authorize-download`,
    { method: 'POST', ...jsonBody({ password }) })).json() as DownloadGrant;
  const expected = `/api/v1/exports/${encodeURIComponent(item.id)}/file`;
  const signed = new URL(grant.url, 'https://mylog.invalid');
  if (signed.pathname !== expected || !signed.searchParams.has('expires') || !signed.searchParams.has('signature')) {
    throw new Error('Đường dẫn tải không hợp lệ.');
  }
  const response = await apiRequest(`/exports/${encodeURIComponent(item.id)}/file${signed.search}`);
  const bytes = new Uint8Array(await response.arrayBuffer());
  const name = `mylog-export-${item.id}.${item.format.toLowerCase()}`;
  if (Platform.OS === 'web') {
    const url = URL.createObjectURL(new Blob([bytes], { type: item.format === 'PDF' ? 'application/pdf' : 'text/csv' }));
    const link = document.createElement('a'); link.href = url; link.download = name; link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    return;
  }
  const [{ File, Paths }, Sharing] = await Promise.all([import('expo-file-system'), import('expo-sharing')]);
  if (!await Sharing.isAvailableAsync()) throw new Error('Thiết bị chưa hỗ trợ chia sẻ tệp.');
  const file = new File(Paths.cache, name);
  try { file.create({ overwrite: true }); file.write(bytes); await Sharing.shareAsync(file.uri,
    { mimeType: item.format === 'PDF' ? 'application/pdf' : 'text/csv', dialogTitle: 'Lưu bản xuất MyLog' }); }
  finally { if (file.exists) file.delete(); }
}
