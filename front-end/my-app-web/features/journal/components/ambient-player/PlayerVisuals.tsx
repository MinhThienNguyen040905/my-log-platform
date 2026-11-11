import { Disc3 } from 'lucide-react';

/** Vinyl that turns while music plays. Global reduced-motion CSS stops the spin. */
export function SpinningDisc({ playing, className = 'h-11 w-11' }: { playing: boolean; className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`relative inline-flex shrink-0 items-center justify-center rounded-full border-2 border-black bg-neutral-900 ${className} ${
        playing ? 'animate-[spin_3s_linear_infinite]' : ''
      }`}
    >
      <span className="absolute inset-[22%] rounded-full border border-white/20" />
      <span className="h-[34%] w-[34%] rounded-full border-2 border-black bg-primary-container" />
    </span>
  );
}

/** Three bouncing bars; frozen when paused (and by reduced-motion CSS). */
export function Equalizer({ playing }: { playing: boolean }) {
  return (
    <span aria-hidden="true" className="inline-flex h-4 items-end gap-[3px]">
      {[0, 0.25, 0.5].map((delay) => (
        <span
          key={delay}
          className={`w-[3px] rounded-sm bg-black ${playing ? 'mylog-equalizer-bar h-4' : 'h-1.5'}`}
          style={playing ? { animationDelay: `${delay}s` } : undefined}
        />
      ))}
    </span>
  );
}

/** Small disc icon for the tool rail; spins while playing. */
export function RailDiscIcon({ playing }: { playing: boolean }) {
  return <Disc3 className={`h-5 w-5 ${playing ? 'animate-[spin_3s_linear_infinite]' : ''}`} />;
}
