import Link from 'next/link';
import { cookies } from 'next/headers';
import { isAppLocale, resources } from '@/lib/i18n';
import { LanguageSwitcher } from '@/components/ui/LanguageSwitcher';

export default async function ForgotPasswordPage() {
  const stored = (await cookies()).get('mylog_locale')?.value;
  const locale = isAppLocale(stored) ? stored : 'vi';
  const labels = resources[locale].translation.auth;
  return <main className="min-h-screen flex items-center justify-center px-4 bg-bg-canvas">
    <section className="max-w-md bg-surface-card border-neo rounded-3xl p-6 shadow-neo space-y-4">
      <LanguageSwitcher />
      <h1 className="font-space text-2xl font-extrabold">{labels.forgot}</h1>
      <p className="text-sm text-on-surface-variant">{labels.forgotDescription}</p>
      <Link href="/auth/login" className="inline-block underline font-bold">{labels.backToLogin}</Link>
    </section>
  </main>;
}

