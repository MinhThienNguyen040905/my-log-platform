/**
 * Readable session name such as "MyLog Web · Chrome · Windows", derived from the User-Agent at sign-in so the
 * user can tell their devices apart in Settings. Only browser and OS family are kept; no version numbers.
 */
const BROWSERS: Array<[RegExp, string]> = [
  [/Edg(e|A|iOS)?\//, 'Edge'],
  [/OPR\/|Opera/, 'Opera'],
  [/SamsungBrowser\//, 'Samsung Internet'],
  [/Firefox\/|FxiOS\//, 'Firefox'],
  [/Chrome\/|CriOS\//, 'Chrome'],
  [/Safari\//, 'Safari'],
];
const SYSTEMS: Array<[RegExp, string]> = [
  [/Windows/, 'Windows'],
  [/Android/, 'Android'],
  [/iPhone|iPod/, 'iPhone'],
  [/iPad/, 'iPad'],
  [/Mac OS X|Macintosh/, 'macOS'],
  [/CrOS/, 'ChromeOS'],
  [/Linux/, 'Linux'],
];

const match = (value: string, table: Array<[RegExp, string]>) => table.find(([pattern]) => pattern.test(value))?.[1];

export function deviceNameFrom(userAgent: string | null | undefined): string {
  const parts = ['MyLog Web'];
  if (userAgent) {
    const browser = match(userAgent, BROWSERS);
    const system = match(userAgent, SYSTEMS);
    if (browser) parts.push(browser);
    if (system) parts.push(system);
  }
  return parts.join(' · ').slice(0, 120);
}
