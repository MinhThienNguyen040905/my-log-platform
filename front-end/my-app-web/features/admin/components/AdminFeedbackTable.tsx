'use client';

import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Eye, MessageSquare, RefreshCw } from 'lucide-react';
import {
  getAdminFeedback,
  listAdminFeedback,
  updateAdminFeedback,
  type Feedback,
} from '@/features/feedback';
import { useToast } from '@/providers/ToastProvider';

const STATUS_OPTIONS = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'] as const;

export function AdminFeedbackTable({ userId }: { userId: string }) {
  const client = useQueryClient();
  const { showToast } = useToast();
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [selected, setSelected] = useState<Feedback | null>(null);
  const [nextStatus, setNextStatus] = useState<string>('IN_PROGRESS');
  const [assignedTo, setAssignedTo] = useState<string>('');
  const [updating, setUpdating] = useState(false);

  const queryKey = ['admin', userId, 'feedback', statusFilter] as const;
  const feedbackQuery = useQuery({
    queryKey,
    queryFn: () =>
      listAdminFeedback({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        limit: 50,
      }),
  });

  const items = feedbackQuery.data ?? [];

  const handleInspect = async (item: Feedback) => {
    setSelected(item);
    setNextStatus(item.status);
    setAssignedTo(item.assignedTo ?? '');
    try {
      const fresh = await getAdminFeedback(item.id);
      setSelected(fresh);
      setNextStatus(fresh.status);
      setAssignedTo(fresh.assignedTo ?? '');
    } catch {
      // Keep row snapshot if detail call fails.
    }
  };

  const handleUpdate = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selected) return;
    setUpdating(true);
    try {
      const updated = await updateAdminFeedback(selected.id, selected.version, {
        status: nextStatus,
        assignedTo: assignedTo.trim() || null,
      });
      setSelected(updated);
      await client.invalidateQueries({ queryKey: ['admin', userId, 'feedback'] });
      showToast({
        title: 'Đã cập nhật góp ý',
        message: `Phiếu #${updated.id.slice(0, 8)} đã chuyển sang ${updated.status}.`,
        type: 'success',
      });
    } catch {
      showToast({
        title: 'Cập nhật thất bại',
        message: 'Không thể cập nhật phiếu góp ý. Kiểm tra quyền hoặc tải lại dữ liệu.',
        type: 'error',
      });
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-3.5 rounded-2xl border-2 border-black shadow-neo-sm">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-space text-xs font-extrabold uppercase text-black flex items-center gap-1.5">
            <MessageSquare className="w-4 h-4" />
            <span>Trạng thái:</span>
          </span>
          {(['ALL', ...STATUS_OPTIONS] as const).map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1 rounded-xl border border-black font-space text-xs font-bold cursor-pointer ${
                statusFilter === st
                  ? 'bg-primary-container text-black shadow-neo-xs'
                  : 'bg-paper-warm/40 text-gray-700 hover:bg-paper-warm'
              }`}
            >
              {st}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={() => void feedbackQuery.refetch()}
          disabled={feedbackQuery.isFetching}
          className="px-3 py-1.5 rounded-xl border border-black bg-white hover:bg-paper-warm text-black font-space text-xs font-bold shadow-neo-xs flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${feedbackQuery.isFetching ? 'animate-spin' : ''}`} />
          <span>Làm mới</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 items-start">
        <div className="lg:col-span-7 bg-white border-2 border-black rounded-2xl shadow-neo-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-paper-warm border-b-2 border-black text-[11px] font-space font-extrabold uppercase text-gray-700">
                  <th className="py-3 px-4">Mã Phiếu</th>
                  <th className="py-3 px-4">Loại</th>
                  <th className="py-3 px-4">Trạng Thái</th>
                  <th className="py-3 px-4">Nội Dung</th>
                  <th className="py-3 px-4 text-right">Chi Tiết</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-black/15 text-xs font-space">
                {feedbackQuery.isPending ? (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-gray-500 font-bold">
                      Đang tải danh sách góp ý...
                    </td>
                  </tr>
                ) : items.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-gray-500 font-bold">
                      Chưa có phiếu góp ý nào trong danh sách.
                    </td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.id} className="hover:bg-paper-warm/30">
                      <td className="py-3 px-4 font-mono font-bold">{item.id.slice(0, 8)}...</td>
                      <td className="py-3 px-4 font-bold">{item.category}</td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded border border-black bg-paper-warm text-[11px] font-bold">
                          {item.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 max-w-xs truncate font-sans">{item.message}</td>
                      <td className="py-3 px-4 text-right">
                        <button
                          type="button"
                          onClick={() => void handleInspect(item)}
                          aria-label={`Xem chi tiết góp ý ${item.id}`}
                          className="inline-flex items-center gap-1 rounded-lg border border-black bg-white px-2.5 py-1 text-xs font-bold hover:bg-paper-warm cursor-pointer"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Xử lý</span>
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Khung xử lý chi tiết */}
        <div className="lg:col-span-5 bg-white border-2 border-black rounded-2xl p-5 shadow-neo-sm flex flex-col gap-3 text-xs">
          <h3 className="font-space text-sm font-extrabold uppercase text-black border-b border-black/15 pb-2">
            Chi Tiết &amp; Cập Nhật Trạng Thái
          </h3>

          {!selected ? (
            <p className="text-gray-500 font-space">
              Chọn một phiếu góp ý ở bảng bên trái để xem toàn bộ nội dung và cập nhật trạng thái xử lý.
            </p>
          ) : (
            <form onSubmit={(e) => void handleUpdate(e)} className="flex flex-col gap-3">
              <div className="rounded-xl border border-black/20 bg-paper-warm p-3 flex flex-col gap-1 font-mono text-[11px]">
                <div>ID: {selected.id}</div>
                <div>User ID: {selected.userId}</div>
                <div>Version: v{selected.version}</div>
              </div>

              <div className="rounded-xl border border-black bg-white p-3 whitespace-pre-wrap text-gray-900">
                {selected.message}
              </div>

              <label className="flex flex-col gap-1 font-space font-bold text-black">
                <span>Trạng thái mới</span>
                <select
                  value={nextStatus}
                  onChange={(e) => setNextStatus(e.target.value)}
                  aria-label="Trạng thái xử lý góp ý"
                  className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs text-black"
                >
                  {STATUS_OPTIONS.map((st) => (
                    <option key={st} value={st}>
                      {st}
                    </option>
                  ))}
                </select>
              </label>

              <label className="flex flex-col gap-1 font-space font-bold text-black">
                <span>Người phụ trách (UUID, tùy chọn)</span>
                <input
                  type="text"
                  value={assignedTo}
                  onChange={(e) => setAssignedTo(e.target.value)}
                  placeholder="Để trống hoặc nhập UUID admin..."
                  aria-label="UUID người phụ trách"
                  className="rounded-xl border border-black bg-paper-warm/40 px-3 py-2 text-xs font-mono text-black"
                />
              </label>

              <button
                type="submit"
                disabled={updating}
                className="self-start rounded-xl border-2 border-black bg-primary-container px-4 py-2 font-space text-xs font-extrabold text-black shadow-neo-xs cursor-pointer disabled:opacity-50"
              >
                {updating ? 'Đang lưu...' : 'Lưu cập nhật'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
