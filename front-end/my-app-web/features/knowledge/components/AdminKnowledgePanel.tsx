'use client';

import React, { useState } from 'react';
import {
  Archive,
  BookOpen,
  CheckCircle2,
  FilePlus2,
  Search,
  Send,
  XCircle,
} from 'lucide-react';
import {
  approveKnowledgeVersion,
  archiveKnowledgeVersion,
  createKnowledgeItem,
  createKnowledgeVersion,
  getKnowledgeVersion,
  rejectKnowledgeVersion,
  submitKnowledgeVersion,
  updateKnowledgeDraft,
  type KnowledgeVersion,
} from '../api/admin-knowledge';

const TOPIC_CODES = ['SELF_CARE', 'SLEEP', 'WORK', 'HEALTH', 'RELATIONSHIPS', 'OTHER'] as const;
const REASON_CODES = [
  { value: 'INACCURATE_SOURCE', label: 'Nguồn chưa chính xác (INACCURATE_SOURCE)' },
  { value: 'POLICY_MISMATCH', label: 'Chưa đúng chính sách an toàn (POLICY_MISMATCH)' },
  { value: 'OUTDATED_CONTENT', label: 'Nội dung đã cũ (OUTDATED_CONTENT)' },
] as const;

export function AdminKnowledgePanel() {
  // Lookup existing item/version
  const [lookupItemId, setLookupItemId] = useState('');
  const [lookupVersion, setLookupVersion] = useState<number>(1);

  // Create new item form
  const [slug, setSlug] = useState('');
  const [topicCode, setTopicCode] = useState<string>('SELF_CARE');
  const [locale, setLocale] = useState<string>('vi-VN');
  const [sourceName, setSourceName] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');
  const [ownerTeam, setOwnerTeam] = useState('wellness-editorial');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');

  // Active version state & editing
  const [current, setCurrent] = useState<KnowledgeVersion | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [reasonCode, setReasonCode] = useState<string>('OUTDATED_CONTENT');

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const syncCurrent = (ver: KnowledgeVersion, message: string) => {
    setCurrent(ver);
    setLookupItemId(ver.itemId);
    setLookupVersion(ver.version);
    setEditTitle(ver.title);
    setEditContent(ver.content);
    setNotice(message);
    setError(null);
  };

  const handleCreateItem = async (event: React.FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const created = await createKnowledgeItem({
        slug: slug.trim().toLowerCase(),
        topicCode,
        locale,
        sourceName: sourceName.trim(),
        sourceUrl: sourceUrl.trim() || null,
        ownerTeam: ownerTeam.trim(),
        title: title.trim(),
        content: content.trim(),
      });
      syncCurrent(created, `Đã tạo mục kiến thức mới (v${created.version} - ${created.status}).`);
      setSlug('');
      setTitle('');
      setContent('');
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Không thể tạo mục kiến thức. Kiểm tra slug (chữ thường, gạch ngang) và các trường bắt buộc.',
      );
    } finally {
      setBusy(false);
    }
  };

  const handleLookup = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!lookupItemId.trim()) return;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const found = await getKnowledgeVersion(lookupItemId.trim(), lookupVersion);
      syncCurrent(found, `Đã tải phiên bản v${found.version} (${found.status}).`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tìm thấy phiên bản kiến thức.');
    } finally {
      setBusy(false);
    }
  };

  const runVersionAction = async (
    action: () => Promise<KnowledgeVersion>,
    successMsg: string,
  ) => {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await action();
      syncCurrent(updated, successMsg);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Thao tác không thành công.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      {/* Cột trái: Tạo mục kiến thức mới */}
      <form
        onSubmit={(e) => void handleCreateItem(e)}
        aria-label="Tạo mục kiến thức mới"
        className="lg:col-span-6 bg-white border-2 border-black rounded-2xl p-5 shadow-neo-sm flex flex-col gap-3.5 text-xs"
      >
        <div className="flex items-center gap-2 border-b-2 border-black/15 pb-3">
          <FilePlus2 className="w-4 h-4 text-black" />
          <h3 className="font-space text-sm font-extrabold uppercase text-black">
            Tạo Mục Kiến Thức Mới (Draft v1)
          </h3>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Slug định danh (a-z0-9-)</span>
            <input
              type="text"
              value={slug}
              onChange={(e) => setSlug(e.target.value)}
              required
              placeholder="vd: hieu-ve-giac-ngu-sau"
              aria-label="Slug định danh"
              className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs font-mono text-black"
            />
          </label>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Chủ đề (Topic Code)</span>
            <select
              value={topicCode}
              onChange={(e) => setTopicCode(e.target.value)}
              aria-label="Chủ đề kiến thức"
              className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs text-black"
            >
              {TOPIC_CODES.map((code) => (
                <option key={code} value={code}>
                  {code}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Ngôn ngữ (Locale)</span>
            <select
              value={locale}
              onChange={(e) => setLocale(e.target.value)}
              aria-label="Ngôn ngữ tài liệu"
              className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs text-black"
            >
              <option value="vi-VN">vi-VN</option>
              <option value="vi">vi</option>
              <option value="en-US">en-US</option>
              <option value="en">en</option>
            </select>
          </label>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Tên nguồn trích dẫn</span>
            <input
              type="text"
              value={sourceName}
              onChange={(e) => setSourceName(e.target.value)}
              required
              placeholder="VD: Cẩm nang Chánh niệm"
              aria-label="Tên nguồn trích dẫn"
              className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs text-black"
            />
          </label>

          <label className="flex flex-col gap-1 font-space font-bold text-black">
            <span>Nhóm phụ trách</span>
            <input
              type="text"
              value={ownerTeam}
              onChange={(e) => setOwnerTeam(e.target.value)}
              required
              aria-label="Nhóm phụ trách"
              className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs text-black"
            />
          </label>
        </div>

        <label className="flex flex-col gap-1 font-space font-bold text-black">
          <span>URL nguồn (tùy chọn, https://...)</span>
          <input
            type="url"
            value={sourceUrl}
            onChange={(e) => setSourceUrl(e.target.value)}
            placeholder="https://example.org/sleep-hygiene"
            aria-label="URL nguồn"
            className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs text-black"
          />
        </label>

        <label className="flex flex-col gap-1 font-space font-bold text-black">
          <span>Tiêu đề phiên bản</span>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            placeholder="Tiêu đề tài liệu kiến thức"
            aria-label="Tiêu đề phiên bản"
            className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs text-black"
          />
        </label>

        <label className="flex flex-col gap-1 font-space font-bold text-black">
          <span>Nội dung kiến thức</span>
          <textarea
            rows={4}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            required
            placeholder="Nội dung đã biên tập chuẩn bị cho quy trình duyệt và chia đoạn (chunking)..."
            aria-label="Nội dung kiến thức"
            className="rounded-xl border border-black bg-paper-warm/40 p-3 text-xs text-black"
          />
        </label>

        <button
          type="submit"
          disabled={busy}
          className="self-start rounded-xl border-2 border-black bg-primary-container px-4 py-2 font-space text-xs font-extrabold text-black shadow-neo-xs hover:bg-lime-200 cursor-pointer disabled:opacity-50"
        >
          {busy ? 'Đang xử lý...' : 'Tạo bản nháp kiến thức'}
        </button>
      </form>

      {/* Cột phải: Tra cứu & Quy trình duyệt phiên bản */}
      <div className="lg:col-span-6 flex flex-col gap-4">
        <form
          onSubmit={(e) => void handleLookup(e)}
          aria-label="Tra cứu phiên bản kiến thức"
          className="bg-white border-2 border-black rounded-2xl p-5 shadow-neo-sm flex flex-col gap-3 text-xs"
        >
          <div className="flex items-center gap-2 border-b-2 border-black/15 pb-3">
            <Search className="w-4 h-4 text-black" />
            <h3 className="font-space text-sm font-extrabold uppercase text-black">
              Tra Cứu &amp; Duyệt Phiên Bản Kiến Thức
            </h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-2.5 items-end">
            <label className="sm:col-span-2 flex flex-col gap-1 font-space font-bold text-black">
              <span>Item UUID</span>
              <input
                type="text"
                value={lookupItemId}
                onChange={(e) => setLookupItemId(e.target.value)}
                required
                placeholder="UUID của mục kiến thức"
                aria-label="UUID mục kiến thức"
                className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs font-mono text-black"
              />
            </label>

            <label className="flex flex-col gap-1 font-space font-bold text-black">
              <span>Phiên bản</span>
              <input
                type="number"
                min={1}
                value={lookupVersion}
                onChange={(e) => setLookupVersion(Number(e.target.value) || 1)}
                aria-label="Số phiên bản"
                className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs font-mono text-black"
              />
            </label>

            <button
              type="submit"
              disabled={busy}
              className="rounded-xl border-2 border-black bg-white hover:bg-paper-warm px-3 py-2 font-space text-xs font-extrabold text-black shadow-neo-xs cursor-pointer disabled:opacity-50"
            >
              Tải phiên bản
            </button>
          </div>
        </form>

        {error && (
          <p role="alert" className="rounded-xl border-2 border-red-700 bg-red-50 p-3 text-xs font-bold text-red-900">
            {error}
          </p>
        )}
        {notice && (
          <p role="status" className="rounded-xl border-2 border-emerald-700 bg-emerald-50 p-3 text-xs font-bold text-emerald-900">
            {notice}
          </p>
        )}

        {current && (
          <div className="bg-white border-2 border-black rounded-2xl p-5 shadow-neo-sm flex flex-col gap-4 text-xs">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-black/15 pb-3">
              <div className="flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-black" />
                <span className="font-space font-extrabold text-sm text-black">
                  {current.slug} · v{current.version}
                </span>
              </div>
              <span className="px-2.5 py-1 rounded-lg border border-black bg-primary-container font-space text-[11px] font-extrabold text-black">
                {current.status}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-2 font-mono text-[11px] bg-paper-warm p-3 rounded-xl border border-black/20">
              <div>Item ID: {current.itemId}</div>
              <div>Chủ đề: {current.topicCode} ({current.locale})</div>
            </div>

            <label className="flex flex-col gap-1 font-space font-bold text-black">
              <span>Tiêu đề phiên bản</span>
              <input
                type="text"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                aria-label="Chỉnh sửa tiêu đề phiên bản"
                className="rounded-xl border border-black bg-white px-3 py-2 text-xs text-black"
              />
            </label>

            <label className="flex flex-col gap-1 font-space font-bold text-black">
              <span>Nội dung phiên bản</span>
              <textarea
                rows={4}
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                aria-label="Chỉnh sửa nội dung phiên bản"
                className="rounded-xl border border-black bg-white p-3 text-xs text-black"
              />
            </label>

            {/* Nhóm nút chỉnh sửa Draft / tạo Version mới */}
            <div className="flex flex-wrap items-center gap-2">
              {current.status === 'DRAFT' && (
                <>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() =>
                      void runVersionAction(
                        () =>
                          updateKnowledgeDraft(current.itemId, current.version, {
                            title: editTitle.trim(),
                            content: editContent.trim(),
                          }),
                        'Đã lưu cập nhật bản nháp.',
                      )
                    }
                    className="rounded-xl border border-black bg-white px-3 py-1.5 font-space text-xs font-bold hover:bg-paper-warm cursor-pointer disabled:opacity-50"
                  >
                    Cập nhật Draft
                  </button>

                  <button
                    type="button"
                    disabled={busy}
                    onClick={() =>
                      void runVersionAction(
                        () => submitKnowledgeVersion(current.itemId, current.version),
                        'Đã gửi phiên bản sang trạng thái chờ duyệt (IN_REVIEW).',
                      )
                    }
                    className="inline-flex items-center gap-1 rounded-xl border-2 border-black bg-primary-container px-3 py-1.5 font-space text-xs font-extrabold shadow-neo-xs cursor-pointer disabled:opacity-50"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>Gửi duyệt (Submit)</span>
                  </button>
                </>
              )}

              <button
                type="button"
                disabled={busy}
                onClick={() =>
                  void runVersionAction(
                    () =>
                      createKnowledgeVersion(current.itemId, {
                        title: editTitle.trim(),
                        content: editContent.trim(),
                      }),
                    'Đã tạo phiên bản Draft mới tiếp theo.',
                  )
                }
                className="rounded-xl border border-black bg-paper-warm px-3 py-1.5 font-space text-xs font-bold hover:bg-white cursor-pointer disabled:opacity-50"
              >
                + Tạo Version Mới
              </button>
            </div>

            {/* Nhóm nút Duyệt / Từ chối / Lưu trữ */}
            {(current.status === 'IN_REVIEW' || current.status === 'APPROVED') && (
              <div className="border-t border-black/15 pt-3 flex flex-col gap-2.5">
                <label className="flex flex-col gap-1 font-space font-bold text-black">
                  <span>Mã lý do (dùng khi Từ chối hoặc Lưu trữ)</span>
                  <select
                    value={reasonCode}
                    onChange={(e) => setReasonCode(e.target.value)}
                    aria-label="Mã lý do từ chối hoặc lưu trữ"
                    className="rounded-xl border border-black bg-white px-3 py-1.5 text-xs text-black"
                  >
                    {REASON_CODES.map((rc) => (
                      <option key={rc.value} value={rc.value}>
                        {rc.label}
                      </option>
                    ))}
                  </select>
                </label>

                <div className="flex flex-wrap items-center gap-2">
                  {current.status === 'IN_REVIEW' && (
                    <>
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() =>
                          void runVersionAction(
                            () => approveKnowledgeVersion(current.itemId, current.version),
                            'Đã phê duyệt phiên bản (APPROVED).',
                          )
                        }
                        className="inline-flex items-center gap-1 rounded-xl border-2 border-black bg-primary-container px-3 py-1.5 font-space text-xs font-extrabold shadow-neo-xs cursor-pointer disabled:opacity-50"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Phê duyệt (Approve)</span>
                      </button>

                      <button
                        type="button"
                        disabled={busy}
                        onClick={() =>
                          void runVersionAction(
                            () =>
                              rejectKnowledgeVersion(current.itemId, current.version, reasonCode),
                            'Đã từ chối phiên bản (REJECTED).',
                          )
                        }
                        className="inline-flex items-center gap-1 rounded-xl border border-black bg-rose-100 text-rose-900 px-3 py-1.5 font-space text-xs font-bold cursor-pointer disabled:opacity-50"
                      >
                        <XCircle className="w-3.5 h-3.5" />
                        <span>Từ chối (Reject)</span>
                      </button>
                    </>
                  )}

                  {current.status === 'APPROVED' && (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() =>
                        void runVersionAction(
                          () =>
                            archiveKnowledgeVersion(current.itemId, current.version, reasonCode),
                          'Đã lưu trữ phiên bản (ARCHIVED).',
                        )
                      }
                      className="inline-flex items-center gap-1 rounded-xl border border-black bg-amber-100 text-amber-950 px-3 py-1.5 font-space text-xs font-bold cursor-pointer disabled:opacity-50"
                    >
                      <Archive className="w-3.5 h-3.5" />
                      <span>Lưu trữ (Archive)</span>
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
