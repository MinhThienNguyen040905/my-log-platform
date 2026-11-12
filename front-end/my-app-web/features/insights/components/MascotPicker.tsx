'use client';

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Check, EyeOff, Shuffle, Sparkles } from 'lucide-react';
import { useAccount } from '@/features/user';
import { useToast } from '@/providers/ToastProvider';
import { MoodMascot } from './MoodMascot';
import { DEFAULT_MASCOT, MASCOT_IDS, MASCOT_MOODS, MASCOTS, toMascotChoice, type MascotChoice, type MascotMood } from '../utils/mascots';

/** Settings section: pick the Insight-page mascot (saved to the profile right away) and preview its expressions. */
export function MascotPicker() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const { userProfile, updateProfile } = useAccount();
  const saved = toMascotChoice(userProfile.mascot);
  const [pending, setPending] = useState<MascotChoice | null>(null);
  const [previewMood, setPreviewMood] = useState<MascotMood>('happy');
  const current = pending ?? saved;
  const previewMascot = current === 'off' ? DEFAULT_MASCOT : current;

  const choose = async (choice: MascotChoice) => {
    if (choice === saved || pending) return;
    setPending(choice);
    try {
      await updateProfile({ mascot: choice });
      showToast({ title: t('mascot.saved'), type: 'success' });
    } catch (error) {
      showToast({ title: t('mascot.saveError'), message: error instanceof Error ? error.message : undefined, type: 'error' });
    } finally {
      setPending(null);
    }
  };

  const nextMood = () => setPreviewMood((mood) => MASCOT_MOODS[(MASCOT_MOODS.indexOf(mood as (typeof MASCOT_MOODS)[number]) + 1) % MASCOT_MOODS.length]);

  const optionClass = (selected: boolean) => `px-3 py-1.5 rounded-xl border-2 border-black font-space text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 disabled:cursor-wait ${
    selected ? 'bg-primary-container text-black shadow-neo-xs -translate-y-0.5' : 'bg-paper-warm/50 hover:bg-paper-warm text-gray-700'
  }`;

  return (
    <div className="flex flex-col gap-2" data-testid="mascot-picker">
      <span className="font-space text-xs font-extrabold text-black uppercase flex items-center gap-1.5">
        <Sparkles className="w-3.5 h-3.5 text-amber-500" />
        {t('mascot.settingsTitle')}
      </span>
      <p className="font-sans text-xs text-gray-600">{t('mascot.settingsHint')}</p>
      <div className="flex items-center gap-4 flex-wrap">
        <div className={`flex flex-col items-center gap-1.5 ${current === 'off' ? 'opacity-40' : ''}`}>
          <MoodMascot mascot={previewMascot} mood={previewMood} size={88} />
          <button
            type="button"
            onClick={nextMood}
            disabled={current === 'off'}
            className="inline-flex items-center gap-1 text-[11px] font-space font-bold underline disabled:no-underline disabled:cursor-not-allowed"
          >
            <Shuffle className="w-3 h-3" />
            {t('mascot.tryExpression', { mood: t(`mascot.moods.${previewMood}`) })}
          </button>
        </div>
        <div role="radiogroup" aria-label={t('mascot.settingsTitle')} className="flex items-center gap-2 flex-wrap">
          {MASCOT_IDS.map((id) => (
            <button
              key={id}
              type="button"
              role="radio"
              aria-checked={current === id}
              disabled={pending !== null}
              onClick={() => void choose(id)}
              className={optionClass(current === id)}
            >
              <span aria-hidden="true">{MASCOTS[id].emoji}</span>
              <span>{t(`mascot.names.${id}`)}</span>
              {current === id && <Check className="w-3.5 h-3.5 stroke-[3] text-black" />}
            </button>
          ))}
          <button
            type="button"
            role="radio"
            aria-checked={current === 'off'}
            disabled={pending !== null}
            onClick={() => void choose('off')}
            className={optionClass(current === 'off')}
          >
            <EyeOff className="w-3.5 h-3.5" />
            <span>{t('mascot.off')}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
