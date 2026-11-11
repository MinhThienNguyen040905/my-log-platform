'use client';

import type { ReactNode, RefObject } from 'react';
import { Check, HeartHandshake, ImagePlus, Save, Sliders, Wind, X } from 'lucide-react';
import { RailDiscIcon } from './ambient-player/PlayerVisuals';

interface JournalToolRailProps {
  /** Dưới 768px cột công cụ là ngăn kéo nổi; chỉ hiện khi đang mở. */
  isDrawerOpen: boolean;
  onCloseDrawer: () => void;
  isEditMode: boolean;
  isSaving: boolean;
  saveDisabled: boolean;
  onSave: () => void;
  onInsertImage: () => void;
  metricsButtonRef: RefObject<HTMLButtonElement | null>;
  isMetricsOpen: boolean;
  onToggleMetrics: () => void;
  isCompanionOpen: boolean;
  hasCompanionUnread: boolean;
  onToggleCompanion: () => void;
  calmingToolsButtonRef: RefObject<HTMLButtonElement | null>;
  isCalmingToolsActive: boolean;
  onToggleCalmingTools: () => void;
  isMusicOpen: boolean;
  isMusicPlaying: boolean;
  onToggleMusic: () => void;
}

interface RailButtonProps {
  label: string;
  ariaLabel: string;
  title: string;
  icon: ReactNode;
  onClick: () => void;
  active?: boolean;
  primary?: boolean;
  pressed?: boolean;
  disabled?: boolean;
  badge?: boolean;
  buttonRef?: RefObject<HTMLButtonElement | null>;
}

function RailButton({ label, ariaLabel, title, icon, onClick, active, primary, pressed, disabled, badge, buttonRef }: RailButtonProps) {
  const tone = primary || active ? 'bg-primary-container shadow-neo-sm' : 'bg-white group-hover:bg-lime-100';
  return (
    <button
      ref={buttonRef}
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={ariaLabel}
      aria-pressed={pressed}
      title={title}
      className="group flex flex-col items-center gap-1 rounded-xl cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black disabled:cursor-not-allowed disabled:opacity-50"
    >
      <span className={`relative inline-flex h-11 w-11 items-center justify-center rounded-xl border-2 border-black transition-all group-hover:-translate-y-0.5 group-active:translate-y-0 group-active:scale-95 ${tone}`}>
        {icon}
        {badge && <span aria-hidden="true" className="absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full border border-black bg-red-500" />}
      </span>
      <span aria-hidden="true" className="font-space text-[10px] font-bold leading-tight text-gray-700 text-center">{label}</span>
    </button>
  );
}

export function JournalToolRail(props: JournalToolRailProps) {
  const saveLabel = props.isSaving ? 'Đang lưu' : props.isEditMode ? 'Cập nhật bài' : 'Lưu';

  return (
    <aside
      aria-label="Công cụ trang viết"
      className={`${props.isDrawerOpen ? 'fixed inset-x-3 bottom-[calc(env(safe-area-inset-bottom)+10rem)] z-30 block' : 'hidden'} md:static md:z-auto md:block md:w-[76px] md:shrink-0 md:border-l-2 md:border-black`}
    >
      <div className="max-md:rounded-2xl max-md:border-2 max-md:border-black max-md:bg-paper-warm max-md:p-3 max-md:shadow-neo-lg md:sticky md:top-20 md:px-2 md:py-4">
        <div className="mb-2 flex items-center justify-between md:hidden">
          <span className="font-space text-sm font-extrabold">Công cụ</span>
          <button type="button" onClick={props.onCloseDrawer} aria-label="Đóng công cụ" className="rounded-lg p-2 hover:bg-white">
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="grid grid-cols-3 gap-2 md:flex md:flex-col md:items-center md:gap-3">
          <RailButton
            label={props.isEditMode ? 'Cập nhật' : 'Lưu'}
            ariaLabel={saveLabel}
            title={saveLabel}
            primary
            disabled={props.isSaving || props.saveDisabled}
            onClick={props.onSave}
            icon={props.isEditMode ? <Check className="h-5 w-5" /> : <Save className="h-5 w-5" />}
          />
          <RailButton
            label="Chèn ảnh"
            ariaLabel="Chèn ảnh"
            title="Chèn ảnh vào bài viết"
            onClick={props.onInsertImage}
            icon={<ImagePlus className="h-5 w-5" />}
          />
          <RailButton
            label="Chỉ số"
            ariaLabel={props.isMetricsOpen ? 'Ẩn chỉ số' : 'Hiện chỉ số'}
            title={props.isMetricsOpen ? 'Ẩn chỉ số' : 'Hiện chỉ số'}
            buttonRef={props.metricsButtonRef}
            active={props.isMetricsOpen}
            pressed={props.isMetricsOpen}
            onClick={props.onToggleMetrics}
            icon={<Sliders className="h-5 w-5" />}
          />
          <RailButton
            label="Trợ lý"
            ariaLabel="Trợ lý đồng hành"
            title="Trợ lý đồng hành (Chạm là có người hỏi han)"
            active={props.isCompanionOpen}
            pressed={props.isCompanionOpen}
            badge={props.hasCompanionUnread}
            onClick={props.onToggleCompanion}
            icon={<HeartHandshake className="h-5 w-5" />}
          />
          <RailButton
            label="Công cụ dịu"
            ariaLabel="Công cụ dịu"
            title="Công cụ dịu (Dùng ngay, không cần trò chuyện)"
            buttonRef={props.calmingToolsButtonRef}
            active={props.isCalmingToolsActive}
            pressed={props.isCalmingToolsActive}
            onClick={props.onToggleCalmingTools}
            icon={<Wind className="h-5 w-5" />}
          />
          <RailButton
            label="Nhạc nền"
            ariaLabel={props.isMusicOpen ? 'Tắt nhạc nền' : 'Mở nhạc nền'}
            title={props.isMusicOpen ? 'Tắt nhạc nền' : 'Nhạc nền khi viết (chạy trên máy, không cần mạng)'}
            active={props.isMusicOpen}
            pressed={props.isMusicOpen}
            onClick={props.onToggleMusic}
            icon={<RailDiscIcon playing={props.isMusicPlaying} />}
          />
        </div>
      </div>
    </aside>
  );
}
