export interface ApprovedSafetyResource {
  id: string;
  name: string;
  resourceType: string;
  contactValue: string | null;
  description: string | null;
  sourceUrl: string;
  version: number;
}

export async function loadApprovedSafetyResources(signal: AbortSignal): Promise<ApprovedSafetyResource[]> {
  const backend = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080';
  const url = new URL('/api/v1/safety/resources?locale=vi-VN&country=VN', backend);
  const response = await fetch(url, { signal, cache: 'no-store' });
  if (!response.ok) throw new Error('Safety resources unavailable');
  const items: unknown = await response.json();
  if (!Array.isArray(items)) throw new Error('Invalid safety resources response');
  return items.filter((item): item is ApprovedSafetyResource => {
    if (typeof item !== 'object' || item === null) return false;
    const resource = item as Record<string, unknown>;
    return typeof resource.id === 'string' && typeof resource.name === 'string'
      && typeof resource.resourceType === 'string'
      && (resource.contactValue === null || typeof resource.contactValue === 'string')
      && (resource.description === null || typeof resource.description === 'string')
      && typeof resource.sourceUrl === 'string' && resource.sourceUrl.startsWith('https://')
      && typeof resource.version === 'number';
  });
}
