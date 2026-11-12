'use client';

import { useId, useState } from 'react';
import Link from 'next/link';
import { useTranslation } from 'react-i18next';
import { CheckCircle2, ClipboardCheck, Edit3 } from 'lucide-react';
import { localDateIn, summarizeCheckin, useDailyCheckin } from '@/features/checkin';
import { MASCOT_CARE_ICON, toMascotChoice } from '@/features/insights';
import { useAccount } from '@/features/user';
import { CheckinDialog } from './CheckinDialog';

const FOCUS = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black';
const PRIMARY = `inline-flex items-center justify-center gap-2 rounded-xl border-neo bg-primary-container px-5 py-3 font-space text-sm sm:text-base font-extrabold text-on-primary-container shadow-neo transition-all hover:-translate-y-0.5 hover:shadow-neo-lg cursor-pointer ${FOCUS}`;
const SECONDARY = `inline-flex items-center justify-center gap-2 rounded-xl border-2 border-black bg-white px-4 py-3 font-space text-sm font-bold text-black shadow-neo-sm transition-all hover:bg-paper-warm cursor-pointer ${FOCUS}`;

/**
 * One main action a day: check in first (icon follows the mascot: water, food, coffee beans), then write.
 * Once today's check-in exists, writing becomes the main action and the check-in opens for review or edit.
 */
export function HeroActions({ userId, timezone }: { userId: string; timezone: string }) {
  const { t } = useTranslation();
  const { userProfile } = useAccount();
  const choice = toMascotChoice(userProfile.mascot);
  const CareIcon = choice === 'off' ? ClipboardCheck : MASCOT_CARE_ICON[choice];
  const { query } = useDailyCheckin(userId, localDateIn(timezone));
  const checkin = query.data ?? null;
  const [open, setOpen] = useState(false);
  const dialogId = useId();

  const openButton = (className: string, label: string) => (
    <button
      type="button"
      onClick={() => setOpen(true)}
      aria-haspopup="dialog"
      aria-expanded={open}
      aria-controls={dialogId}
      data-testid="checkin-open"
      className={className}
    >
      <CareIcon className="w-5 h-5 stroke-[2.5]" aria-hidden="true" />
      {label}
    </button>
  );
  const writeLink = (className: string) => (
    <Link href="/journal-editor" className={className}>
      <Edit3 className="w-5 h-5 stroke-[2.5]" aria-hidden="true" />
      {t('dashboard.write')}
    </Link>
  );

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col sm:flex-row sm:flex-wrap gap-3">
        {checkin
          ? <>{writeLink(PRIMARY)}{openButton(SECONDARY, t('dashboard.viewCheckin'))}</>
          : <>{openButton(PRIMARY, t('checkin.title'))}{writeLink(SECONDARY)}</>}
      </div>
      {checkin && (
        <p className="flex flex-wrap items-center gap-x-1.5 gap-y-1 font-sans text-sm text-on-surface-variant">
          <CheckCircle2 className="w-4 h-4 text-green-700 shrink-0" aria-hidden="true" />
          <span className="font-space font-bold text-black">{t('dashboard.checkedIn')}</span>
          {summarizeCheckin(checkin, t, false).join(' · ')}
        </p>
      )}
      <CheckinDialog id={dialogId} open={open} onClose={() => setOpen(false)} userId={userId} timezone={timezone} />
    </div>
  );
}
