#!/usr/bin/env node
/**
 * Imports knowledge articles (markdown + front matter) into the backend through the admin API, so each one
 * goes through the normal review workflow: create DRAFT -> submit (chunk, IN_REVIEW) -> a different account
 * with knowledge:review approves it in the admin screen. This script never approves.
 *
 *   node recommend-model/tools/import-knowledge.mjs                      # dry run: validate + show chunks
 *   node recommend-model/tools/import-knowledge.mjs --apply              # create/revise + submit
 *   node recommend-model/tools/import-knowledge.mjs --archive-legacy legacy.csv --apply
 *
 * Options: --dir <folder> (repeatable; default knowledge/tp-v1 and knowledge/extra),
 *          --env <file> (default backend/.env), --api <url> (default MYLOG_API_URL or http://localhost:8080).
 * Credentials (env file or process env): MYLOG_KNOWLEDGE_EDITOR_EMAIL / _PASSWORD for import,
 * MYLOG_KNOWLEDGE_APPROVER_EMAIL / _PASSWORD for --archive-legacy. Nothing secret is printed.
 * Import state (slug -> itemId/version/checksum) is kept in scratch/knowledge-import/state.json (git-ignored).
 */
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const REQUIRED_FIELDS = ['slug', 'topicCode', 'locale', 'title', 'sourceName', 'ownerTeam'];
const REQUIRED_SECTIONS = ['Đây là gì', 'Nhận ra qua những câu như', 'Vì sao nhiều người gặp',
  'Thử từng bước (3 tuần)', 'Khi nào nên tìm thêm hỗ trợ', 'Nguồn'];
/** Sections stored as retrievable chunks; "Nguồn" stays in the file and in sourceName. */
const CHUNKED_SECTIONS = REQUIRED_SECTIONS.filter((s) => s !== 'Nguồn');
const STATE_FILE = join(ROOT, 'scratch', 'knowledge-import', 'state.json');

const args = parseArgs(process.argv.slice(2));
const env = { ...loadEnv(resolve(ROOT, args.env ?? 'backend/.env')), ...process.env };
const API = (args.api ?? env.MYLOG_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const APPLY = Boolean(args.apply);

if (args['archive-legacy']) {
  await archiveLegacy(resolve(args['archive-legacy']));
} else {
  await importArticles();
}

// ---- import --------------------------------------------------------------------------------------

async function importArticles() {
  const dirs = (args.dir ?? ['recommend-model/knowledge/tp-v1', 'recommend-model/knowledge/extra'])
    .map((d) => resolve(ROOT, d)).filter(existsSync);
  const articles = dirs.flatMap((d) => readdirSync(d).filter((f) => f.endsWith('.md') && f !== 'README.md')
    .map((f) => parseArticle(join(d, f))));
  const problems = articles.flatMap((a) => a.problems.map((p) => `${a.file}: ${p}`));
  const slugs = new Map();
  for (const a of articles) {
    if (slugs.has(a.meta.slug)) problems.push(`${a.file}: slug trùng với ${slugs.get(a.meta.slug)}`);
    slugs.set(a.meta.slug, a.file);
  }
  for (const a of articles) {
    console.log(`${a.meta.slug ?? a.file}  topic=${a.meta.topicCode}  chunks=${a.chunks.length}`
      + (a.unverifiedSources ? '  [nguồn chưa xác minh]' : ''));
    for (const c of a.chunks) if (c.length > 800) console.log(`    chunk ${c.length} ký tự sẽ bị backend cắt đôi`);
  }
  if (problems.length) {
    console.error('\nKhông hợp lệ:\n  ' + problems.join('\n  '));
    process.exit(1);
  }
  console.log(`\n${articles.length} bài hợp lệ.`);
  if (!APPLY) {
    console.log('Chạy thử: chưa gọi API. Thêm --apply để tạo DRAFT và gửi duyệt.');
    return;
  }

  const state = existsSync(STATE_FILE) ? JSON.parse(readFileSync(STATE_FILE, 'utf8')) : {};
  const session = await login(env.MYLOG_KNOWLEDGE_EDITOR_EMAIL, env.MYLOG_KNOWLEDGE_EDITOR_PASSWORD, 'editor');
  const summary = { created: 0, revised: 0, unchanged: 0 };
  for (const a of articles) {
    const known = state[a.meta.slug];
    if (known && known.checksum === a.checksum) { summary.unchanged++; continue; }
    let view;
    if (!known) {
      const { status, data } = await http(session, 'POST', '/api/v1/admin/knowledge-items', {
        slug: a.meta.slug, topicCode: a.meta.topicCode, locale: a.meta.locale, sourceName: a.sourceName,
        sourceUrl: a.meta.sourceUrl || null, ownerTeam: a.meta.ownerTeam, title: a.meta.title, content: a.content,
      }, [409]);
      if (status === 409) {
        console.error(`${a.meta.slug}: slug đã tồn tại trên server nhưng chưa có trong ${STATE_FILE}. Bỏ qua.`);
        continue;
      }
      view = data;
      summary.created++;
    } else {
      // Approved content is immutable: a change becomes a new version that goes through review again.
      ({ data: view } = await http(session, 'POST', `/api/v1/admin/knowledge-items/${known.itemId}/versions`,
        { title: a.meta.title, content: a.content }));
      summary.revised++;
    }
    await http(session, 'POST', `/api/v1/admin/knowledge-items/${view.itemId}/versions/${view.version}:submit`);
    state[a.meta.slug] = { itemId: view.itemId, version: view.version, checksum: a.checksum };
    mkdirSync(dirname(STATE_FILE), { recursive: true });
    writeFileSync(STATE_FILE, JSON.stringify(state, null, 2));
    console.log(`${a.meta.slug}: v${view.version} đã gửi duyệt (IN_REVIEW)`);
  }
  // The admin screen looks items up by ID, so hand the reviewer a list.
  const reviewList = join(dirname(STATE_FILE), 'review-list.md');
  writeFileSync(reviewList, ['| Slug | Tiêu đề | Item ID | Version |', '|---|---|---|---|',
    ...articles.filter((a) => state[a.meta.slug]).map((a) =>
      `| ${a.meta.slug} | ${a.meta.title} | ${state[a.meta.slug].itemId} | ${state[a.meta.slug].version} |`)].join('\n') + '\n');
  console.log(`\nTạo mới ${summary.created}, version mới ${summary.revised}, không đổi ${summary.unchanged}.`
    + ` Danh sách cho người duyệt: ${reviewList}`);
}

function parseArticle(file) {
  const raw = readFileSync(file, 'utf8').replace(/\r\n/g, '\n');
  const problems = [];
  const match = raw.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/);
  if (!match) return { file, meta: {}, chunks: [], problems: ['thiếu front matter'] };
  const meta = {};
  for (const line of match[1].split('\n')) {
    const m = line.match(/^(\w+):\s*(.*)$/);
    if (m) meta[m[1]] = m[2].trim();
  }
  for (const field of REQUIRED_FIELDS) if (!meta[field]) problems.push(`thiếu ${field}`);
  if (meta.slug && !/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(meta.slug)) problems.push('slug không hợp lệ');
  if (meta.topicCode && !/^[A-Z0-9_]{1,48}$/.test(meta.topicCode)) problems.push('topicCode không hợp lệ');
  if (meta.locale && !/^[a-z]{2}(?:-[A-Z]{2})?$/.test(meta.locale)) problems.push('locale không hợp lệ');
  if (meta.sourceUrl && !meta.sourceUrl.startsWith('https://')) problems.push('sourceUrl phải là https');

  const sections = new Map();
  let current = null;
  for (const line of match[2].split('\n')) {
    const heading = line.match(/^##\s+(.+)$/);
    if (heading) { current = heading[1].trim(); sections.set(current, []); continue; }
    if (current && line.trim()) sections.get(current).push(line.trim());
  }
  for (const name of REQUIRED_SECTIONS) if (!sections.get(name)?.length) problems.push(`thiếu mục "${name}"`);

  // One section = one paragraph = one chunk (the backend chunker splits on blank lines).
  const chunks = CHUNKED_SECTIONS.filter((n) => sections.get(n)?.length)
    .map((n) => `${n}: ${sections.get(n).map((l) => l.replace(/^-\s+/, '')).join(' ')}`);
  const content = chunks.join('\n\n');
  const references = (sections.get('Nguồn') ?? []).map((l) => l.replace(/^-\s+/, ''))
    .filter((l) => !l.startsWith('Cần người duyệt'));
  const unverifiedSources = (sections.get('Nguồn') ?? []).some((l) => l.includes('Cần người duyệt'));
  const shortRefs = references.map((r) => r.match(/^([^,(]+)[^(]*\((\d{4})\)/)).filter(Boolean)
    .map((m) => `${m[1].trim()} (${m[2]})`);
  let sourceName = meta.sourceName ?? '';
  if (shortRefs.length) sourceName = `${sourceName.replace(/\s*\(.*\)$/, '')}; tham khảo: ${shortRefs.join('; ')}`;
  if (sourceName.length > 240) sourceName = sourceName.slice(0, 239) + '…';
  if (title(meta).length > 300) problems.push('title quá 300 ký tự');
  return { file, meta, chunks, content, sourceName, unverifiedSources, problems,
    checksum: createHash('sha256').update(JSON.stringify([meta, content, sourceName])).digest('hex') };
}

function title(meta) { return meta.title ?? ''; }

// ---- archive the legacy corpus -------------------------------------------------------------------

/**
 * Archives APPROVED versions listed in a CSV with header "item_id,version" (see knowledge/README.md for
 * the read-only query that produces it). Archived content stays in the database for citation history.
 */
async function archiveLegacy(csvFile) {
  const rows = readFileSync(csvFile, 'utf8').replace(/\r\n/g, '\n').split('\n').map((l) => l.trim())
    .filter(Boolean).slice(1).map((l) => l.split(',').map((c) => c.replace(/^"|"$/g, '').trim()));
  const valid = rows.filter(([id, v]) => /^[0-9a-f-]{36}$/i.test(id) && /^\d+$/.test(v));
  if (valid.length !== rows.length) {
    console.error('CSV phải có header item_id,version và mỗi dòng là UUID,số.');
    process.exit(1);
  }
  console.log(`${valid.length} version sẽ được chuyển sang ARCHIVED (reasonCode RETIRED).`);
  if (!APPLY) { console.log('Chạy thử: chưa gọi API. Thêm --apply để archive.'); return; }
  const session = await login(env.MYLOG_KNOWLEDGE_APPROVER_EMAIL, env.MYLOG_KNOWLEDGE_APPROVER_PASSWORD, 'approver');
  let done = 0;
  for (const [itemId, version] of valid) {
    const { status } = await http(session, 'POST',
      `/api/v1/admin/knowledge-items/${itemId}/versions/${version}:archive`, { reasonCode: 'RETIRED' }, [400, 404, 409]);
    if (status >= 400) console.error(`${itemId} v${version}: HTTP ${status}, bỏ qua (có thể đã archive).`);
    else done++;
  }
  console.log(`Đã archive ${done}/${valid.length}.`);
}

// ---- http ----------------------------------------------------------------------------------------

async function login(email, password, role) {
  if (!email || !password) {
    console.error(`Thiếu thông tin đăng nhập ${role}: đặt MYLOG_KNOWLEDGE_${role.toUpperCase()}_EMAIL và _PASSWORD.`);
    process.exit(1);
  }
  const session = { token: null };
  const { data } = await http(session, 'POST', '/api/v1/auth/login',
    { email, password, deviceName: 'knowledge-import' });
  session.token = data.accessToken;
  return session;
}

async function http(session, method, path, body, okStatuses = []) {
  for (let attempt = 0; attempt < 20; attempt++) {
    const res = await fetch(API + path, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (res.status === 429) { console.log('  Đạt giới hạn ghi, chờ 60 giây...'); await new Promise((r) => setTimeout(r, 60_000)); continue; }
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = null; }
    if (!res.ok && !okStatuses.includes(res.status)) {
      throw new Error(`${method} ${path} -> HTTP ${res.status} ${data?.title ?? ''}`.trim());
    }
    return { status: res.status, data };
  }
  throw new Error(`${method} ${path}: vẫn bị giới hạn sau nhiều lần chờ.`);
}

// ---- helpers -------------------------------------------------------------------------------------

function parseArgs(list) {
  const out = {};
  for (let i = 0; i < list.length; i++) {
    const key = list[i].replace(/^--/, '');
    const value = list[i + 1] && !list[i + 1].startsWith('--') ? list[++i] : true;
    if (key === 'dir') (out.dir ??= []).push(value);
    else out[key] = value;
  }
  return out;
}

function loadEnv(file) {
  if (!existsSync(file)) return {};
  const out = {};
  for (const line of readFileSync(file, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
    if (m) out[m[1]] = m[2].replace(/^["']|["']$/g, '');
  }
  return out;
}
