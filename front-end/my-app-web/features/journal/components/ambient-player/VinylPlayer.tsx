'use client';

import { useEffect, useState } from 'react';
import { Pause, Play, SkipBack, SkipForward, Square, Volume2, VolumeX, X } from 'lucide-react';
import type { AmbientPlayer, AmbientTimer } from '../../hooks/useAmbientPlayer';

const GROOVES =
  'repeating-radial-gradient(circle at center, #141414 0px, #141414 2px, #2c2c2c 3px, #141414 4px)';
/** Static highlight on top of the spinning disc, so the spin reads as light sliding over vinyl. */
const SHEEN =
  'conic-gradient(from 20deg, transparent 0 18%, rgba(255,255,255,0.16) 24%, transparent 30% 68%, rgba(255,255,255,0.1) 74%, transparent 80%)';

const ICON_BUTTON =
  'inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full transition-colors hover:bg-lime-100 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black';

function formatClock(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60);
  return `${minutes}:${String(totalSeconds % 60).padStart(2, '0')}`;
}

/** Ticks every second while `active`; returns Date.now() so timer progress can be drawn. */
function useNow(active: boolean) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (!active) return;
    const first = window.setTimeout(() => setNow(Date.now()), 0);
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => { window.clearTimeout(first); window.clearInterval(id); };
  }, [active]);
  return now;
}

/** Thời gian đã nghe bài hiện tại; được key theo bài nên tự về 0 khi đổi bài. */
function ListenedClock({ playing }: { playing: boolean }) {
  const [seconds, setSeconds] = useState(0);
  useEffect(() => {
    if (!playing) return;
    const id = window.setInterval(() => setSeconds((value) => value + 1), 1000);
    return () => window.clearInterval(id);
  }, [playing]);
  return <span className="tabular-nums">{formatClock(seconds)}</span>;
}

function Turntable({ playing }: { playing: boolean }) {
  return (
    <div aria-hidden="true" className="relative mx-auto h-[248px] w-[264px]">
      <div className="absolute left-0 top-3 h-56 w-56 rounded-full border-2 border-black shadow-neo">
        <div
          className="absolute inset-0 animate-[spin_4s_linear_infinite] rounded-full"
          style={{ background: GROOVES, animationPlayState: playing ? 'running' : 'paused' }}
        >
          <div className="absolute inset-[31%] flex items-center justify-center rounded-full border-2 border-black bg-primary-container">
            <span className="absolute top-[18%] font-space text-[9px] font-extrabold tracking-[0.2em] text-black">MYLOG</span>
            <span className="h-3 w-3 rounded-full border-2 border-black bg-paper-warm" />
          </div>
        </div>
        <div className="pointer-events-none absolute inset-0 rounded-full" style={{ background: SHEEN }} />
      </div>

      {/* Tonearm: rests off the disc, swings onto the grooves while playing. */}
      <span className="absolute right-1 top-1 h-9 w-9 rounded-full border-2 border-black bg-gray-200 shadow-neo-xs" />
      <div
        className={`absolute right-[16px] top-[16px] h-[150px] w-[6px] origin-top transition-transform duration-700 ease-out ${
          playing ? 'rotate-[26deg]' : 'rotate-[4deg]'
        }`}
      >
        <span className="absolute inset-x-0 top-0 h-full rounded-full border border-black bg-gradient-to-r from-gray-300 via-white to-gray-400" />
        <span className="absolute -bottom-2 -left-[5px] h-5 w-4 rounded-sm border-2 border-black bg-gray-300" />
      </div>
      <span className="absolute right-[10px] top-[10px] h-[18px] w-[18px] rounded-full border-2 border-black bg-white" />
    </div>
  );
}

/** Máy phát nhạc nền dạng đĩa than trong panel Công cụ dịu. */
export function VinylPlayer({ player }: { player: AmbientPlayer }) {
  const playing = player.status === 'playing';
  const now = useNow(player.timerEndsAt !== null);
  const statusLabel = playing ? 'Đang phát' : player.status === 'paused' ? 'Tạm dừng' : 'Đã dừng';

  const timerTotal = player.timer === null ? null : player.timer * 60;
  const timerLeft = player.timerEndsAt === null ? null : Math.max(0, Math.ceil((player.timerEndsAt - now) / 1000));
  const timerFraction = timerTotal && timerLeft !== null ? Math.min(1, (timerTotal - timerLeft) / timerTotal) : null;

  return (
    <section aria-label="Nhạc nền" className="flex flex-col gap-4 px-4 py-5">
      <Turntable playing={playing} />

      <div className="flex flex-col gap-0.5">
        <span className="truncate font-space text-base font-extrabold" aria-live="polite">{player.track.title}</span>
        <span className="truncate text-xs text-gray-600">{statusLabel} · {player.track.description}</span>
      </div>

      {/* Ambient tracks loop forever, so the bar shows the sleep timer instead of a song length. */}
      <div className="flex flex-col gap-1">
        <div className="relative h-1.5 rounded-full bg-black/15">
          {timerFraction !== null && (
            <>
              <span className="absolute inset-y-0 left-0 rounded-full bg-black" style={{ width: `${timerFraction * 100}%` }} />
              <span
                className="absolute top-1/2 h-3.5 w-3.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-black bg-primary-container"
                style={{ left: `${timerFraction * 100}%` }}
              />
            </>
          )}
        </div>
        <div className="flex justify-between font-space text-[11px] font-bold text-gray-600">
          <ListenedClock key={player.track.id} playing={playing} />
          <span>{timerLeft !== null ? `tắt sau ${formatClock(timerLeft)}` : 'Phát liên tục'}</span>
        </div>
      </div>

      {player.notice && (
        <p role="status" className="flex items-center justify-between gap-2 rounded-xl border border-black bg-white px-3 py-1.5 text-xs">
          {player.notice}
          <button type="button" onClick={player.dismissNotice} aria-label="Đóng thông báo" className="rounded p-0.5 hover:bg-gray-100">
            <X className="h-3.5 w-3.5" />
          </button>
        </p>
      )}
      {player.unavailable && (
        <p role="status" className="rounded-xl border border-black bg-red-50 px-3 py-1.5 text-xs">
          Trình duyệt chưa cho phép phát âm thanh lúc này. Thử lại nhé.
        </p>
      )}

      <div className="flex items-center justify-between" role="group" aria-label="Điều khiển phát">
        <button
          type="button"
          onClick={player.close}
          disabled={!player.open}
          aria-label="Tắt và đóng nhạc nền"
          title="Tắt nhạc nền"
          className={`${ICON_BUTTON} disabled:cursor-not-allowed disabled:opacity-40`}
        >
          <Square className="h-4 w-4" />
        </button>
        <button type="button" onClick={player.previous} aria-label="Bài trước" title="Bài trước" className={ICON_BUTTON}>
          <SkipBack className="h-5 w-5" />
        </button>
        <button
          type="button"
          onClick={player.togglePlay}
          aria-label={playing ? 'Tạm dừng' : 'Phát'}
          title={playing ? 'Tạm dừng' : 'Phát'}
          className="inline-flex h-14 w-14 items-center justify-center rounded-full border-2 border-black bg-black text-white shadow-neo-sm transition-transform hover:-translate-y-0.5 active:translate-y-0 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-black"
        >
          {playing ? <Pause className="h-6 w-6" /> : <Play className="h-6 w-6 translate-x-0.5" />}
        </button>
        <button type="button" onClick={player.next} aria-label="Bài sau" title="Bài sau" className={ICON_BUTTON}>
          <SkipForward className="h-5 w-5" />
        </button>
        <button
          type="button"
          onClick={player.toggleMute}
          aria-label={player.muted ? 'Bật tiếng' : 'Tắt tiếng'}
          aria-pressed={player.muted}
          title={player.muted ? 'Bật tiếng' : 'Tắt tiếng'}
          className={ICON_BUTTON}
        >
          {player.muted || player.volume === 0 ? <VolumeX className="h-5 w-5" /> : <Volume2 className="h-5 w-5" />}
        </button>
      </div>

      <div className="flex items-center gap-3">
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
        <select
          value={player.timer ?? ''}
          onChange={(event) => player.setTimer(event.target.value === '' ? null : (Number(event.target.value) as AmbientTimer))}
          aria-label="Hẹn giờ tắt nhạc nền"
          className="shrink-0 rounded-xl border-2 border-black bg-white px-2 py-1.5 font-space text-xs font-bold cursor-pointer"
        >
          <option value="">Không hẹn giờ</option>
          <option value="15">Tắt sau 15 phút</option>
          <option value="30">Tắt sau 30 phút</option>
        </select>
      </div>

      <ul aria-label="Danh sách nhạc nền" className="flex flex-col gap-1">
        {player.tracks.map((track) => {
          const current = track.id === player.track.id;
          return (
            <li key={track.id}>
              <button
                type="button"
                aria-current={current}
                onClick={() => player.select(track.id)}
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
    </section>
  );
}
