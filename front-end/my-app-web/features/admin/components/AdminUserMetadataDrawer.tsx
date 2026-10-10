'use client';

import React from 'react';
import { X, Shield, Clock, Hash, CheckCircle2, Ban } from 'lucide-react';
import { AdminUserItem } from '../api/types';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';

interface AdminUserMetadataDrawerProps {
  user: AdminUserItem | null;
  isOpen: boolean;
  onClose: () => void;
}

export function AdminUserMetadataDrawer({ user, isOpen, onClose }: AdminUserMetadataDrawerProps) {
  if (!isOpen || !user) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/40 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="absolute inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-paper-warm border-l-[2.5px] border-black p-6 sm:p-8 shadow-neo-lg relative flex flex-col gap-6 animate-in slide-in-from-right duration-200 overflow-y-auto">
          
          <WashiTape color="lime" rotate={-2} className="absolute -top-3 left-10 w-28" />

          {/* Top Bar */}
          <div className="flex items-center justify-between pb-4 border-b-2 border-black/15">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-white border border-black flex items-center justify-center shadow-neo-xs">
                <Shield className="w-4 h-4 text-black stroke-[2.3]" />
              </div>
              <h2 className="font-space text-lg font-extrabold text-black uppercase tracking-tight">
                Metadata Người Dùng
              </h2>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-xl border border-black bg-white hover:bg-gray-100 transition-colors cursor-pointer shadow-neo-xs"
              title="Đóng bảng chi tiết"
            >
              <X className="w-4 h-4 text-black stroke-[2.5]" />
            </button>
          </div>

          {/* User ID Section */}
          <div className="bg-white p-4 rounded-2xl border-2 border-black shadow-neo-sm flex flex-col gap-1.5">
            <span className="font-space text-[11px] font-extrabold uppercase text-gray-500">
              Định Danh UUID v7
            </span>
            <code className="font-mono text-xs font-bold text-black break-all bg-gray-50 p-2 rounded-lg border border-black/20">
              {user.id}
            </code>
          </div>

          {/* Status Chip */}
          <div className="bg-white p-4 rounded-2xl border-2 border-black shadow-neo-sm flex items-center justify-between">
            <span className="font-space text-xs font-extrabold uppercase text-gray-600">
              Trạng thái tài khoản
            </span>
            {user.status === 'ACTIVE' ? (
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-primary-container text-black font-space text-xs font-extrabold border border-black shadow-neo-xs">
                <CheckCircle2 className="w-3.5 h-3.5 stroke-[2.5]" />
                Hoạt động (ACTIVE)
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-rose-200 text-rose-900 font-space text-xs font-extrabold border border-black shadow-neo-xs">
                <Ban className="w-3.5 h-3.5 stroke-[2.5]" />
                Đình chỉ (SUSPENDED)
              </span>
            )}
          </div>

          {/* Technical Metadata Rows */}
          <div className="bg-white p-5 rounded-2xl border-2 border-black shadow-neo-sm flex flex-col gap-4">
            <h3 className="font-space text-xs font-extrabold uppercase text-black border-b border-black/15 pb-2">
              Thông số vận hành hệ thống
            </h3>

            <div className="flex items-center justify-between text-xs">
              <span className="font-space text-gray-600 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5" />
                Thời điểm tạo tài khoản:
              </span>
              <span className="font-mono font-bold text-black">
                {new Date(user.createdAt).toLocaleString('vi-VN')}
              </span>
            </div>

            <div className="flex items-center justify-between text-xs">
              <span className="font-space text-gray-600 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5" />
                Đăng nhập gần nhất:
              </span>
              <span className="font-mono font-bold text-black">
                {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('vi-VN') : 'Chưa có thông tin'}
              </span>
            </div>

            <div className="flex items-center justify-between text-xs">
              <span className="font-space text-gray-600 flex items-center gap-1.5">
                <Hash className="w-3.5 h-3.5" />
                Optimistic Lock Version:
              </span>
              <span className="font-mono font-bold bg-paper-warm px-2 py-0.5 rounded border border-black">
                v{user.version}
              </span>
            </div>
          </div>

          {/* Security Notice */}
          <div className="p-4 rounded-2xl border border-black bg-amber-50 text-[11px] font-space text-gray-700 leading-relaxed shadow-neo-xs">
            <span className="font-extrabold block text-amber-900 mb-1">
              CHÍNH SÁCH BẢO MẬT & QUYỀN RIÊNG TƯ (ADR-0002):
            </span>
            Admin Console không hiển thị nhật ký thô, tiêu đề hoặc nội dung suy tư cá nhân của người dùng. Chỉ có các trường kỹ thuật phi định danh được truy xuất cho mục đích bảo đảm an toàn hệ thống.
          </div>
        </div>
      </div>
    </div>
  );
}

