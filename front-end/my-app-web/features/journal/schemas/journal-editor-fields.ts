import { z } from 'zod';

export const journalEditorFieldsSchema = z.object({
  title: z.string().max(160),
  topics: z.array(z.string().refine((value) => [...value].length <= 40)).max(20),
});
