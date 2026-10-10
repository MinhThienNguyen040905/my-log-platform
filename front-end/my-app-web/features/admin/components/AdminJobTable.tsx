'use client';

import React, { useState } from 'react';
import {
  Cpu,
  Search,
  RefreshCw,
  RotateCw,
  AlertCircle,
  CheckCircle2,
  Clock,
  Loader2,
} from 'lucide-react';
import { AdminJobItem } from '../api/types';
import { retryAdminJob } from '../api/admin-api';
import { useToast } from '@/providers/ToastProvider';

interface AdminJobTableProps {
  jobs: AdminJobItem[];
  loading: boolean;
  onRefresh: () => void;
}

export function AdminJobTable({ jobs, loading, onRefresh }: AdminJobTableProps) {
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [retryingJobId, setRetryingJobId] = useState<string | null>(null);
  const { showToast } = useToast();

  const handleRetry = async (jobId: string) => {
    setRetryingJobId(jobId);
    try {
      const ok = await retryAdminJob(jobId);
      if (ok) {
        showToast({
          title: 'Đã kích hoạt chạy lại!',
          message: `Công việc ${jobId.slice(0, 8)}... đã được đưa vào hàng đợi xử lý.`,
          type: 'success',
        });
        onRefresh();
      } else {
        throw new Error('Retry failed');
      }
    } catch {
      showToast({
        title: 'Thao tác retry thất bại',
        message: 'Hệ thống không thể tái kích hoạt tác vụ vào lúc này.',
        type: 'error',
      });
    } finally {
      setRetryingJobId(null);
    }
  };

  const filteredJobs = jobs.filter(job => {
    const matchesFilter = filterStatus === 'ALL' || job.status === filterStatus;
    const matchesSearch =
      job.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      job.type.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (job.errorCode && job.errorCode.toLowerCase().includes(searchTerm.toLowerCase()));
    return matchesFilter && matchesSearch;
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-primary-container text-black font-space text-[11px] font-extrabold border border-black shadow-neo-xs">
            <CheckCircle2 className="w-3 h-3 stroke-[2.5]" />
            COMPLETED
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-rose-200 text-rose-900 font-space text-[11px] font-extrabold border border-black shadow-neo-xs">
            <AlertCircle className="w-3 h-3 stroke-[2.5]" />
            FAILED
          </span>
        );
      case 'RUNNING':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-amber-200 text-black font-space text-[11px] font-extrabold border border-black shadow-neo-xs">
            <Loader2 className="w-3 h-3 animate-spin stroke-[2.5]" />
            RUNNING
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-paper-warm text-black font-space text-[11px] font-extrabold border border-black shadow-neo-xs">
            <Clock className="w-3 h-3 stroke-[2]" />
            {status}
          </span>
        );
    }
  };

  return (
    <div className="flex flex-col gap-4">
      {/* Controls Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-3.5 rounded-2xl border-2 border-black shadow-neo-sm">
        {/* Status Filters */}
        <div className="flex items-center gap-1.5 p-1 bg-paper-warm/50 rounded-xl border border-black">
          {['ALL', 'FAILED', 'RUNNING', 'COMPLETED', 'PENDING'].map(st => (
            <button
              key={st}
              type="button"
              onClick={() => setFilterStatus(st)}
              className={`px-3 py-1 rounded-lg font-space text-xs font-extrabold transition-all cursor-pointer ${
                filterStatus === st
                  ? 'bg-black text-white shadow-neo-xs'
                  : 'text-gray-700 hover:text-black hover:bg-white'
              }`}
            >
              {st}
            </button>
          ))}
        </div>

        {/* Search & Refresh */}
        <div className="flex items-center gap-2 flex-1 max-w-sm justify-end">
          <div className="relative w-full max-w-xs">
            <Search className="w-4 h-4 text-gray-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              placeholder="Tìm theo Type, Job ID hoặc ErrorCode..."
              className="w-full bg-paper-warm/40 border border-black rounded-xl pl-9 pr-3 py-1.5 font-space text-xs focus:outline-none focus:border-black focus:bg-white transition-all placeholder:text-gray-400 font-bold"
            />
          </div>

          <button
            type="button"
            onClick={onRefresh}
            disabled={loading}
            className="px-3 py-1.5 rounded-xl border border-black bg-white hover:bg-paper-warm text-black font-space text-xs font-bold transition-all shadow-neo-xs flex items-center gap-1.5 cursor-pointer disabled:opacity-50 shrink-0"
            title="Tải lại danh sách jobs"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* Jobs Table */}
      <div className="bg-white border-2 border-black rounded-2xl shadow-neo-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-paper-warm border-b-2 border-black text-[11px] font-space font-extrabold uppercase text-gray-700">
                <th className="py-3 px-4">Job ID / User ID</th>
                <th className="py-3 px-4">Loại Tác Vụ (Type)</th>
                <th className="py-3 px-4">Trạng Thái</th>
                <th className="py-3 px-4 text-center">Lần Thử (Attempts)</th>
                <th className="py-3 px-4">Mã Lỗi (Error Code)</th>
                <th className="py-3 px-4">Thời Gian Tạo</th>
                <th className="py-3 px-4 text-right">Điều Phối (Action)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-black/15 text-xs font-space font-medium">
              {loading && jobs.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-gray-500 font-space font-bold">
                    Đang tải danh sách hàng đợi AI...
                  </td>
                </tr>
              ) : filteredJobs.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-gray-500 font-space font-bold">
                    Không có tiến trình AI nào trong hàng đợi.
                  </td>
                </tr>
              ) : (
                filteredJobs.map(job => {
                  const isRetrying = retryingJobId === job.id;
                  const isFailed = job.status === 'FAILED';
                  return (
                    <tr key={job.id} className="hover:bg-paper-warm/30 transition-colors">
                      {/* Job & User ID */}
                      <td className="py-3.5 px-4">
                        <div className="flex flex-col gap-0.5">
                          <code className="font-mono text-xs font-bold text-black">
                            {job.id.slice(0, 16)}...
                          </code>
                          <span className="font-mono text-[10px] text-gray-500">
                            User: {job.userId.slice(0, 8)}...
                          </span>
                        </div>
                      </td>

                      {/* Type */}
                      <td className="py-3.5 px-4 font-space font-bold text-gray-900">
                        {job.type}
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-4">{getStatusBadge(job.status)}</td>

                      {/* Attempts */}
                      <td className="py-3.5 px-4 text-center font-mono">
                        <span className="bg-paper-warm px-2 py-0.5 rounded border border-black font-extrabold text-xs">
                          {job.attempt}/5
                        </span>
                      </td>

                      {/* Error Code (Safe technical error code only) */}
                      <td className="py-3.5 px-4">
                        {job.errorCode ? (
                          <span className="font-mono text-[11px] text-rose-700 bg-rose-50 px-2 py-0.5 rounded border border-rose-300 font-bold inline-block max-w-[220px] truncate" title={job.errorCode}>
                            {job.errorCode}
                          </span>
                        ) : (
                          <span className="text-gray-400 font-sans italic">—</span>
                        )}
                      </td>

                      {/* Created At */}
                      <td className="py-3.5 px-4 font-sans text-gray-600">
                        {new Date(job.createdAt).toLocaleString('vi-VN', {
                          hour: '2-digit',
                          minute: '2-digit',
                          day: '2-digit',
                          month: '2-digit',
                        })}
                      </td>

                      {/* Action */}
                      <td className="py-3.5 px-4 text-right">
                        {isFailed ? (
                          <button
                            type="button"
                            onClick={() => handleRetry(job.id)}
                            disabled={isRetrying}
                            className="px-2.5 py-1.5 bg-primary-container hover:bg-[#a5f01e] text-black border border-black rounded-lg text-xs font-extrabold transition-all shadow-neo-xs inline-flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                            title="Tái kích hoạt tiến trình chạy lại"
                          >
                            <RotateCw
                              className={`w-3.5 h-3.5 stroke-[2.5] ${
                                isRetrying ? 'animate-spin' : ''
                              }`}
                            />
                            <span>{isRetrying ? 'Đang gửi...' : 'Thử lại (Retry)'}</span>
                          </button>
                        ) : (
                          <span className="text-gray-400 font-sans text-xs italic">
                            {job.status === 'RUNNING' ? 'Đang thực thi' : 'Hoàn tất'}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

