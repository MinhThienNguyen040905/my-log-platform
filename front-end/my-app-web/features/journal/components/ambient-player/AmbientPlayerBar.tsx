'use client';

import { useEffect, useRef, useState } from 'react';
import { ListMusic, Pause, Play, SkipBack, SkipForward, Volume2, VolumeX, X } from 'lucide-react';
import type { AmbientPlayer, AmbientTimer } from '../../hooks/useAmbientPlayer';
import { Equalizer, SpinningDisc } from './PlayerVisuals';

const iconButton =
  'inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border-2 border-black bg-white transition-colors hover:bg-lime-100 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black disabled:opacity-50';

function useMinutesLeft(endsAt: number | null) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (endsAt === null) return;
    // Refresh "now" as soon as a timer is (re)set — the bar may have mounted minutes earlier.
    const first = window.setTimeout(() => setNow(Date.now()), 0);
    const id = window.setInterval(() => setNow(Date.now()), 15_000);
    return () => { window.clearTimeout(first); window.clearInterval(id); };
  }, [endsAt]);
  return endsAt === null ? null : Math.max(1, Math.ceil((endsAt - now) / 60_000));
}

/** Thanh máy phát nhạc nền mini, đặt ngay trên vùng viết. */
export function AmbientPlayerBar({ player }: { player: AmbientPlayer }) {
  const [listOpen, setListOpen] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const playing = player.status === 'playing';
  const minutesLeft = useMinutesLeft(player.timerEndsAt);

  useEffect(() => {
    if (!listOpen) return;
    const onPointer = (event: MouseEvent) => {
      if (listRef.current && !listRef.current.contains(event.target as Node)) setListOpen(false);
    };
    const onKey = (event: KeyboardEvent) => { if (event.key === 'Escape') setListOpen(false); };
    document.addEventListener('mousedown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => { document.removeEventListener('mousedown', onPointer); document.removeEventListener('keydown', onKey); };
  }, [listOpen]);

  const statusLabel = player.status === 'playing' ? 'Đang phát' : player.status === 'paused' ? 'Tạm dừng' : 'Đã dừng';

  return (
    <section
      aria-label="Nhạc nền"
      className="relative flex flex-col gap-3 rounded-2xl border-2 border-black bg-sky-50 px-3 py-3 shadow-neo-sm sm:px-4 animate-in fade-in"
    >
      <div className="flex items-center gap-3">
        <SpinningDisc playing={playing} />
        <div className="flex min-w-0 flex-1 flex-col leading-tight">
          <span className="truncate font-space text-sm font-extrabold" aria-live="polite">{player.track.title}</span>
          <span className="truncate text-xs text-gray-600">
            {statusLabel} · {player.track.description}{minutesLeft !== null ? ` · tắt sau ${minutesLeft} phút` : ''}
          </span>
        </div>
        <Equalizer playing={playing} />
        <button type="button" onClick={player.close} aria-label="Tắt và đóng nhạc nền" title="Tắt nhạc nền" className={iconButton}>
          <X className="h-4 w-4" />
        </button>
      </div>

      {player.notice && (
        <p role="status" className="flex items-center justify-between gap-2 rounded-xl border border-black bg-white px-3 py-1.5 text-xs">
          {player.notice}
          <button type="button" onClick={player.dismissNotice} aria-label="Đóng thông báo" className="rounded p-0.5 hover:bg-gray-100">
            <X className="h-3.5 w-3.5" />
          </button>
        </p>
      )}

      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-1.5" role="group" aria-label="Điều khiển phát">
          <button type="button" onClick={player.previous} aria-label="Bài trước" title="Bài trước" className={iconButton}>
            <SkipBack className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={player.togglePlay}
            aria-label={playing ? 'Tạm dừng' : 'Phát'}
            title={playing ? 'Tạm dừng' : 'Phát'}
            className={`${iconButton} h-10 w-10 bg-primary-container hover:bg-lime-200`}
          >
            {playing ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
          </button>
          <button type="button" onClick={player.next} aria-label="Bài sau" title="Bài sau" className={iconButton}>
            <SkipForward className="h-4 w-4" />
          </button>
        </div>

        <div className="flex min-w-[150px] flex-1 items-center gap-2">
          <button
            type="button"
            onClick={player.toggleMute}
            aria-label={player.muted ? 'Bật tiếng' : 'Tắt tiếng'}
            aria-pressed={player.muted}
            title={player.muted ? 'Bật tiếng' : 'Tắt tiếng'}
            className={iconButton}
          >
            {player.muted || player.volume === 0 ? <VolumeX className="h-4 w-4" /> : <Volume2 className="h-4 w-4" />}
          </button>
          <input
            type="range"
            min={0}
            max={100}
            step={5}
            value={Math.round(player.volume * 100)}
            onChange={(event) => player.setVolume(Number(event.target.value) / 100)}
            aria-label="Âm lượng"
            aria-valuetext={`${Math.round(player.volume * 100)}%${player.muted ? ', đang tắt tiếng' : ''}`}
            className="w-full min-w-0 cursor-pointer accent-black"
          />
        </div>

        <label className="flex items-center gap-1.5 font-space text-xs font-bold">
          <span className="sr-only sm:not-sr-only">Hẹn giờ</span>
          <select
            value={player.timer ?? ''}
            onChange={(event) => player.setTimer(event.target.value === '' ? null : (Number(event.target.value) as AmbientTimer))}
            aria-label="Hẹn giờ tắt nhạc nền"
            className="rounded-xl border-2 border-black bg-white px-2 py-1.5 font-space text-xs font-bold cursor-pointer"
          >
            <option value="">Không hẹn giờ</option>
            <option value="15">15 phút</option>
            <option value="30">30 phút</option>
          </select>
        </label>

        <div ref={listRef}>
          <button
            type="button"
            onClick={() => setListOpen((value) => !value)}
            aria-expanded={listOpen}
            aria-haspopup="true"
            aria-label="Danh sách nhạc nền"
            title="Danh sách nhạc nền"
            className={iconButton}
          >
            <ListMusic className="h-4 w-4" />
          </button>
          {listOpen && (
            <ul
              aria-label="Danh sách nhạc nền"
              // Anchored to the whole player bar (not the button, whose position changes as the row
              // wraps): full bar width on phones, a 16rem panel at the right edge on wider screens.
              className="absolute inset-x-3 top-full z-30 mt-2 flex flex-col gap-1 rounded-2xl border-2 border-black bg-paper-warm p-2 shadow-neo-lg sm:inset-x-auto sm:right-3 sm:w-64"
            >
              {player.tracks.map((track) => {
                const current = track.id === player.track.id;
                return (
                  <li key={track.id}>
                    <button
                      type="button"
                      aria-current={current}
                      onClick={() => { player.select(track.id); setListOpen(false); }}
                      className={`flex w-full flex-col items-start rounded-xl border px-3 py-2 text-left cursor-pointer ${
                        current ? 'border-black bg-primary-container' : 'border-transparent hover:border-black hover:bg-white'
                      }`}
                    >
                      <span className="font-space text-xs font-extrabold">{track.title}</span>
                      <span className="text-[11px] text-gray-600">{track.description}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </section>
  );
}
