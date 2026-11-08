/**
 * Offline, royalty-free "gentle rain" ambience generated with the Web Audio API. No external
 * audio file or network request is involved, so this works even when the companion is offline.
 */
export function createGentleRainAudioContext(): AudioContext | null {
  try {
    const AudioContextClass =
      window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    const ctx = new AudioContextClass();
    const bufferSize = 2 * ctx.sampleRate;
    const noiseBuffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const output = noiseBuffer.getChannelData(0);
    let lastOut = 0.0;
    let seed = 1337;
    for (let i = 0; i < bufferSize; i++) {
      seed = (seed * 16807) % 2147483647;
      const white = (seed / 2147483647) * 2 - 1;
      output[i] = (lastOut + 0.02 * white) / 1.02;
      lastOut = output[i];
      output[i] *= 0.12; // Gentle rain volume
    }
    const whiteNoise = ctx.createBufferSource();
    whiteNoise.buffer = noiseBuffer;
    whiteNoise.loop = true;

    const filter = ctx.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.value = 650;

    whiteNoise.connect(filter);
    filter.connect(ctx.destination);
    whiteNoise.start(0);
    return ctx;
  } catch {
    return null;
  }
}

/** Closes an ambient sound context started by {@link createGentleRainAudioContext}, if any. */
export function closeAmbientAudioContext(ctx: AudioContext | null): void {
  if (ctx) void ctx.close().catch(() => {});
}
