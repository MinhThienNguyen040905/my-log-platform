'use client';

import React, { useState } from 'react';
import {
  Users,
  Search,
  RefreshCw,
  Eye,
  Ban,
  CheckCircle2,
  Copy,
  Check,
} from 'lucide-react';
import { AdminUserItem } from '../api/types';
import { AdminUserActionModal } from './AdminUserActionModal';
import { AdminUserMetadataDrawer } from './AdminUserMetadataDrawer';
import { suspendAdminUser, restoreAdminUser } from '../api/admin-api';
import { useToast } from '@/lib/toast-context';

interface AdminUserTableProps {
  users: AdminUserItem[];
  loading: boolean;
  onRefresh: () => void;
}

export function AdminUserTable({ users, loading, onRefresh }: AdminUserTableProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedUser, setSelectedUser] = useState<AdminUserItem | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [actionModal, setActionModal] = useState<{
    isOpen: boolean;
    type: 'SUSPEND' | 'RESTORE';
    userId: string;
  }>({
    isOpen: false,
    type: 'SUSPEND',
    userId: '',
  });
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const { showToast } = useToast();

  const handleCopyId = (id: string) => {
    navigator.clipboard.writeText(id);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  const handleOpenAction = (type: 'SUSPEND' | 'RESTORE', userId: string) => {
    setActionModal({
      isOpen: true,
      type,
      userId,
    });
  };

  const handleConfirmAction = async (reasonCode: string) => {
    try {
      if (actionModal.type === 'SUSPEND') {
        await suspendAdminUser(actionModal.userId, { reasonCode });
        showToast({
          title: 'Đã đình chỉ tài khoản!',
          message: `Người dùng ${actionModal.userId.slice(0, 8)}... đã bị tạm khóa.`,
          type: 'success',
        });
      } else {
        await restoreAdminUser(actionModal.userId, { reasonCode });
        showToast({
          title: 'Đã khôi phục tài khoản!',
          message: `Người dùng ${actionModal.userId.slice(0, 8)}... đã hoạt động trở lại.`,
          type: 'success',
        });
      }
      onRefresh();
    } catch {
      showToast({
        title: 'Thao tác không thành công',
        message: 'Có lỗi xảy ra khi cập nhật trạng thái người dùng.',
        type: 'error',
      });
    }
  };

  const filteredUsers = users.filter(user =>
    user.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.status.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex flex-col gap-4">
      {/* Search and Action Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-3.5 rounded-2xl border-2 border-black shadow-neo-sm">
        <div className="flex items-center gap-2 flex-1 max-w-md">
          <div className="relative w-full">
            <Search className="w-4 h-4 text-gray-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              placeholder="Tìm kiếm theo UUID người dùng hoặc trạng thái..."
              className="w-full bg-paper-warm/40 border border-black rounded-xl pl-9 pr-3 py-1.5 font-space text-xs focus:outline-none focus:border-black focus:bg-white transition-all placeholder:text-gray-400 font-bold"
            />
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onRefresh}
            disabled={loading}
            className="px-3 py-1.5 rounded-xl border border-black bg-white hover:bg-paper-warm text-black font-space text-xs font-bold transition-all shadow-neo-xs flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
            title="Tải lại danh sách"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* Table Container */}
      <div className="bg-white border-2 border-black rounded-2xl shadow-neo-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-paper-warm border-b-2 border-black text-[11px] font-space font-extrabold uppercase text-gray-700">
                <th className="py-3 px-4">Người Dùng (UUID v7)</th>
                <th className="py-3 px-4">Trạng Thái</th>
                <th className="py-3 px-4">Ngày Tạo</th>
                <th className="py-3 px-4">Đăng Nhập Cuối</th>
                <th className="py-3 px-4 text-center">Version</th>
                <th className="py-3 px-4 text-right">Thao Tác Quản Trị</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-black/15 text-xs font-space font-medium">
              {loading && users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-gray-500 font-space font-bold">
                    Đang tải danh sách người dùng...
                  </td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-gray-500 font-space font-bold">
                    Không tìm thấy người dùng nào phù hợp.
                  </td>
                </tr>
              ) : (
                filteredUsers.map(user => {
                  const isActive = user.status === 'ACTIVE';
                  return (
                    <tr
                      key={user.id}
                      className="hover:bg-paper-warm/30 transition-colors"
                    >
                      {/* UUID Column with copy */}
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2">
                          <code className="font-mono text-xs font-bold text-black bg-gray-100 px-2 py-0.5 rounded border border-black/20">
                            {user.id.slice(0, 18)}...
                          </code>
                          <button
                            type="button"
                            onClick={() => handleCopyId(user.id)}
                            className="p-1 rounded hover:bg-gray-200 transition-colors cursor-pointer text-gray-600 hover:text-black"
                            title="Sao chép toàn bộ UUID"
                          >
                            {copiedId === user.id ? (
                              <Check className="w-3.5 h-3.5 text-green-700 stroke-[3]" />
                            ) : (
                              <Copy className="w-3.5 h-3.5 stroke-[2]" />
                            )}
                          </button>
                        </div>
                      </td>

                      {/* Status Badge */}
                      <td className="py-3.5 px-4">
                        {isActive ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-primary-container text-black font-space text-[11px] font-extrabold border border-black shadow-neo-xs">
                            <CheckCircle2 className="w-3 h-3 stroke-[2.5]" />
                            ACTIVE
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-rose-200 text-rose-900 font-space text-[11px] font-extrabold border border-black shadow-neo-xs">
                            <Ban className="w-3 h-3 stroke-[2.5]" />
                            SUSPENDED
                          </span>
                        )}
                      </td>

                      {/* Created At */}
                      <td className="py-3.5 px-4 font-sans text-gray-600">
                        {new Date(user.createdAt).toLocaleDateString('vi-VN')}
                      </td>

                      {/* Last Login */}
                      <td className="py-3.5 px-4 font-sans text-gray-600">
                        {user.lastLoginAt
                          ? new Date(user.lastLoginAt).toLocaleString('vi-VN', {
                              hour: '2-digit',
                              minute: '2-digit',
                              day: '2-digit',
                              month: '2-digit',
                            })
                          : 'Chưa đăng nhập'}
                      </td>

                      {/* Version */}
                      <td className="py-3.5 px-4 text-center">
                        <span className="font-mono text-[11px] bg-paper-warm px-1.5 py-0.5 rounded border border-black/40 font-bold">
                          v{user.version}
                        </span>
                      </td>

                      {/* Actions */}
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Metadata view */}
                          <button
                            type="button"
                            onClick={() => {
                              setSelectedUser(user);
                              setIsDrawerOpen(true);
                            }}
                            className="px-2 py-1 bg-white hover:bg-paper-warm text-black border border-black rounded-lg text-xs font-bold transition-all shadow-neo-xs flex items-center gap-1 cursor-pointer"
                            title="Xem chi tiết Metadata kỹ thuật"
                          >
                            <Eye className="w-3 h-3" />
                            <span>Chi tiết</span>
                          </button>

                          {/* Suspend or Restore Button */}
                          {isActive ? (
                            <button
                              type="button"
                              onClick={() => handleOpenAction('SUSPEND', user.id)}
                              className="px-2 py-1 bg-rose-100 hover:bg-rose-200 text-rose-900 border border-black rounded-lg text-xs font-extrabold transition-all shadow-neo-xs flex items-center gap-1 cursor-pointer"
                              title="Tạm khóa tài khoản"
                            >
                              <Ban className="w-3 h-3" />
                              <span>Đình chỉ</span>
                            </button>
                          ) : (
                            <button
                              type="button"
                              onClick={() => handleOpenAction('RESTORE', user.id)}
                              className="px-2 py-1 bg-primary-container hover:bg-[#a5f01e] text-black border border-black rounded-lg text-xs font-extrabold transition-all shadow-neo-xs flex items-center gap-1 cursor-pointer"
                              title="Khôi phục quyền truy cập"
                            >
                              <CheckCircle2 className="w-3 h-3" />
                              <span>Khôi phục</span>
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Metadata Detail Drawer */}
      <AdminUserMetadataDrawer
        user={selectedUser}
        isOpen={isDrawerOpen}
        onClose={() => {
          setIsDrawerOpen(false);
          setSelectedUser(null);
        }}
      />

      {/* Suspend / Restore Action Modal */}
      <AdminUserActionModal
        isOpen={actionModal.isOpen}
        actionType={actionModal.type}
        userId={actionModal.userId}
        onClose={() => setActionModal({ isOpen: false, type: 'SUSPEND', userId: '' })}
        onConfirm={handleConfirmAction}
      />
    </div>
  );
}

