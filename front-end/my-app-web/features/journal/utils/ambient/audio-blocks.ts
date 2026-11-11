/**
 * Small Web Audio building blocks shared by the procedural ambient tracks. Everything is generated
 * on the device: no audio file, no network request, no third party.
 */

/** A running track; `stop` must release every node, source and timer it created. */
export type TrackVoice = { stop: () => void };
export type TrackBuilder = (ctx: BaseAudioContext, output: AudioNode) => TrackVoice;

/** Deterministic white noise in [-1, 1] (Park–Miller generator, so every load sounds the same). */
export function whiteNoise(length: number, seed: number): Float32Array {
  const data = new Float32Array(length);
  let state = seed;
  for (let i = 0; i < length; i++) {
    state = (state * 16807) % 2147483647;
    data[i] = (state / 2147483647) * 2 - 1;
  }
  return data;
}

/** Brown (red) noise: leaky integration of white noise — deep, soft rumble. */
export function brownNoise(length: number, seed: number): Float32Array {
  const white = whiteNoise(length, seed);
  const data = new Float32Array(length);
  let last = 0;
  for (let i = 0; i < length; i++) {
    last = (last + 0.02 * white[i]) / 1.02;
    data[i] = last;
  }
  return normalize(data, 0.9);
}

/** Pink noise (Paul Kellet's economy filter): between white and brown, natural for wind/murmur. */
export function pinkNoise(length: number, seed: number): Float32Array {
  const white = whiteNoise(length, seed);
  const data = new Float32Array(length);
  let b0 = 0, b1 = 0, b2 = 0;
  for (let i = 0; i < length; i++) {
    b0 = 0.99765 * b0 + white[i] * 0.099046;
    b1 = 0.963 * b1 + white[i] * 0.2965164;
    b2 = 0.57 * b2 + white[i] * 1.0526913;
    data[i] = b0 + b1 + b2 + white[i] * 0.1848;
  }
  return normalize(data, 0.9);
}

export function normalize(data: Float32Array, peak: number): Float32Array {
  let max = 0;
  for (let i = 0; i < data.length; i++) max = Math.max(max, Math.abs(data[i]));
  if (max > 0) for (let i = 0; i < data.length; i++) data[i] = (data[i] / max) * peak;
  return data;
}

export function loopingSource(ctx: BaseAudioContext, data: Float32Array): AudioBufferSourceNode {
  const buffer = ctx.createBuffer(1, data.length, ctx.sampleRate);
  buffer.getChannelData(0).set(data);
  const source = ctx.createBufferSource();
  source.buffer = buffer;
  source.loop = true;
  return source;
}

export function filter(ctx: BaseAudioContext, type: BiquadFilterType, frequency: number, q = 0.707): BiquadFilterNode {
  const node = ctx.createBiquadFilter();
  node.type = type;
  node.frequency.value = frequency;
  node.Q.value = q;
  return node;
}

export function gain(ctx: BaseAudioContext, value: number): GainNode {
  const node = ctx.createGain();
  node.gain.value = value;
  return node;
}

/**
 * Slow sine modulation of an AudioParam around its current value (e.g. waves swelling).
 * Returns the oscillator so the caller can stop it.
 */
export function lfo(ctx: BaseAudioContext, target: AudioParam, frequencyHz: number, depth: number, phase = 0): OscillatorNode {
  const osc = ctx.createOscillator();
  osc.frequency.value = frequencyHz;
  const amount = gain(ctx, depth);
  osc.connect(amount).connect(target);
  osc.start(ctx.currentTime + phase / Math.max(frequencyHz, 0.001));
  return osc;
}

/**
 * Look-ahead scheduler for event-based tracks (piano notes, crackles, clinks). Every `intervalMs` it
 * asks `schedule(from, to)` to place events in the next `lookaheadSeconds` of audio time. While the
 * context is suspended (paused) audio time stands still, so nothing piles up.
 */
export function scheduler(
  ctx: BaseAudioContext,
  schedule: (from: number, to: number) => void,
  lookaheadSeconds = 0.3,
  intervalMs = 60,
): () => void {
  let scheduledUntil = ctx.currentTime;
  const tick = () => {
    const horizon = ctx.currentTime + lookaheadSeconds;
    if (horizon > scheduledUntil) {
      schedule(Math.max(scheduledUntil, ctx.currentTime), horizon);
      scheduledUntil = horizon;
    }
  };
  tick();
  const id = setInterval(tick, intervalMs);
  return () => clearInterval(id);
}

/** Deterministic pseudo-random sequence for event timing, so a track varies but stays reproducible. */
export function random(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state * 16807) % 2147483647;
    return state / 2147483647;
  };
}

/** Stops sources/oscillators and disconnects nodes, ignoring nodes that already stopped. */
export function release(sources: AudioScheduledSourceNode[], nodes: AudioNode[]): void {
  for (const source of sources) {
    try { source.stop(); } catch { /* already stopped */ }
    source.disconnect();
  }
  for (const node of nodes) node.disconnect();
}
