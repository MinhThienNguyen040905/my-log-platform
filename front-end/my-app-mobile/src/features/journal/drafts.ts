import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

export type JournalDraft = { title: string; body: string; moodCode: string; key: string; version?: number };
const prefix = 'mylog.journal.draft.';
const indexKey = `${prefix}index`;
const memory = new Map<string, string>();
const pending = new Map<string, Promise<void>>();
const storage = {
  async read(key: string) { return Platform.OS === 'web' ? memory.get(key) ?? null : SecureStore.getItemAsync(key); },
  async write(key: string, value: string) { if (Platform.OS === 'web') memory.set(key, value);
    else await SecureStore.setItemAsync(key, value); },
  async remove(key: string) { if (Platform.OS === 'web') memory.delete(key);
    else await SecureStore.deleteItemAsync(key); },
};
function name(userId: string, entryId: string) { return `${prefix}${userId}.${entryId}`; }
async function keys(): Promise<string[]> { try { return JSON.parse(await storage.read(indexKey) ?? '[]') as string[]; } catch { return []; } }
export async function readDraft(userId: string, entryId: string): Promise<JournalDraft | null> {
  const id = name(userId, entryId);
  const pointer = await storage.read(id);
  if (!pointer) return null;
  const [slot, value] = pointer.split(':');
  const count = Number(value);
  if (!['a', 'b'].includes(slot)) return null;
  if (!Number.isInteger(count) || count < 1 || count > 100) return null;
  let raw = '';
  for (let i = 0; i < count; i++) raw += await storage.read(`${id}.${slot}.${i}`) ?? '';
  try { return JSON.parse(raw) as JournalDraft; } catch { return null; }
}
export async function writeDraft(userId: string, entryId: string, draft: JournalDraft) {
  const id = name(userId, entryId);
  const next = (pending.get(id) ?? Promise.resolve()).catch(() => {}).then(() => writeDraftNow(id, draft));
  pending.set(id, next);
  try { await next; } finally { if (pending.get(id) === next) pending.delete(id); }
}
async function writeDraftNow(id: string, draft: JournalDraft) {
  const pointer = await storage.read(id);
  const [oldSlot, oldCountValue] = pointer?.split(':') ?? [];
  const nextSlot = oldSlot === 'a' ? 'b' : 'a';
  const oldCount = Number(oldCountValue) || 0;
  const raw = JSON.stringify(draft);
  const chunks = raw.match(/[\s\S]{1,500}/g) ?? [];
  if (chunks.length > 100) throw new Error('Bản nháp vượt quá dung lượng lưu cục bộ.');
  const known = await keys();
  if (!known.includes(id)) await storage.write(indexKey, JSON.stringify([...known, id]));
  await storage.write(`${id}.${nextSlot}.count`, String(chunks.length));
  for (let i = 0; i < chunks.length; i++) await storage.write(`${id}.${nextSlot}.${i}`, chunks[i]);
  await storage.write(id, `${nextSlot}:${chunks.length}`);
  if (oldSlot) {
    for (let i = 0; i < oldCount; i++) await storage.remove(`${id}.${oldSlot}.${i}`);
    await storage.remove(`${id}.${oldSlot}.count`);
  }
}
export async function removeDraft(userId: string, entryId: string) {
  const id = name(userId, entryId);
  await pending.get(id)?.catch(() => {});
  for (const slot of ['a', 'b']) {
    const count = Number(await storage.read(`${id}.${slot}.count`)) || 0;
    for (let i = 0; i < count; i++) await storage.remove(`${id}.${slot}.${i}`);
    await storage.remove(`${id}.${slot}.count`);
  }
  await storage.remove(id);
  await storage.write(indexKey, JSON.stringify((await keys()).filter((key) => key !== id)));
}
export async function clearDrafts() {
  await Promise.all([...pending.values()].map((value) => value.catch(() => {})));
  for (const key of await keys()) {
    for (const slot of ['a', 'b']) {
      const count = Number(await storage.read(`${key}.${slot}.count`)) || 0;
      for (let i = 0; i < count; i++) await storage.remove(`${key}.${slot}.${i}`);
      await storage.remove(`${key}.${slot}.count`);
    }
    await storage.remove(key);
  }
  await storage.remove(indexKey);
}
