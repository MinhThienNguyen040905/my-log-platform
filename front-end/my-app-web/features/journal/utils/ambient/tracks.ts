import {
  brownNoise, filter, gain, lfo, loopingSource, pinkNoise, random, release, scheduler, whiteNoise,
  type TrackBuilder,
} from './audio-blocks';

/**
 * The five procedural ambient tracks. Each builder connects to `output` (a per-track gain the engine
 * uses for cross-fades) and returns `stop()`. Levels are balanced so switching tracks keeps roughly
 * the same loudness (checked with tests/ambient-player.spec.ts loudness probe).
 */

/** Gentle rain: soft low rumble + a quiet 1–6 kHz patter that small speakers can actually play. */
export const rain: TrackBuilder = (ctx, output) => {
  const length = 4 * ctx.sampleRate;
  const rumble = loopingSource(ctx, brownNoise(length, 1337));
  const rumbleLow = filter(ctx, 'lowpass', 800);
  const rumbleGain = gain(ctx, 0.6);
  const patter = loopingSource(ctx, whiteNoise(length, 4242));
  const patterHigh = filter(ctx, 'highpass', 1000);
  const patterLow = filter(ctx, 'lowpass', 6000);
  const patterGain = gain(ctx, 0.16);
  rumble.connect(rumbleLow).connect(rumbleGain).connect(output);
  patter.connect(patterHigh).connect(patterLow).connect(patterGain).connect(output);
  rumble.start();
  patter.start();
  return { stop: () => release([rumble, patter], [rumbleLow, rumbleGain, patterHigh, patterLow, patterGain]) };
};

/** Ocean waves & breeze: surf that swells every ~11 s, foam hiss on the crest, a light wind bed. */
export const ocean: TrackBuilder = (ctx, output) => {
  const length = 6 * ctx.sampleRate;
  const surf = loopingSource(ctx, brownNoise(length, 2024));
  const surfLow = filter(ctx, 'lowpass', 650);
  const surfGain = gain(ctx, 0.5);
  const surfSwell = lfo(ctx, surfGain.gain, 0.09, 0.38);
  const foam = loopingSource(ctx, whiteNoise(length, 777));
  const foamBand = filter(ctx, 'bandpass', 2600, 0.6);
  const foamGain = gain(ctx, 0.06);
  const foamSwell = lfo(ctx, foamGain.gain, 0.09, 0.055);
  const wind = loopingSource(ctx, pinkNoise(length, 99));
  const windBand = filter(ctx, 'bandpass', 420, 0.8);
  const windGain = gain(ctx, 0.12);
  const windDrift = lfo(ctx, windGain.gain, 0.031, 0.05, 2);
  surf.connect(surfLow).connect(surfGain).connect(output);
  foam.connect(foamBand).connect(foamGain).connect(output);
  wind.connect(windBand).connect(windGain).connect(output);
  surf.start(); foam.start(); wind.start();
  return {
    stop: () => release([surf, foam, wind, surfSwell, foamSwell, windDrift],
      [surfLow, surfGain, foamBand, foamGain, windBand, windGain]),
  };
};

/** Cozy fireplace: warm low roar plus irregular crackles and the odd louder pop. */
export const fireplace: TrackBuilder = (ctx, output) => {
  const length = 4 * ctx.sampleRate;
  const roar = loopingSource(ctx, brownNoise(length, 31));
  const roarLow = filter(ctx, 'lowpass', 380);
  const roarGain = gain(ctx, 0.55);
  const flicker = lfo(ctx, roarGain.gain, 0.23, 0.12);
  roar.connect(roarLow).connect(roarGain).connect(output);
  roar.start();

  // One short decaying noise burst, reused for every crackle with a different playback rate/level.
  const burstLength = Math.floor(0.03 * ctx.sampleRate);
  const burstData = whiteNoise(burstLength, 5);
  for (let i = 0; i < burstLength; i++) burstData[i] *= Math.exp(-i / (burstLength / 6));
  const burst = ctx.createBuffer(1, burstLength, ctx.sampleRate);
  burst.getChannelData(0).set(burstData);
  const crackleHigh = filter(ctx, 'highpass', 1400);
  crackleHigh.connect(output);
  const next = random(8191);
  const stopScheduler = scheduler(ctx, (from, to) => {
    for (let t = from; t < to; t += 0.02) {
      if (next() > 0.93) {
        const source = ctx.createBufferSource();
        source.buffer = burst;
        source.playbackRate.value = 0.6 + next() * 1.4;
        const level = gain(ctx, next() > 0.9 ? 0.55 : 0.08 + next() * 0.18);
        source.connect(level).connect(crackleHigh);
        source.onended = () => { source.disconnect(); level.disconnect(); };
        source.start(t + next() * 0.02);
      }
    }
  });
  return {
    stop: () => {
      stopScheduler();
      release([roar, flicker], [roarLow, roarGain, crackleHigh]);
    },
  };
};

/** Calm lofi piano: slow jazzy seventh chords, soft felt-like notes, faint vinyl crackle. */
const PROGRESSION: number[][] = [
  [53, 57, 60, 64], // Fmaj7
  [52, 55, 59, 62], // Em7
  [50, 53, 57, 60], // Dm7
  [48, 52, 55, 59], // Cmaj7
];
const midiToHz = (note: number) => 440 * 2 ** ((note - 69) / 12);

export const lofiPiano: TrackBuilder = (ctx, output) => {
  const tone = filter(ctx, 'lowpass', 1700);
  const bus = gain(ctx, 0.9);
  tone.connect(bus).connect(output);

  const crackle = loopingSource(ctx, whiteNoise(2 * ctx.sampleRate, 606));
  const crackleBand = filter(ctx, 'bandpass', 3200, 0.5);
  const crackleGain = gain(ctx, 0.012);
  crackle.connect(crackleBand).connect(crackleGain).connect(output);
  crackle.start();

  const beat = 60 / 64; // 64 BPM
  const next = random(1234);
  let step = 0; // one step = one beat; a chord lasts 4 beats
  let nextStepAt = ctx.currentTime + 0.1;

  const note = (midi: number, at: number, velocity: number, length: number) => {
    const osc = ctx.createOscillator();
    osc.type = 'triangle';
    osc.frequency.value = midiToHz(midi);
    osc.detune.value = (next() - 0.5) * 8; // slightly out of tune, like an old upright
    const env = ctx.createGain();
    env.gain.setValueAtTime(0, at);
    env.gain.linearRampToValueAtTime(velocity, at + 0.012);
    env.gain.exponentialRampToValueAtTime(velocity * 0.35, at + 0.45);
    env.gain.exponentialRampToValueAtTime(0.0001, at + length);
    osc.connect(env).connect(tone);
    osc.onended = () => { osc.disconnect(); env.disconnect(); };
    osc.start(at);
    osc.stop(at + length + 0.05);
  };

  const stopScheduler = scheduler(ctx, (_from, to) => {
    while (nextStepAt < to) {
      const chord = PROGRESSION[Math.floor(step / 4) % PROGRESSION.length];
      const beatInChord = step % 4;
      const swing = (next() - 0.5) * 0.03;
      if (beatInChord === 0) {
        note(chord[0] - 12, nextStepAt, 0.16, beat * 4); // bass
        chord.forEach((midi, i) => note(midi, nextStepAt + i * 0.035 + swing, 0.075, beat * 3.6)); // rolled chord
      } else if (next() > 0.35) {
        const midi = chord[Math.floor(next() * chord.length)] + 12;
        note(midi, nextStepAt + swing, 0.05 + next() * 0.03, beat * 1.8); // sparse melody
      }
      step += 1;
      nextStepAt += beat;
    }
  }, 0.5, 80);

  return {
    stop: () => {
      stopScheduler();
      release([crackle], [crackleBand, crackleGain, tone, bus]);
    },
  };
};

/** Soft cafe ambience: distant murmur in a few drifting voice bands, room tone, an occasional cup clink. */
export const cafe: TrackBuilder = (ctx, output) => {
  const length = 6 * ctx.sampleRate;
  const room = loopingSource(ctx, brownNoise(length, 404));
  const roomLow = filter(ctx, 'lowpass', 250);
  const roomGain = gain(ctx, 0.25);
  room.connect(roomLow).connect(roomGain).connect(output);
  room.start();

  const voices = [320, 620, 1100].map((frequency, i) => {
    const source = loopingSource(ctx, pinkNoise(length, 900 + i));
    const band = filter(ctx, 'bandpass', frequency, 1.6);
    const level = gain(ctx, 0.14 - i * 0.03);
    const drift = lfo(ctx, level.gain, 0.17 + i * 0.07, 0.07, i);
    source.connect(band).connect(level).connect(output);
    source.start();
    return { source, band, level, drift };
  });

  const next = random(4321);
  let nextClinkAt = ctx.currentTime + 4 + next() * 6;
  const stopScheduler = scheduler(ctx, (_from, to) => {
    while (nextClinkAt < to) {
      const at = nextClinkAt;
      [2650, 3970].forEach((frequency, i) => {
        const osc = ctx.createOscillator();
        osc.frequency.value = frequency * (0.97 + next() * 0.06);
        const env = ctx.createGain();
        env.gain.setValueAtTime(0, at);
        env.gain.linearRampToValueAtTime(0.03 / (i + 1), at + 0.004);
        env.gain.exponentialRampToValueAtTime(0.0001, at + 0.5);
        osc.connect(env).connect(output);
        osc.onended = () => { osc.disconnect(); env.disconnect(); };
        osc.start(at);
        osc.stop(at + 0.55);
      });
      nextClinkAt += 6 + next() * 10;
    }
  }, 0.5, 120);

  return {
    stop: () => {
      stopScheduler();
      release([room, ...voices.flatMap((v) => [v.source, v.drift])],
        [roomLow, roomGain, ...voices.flatMap((v) => [v.band, v.level])]);
    },
  };
};
