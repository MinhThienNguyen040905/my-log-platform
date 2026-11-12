/**
 * Display helpers for ISO calendar dates ("2026-11-02") coming from the API. The date is a calendar day,
 * not an instant, so it is formatted in UTC to stay independent of the browser timezone.
 */
type Locale = 'vi' | 'en' | string;

const parts = (iso: string) => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d));
};

const formatter = (locale: Locale) => new Intl.DateTimeFormat(locale === 'en' ? 'en-US' : 'vi-VN', {
  day: 'numeric', month: locale === 'en' ? 'short' : 'long', year: 'numeric', timeZone: 'UTC',
});

/** "2 tháng 11, 2026" / "Nov 2, 2026". */
export function formatDate(iso: string, locale: Locale): string {
  return formatter(locale).format(parts(iso));
}

/** "2 – 8 tháng 11, 2026" / "Nov 2 – 8, 2026"; shared parts are written once. */
export function formatDateRange(from: string, to: string, locale: Locale): string {
  return formatter(locale).formatRange(parts(from), parts(to));
}
