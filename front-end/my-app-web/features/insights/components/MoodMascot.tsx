'use client';

import { useId } from 'react';
import { useTranslation } from 'react-i18next';
import { toMascotExpression, type MascotId, type MascotMood } from '../utils/mascots';
import { MascotArt } from './MascotArt';
import s from './MoodMascot.module.css';

type Props = { mascot: MascotId; mood: MascotMood; calm?: boolean; size?: number; className?: string };

/**
 * The animated mascot (inline SVG + CSS). `calm` (high-risk week) keeps it still and gentle; reduced-motion
 * users get the still drawing through the stylesheet.
 */
export function MoodMascot({ mascot, mood, calm = false, size = 128, className = '' }: Props) {
  const { t } = useTranslation();
  const clipId = useId();
  const expression = toMascotExpression(mood, calm);
  const label = t('mascot.ariaLabel', { name: t(`mascot.names.${mascot}`), mood: t(`mascot.moods.${mood}`) });
  const sigh = mascot === 'coffee' ? '' : t(`mascot.sighs.${mascot}`);

  return (
    <div
      role="img"
      aria-label={label}
      data-mascot={mascot}
      data-mood={mood}
      data-expression={expression}
      className={`relative shrink-0 ${className}`}
      style={{ width: size, height: size }}
    >
      <svg viewBox="0 0 200 200" aria-hidden="true" className={`${s.art} ${s[expression]} ${calm ? s.still : ''}`}>
        <MascotArt mascot={mascot} expression={expression} sigh={sigh} clipId={`mascot-cup-${clipId}`} />
      </svg>
    </div>
  );
}
