import type { TrackVoice } from './audio-blocks';
import type { AmbientTrack } from './ambient-tracks';

/** Loudest master level (volume slider at 100%). Keeps peaks well below clipping. */
const MAX_MASTER_GAIN = 0.7;
const CROSSFADE_SECONDS = 1;

type Playing = { track: AmbientTrack; output: GainNode; voice: TrackVoice };

/**
 * One AudioContext for the ambient player. Created lazily on the first `play` (which always comes
 * from a click, so browsers allow it) and closed by `close()` — the editor calls that on unmount so
 * leaving the page always stops the sound.
 */
export class AmbientEngine {
  private ctx: AudioContext | null = null;
  private master: GainNode | null = null;
  private current: Playing | null = null;
  private volume = 0.8;
  private muted = false;

  /** Starts (or cross-fades to) a track. Returns false when Web Audio is unavailable. */
  play(track: AmbientTrack): boolean {
    const ctx = this.ensureContext();
    if (!ctx || !this.master) return false;
    if (ctx.state === 'suspended') void ctx.resume().catch(() => {});
    if (this.current?.track.id === track.id) return true;

    const now = ctx.currentTime;
    const previous = this.current;
    if (previous) {
      previous.output.gain.cancelScheduledValues(now);
      previous.output.gain.setValueAtTime(previous.output.gain.value, now);
      previous.output.gain.linearRampToValueAtTime(0, now + CROSSFADE_SECONDS);
      setTimeout(() => { previous.voice.stop(); previous.output.disconnect(); }, CROSSFADE_SECONDS * 1000 + 100);
    }
    const output = ctx.createGain();
    output.gain.setValueAtTime(0, now);
    output.gain.linearRampToValueAtTime(track.level, now + (previous ? CROSSFADE_SECONDS : 1.5));
    output.connect(this.master);
    this.current = { track, output, voice: track.build(ctx, output) };
    return true;
  }

  pause(): void {
    if (this.ctx?.state === 'running') void this.ctx.suspend().catch(() => {});
  }

  resume(): void {
    if (this.ctx?.state === 'suspended') void this.ctx.resume().catch(() => {});
  }

  setVolume(volume: number, muted: boolean): void {
    this.volume = Math.min(1, Math.max(0, volume));
    this.muted = muted;
    if (this.ctx && this.master) {
      this.master.gain.setTargetAtTime(this.targetGain(), this.ctx.currentTime, 0.05);
    }
  }

  /** Fades everything out over `seconds`, then closes the context. */
  fadeOutAndClose(seconds: number): Promise<void> {
    const ctx = this.ctx;
    const master = this.master;
    if (!ctx || !master || ctx.state !== 'running') { this.close(); return Promise.resolve(); }
    master.gain.cancelScheduledValues(ctx.currentTime);
    master.gain.setValueAtTime(master.gain.value, ctx.currentTime);
    master.gain.linearRampToValueAtTime(0, ctx.currentTime + seconds);
    return new Promise((resolve) => setTimeout(() => { this.close(); resolve(); }, seconds * 1000));
  }

  close(): void {
    this.current?.voice.stop();
    this.current = null;
    const ctx = this.ctx;
    this.ctx = null;
    this.master = null;
    if (ctx) void ctx.close().catch(() => {});
  }

  private targetGain(): number {
    return this.muted ? 0 : this.volume * MAX_MASTER_GAIN;
  }

  private ensureContext(): AudioContext | null {
    if (this.ctx) return this.ctx;
    try {
      const AudioContextClass =
        window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      const ctx = new AudioContextClass();
      const master = ctx.createGain();
      master.gain.value = this.targetGain();
      master.connect(ctx.destination);
      this.ctx = ctx;
      this.master = master;
      return ctx;
    } catch {
      return null;
    }
  }
}
