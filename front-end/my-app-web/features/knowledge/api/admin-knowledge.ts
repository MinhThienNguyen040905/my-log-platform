import { apiJson, apiJsonBody } from '@/lib/api/client';

export type KnowledgeVersion = {
  itemId: string; versionId: string; version: number; slug: string;
  topicCode: string; locale: string; title: string; content: string;
  status: string; checksum: string; chunkStrategyVersion: string;
  effectiveFrom: string | null; effectiveTo: string | null;
};
export type CreateKnowledgeItem = {
  slug: string; topicCode: string; locale: string; sourceName: string;
  sourceUrl?: string | null; ownerTeam: string; title: string; content: string;
  effectiveFrom?: string | null; effectiveTo?: string | null;
};
export type WriteKnowledgeVersion = {
  title: string; content: string; effectiveFrom?: string | null; effectiveTo?: string | null;
};

const itemPath = (itemId: string) => `admin/knowledge-items/${encodeURIComponent(itemId)}`;
const versionPath = (itemId: string, version: number) => `${itemPath(itemId)}/versions/${version}`;

export function createKnowledgeItem(input: CreateKnowledgeItem): Promise<KnowledgeVersion> {
  return apiJson('admin/knowledge-items', { method: 'POST', ...apiJsonBody(input) });
}
export function getKnowledgeVersion(itemId: string, version: number): Promise<KnowledgeVersion> {
  return apiJson(versionPath(itemId, version));
}
export function createKnowledgeVersion(itemId: string, input: WriteKnowledgeVersion): Promise<KnowledgeVersion> {
  return apiJson(`${itemPath(itemId)}/versions`, { method: 'POST', ...apiJsonBody(input) });
}
export function updateKnowledgeDraft(itemId: string, version: number, input: WriteKnowledgeVersion): Promise<KnowledgeVersion> {
  return apiJson(versionPath(itemId, version), { method: 'PATCH', ...apiJsonBody(input) });
}
export function submitKnowledgeVersion(itemId: string, version: number): Promise<KnowledgeVersion> {
  return apiJson(`${versionPath(itemId, version)}:submit`, { method: 'POST' });
}
export function approveKnowledgeVersion(itemId: string, version: number): Promise<KnowledgeVersion> {
  return apiJson(`${versionPath(itemId, version)}:approve`, { method: 'POST' });
}
export function rejectKnowledgeVersion(itemId: string, version: number, reasonCode: string): Promise<KnowledgeVersion> {
  return apiJson(`${versionPath(itemId, version)}:reject`, { method: 'POST', ...apiJsonBody({ reasonCode }) });
}
export function archiveKnowledgeVersion(itemId: string, version: number, reasonCode: string): Promise<KnowledgeVersion> {
  return apiJson(`${versionPath(itemId, version)}:archive`, { method: 'POST', ...apiJsonBody({ reasonCode }) });
}
