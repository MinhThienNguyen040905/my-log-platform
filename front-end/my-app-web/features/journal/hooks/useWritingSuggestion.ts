'use client';

import { useEffect, useRef, useState } from 'react';
import { getWritingSuggestion, type WritingSuggestion } from '../api/writing-suggestions';

export function useWritingSuggestion(content: string) {
  const [result, setResult] = useState<WritingSuggestion | null>(null);
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(false);
  const activeRequest = useRef<AbortController | null>(null);

  useEffect(() => {
    activeRequest.current?.abort();
    activeRequest.current = null;
    // A suggestion applies only to the draft version sent to the backend.
    queueMicrotask(() => {
      setResult(null);
      setError(false);
      setLoading(false);
    });
    return () => activeRequest.current?.abort();
  }, [content]);

  async function request(text: string) {
    const draft = text.trim();
    if (!draft || draft.length > 4000) return;
    activeRequest.current?.abort();
    const controller = new AbortController();
    activeRequest.current = controller;
    setLoading(true);
    setResult(null);
    setError(false);
    try {
      const response = await getWritingSuggestion(draft, controller.signal);
      if (!controller.signal.aborted) setResult(response);
    } catch {
      if (!controller.signal.aborted) setError(true);
    } finally {
      if (activeRequest.current === controller) {
        activeRequest.current = null;
        setLoading(false);
      }
    }
  }

  return { result, error, loading, request };
}
