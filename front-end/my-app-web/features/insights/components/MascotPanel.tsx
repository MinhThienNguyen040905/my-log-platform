'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Bean, Droplets, Fish, type LucideIcon } from 'lucide-react';
import { useAccount } from '@/features/user';
import { getMascotMood } from '../api/insights';
import { toMascotChoice, toMascotMood, type MascotId } from '../utils/mascots';
import { MoodMascot } from './MoodMascot';

/** What each mascot is given when the user checks in: water, food, coffee beans. */
export const MASCOT_CARE_ICON: Record<MascotId, LucideIcon> = { sprout: Droplets, cat: Fish, coffee: Bean };

/**
 * The chosen mascot wearing the mood the user picked most in the last 7 days, with a short message.
 * Renders nothing when the mascot is hidden in Settings.
 */
export function MascotPanel({ className = '' }: { className?: string }) {
  const { t } = useTranslation();
  const { userId, userProfile } = useAccount();
  const choice = toMascotChoice(userProfile.mascot);
  const query = useQuery({ queryKey: ['insights', userId, 'mascot'], queryFn: getMascotMood, enabled: choice !== 'off' });
  if (choice === 'off') return null;

  const data = query.data;
  const mood = toMascotMood(data?.mood);
  const calm = data?.calm === true;
  const name = t(`mascot.names.${choice}`);

  return (
    <section aria-labelledby="mascot-card-title" className={`flex flex-col items-center text-center gap-2 ${className}`} data-testid="mascot-card">
      <MoodMascot mascot={choice} mood={query.isSuccess ? mood : 'idle'} calm={calm} size={136} />
      <h2 id="mascot-card-title" className="font-space text-sm font-extrabold text-black">{t('mascot.cardTitle', { name })}</h2>
      {query.isPending && <p className="text-xs text-gray-600">{t('common.loading')}</p>}
      {query.isError && (
        <p role="alert" className="text-xs text-red-700">
          {t('mascot.loadError')}{' '}
          <button type="button" className="underline" onClick={() => void query.refetch()}>{t('common.retry')}</button>
        </p>
      )}
      {data && (
        <>
          <p className="font-space text-xs font-bold text-black">
            {data.mood ? t('mascot.summary', { mood: t(`mascot.moods.${mood}`), count: data.moodCount, total: data.entryCount }) : t('mascot.noMood')}
          </p>
          <p className="font-sans text-sm text-gray-700 leading-relaxed max-w-xs">
            {calm ? t('mascot.messages.calm', { name }) : t(`mascot.messages.${mood}`, { name })}
          </p>
          {(calm || mood === 'very-bad') && (
            <Link href="/journal-editor" className="text-xs font-space font-bold underline">{t('mascot.calmingLink')}</Link>
          )}
        </>
      )}
    </section>
  );
}
