import { apiJson, apiQuery } from '@/lib/api/client';

export type Recommendation = {
  topicCode: string;
  excerpts: Array<{
    text: string;
    citation: { itemId: string; versionId: string; version: number; chunkId: string; sourceName: string; sourceUrl: string };
  }>;
};

export function getRecommendations(topicCode: string): Promise<Recommendation> {
  return apiJson(`recommendations${apiQuery({ topicCode })}`);
}
