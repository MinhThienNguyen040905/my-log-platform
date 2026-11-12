'use client';

import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import { DailyCheckinCard } from '@/features/checkin';

type Props = { id: string; open: boolean; onClose: () => void; userId: string; timezone: string };

/** Today's check-in in a native modal dialog (focus stays inside, Escape and the backdrop close it). */
export function CheckinDialog({ id, open, onClose, userId, timezone }: Props) {
  const { t } = useTranslation();
  const dialog = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const element = dialog.current;
    if (!element) return;
    if (open && !element.open) element.showModal();
    if (!open && element.open) element.close();
  }, [open]);

  return (
    <dialog
      id={id}
      ref={dialog}
      aria-label={t('checkin.title')}
      // Escape closes the native dialog; keep React state in sync.
      onClose={onClose}
      // A click on the backdrop lands on the <dialog> itself, not on the card inside it.
      onClick={(event) => { if (event.target === event.currentTarget) onClose(); }}
      className="m-auto w-[calc(100%-2rem)] max-w-3xl max-h-[calc(100dvh-2rem)] overflow-visible bg-transparent p-0 backdrop:bg-black/40"
    >
      <div className="relative max-h-[calc(100dvh-2rem)] overflow-y-auto rounded-3xl">
        {open && <DailyCheckinCard userId={userId} timezone={timezone} />}
      </div>
      <button
        type="button"
        onClick={onClose}
        aria-label={t('common.close')}
        className="absolute -top-3 -right-3 z-10 flex h-9 w-9 items-center justify-center rounded-full border-2 border-black bg-white shadow-neo-sm cursor-pointer hover:bg-paper-warm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
      >
        <X className="h-4 w-4 stroke-[3]" aria-hidden="true" />
      </button>
    </dialog>
  );
}
