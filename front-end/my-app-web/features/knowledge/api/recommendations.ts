import { apiJson, apiJsonBody, apiQuery } from '@/lib/api/client';

export type Recommendation = {
  topicCode: string;
  excerpts: Array<{
    text: string;
    citation: { itemId: string; versionId: string; version: number; chunkId: string; sourceName: string; sourceUrl: string | null };
  }>;
};

export function getRecommendations(topicCode: string): Promise<Recommendation> {
  return apiJson(`recommendations${apiQuery({ topicCode })}`);
}

export type ReflectiveQuestions = {
  topicCode: string;
  questions: string[];
  message: string | null;
};

export function generateReflectiveQuestions(topicCode: string): Promise<ReflectiveQuestions> {
  return apiJson('recommendations/questions', {
    method: 'POST',
    ...apiJsonBody({ topicCode }),
  });
}
