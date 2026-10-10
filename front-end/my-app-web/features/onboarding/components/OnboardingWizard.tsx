'use client';

import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { Cat, Coffee, Palette, Sprout } from 'lucide-react';
import { NeoButton } from '@/components/ui/NeoButton';
import { useToast } from '@/providers/ToastProvider';
import { GOALS, OPTIONAL_CONSENTS, useOnboarding } from '../hooks/useOnboarding';
import { useTranslation } from 'react-i18next';

const AVATARS = [
  { id: 'sprout', label: 'Mầm Xanh', icon: Sprout },
  { id: 'coffee', label: 'Ly Cà Phê', icon: Coffee },
  { id: 'cat', label: 'Mèo Cam', icon: Cat },
  { id: 'palette', label: 'Bút Vẽ', icon: Palette },
] as const;

const WRITING_TIMES = [
  { id: 'evening', label: 'Mỗi tối trước khi ngủ', description: 'Khoảng lặng để khép lại một ngày.' },
  { id: 'morning', label: 'Mỗi sáng sớm thức dậy', description: 'Bắt đầu ngày mới bằng một trang viết.' },
  { id: 'anytime', label: 'Bất cứ khi nào cần', description: 'Viết tự do khi bạn muốn.' },
] as const;

export function OnboardingWizard() {
  const { t } = useTranslation();
  const router = useRouter();
  const { showToast } = useToast();
  const form = useOnboarding();

  const finish = async (target: '/dashboard' | '/journal-editor', skip = false) => {
    try {
      await form.submit(skip);
      showToast({ title: t('onboarding.success'), message: t('onboarding.successDetail'), type: 'success' });
      router.replace(target);
    } catch (error) {
      showToast({ title: t('onboarding.error'), message: error instanceof Error ? error.message : t('auth.tryAgain'), type: 'error' });
    }
  };

  return (
    <div className='min-h-screen bg-bg-canvas text-on-surface'>
      <header className='border-b-2 border-on-background px-4 sm:px-8 py-4 flex items-center justify-between'>
        <Link href='/' aria-label='MyLog'><Image src='/logo.png' alt='MyLog' width={130} height={40} priority /></Link>
        <button type='button' disabled={form.submitting} onClick={() => void finish('/dashboard', true)} className='font-space font-bold text-sm underline disabled:opacity-50'>{t('onboarding.skip')}</button>
      </header>

      <main className='max-w-3xl mx-auto px-4 py-10 space-y-7'>
        <div className='text-center space-y-2'>
          <h1 className='font-space text-3xl sm:text-4xl font-extrabold'>{t('onboarding.title')}</h1>
          <p className='text-on-surface-variant'>{t('onboarding.intro')}</p>
        </div>

        <section className='bg-surface-card border-neo rounded-3xl p-6 shadow-neo space-y-4' aria-labelledby='profile-title'>
          <h2 id='profile-title' className='font-space text-xl font-bold'>{t('onboarding.profile')}</h2>
          <label className='block font-medium' htmlFor='onboarding-display-name'>{t('onboarding.name')}</label>
          <input id='onboarding-display-name' autoComplete='name' maxLength={120} value={form.displayName} onChange={(event) => form.setDisplayName(event.target.value)} aria-invalid={!!form.fieldErrors.displayName} aria-describedby={form.fieldErrors.displayName ? 'onboarding-display-name-error' : undefined} className='w-full rounded-xl border-2 border-on-background bg-white px-4 py-3' placeholder={t('onboarding.namePlaceholder')} />
          {form.fieldErrors.displayName && <p id='onboarding-display-name-error' role='alert' className='text-sm text-red-700'>{form.fieldErrors.displayName}</p>}
          <label className='block font-medium' htmlFor='onboarding-pen-name'>{t('onboarding.penName')}</label>
          <input id='onboarding-pen-name' maxLength={120} value={form.penName} onChange={(event) => form.setPenName(event.target.value)} aria-invalid={!!form.fieldErrors.penName} aria-describedby={form.fieldErrors.penName ? 'onboarding-pen-name-error' : undefined} className='w-full rounded-xl border-2 border-on-background bg-white px-4 py-3' placeholder={t('onboarding.penNamePlaceholder')} />
          {form.fieldErrors.penName && <p id='onboarding-pen-name-error' role='alert' className='text-sm text-red-700'>{form.fieldErrors.penName}</p>}
        </section>

        <section className='bg-surface-card border-neo rounded-3xl p-6 shadow-neo space-y-4' aria-labelledby='avatar-title'>
          <h2 id='avatar-title' className='font-space text-xl font-bold'>{t('onboarding.avatar')}</h2>
          <div className='grid grid-cols-2 sm:grid-cols-4 gap-3'>
            {AVATARS.map(({ id, icon: Icon }) => <button key={id} type='button' aria-pressed={form.avatar === id} onClick={() => form.setAvatar(id)} className={`flex flex-col items-center gap-2 rounded-2xl border-2 border-on-background p-4 ${form.avatar === id ? 'bg-primary-container shadow-neo' : 'bg-white'}`}>
              <Icon className='h-7 w-7' aria-hidden='true' /><span className='font-space font-bold text-sm'>{t(`onboarding.avatarName.${id}`)}</span>
            </button>)}
          </div>
          <p className='text-xs text-on-surface-variant'>{t('onboarding.localOnly')}</p>
        </section>

        <section className='bg-surface-card border-neo rounded-3xl p-6 shadow-neo space-y-4' aria-labelledby='goals-title'>
          <h2 id='goals-title' className='font-space text-xl font-bold'>{t('onboarding.goals')}</h2>
          <p className='text-sm text-on-surface-variant'>{t('onboarding.goalsHint')}</p>
          <div className='grid sm:grid-cols-2 gap-3'>
            {GOALS.map((goal) => <label key={goal.id} className='flex items-center gap-3 border-2 border-on-background rounded-xl p-4 cursor-pointer'>
              <input type='checkbox' checked={form.goals.includes(goal.id)} onChange={() => form.toggleGoal(goal.id)} className='h-5 w-5' />
              <span>{t(`onboarding.goal.${goal.id}`)}</span>
            </label>)}
          </div>
        </section>

        <section className='bg-surface-card border-neo rounded-3xl p-6 shadow-neo space-y-4' aria-labelledby='writing-time-title'>
          <h2 id='writing-time-title' className='font-space text-xl font-bold'>{t('onboarding.writingTime')}</h2>
          <div className='grid sm:grid-cols-3 gap-3'>
            {WRITING_TIMES.map(({ id }) => <button key={id} type='button' aria-pressed={form.writingTime === id} onClick={() => form.setWritingTime(id)} className={`rounded-2xl border-2 border-on-background p-4 text-left ${form.writingTime === id ? 'bg-primary-container shadow-neo' : 'bg-white'}`}>
              <strong className='block font-space text-sm'>{t(`onboarding.time.${id}`)}</strong><span className='block text-xs mt-2'>{t(`onboardingExtra.${id}`)}</span>
            </button>)}
          </div>
          <p className='text-xs text-on-surface-variant'>{t('onboarding.localOnly')}</p>
        </section>

        <section className='bg-surface-card border-neo rounded-3xl p-6 shadow-neo space-y-4' aria-labelledby='consents-title'>
          <h2 id='consents-title' className='font-space text-xl font-bold'>{t('onboarding.consents')}</h2>
          {form.loadingConsents && <p role='status'>{t('onboarding.consentLoading')}</p>}
          {form.consentError && <div role='alert' className='space-y-2'><p>{form.consentError}</p><button type='button' onClick={() => void form.loadConsents()} className='underline font-bold'>{t('onboarding.consentRetry')}</button></div>}
          {!form.loadingConsents && !form.consentError && <>
            {(['TERMS', 'PRIVACY'] as const).map((type) => {
              const item = form.consents.find((consent) => consent.type === type);
              return <p key={type} className='text-sm'>{type === 'TERMS' ? t('onboarding.terms') : t('onboarding.privacy')}: <strong>{item?.granted ? t('onboarding.granted') : t('onboarding.notGranted')}</strong>{item?.documentVersion ? ` (${item.documentVersion})` : ''}.</p>;
            })}
            <div className='space-y-3 pt-2'>
              {OPTIONAL_CONSENTS.map(({ type }) => {
                const current = form.consents.find((item) => item.type === type);
                const revoked = form.revocations.includes(type);
                return <div key={type} className='border-2 border-on-background rounded-xl p-4'>
                  <label className='flex items-start gap-3'>
                    <input type='checkbox' checked={!!current?.granted && !revoked} disabled={!current?.granted} onChange={() => form.toggleRevocation(type)} className='h-5 w-5 mt-0.5' />
                    <span><strong>{t(`onboardingExtra.${type}`)}</strong><span className='block text-sm text-on-surface-variant'>{current?.granted ? t('onboardingExtra.consentGranted', { version: current.documentVersion }) : t('onboardingExtra.consentUnavailable')}</span></span>
                  </label>
                </div>;
              })}
            </div>
          </>}
        </section>

        <div className='flex flex-wrap justify-center gap-3 pb-8'>
          <NeoButton onClick={() => void finish('/dashboard')} disabled={form.submitting || form.loadingConsents || !!form.consentError}>{form.submitting ? t('onboarding.saving') : t('onboarding.finish')}</NeoButton>
          <NeoButton onClick={() => void finish('/journal-editor')} disabled={form.submitting || form.loadingConsents || !!form.consentError}>{t('onboarding.startWriting')}</NeoButton>
        </div>
      </main>
    </div>
  );
}
