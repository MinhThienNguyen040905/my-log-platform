import { randomBytes, randomUUID } from 'node:crypto';

const apiBase = process.env.MYLOG_E2E_API_BASE ?? 'http://127.0.0.1:18080';
const mailBase = process.env.MYLOG_E2E_MAILPIT_BASE ?? 'http://127.0.0.1:8025';
for (const address of [apiBase, mailBase]) {
  const parsed = new URL(address);
  if (parsed.protocol !== 'http:' || !['127.0.0.1', 'localhost'].includes(parsed.hostname)) {
    throw new Error('This smoke test only accepts local HTTP services.');
  }
}

const email = `mobile-e2e-${randomUUID()}@example.invalid`;
const password = `Synthetic-${randomBytes(18).toString('hex')}!`;
const steps = [];

async function request(path, method = 'GET', body, token, headers = {}) {
  const response = await fetch(`${apiBase}/api/v1${path}`, {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  return response;
}
async function expectStatus(response, status, name) {
  if (response.status !== status) throw new Error(`${name}: expected ${status}, got ${response.status}`);
  steps.push(name);
  return response;
}
function wait(ms) { return new Promise((resolve) => setTimeout(resolve, ms)); }

try {
  await expectStatus(await request('/auth/register', 'POST', {
    email, password, timezone: 'Asia/Ho_Chi_Minh', locale: 'vi',
    termsVersion: 'v1', privacyVersion: 'v1', acceptTerms: true, acceptPrivacy: true,
  }), 202, 'register');

  let code;
  for (let attempt = 0; attempt < 12 && !code; attempt++) {
    await wait(500);
    const messagesResponse = await fetch(`${mailBase}/api/v1/messages`);
    if (!messagesResponse.ok) throw new Error(`Mailpit list returned ${messagesResponse.status}`);
    const listing = await messagesResponse.json();
    const message = listing.messages?.find((item) => JSON.stringify(item.To).includes(email));
    if (!message) continue;
    const detailResponse = await fetch(`${mailBase}/api/v1/message/${encodeURIComponent(message.ID)}`);
    if (!detailResponse.ok) throw new Error(`Mailpit detail returned ${detailResponse.status}`);
    const detail = await detailResponse.json();
    const body = `${detail.Text ?? ''} ${detail.HTML ?? ''}`;
    code = body.match(/\b[0-9]{8}\b/)?.[0];
  }
  if (!code) throw new Error('No verification code arrived in local Mailpit.');

  await expectStatus(await request('/auth/email-verifications:confirm-code', 'POST', { email, code }), 204, 'verify-code');
  const loginResponse = await expectStatus(await request('/auth/login', 'POST', {
    email, password, deviceName: 'Mobile E2E synthetic',
  }), 200, 'login');
  const first = await loginResponse.json();
  if (!first.accessToken || !first.refreshToken) throw new Error('Login response has no tokens.');

  const meResponse = await expectStatus(await request('/me', 'GET', undefined, first.accessToken), 200, 'profile-read');
  const me = await meResponse.json();
  const etag = meResponse.headers.get('etag');
  if (!etag || me.userId !== first.userId) throw new Error('Profile identity or ETag mismatch.');

  const consentsResponse = await expectStatus(await request('/me/consents', 'GET', undefined, first.accessToken), 200, 'consents-read');
  const consents = await consentsResponse.json();
  if (!consents.some((item) => item.type === 'TERMS' && item.granted)
    || !consents.some((item) => item.type === 'PRIVACY' && item.granted)
    || consents.some((item) => ['AI_PROCESSING', 'ANALYTICS', 'MODEL_TRAINING'].includes(item.type) && item.granted)) {
    throw new Error('Initial consent state is unexpected.');
  }

  const updatedResponse = await expectStatus(await request('/me', 'PATCH', {
    displayName: 'Synthetic Mobile Tester', penName: 'Test Notebook', onboardingGoals: ['REFLECTION'],
  }, first.accessToken, { 'If-Match': etag }), 200, 'profile-update');
  const updated = await updatedResponse.json();
  if (updated.displayName !== 'Synthetic Mobile Tester') throw new Error('Profile update was not persisted.');
  await expectStatus(await request('/me', 'PATCH', { penName: 'Stale' }, first.accessToken,
    { 'If-Match': etag }), 409, 'stale-profile-conflict');

  const completedResponse = await expectStatus(await request('/me/onboarding:complete', 'POST', undefined,
    first.accessToken, { 'If-Match': updatedResponse.headers.get('etag') }), 200, 'onboarding-complete');
  const completed = await completedResponse.json();
  if (!completed.onboardingCompletedAt) throw new Error('Onboarding state did not change.');

  const contentJson = { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'A synthetic journal entry for API verification.' }] }] };
  const createResponse = await expectStatus(await request('/journal-entries', 'POST', {
    title: 'Synthetic mobile entry', contentJson, timezone: 'Asia/Ho_Chi_Minh', moodCode: 'calm',
  }, first.accessToken, { 'Idempotency-Key': randomUUID() }), 201, 'journal-create');
  const created = await createResponse.json();
  const journalId = created.id;
  const journalEtag = createResponse.headers.get('etag');
  if (!journalId || !journalEtag) throw new Error('Journal create omitted id or ETag.');
  await expectStatus(await request(`/journal-entries/${journalId}`, 'GET', undefined, first.accessToken), 200, 'journal-read');
  const listResponse = await expectStatus(await request('/journal-entries?limit=1', 'GET', undefined, first.accessToken), 200, 'journal-list');
  const listed = await listResponse.json();
  if (!listed.items.some((item) => item.id === journalId)) throw new Error('Created journal is absent from list.');
  const journalUpdate = await expectStatus(await request(`/journal-entries/${journalId}`, 'PATCH', {
    title: 'Updated synthetic entry', contentJson,
  }, first.accessToken, { 'If-Match': journalEtag }), 200, 'journal-update');
  await expectStatus(await request(`/journal-entries/${journalId}`, 'PATCH', { title: 'Stale update' },
    first.accessToken, { 'If-Match': journalEtag }), 409, 'stale-journal-conflict');
  await expectStatus(await request(`/journal-entries/${journalId}/favorite`, 'PUT', undefined, first.accessToken), 200, 'journal-favorite');
  const tagResponse = await expectStatus(await request('/journal-tags', 'POST', { name: `mobile-${randomUUID().slice(0, 8)}` }, first.accessToken), 201, 'tag-create');
  const tag = await tagResponse.json();
  await expectStatus(await request(`/journal-entries/${journalId}/tags/${tag.id}`, 'PUT', undefined, first.accessToken), 204, 'tag-attach');
  await expectStatus(await request('/journal-tags', 'GET', undefined, first.accessToken), 200, 'tag-list');
  await expectStatus(await request('/dashboard?range=30d', 'GET', undefined, first.accessToken), 200, 'dashboard-read');
  await expectStatus(await request('/insights?limit=20', 'GET', undefined, first.accessToken), 200, 'insights-read');
  await expectStatus(await request('/reports?limit=20', 'GET', undefined, first.accessToken), 200, 'reports-read');
  await expectStatus(await request('/self-care/goals', 'GET', undefined, first.accessToken), 200, 'goals-read');
  await expectStatus(await request('/me/sessions', 'GET', undefined, first.accessToken), 200, 'sessions-read');
  const latestResponse = await expectStatus(await request(`/journal-entries/${journalId}`, 'GET', undefined, first.accessToken), 200, 'journal-read-after-favorite');
  await expectStatus(await request(`/journal-entries/${journalId}`, 'DELETE', undefined, first.accessToken,
    { 'If-Match': latestResponse.headers.get('etag') }), 204, 'journal-delete');
  const exportResponse = await expectStatus(await request('/exports', 'POST', { format: 'CSV' }, first.accessToken), 202, 'export-request');
  let dataExport = await exportResponse.json();
  for (let attempt = 0; attempt < 20 && dataExport.status !== 'READY'; attempt++) {
    await wait(500);
    const statusResponse = await request(`/exports/${dataExport.id}`, 'GET', undefined, first.accessToken);
    if (!statusResponse.ok) throw new Error(`Export status returned HTTP ${statusResponse.status}`);
    dataExport = await statusResponse.json();
  }
  if (dataExport.status !== 'READY') throw new Error(`Export remained ${dataExport.status}`);
  steps.push('export-ready');
  const grantResponse = await expectStatus(await request(`/exports/${dataExport.id}:authorize-download`, 'POST',
    { password }, first.accessToken), 200, 'export-authorize');
  const grant = await grantResponse.json();
  if (!grant.url.startsWith(`/api/v1/exports/${dataExport.id}/file?`)) throw new Error('Export grant URL is invalid.');
  const fileResponse = await fetch(`${apiBase}${grant.url}`, { headers: { Authorization: `Bearer ${first.accessToken}` } });
  await expectStatus(fileResponse, 200, 'export-download');
  if (!(await fileResponse.arrayBuffer()).byteLength) throw new Error('Export file was empty.');

  const refreshResponse = await expectStatus(await request('/auth/refresh', 'POST', {
    refreshToken: first.refreshToken,
  }), 200, 'refresh');
  const rotated = await refreshResponse.json();
  if (!rotated.accessToken || !rotated.refreshToken || rotated.refreshToken === first.refreshToken) {
    throw new Error('Refresh token was not rotated.');
  }
  await expectStatus(await request('/me', 'GET', undefined, rotated.accessToken), 200, 'profile-after-refresh');
  await expectStatus(await request('/auth/logout', 'POST', undefined, rotated.accessToken), 204, 'logout');
  await expectStatus(await request('/me', 'GET', undefined, rotated.accessToken), 401, 'revoked-access');

  process.stdout.write(`PASS ${steps.join(', ')}\n`);
} catch (error) {
  process.stderr.write(`FAIL after ${steps.join(', ')}: ${error instanceof Error ? error.message : 'unknown error'}\n`);
  process.exitCode = 1;
}
