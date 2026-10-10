'use client';

import React from 'react';
import { Users, BookOpen, CheckSquare, ShieldAlert, ShieldCheck } from 'lucide-react';
import { AdminDashboardData } from '../api/types';
import { WashiTape } from '@/components/ui/ScrapbookDecorations';

interface AdminDashboardMetricsProps {
  data: AdminDashboardData | null;
  loading: boolean;
}

export function AdminDashboardMetrics({ data, loading }: AdminDashboardMetricsProps) {
  if (loading || !data) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
        {[1, 2, 3, 4].map(i => (
          <div key={i} className="h-32 bg-paper-warm/50 border-2 border-black rounded-2xl shadow-neo-sm" />
        ))}
      </div>
    );
  }

  const { suppressed, activeUsers, journalEntries, dailyCheckins, minimumCohortSize } = data;

  return (
    <div className="flex flex-col gap-4">
      {/* Privacy Notice Banner if Cohort Suppressed */}
      {suppressed && (
        <div className="bg-amber-100 border-2 border-black rounded-2xl p-4 shadow-neo-sm relative flex items-start gap-3">
          <WashiTape color="peach" rotate={-1} className="absolute -top-3 left-8 w-28" />
          <ShieldAlert className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
          <div className="text-xs font-space">
            <span className="font-extrabold uppercase text-amber-900 block mb-0.5">
              Cảnh báo bảo mật dữ liệu (k-anonymity privacy protection)
            </span>
            <p className="text-gray-800 leading-relaxed font-sans">
              Số lượng người dùng hoạt động hiện nhỏ hơn ngưỡng an toàn tối thiểu (≥ {minimumCohortSize} người). 
              Số liệu chi tiết được làm mờ để bảo vệ quyền riêng tư cá nhân theo nguyên tắc Zero-Knowledge.
            </p>
          </div>
        </div>
      )}

      {/* 4 Neo-Brutalist Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 relative">
        {/* Metric 1: Active Users */}
        <div className="bg-white border-2 border-black rounded-2xl p-5 shadow-neo-sm flex flex-col justify-between relative overflow-hidden transition-all hover:-translate-y-0.5">
          <WashiTape color="lime" rotate={1} className="absolute -top-3 right-6 w-20" />
          <div className="flex items-center justify-between">
            <span className="font-space text-xs font-extrabold uppercase text-gray-600 tracking-wider">
              Người dùng hoạt động
            </span>
            <div className="w-8 h-8 rounded-xl bg-primary-container border border-black flex items-center justify-center shadow-neo-xs">
              <Users className="w-4 h-4 text-black stroke-[2.3]" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-space text-3xl font-extrabold text-black">
              {suppressed ? '—' : activeUsers?.toLocaleString('vi-VN') ?? '0'}
            </div>
            <span className="text-[11px] font-sans text-gray-500 font-medium block mt-1">
              Đã xác thực và có phiên hợp lệ
            </span>
          </div>
        </div>

        {/* Metric 2: Total Journal Entries */}
        <div className="bg-paper-warm border-2 border-black rounded-2xl p-5 shadow-neo-sm flex flex-col justify-between relative overflow-hidden transition-all hover:-translate-y-0.5">
          <div className="flex items-center justify-between">
            <span className="font-space text-xs font-extrabold uppercase text-gray-600 tracking-wider">
              Tổng trang nhật ký
            </span>
            <div className="w-8 h-8 rounded-xl bg-white border border-black flex items-center justify-center shadow-neo-xs">
              <BookOpen className="w-4 h-4 text-black stroke-[2.3]" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-space text-3xl font-extrabold text-black">
              {suppressed ? '—' : journalEntries?.toLocaleString('vi-VN') ?? '0'}
            </div>
            <span className="text-[11px] font-sans text-gray-500 font-medium block mt-1">
              Mã hóa envelope encryption AES-256
            </span>
          </div>
        </div>

        {/* Metric 3: Daily Checkins */}
        <div className="bg-white border-2 border-black rounded-2xl p-5 shadow-neo-sm flex flex-col justify-between relative overflow-hidden transition-all hover:-translate-y-0.5">
          <div className="flex items-center justify-between">
            <span className="font-space text-xs font-extrabold uppercase text-gray-600 tracking-wider">
              Điểm danh cảm xúc
            </span>
            <div className="w-8 h-8 rounded-xl bg-amber-200 border border-black flex items-center justify-center shadow-neo-xs">
              <CheckSquare className="w-4 h-4 text-black stroke-[2.3]" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-space text-3xl font-extrabold text-black">
              {suppressed ? '—' : dailyCheckins?.toLocaleString('vi-VN') ?? '0'}
            </div>
            <span className="text-[11px] font-sans text-gray-500 font-medium block mt-1">
              Lượt check-in chỉ số tâm trạng
            </span>
          </div>
        </div>

        {/* Metric 4: Privacy & Cohort Mode */}
        <div className="bg-white border-2 border-black rounded-2xl p-5 shadow-neo-sm flex flex-col justify-between relative overflow-hidden transition-all hover:-translate-y-0.5">
          <div className="flex items-center justify-between">
            <span className="font-space text-xs font-extrabold uppercase text-gray-600 tracking-wider">
              Bảo vệ quyền riêng tư
            </span>
            <div className="w-8 h-8 rounded-xl bg-primary-container border border-black flex items-center justify-center shadow-neo-xs">
              <ShieldCheck className="w-4 h-4 text-black stroke-[2.3]" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-space text-2xl font-extrabold text-black">
              Cohort ≥ {minimumCohortSize}
            </div>
            <span className="text-[11px] font-sans text-gray-500 font-medium block mt-1">
              {suppressed ? 'Đang kích hoạt ẩn dữ liệu' : 'Mức an toàn dữ liệu: Đạt'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

