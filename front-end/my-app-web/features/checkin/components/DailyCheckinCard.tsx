'use client';

import { useState, type ReactNode } from 'react';
import { BatteryCharging, CheckCircle2, Moon, Pencil, Zap } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { JOURNAL_EMOTIONS, JOURNAL_EMOTION_ORDER } from '@/features/journal';
import type { JournalEmotion } from '@/types';
import type { Checkin } from '../api/checkin';
import { useDailyCheckin } from '../hooks/useDailyCheckin';
import { CHECKIN_LIMITS, CHECKIN_MOOD_CODES, emotionFromMoodCode, localDateIn, roundScore } from '../utils/checkin-values';
import { activitiesForSave, activityChoices } from '../utils/checkin-activities';
import { summarizeCheckin } from '../utils/checkin-summary';

type Draft = { emotion: JournalEmotion | null; stress: number; energy: number; sleepHours: number; note: string; activities: string[] };

const DEFAULT_DRAFT: Draft = { emotion: null, stress: 5, energy: 5, sleepHours: 7, note: '', activities: [] };

function draftFrom(checkin: Checkin | null): Draft {
  if (!checkin) return DEFAULT_DRAFT;
  return {
    emotion: emotionFromMoodCode(checkin.moodCode),
    stress: checkin.stressScore ?? DEFAULT_DRAFT.stress,
    energy: checkin.energyScore ?? DEFAULT_DRAFT.energy,
    sleepHours: checkin.sleepMinutes === null ? DEFAULT_DRAFT.sleepHours : checkin.sleepMinutes / 60,
    note: checkin.note ?? '',
    activities: checkin.activities.map((activity) => activity.code),
  };
}

/**
 * "Check-in hôm nay" trên dashboard. Chỉ tạo check-in khi người dùng bấm lưu — không tự suy ra từ bài
 * nhật ký. Ngày lấy theo múi giờ của hồ sơ.
 */
export function DailyCheckinCard({ userId, timezone }: { userId: string; timezone: string }) {
  const { t } = useTranslation();
  const localDate = localDateIn(timezone);
  const { query, mutation } = useDailyCheckin(userId, localDate);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState<Draft>(DEFAULT_DRAFT);
  const [formError, setFormError] = useState('');

  const existing = query.data ?? null;
  const showForm = editing || (query.isSuccess && !existing);

  const startEdit = () => { setDraft(draftFrom(existing)); setFormError(''); setEditing(true); };

  const save = () => {
    if (!draft.emotion) { setFormError(t('checkin.pickMood')); return; }
    if (draft.note.length > CHECKIN_LIMITS.noteMaxLength) { setFormError(t('checkin.noteTooLong')); return; }
    setFormError('');
    const note = draft.note.trim();
    mutation.mutate({
      timezone,
      moodCode: CHECKIN_MOOD_CODES[draft.emotion],
      moodScore: roundScore(JOURNAL_EMOTIONS[draft.emotion].score),
      stressScore: roundScore(draft.stress),
      energyScore: roundScore(draft.energy),
      sleepMinutes: Math.round(draft.sleepHours * 60),
      note: note ? note : null,
      activities: activitiesForSave(draft.activities, existing?.activities ?? []),
    }, { onSuccess: () => setEditing(false) });
  };

  const toggleActivity = (code: string) => setDraft((current) => ({
    ...current,
    activities: current.activities.includes(code)
      ? current.activities.filter((value) => value !== code)
      : [...current.activities, code],
  }));

  return (
    <section aria-labelledby="checkin-title" className="bg-surface-card border-neo rounded-3xl p-5 sm:p-6 shadow-neo">
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b-2 border-on-background mb-4">
        <div>
          <h2 id="checkin-title" className="font-space text-lg font-bold text-on-surface">{t('checkin.title')}</h2>
          <p className="font-sans text-xs text-on-surface-variant">{t('checkin.subtitle')}</p>
        </div>
        {existing && !editing && (
          <button
            type="button"
            onClick={startEdit}
            className="inline-flex items-center gap-1.5 rounded-xl border-2 border-black bg-white px-3 py-1.5 font-space text-xs font-bold shadow-neo-sm cursor-pointer hover:bg-lime-50"
          >
            <Pencil className="w-3.5 h-3.5" aria-hidden="true" />
            {t('checkin.edit')}
          </button>
        )}
      </div>

      {query.isPending && <p role="status" className="font-space text-sm">{t('common.loading')}</p>}
      {query.isError && (
        <p role="alert" className="text-sm text-red-700">
          {t('checkin.loadError')}{' '}
          <button type="button" className="underline" onClick={() => void query.refetch()}>{t('common.retry')}</button>
        </p>
      )}

      {existing && !editing && <CheckinSummary checkin={existing} />}

      {showForm && (
        <form
          onSubmit={(event) => { event.preventDefault(); save(); }}
          className="flex flex-col gap-4"
          aria-label={t('checkin.title')}
        >
          <fieldset className="flex flex-col gap-2">
            <legend className="font-space text-xs font-bold uppercase mb-2">{t('checkin.mood')}</legend>
            <div className="flex flex-wrap gap-2">
              {JOURNAL_EMOTION_ORDER.map((id) => {
                const selected = draft.emotion === id;
                return (
                  <button
                    key={id}
                    type="button"
                    aria-pressed={selected}
                    onClick={() => setDraft((current) => ({ ...current, emotion: id }))}
                    className={`rounded-xl border-2 border-black px-3 py-1.5 font-space text-sm font-bold cursor-pointer transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${
                      selected ? 'bg-primary-container shadow-neo-sm' : 'bg-white'
                    }`}
                  >
                    {JOURNAL_EMOTIONS[id].label}
                  </button>
                );
              })}
            </div>
          </fieldset>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Slider id="checkin-stress" label={t('checkin.stress')} icon={<Zap className="w-3.5 h-3.5 text-amber-500" />}
            value={draft.stress} min={1} max={10} step={0.5} suffix="/10" accent="accent-amber-500"
            onChange={(stress) => setDraft((current) => ({ ...current, stress }))} />
          <Slider id="checkin-energy" label={t('checkin.energy')} icon={<BatteryCharging className="w-3.5 h-3.5 text-green-600" />}
            value={draft.energy} min={1} max={10} step={0.5} suffix="/10" accent="accent-green-600"
            onChange={(energy) => setDraft((current) => ({ ...current, energy }))} />
          <Slider id="checkin-sleep" label={t('checkin.sleep')} icon={<Moon className="w-3.5 h-3.5 text-blue-500" />}
            value={draft.sleepHours} min={0} max={CHECKIN_LIMITS.sleepHoursMax} step={0.5} suffix="h" accent="accent-blue-500"
            onChange={(sleepHours) => setDraft((current) => ({ ...current, sleepHours }))} />
          </div>

          <fieldset className="flex flex-col gap-2">
            <legend className="font-space text-xs font-bold uppercase mb-1">{t('checkin.activities')}</legend>
            <p className="font-sans text-xs text-on-surface-variant">{t('checkin.activitiesHint')}</p>
            <div className="flex flex-wrap gap-2">
              {activityChoices(existing?.activities ?? []).map((code) => {
                const selected = draft.activities.includes(code);
                return (
                  <button
                    key={code}
                    type="button"
                    aria-pressed={selected}
                    onClick={() => toggleActivity(code)}
                    className={`rounded-full border-2 border-black px-3 py-1 font-space text-xs font-bold cursor-pointer transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black ${
                      selected ? 'bg-primary-container shadow-neo-sm' : 'bg-white'
                    }`}
                  >
                    {t(`checkin.activityLabels.${code}`, { defaultValue: code.replace(/-/g, ' ') })}
                  </button>
                );
              })}
            </div>
          </fieldset>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="checkin-note" className="font-space text-xs font-bold uppercase">{t('checkin.note')}</label>
            <textarea
              id="checkin-note"
              value={draft.note}
              onChange={(event) => setDraft((current) => ({ ...current, note: event.target.value }))}
              rows={2}
              maxLength={CHECKIN_LIMITS.noteMaxLength}
              placeholder={t('checkin.notePlaceholder')}
              className="w-full rounded-xl border-2 border-black bg-paper-warm px-3 py-2 font-sans text-sm focus:outline-none focus:bg-white"
            />
            <span className="self-end font-mono text-[11px] text-gray-500">{draft.note.length}/{CHECKIN_LIMITS.noteMaxLength}</span>
          </div>

          {(formError || mutation.isError) && (
            <p role="alert" className="text-sm text-red-700">{formError || t('checkin.saveError')}</p>
          )}

          <div className="flex flex-wrap gap-2">
            <button
              type="submit"
              disabled={mutation.isPending}
              className="rounded-xl border-2 border-black bg-primary-container px-4 py-2 font-space text-sm font-extrabold shadow-neo-sm cursor-pointer disabled:opacity-60"
            >
              {mutation.isPending ? t('checkin.saving') : t('checkin.save')}
            </button>
            {editing && existing && (
              <button
                type="button"
                onClick={() => setEditing(false)}
                className="rounded-xl border-2 border-black bg-white px-4 py-2 font-space text-sm font-bold cursor-pointer"
              >
                {t('common.cancel')}
              </button>
            )}
          </div>
        </form>
      )}
    </section>
  );
}

function CheckinSummary({ checkin }: { checkin: Checkin }) {
  const { t } = useTranslation();
  const items = summarizeCheckin(checkin, t);
  return (
    <div className="flex flex-col gap-2" role="status">
      <p className="inline-flex items-center gap-1.5 font-space text-sm font-bold">
        <CheckCircle2 className="w-4 h-4 text-green-700" aria-hidden="true" />
        {t('checkin.done')}
      </p>
      <ul className="flex flex-wrap gap-2" aria-label={t('checkin.title')}>
        {items.map((item) => (
          <li key={item} className="rounded-xl border border-black bg-paper-warm px-2.5 py-1 font-space text-xs font-bold">{item}</li>
        ))}
      </ul>
    </div>
  );
}

function Slider({ id, label, icon, value, min, max, step, suffix, accent, onChange }: {
  id: string; label: string; icon: ReactNode; value: number; min: number; max: number; step: number;
  suffix: string; accent: string; onChange: (value: number) => void;
}) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="flex items-center justify-between font-space text-xs font-bold">
        <span className="inline-flex items-center gap-1">{icon}{label}</span>
        <span>{value}{suffix}</span>
      </label>
      <input
        id={id}
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(event) => onChange(parseFloat(event.target.value))}
        className={`w-full cursor-pointer ${accent}`}
      />
    </div>
  );
}
