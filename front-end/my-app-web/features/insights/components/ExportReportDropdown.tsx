'use client';

import React, { useState, useRef, useEffect } from 'react';
import { Download, FileText, Table, ChevronDown, Check } from 'lucide-react';
import { NeoButton } from '@/components/ui/NeoButton';
import { useToast } from '@/lib/toast-context';
import {
  exportWeeklyReportCSV,
  exportWeeklyReportPDF,
  WeeklyReportExportData,
} from '../utils/export-helpers';

interface ExportReportDropdownProps {
  reportData: WeeklyReportExportData;
}

export function ExportReportDropdown({ reportData }: ExportReportDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [exportingType, setExportingType] = useState<'csv' | 'pdf' | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { showToast } = useToast();

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleExportCSV = () => {
    setExportingType('csv');
    try {
      exportWeeklyReportCSV(reportData);
      showToast({
        title: 'Đã xuất file CSV thành công!',
        message: 'Tệp bảng tính số liệu đã được tải về máy của bạn.',
        type: 'success',
      });
    } catch (e) {
      showToast({
        title: 'Xuất CSV không thành công',
        message: 'Có lỗi xảy ra khi tạo tệp, vui lòng thử lại.',
        type: 'error',
      });
    } finally {
      setTimeout(() => {
        setExportingType(null);
        setIsOpen(false);
      }, 400);
    }
  };

  const handleExportPDF = () => {
    setExportingType('pdf');
    showToast({
      title: 'Đang mở hộp thoại in / xuất PDF...',
      message: 'Vui lòng chọn "Lưu dưới dạng PDF" (Save as PDF) trên trình duyệt.',
      type: 'info',
    });
    setIsOpen(false);
    setTimeout(() => {
      exportWeeklyReportPDF();
      setExportingType(null);
    }, 500);
  };

  return (
    <div className="relative inline-block no-print" ref={dropdownRef}>
      <NeoButton
        type="button"
        variant="primary"
        size="sm"
        onClick={() => setIsOpen(!isOpen)}
        className="font-space font-extrabold text-xs shadow-neo-sm cursor-pointer"
        icon={<Download className="w-3.5 h-3.5" />}
      >
        <span className="inline-flex items-center gap-1.5 leading-none">
          <span>Xuất báo cáo</span>
          <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 shrink-0 ${isOpen ? 'rotate-180' : ''}`} />
        </span>
      </NeoButton>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 bg-paper-warm border-2 border-black rounded-2xl p-2 shadow-neo-lg z-50 animate-in fade-in slide-in-from-top-2 flex flex-col gap-1.5">
          <div className="px-2 py-1 border-b border-black/20 flex items-center justify-between">
            <span className="font-space text-[10px] font-extrabold uppercase text-gray-600">
              Chọn định dạng xuất file:
            </span>
            <span className="font-mono text-[9px] font-bold text-gray-500">A4 / Excel</span>
          </div>

          {/* Option 1: PDF */}
          <button
            type="button"
            onClick={handleExportPDF}
            className="w-full p-2.5 bg-white hover:bg-surface-card rounded-xl border border-black/80 flex items-start gap-2.5 text-left transition-all cursor-pointer shadow-neo-xs hover:-translate-y-0.5 group"
          >
            <div className="w-8 h-8 rounded-lg bg-rose-100 border border-black flex items-center justify-center shrink-0 mt-0.5 group-hover:bg-rose-200">
              <FileText className="w-4 h-4 text-rose-700" />
            </div>
            <div className="flex flex-col">
              <span className="font-space text-xs font-extrabold text-black flex items-center gap-1">
                Xuất file PDF (.pdf)
                {exportingType === 'pdf' && <Check className="w-3 h-3 text-green-700" />}
              </span>
              <span className="font-sans text-[11px] text-gray-600 leading-tight mt-0.5">
                Bản in màu A4 chuẩn phong cách Scrapbook
              </span>
            </div>
          </button>

          {/* Option 2: CSV */}
          <button
            type="button"
            onClick={handleExportCSV}
            className="w-full p-2.5 bg-white hover:bg-surface-card rounded-xl border border-black/80 flex items-start gap-2.5 text-left transition-all cursor-pointer shadow-neo-xs hover:-translate-y-0.5 group"
          >
            <div className="w-8 h-8 rounded-lg bg-green-100 border border-black flex items-center justify-center shrink-0 mt-0.5 group-hover:bg-green-200">
              <Table className="w-4 h-4 text-green-700" />
            </div>
            <div className="flex flex-col">
              <span className="font-space text-xs font-extrabold text-black flex items-center gap-1">
                Xuất file CSV (.csv)
                {exportingType === 'csv' && <Check className="w-3 h-3 text-green-700" />}
              </span>
              <span className="font-sans text-[11px] text-gray-600 leading-tight mt-0.5">
                Bảng số liệu thô cho Microsoft Excel & Sheets
              </span>
            </div>
          </button>
        </div>
      )}
    </div>
  );
}

