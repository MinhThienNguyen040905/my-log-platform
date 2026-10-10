'use client';

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useJournal } from '@/features/journal';
import Link from 'next/link';
import {
  ShieldCheck,
  Users,
  Cpu,
  ArrowLeft,
  RefreshCw,
  Lock,
} from 'lucide-react';
import { ApiError } from '@/lib/api/client';
import {
  AdminDashboardMetrics,
  AdminUserTable,
  AdminJobTable,
  fetchAdminDashboard,
  fetchAdminUsers,
  fetchAdminJobs,
} from '@/features/admin';

export function AdminConsoleView() {
  const { userId } = useJournal();
  const [activeTab, setActiveTab] = useState<'users' | 'jobs'>('users');
  const dashboardQuery = useQuery({ queryKey: ['admin', userId, 'dashboard'], queryFn: fetchAdminDashboard });
  const usersQuery = useQuery({ queryKey: ['admin', userId, 'users'], queryFn: () => fetchAdminUsers(), enabled: activeTab === 'users' });
  const jobsQuery = useQuery({ queryKey: ['admin', userId, 'jobs'], queryFn: () => fetchAdminJobs(), enabled: activeTab === 'jobs' });
  const dashboardData = dashboardQuery.data ?? null;
  const users = usersQuery.data?.items ?? [];
  const jobs = jobsQuery.data?.items ?? [];
  const loadingDashboard = dashboardQuery.isPending;
  const loadingUsers = usersQuery.isPending;
  const loadingJobs = jobsQuery.isPending;
  const failure = dashboardQuery.error ?? usersQuery.error ?? jobsQuery.error;
  const accessDenied = failure instanceof ApiError && (failure.status === 401 || failure.status === 403);
  const error = failure && !accessDenied ? failure instanceof Error ? failure.message : 'Không thể tải dữ liệu quản trị.' : null;
  const loadUsers = async () => { await usersQuery.refetch(); };
  const loadJobs = async () => { await jobsQuery.refetch(); };
  const handleRefreshAll = () => {
    void dashboardQuery.refetch();
    if (activeTab === 'users') void usersQuery.refetch();
    if (activeTab === 'jobs') void jobsQuery.refetch();
  };

  if (accessDenied) return (
    <main className="max-w-xl mx-auto p-8" role="alert">
      <h1 className="text-2xl font-bold">Không có quyền truy cập quản trị</h1>
      <p className="mt-3">Tài khoản này không được phép xem dữ liệu quản trị.</p>
      <Link href="/dashboard" className="underline mt-4 inline-block">Về bảng điều khiển</Link>
    </main>
  );

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-8 selection:bg-primary-container selection:text-black">
      {/* Top Breadcrumb & Admin Console Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b-2 border-black">
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <Link
              href="/dashboard"
              className="font-space text-xs font-bold text-gray-600 hover:text-black flex items-center gap-1 hover:underline transition-all"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Bảng điều khiển cá nhân</span>
            </Link>
            <span className="text-gray-400">/</span>
            <span className="px-2 py-0.5 bg-paper-warm border border-black rounded text-[10px] font-space font-extrabold uppercase">
              HỆ THỐNG VẬN HÀNH
            </span>
          </div>

          <h1 className="font-space text-2xl sm:text-3xl lg:text-4xl font-extrabold text-on-surface tracking-tight mt-1 flex items-center gap-2.5">
            <span>Bàn Điều Khiển Quản Trị</span>
            <span className="px-2.5 py-1 rounded-xl bg-primary-container text-black border border-black font-space text-xs font-extrabold shadow-neo-xs uppercase tracking-wider hidden sm:inline-block">
              Admin Console
            </span>
          </h1>
          <p className="font-sans text-xs sm:text-sm text-gray-700">
            Giám sát số liệu bảo vệ quyền riêng tư, điều phối tiến trình AI và quản lý trạng thái tài khoản người dùng
          </p>
        </div>

        {/* Top Right Action & Security Badge */}
        <div className="flex items-center gap-2">
          <div className="inline-flex items-center gap-1.5 bg-white px-3 py-1.5 rounded-xl border-2 border-black shadow-neo-xs font-space text-xs font-extrabold text-black">
            <Lock className="w-3.5 h-3.5 stroke-[2.5]" />
            <span>Quyền quản trị</span>
          </div>

          <button
            type="button"
            onClick={handleRefreshAll}
            className="p-2 rounded-xl border-2 border-black bg-white hover:bg-paper-warm transition-all cursor-pointer shadow-neo-xs"
            title="Làm mới toàn bộ dữ liệu"
          >
            <RefreshCw className="w-4 h-4 text-black stroke-[2.5]" />
          </button>
        </div>
      </div>

      {error && <div role="alert" className="rounded-xl border-2 border-red-700 bg-red-50 p-4 text-red-900">{error}</div>}

      {/* SECTION 1: METRICS DASHBOARD (GET /admin/dashboard) */}
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="font-space text-sm font-extrabold uppercase tracking-wider text-gray-700 flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-black stroke-[2.5]" />
            <span>Chỉ Số Vận Hành &amp; Bảo Mật Toàn Hệ Thống</span>
          </h2>
        </div>
        <AdminDashboardMetrics data={dashboardData} loading={loadingDashboard} />
      </section>

      {/* SECTION 2: TAB CONTROLS & MANAGEMENT INTERFACE */}
      <section className="flex flex-col gap-5 pt-2">
        {/* Scrapbook Neo-brutalist Tab Switcher */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b-2 border-black/15 pb-3">
          <div className="flex items-center gap-2 p-1.5 bg-surface-card rounded-2xl border-2 border-black shadow-neo-sm">
            <button
              type="button"
              onClick={() => setActiveTab('users')}
              className={`px-4 py-2 rounded-xl font-space text-xs font-extrabold transition-all cursor-pointer flex items-center gap-2 ${
                activeTab === 'users'
                  ? 'bg-primary-container text-black border border-black shadow-neo-sm'
                  : 'text-gray-600 hover:text-black hover:bg-paper-warm'
              }`}
            >
              <Users className="w-4 h-4 stroke-[2.3]" />
              <span>Quản Lý Người Dùng ({users.length})</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('jobs')}
              className={`px-4 py-2 rounded-xl font-space text-xs font-extrabold transition-all cursor-pointer flex items-center gap-2 ${
                activeTab === 'jobs'
                  ? 'bg-primary-container text-black border border-black shadow-neo-sm'
                  : 'text-gray-600 hover:text-black hover:bg-paper-warm'
              }`}
            >
              <Cpu className="w-4 h-4 stroke-[2.3]" />
              <span>Hàng Đợi AI Jobs ({jobs.length})</span>
            </button>
          </div>

          <div className="text-xs font-space text-gray-500 font-bold hidden sm:block">
            {activeTab === 'users' ? 'Quyền hạn: users:read-metadata, users:suspend' : 'Quyền hạn: jobs:read, jobs:retry'}
          </div>
        </div>

        {/* Tab 1: User Management */}
        {activeTab === 'users' && (
          <div className="animate-in fade-in duration-200">
            <AdminUserTable
              users={users}
              loading={loadingUsers}
              onRefresh={loadUsers}
            />
          </div>
        )}

        {/* Tab 2: AI Jobs Dispatcher */}
        {activeTab === 'jobs' && (
          <div className="animate-in fade-in duration-200">
            <AdminJobTable
              jobs={jobs}
              loading={loadingJobs}
              onRefresh={loadJobs}
            />
          </div>
        )}
      </section>
    </div>
  );
}

