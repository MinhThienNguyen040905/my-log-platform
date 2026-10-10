import 'server-only';
import { authBackend, sameOrigin } from './backend';
import { accessToken, clearTokens, refreshSession } from './session';

const uuid = '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}';
const date = '\\d{4}-\\d{2}-\\d{2}';

type AllowedRoute = { methods: readonly string[]; pattern: RegExp; public?: boolean; file?: boolean };
const routes: AllowedRoute[] = [
  { methods: ['GET'], pattern: new RegExp(`^journal-entries/${uuid}/analysis$`) },
  { methods: ['POST'], pattern: new RegExp(`^journal-entries/${uuid}/analysis:retry$`) },
  { methods: ['GET'], pattern: /^(dashboard|insights|reports)$/ },
  { methods: ['GET'], pattern: new RegExp(`^reports/${uuid}$`) },
  { methods: ['GET', 'POST'], pattern: /^self-care\/goals$/ },
  { methods: ['PATCH'], pattern: new RegExp(`^self-care/goals/${uuid}$`) },
  { methods: ['POST'], pattern: new RegExp(`^self-care/goals/${uuid}/habits$`) },
  { methods: ['PUT', 'DELETE'], pattern: new RegExp(`^self-care/habits/${uuid}/completions/${date}$`) },
  { methods: ['GET'], pattern: /^recommendations$/ },
  { methods: ['POST'], pattern: /^exports$/ },
  { methods: ['GET'], pattern: new RegExp(`^exports/${uuid}$`) },
  { methods: ['POST'], pattern: new RegExp(`^exports/${uuid}:authorize-download$`) },
  { methods: ['GET'], pattern: new RegExp(`^exports/${uuid}/file$`), file: true },
  { methods: ['POST'], pattern: /^account-deletion-requests$/ },
  { methods: ['POST'], pattern: new RegExp(`^account-deletion-requests/${uuid}:(status|cancel)$`), public: true },
  { methods: ['POST'], pattern: /^feedback$/ },
  { methods: ['GET'], pattern: new RegExp(`^feedback/${uuid}$`) },
  { methods: ['GET'], pattern: /^admin\/feedback$/ },
  { methods: ['GET'], pattern: /^admin\/dashboard$/ },
  { methods: ['GET'], pattern: /^admin\/users$/ },
  { methods: ['GET'], pattern: new RegExp(`^admin/users/${uuid}/metadata$`) },
  { methods: ['POST'], pattern: new RegExp(`^admin/users/${uuid}:(suspend|restore)$`) },
  { methods: ['GET'], pattern: /^admin\/ai-jobs$/ },
  { methods: ['POST'], pattern: new RegExp(`^admin/ai-jobs/${uuid}:retry$`) },
  { methods: ['GET', 'PATCH'], pattern: new RegExp(`^admin/feedback/${uuid}$`) },
  { methods: ['POST'], pattern: /^admin\/knowledge-items$/ },
  { methods: ['POST'], pattern: new RegExp(`^admin/knowledge-items/${uuid}/versions$`) },
  { methods: ['GET', 'PATCH'], pattern: new RegExp(`^admin/knowledge-items/${uuid}/versions/\\d+$`) },
  { methods: ['POST'], pattern: new RegExp(`^admin/knowledge-items/${uuid}/versions/\\d+:(submit|approve|reject|archive)$`) },
];

export async function securedProxy(request: Request, segments: string[]) {
  const path = segments.join('/');
  const route = routes.find((candidate) => candidate.pattern.test(path) && candidate.methods.includes(request.method));
  if (!route) return new Response(null, { status: 404 });
  if (request.method !== 'GET' && !sameOrigin(request)) return new Response(null, { status: 403 });

  try {
    let token = route.public ? null : await accessToken() ?? await refreshSession();
    if (!route.public && !token) return new Response(null, { status: 401 });
    const body = request.method === 'GET' || request.method === 'DELETE' ? undefined : await request.text();
    const backendPath = `/${path}${new URL(request.url).search}`;
    const send = (bearer: string | null) => authBackend(backendPath, {
      method: request.method,
      headers: {
        ...(bearer ? { Authorization: `Bearer ${bearer}` } : {}),
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(request.headers.has('if-match') ? { 'If-Match': request.headers.get('if-match')! } : {}),
      },
      ...(body !== undefined ? { body } : {}),
    });

    let response = await send(token);
    if (response.status === 401 && !route.public) {
      token = await refreshSession();
      if (!token) return new Response(null, { status: 401 });
      response = await send(token);
    }
    if (response.status === 401 && !route.public) await clearTokens();
    if (path === 'account-deletion-requests' && response.ok) await clearTokens();

    const headers = new Headers({ 'Cache-Control': 'no-store' });
    for (const name of ['content-type', 'etag']) {
      const value = response.headers.get(name);
      if (value) headers.set(name, value);
    }
    if (route.file && response.ok) {
      for (const name of ['content-disposition', 'x-content-type-options']) {
        const value = response.headers.get(name);
        if (value) headers.set(name, value);
      }
    }
    return new Response(response.status === 204 ? null : response.body, { status: response.status, headers });
  } catch {
    return new Response(null, { status: 503, headers: { 'Cache-Control': 'no-store' } });
  }
}
