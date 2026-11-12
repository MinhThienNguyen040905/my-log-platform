import type { CSSProperties } from 'react';
import type { MascotExpression, MascotId } from '../utils/mascots';
import s from './MoodMascot.module.css';

/**
 * Vector art for the three mascots (200×200 viewBox), one drawing per expression. Motion lives in
 * MoodMascot.module.css; the parent <svg> carries the expression class that picks the animation cycle.
 */
const INK = '#3b2f2f';
type Cmd = [string, ...number[]];

const cls = (...names: Array<string | false | undefined>) => names.filter(Boolean).join(' ');
/** Builds a path from relative commands: x is mirrored by `sx` and both axes scaled by `k`, then moved to (tx, ty). */
const mk = (cmds: Cmd[], tx: number, ty: number, sx = 1, k = 1) =>
  cmds.map(([op, ...v]) => op + v.map((n, i) => (i % 2 === 0 ? tx + sx * k * n : ty + k * n).toFixed(1)).join(' ')).join('');
const delay = (seconds: number): CSSProperties => ({ animationDelay: `${seconds}s` });

const Dot = ({ x, y, r, c = INK }: { x: number; y: number; r: number; c?: string }) => <circle cx={x} cy={y} r={r} fill={c} />;
const Line = ({ d, w = 3, c = INK }: { d: string; w?: number; c?: string }) => (
  <path d={d} fill="none" stroke={c} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round" />
);
const Shadow = () => <ellipse className={cls(s.shadow, s.fb)} cx={100} cy={184} rx={52} ry={8} />;
const Tear = ({ d, w = 1.8, delay: dl }: { d: string; w?: number; delay?: number }) => (
  <path className={cls(s.tear, s.fb)} style={dl ? delay(dl) : undefined} fill="#8FD0F2" stroke={INK} strokeWidth={w} strokeLinejoin="round" d={d} />
);

function Star({ x, y, r, dl }: { x: number; y: number; r: number; dl: number }) {
  const points = [[0, -r], [r * .22, -r * .22], [r, 0], [r * .22, r * .22], [0, r], [-r * .22, r * .22], [-r, 0], [-r * .22, -r * .22]]
    .map(([px, py]) => `${(x + px).toFixed(1)} ${(y + py).toFixed(1)}`);
  return <path className={cls(s.sparkle, s.fb)} style={delay(dl)} fill="#FFD23F" stroke={INK} strokeWidth={1.6} strokeLinejoin="round" d={`M${points.join(' L')} Z`} />;
}

const HEART: Cmd[] = [['M', 0, 4], ['C', -9, -3, -5, -10, 0, -5], ['C', 5, -10, 9, -3, 0, 4], ['Z']];
const Heart = ({ x, y, k, dl }: { x: number; y: number; k: number; dl: number }) => (
  <path className={cls(s.heart, s.fb)} style={delay(dl)} fill="#F2778F" stroke={INK} strokeWidth={1.8} strokeLinejoin="round" d={mk(HEART, x, y, 1, k)} />
);

function Anger({ x, y, k = 1 }: { x: number; y: number; k?: number }) {
  const d = `M${x - 13 * k} ${y - 4 * k} Q${x - 7 * k} ${y - 4 * k} ${x - 7 * k} ${y - 10 * k} M${x + 7 * k} ${y - 10 * k} Q${x + 7 * k} ${y - 4 * k} ${x + 13 * k} ${y - 4 * k} M${x + 13 * k} ${y + 4 * k} Q${x + 7 * k} ${y + 4 * k} ${x + 7 * k} ${y + 10 * k} M${x - 7 * k} ${y + 10 * k} Q${x - 7 * k} ${y + 4 * k} ${x - 13 * k} ${y + 4 * k}`;
  return <g className={cls(s.anger, s.fb)}><Line d={d} w={3.4 * k} c="#E53935" /></g>;
}

/* ---------- Orange cat ---------- */
const CAT_TAILS: Record<MascotExpression, string> = {
  normal: 'M140 164 C176 168 182 128 164 112', happy: 'M140 164 C180 166 188 124 170 102', sad: 'M140 166 C172 176 188 160 180 142',
  angry: 'M140 164 C170 160 180 130 166 106', tired: 'M140 168 C168 178 188 172 184 162',
};
const CAT_EAR_TILT: Partial<Record<MascotExpression, number>> = { sad: 32, tired: 22, angry: 12 };

function CatFace({ e }: { e: MascotExpression }) {
  const blush = (o: number) => (<><ellipse cx={72} cy={90} rx={8} ry={5} fill="#F28B82" opacity={o} /><ellipse cx={128} cy={90} rx={8} ry={5} fill="#F28B82" opacity={o} /></>);
  const nose = <path className={s.o} strokeWidth={2} d="M96 86 L104 86 L100 91 Z" fill="#E8837D" />;
  switch (e) {
    case 'happy': return (<>
      {blush(.95)}<Line d="M74 80 Q82 70 90 80 M110 80 Q118 70 126 80" w={3.6} />{nose}
      <path className={s.o} strokeWidth={2.6} d="M90 94 Q100 112 110 94 Z" fill="#E4574F" /><ellipse cx={100} cy={100} rx={5} ry={2.6} fill="#F59A96" />
    </>);
    case 'sad': return (<>
      {blush(.3)}<Line d="M72 74 L88 67 M112 67 L128 74" w={2.8} />
      <g className={cls(s.blink, s.fb)}><Dot x={82} y={79} r={4.4} /><Dot x={118} y={79} r={4.4} /><Dot x={83.4} y={77.6} r={1.4} c="#fff" /><Dot x={119.4} y={77.6} r={1.4} c="#fff" /></g>
      {nose}<Line d="M92 101 Q100 93 108 101" w={2.8} />
      <Tear d="M76 86 Q71 94 76 98 Q81 94 76 86Z" />
    </>);
    case 'angry': return (<>
      {blush(.2)}<Line d="M68 65 L92 77 M132 65 L108 77" w={4.2} />
      <g className={cls(s.blink, s.fb)}><Dot x={85} y={82} r={3.2} /><Dot x={115} y={82} r={3.2} /></g>{nose}
      <Line d="M89 99 L93 95 L97 99 L100 95 L103 99 L107 95 L111 99" w={2.8} />
      <path d="M93 95 L95 99 L97 95Z M103 95 L105 99 L107 95Z" fill="#fff" stroke="none" />
    </>);
    case 'tired': return (<>
      {blush(.25)}
      <path d="M78.8 80 A3.6 3.6 0 0 0 85.2 80 Z M114.8 80 A3.6 3.6 0 0 0 121.2 80 Z" fill={INK} /><Line d="M74 80 L90 80 M110 80 L126 80" w={3.2} />
      <Line d="M76 88 Q82 92 88 88 M112 88 Q118 92 124 88" w={2} c="#8E7CC3" />{nose}
      <Line d="M91 99 Q95 96 99 99 Q103 102 107 99" w={2.6} />
      <path className={s.o} strokeWidth={1.8} d="M103 101 L103 105 Q103 108 106 108 Q109 108 109 105 L109 100" fill="#F28B82" />
    </>);
    default: return (<>
      {blush(.45)}<g className={cls(s.blink, s.fb)}><Dot x={82} y={78} r={3.6} /><Dot x={118} y={78} r={3.6} /></g>{nose}
      <Line d="M92 96 Q96 101 100 96 Q104 101 108 96" w={2.6} />
    </>);
  }
}

function Cat({ e, sigh }: { e: MascotExpression; sigh: string }) {
  const OR = '#F5A646', OR2 = '#FBD79A', DK = '#D9822B';
  const [outer, inner] = e === 'angry' ? [26, 20] : [20, 14];
  const tilt = CAT_EAR_TILT[e];
  return (<>
    <Shadow />
    <g className={s.char}>
      <g className={s.tail}>
        <path d={CAT_TAILS[e]} fill="none" stroke={INK} strokeWidth={outer} strokeLinecap="round" />
        <path d={CAT_TAILS[e]} fill="none" stroke={OR} strokeWidth={inner} strokeLinecap="round" />
      </g>
      <path className={s.o} d="M58 168 C48 132 62 100 100 100 C138 100 152 132 142 168 C130 182 70 182 58 168Z" fill={OR} />
      <ellipse cx={100} cy={150} rx={22} ry={20} fill={OR2} />
      <path d="M62 140 Q58 150 62 160 M138 140 Q142 150 138 160" fill="none" stroke={DK} strokeWidth={3} strokeLinecap="round" />
      <ellipse className={s.o} cx={82} cy={176} rx={12} ry={7} fill={OR} /><ellipse className={s.o} cx={118} cy={176} rx={12} ry={7} fill={OR} />
      <g className={s.head}>
        <g className={s.earL} style={tilt ? { transform: `rotate(${-tilt}deg)` } : undefined}>
          <path className={s.o} d="M62 62 L58 30 L90 46 Z" fill={OR} /><path d="M66 56 L64 38 L80 47 Z" fill="#F6A5A0" />
        </g>
        <g className={s.earR} style={tilt ? { transform: `rotate(${tilt}deg)` } : undefined}>
          <path className={s.o} d="M138 62 L142 30 L110 46 Z" fill={OR} /><path d="M134 56 L136 38 L120 47 Z" fill="#F6A5A0" />
        </g>
        <ellipse className={s.o} cx={100} cy={78} rx={42} ry={34} fill={OR} />
        <Line d="M92 48 L92 56 M100 46 L100 56 M108 48 L108 56" c={DK} />
        <Line d="M60 88 L42 83 M60 94 L42 97 M140 88 L158 83 M140 94 L158 97" w={2.2} />
        <CatFace e={e} />
      </g>
      {e === 'angry' && <Anger x={168} y={52} />}
      {e === 'tired' && sigh && <text className={s.say} x={4} y={64}>{sigh}</text>}
    </g>
  </>);
}

/* ---------- Sprout ---------- */
const LEAF: Cmd[] = [['M', 0, 0], ['C', 22, 6, 50, -6, 58, -32], ['C', 32, -36, 4, -24, 0, 0], ['Z']];
const SPIKY: Cmd[] = [['M', 0, 0], ['L', 12, 7], ['L', 17, 1], ['L', 28, 4], ['L', 31, -4], ['L', 43, -3], ['L', 45, -12], ['L', 58, -32], ['L', 46, -30], ['L', 44, -24], ['L', 34, -26], ['L', 30, -18], ['L', 20, -19], ['L', 14, -10], ['L', 5, -12], ['Z']];
const VEIN: Cmd[] = [['M', 2, -2], ['Q', 30, -14, 52, -30]];
const BOLT: Cmd[] = [['M', 2, -14], ['L', -8, 2], ['L', -1, 2], ['L', -6, 16], ['L', 9, -3], ['L', 2, -3], ['L', 8, -14], ['Z']];

type SproutLook = { top: [number, number]; stem: string; col: string; vein: string; aR: number; aL: number; amp: number; face: [number, number]; shape?: Cmd[] };
const SPROUT: Record<MascotExpression, SproutLook> = {
  normal: { top: [100, 120], stem: 'M100 170 C100 152 99 138 100 120', col: '#7BCB5A', vein: '#3f8f3a', aR: -4, aL: 4, amp: 4, face: [100, 142] },
  happy: { top: [100, 118], stem: 'M100 170 C100 150 100 136 100 118', col: '#6FD14B', vein: '#2f8f3a', aR: -16, aL: 16, amp: 7, face: [100, 140] },
  sad: { top: [96, 108], stem: 'M100 170 C102 150 92 130 96 108', col: '#86BF9A', vein: '#4b8a68', aR: 52, aL: -52, amp: 3, face: [98, 142] },
  angry: { top: [100, 120], stem: 'M100 170 C100 152 100 138 100 120', col: '#4E9B3E', vein: '#2a6a2a', aR: -24, aL: 24, amp: 2, face: [100, 142], shape: SPIKY },
  tired: { top: [106, 112], stem: 'M100 170 C100 150 112 134 106 112', col: '#B9B85A', vein: '#8a8a34', aR: 66, aL: -40, amp: 3, face: [105, 144] },
};

function SproutFace({ e, x, y }: { e: MascotExpression; x: number; y: number }) {
  switch (e) {
    case 'happy': return (<>
      <Line d={`M${x - 7} ${y + 1} Q${x - 4.5} ${y - 4} ${x - 2} ${y + 1} M${x + 2} ${y + 1} Q${x + 4.5} ${y - 4} ${x + 7} ${y + 1}`} w={2} />
      <path d={`M${x - 3.5} ${y + 5} Q${x} ${y + 11} ${x + 3.5} ${y + 5}Z`} fill="#E4574F" stroke={INK} strokeWidth={1.5} strokeLinejoin="round" />
      <Dot x={x - 7} y={y + 6} r={2.2} c="#F28B82" /><Dot x={x + 7} y={y + 6} r={2.2} c="#F28B82" />
    </>);
    case 'sad': return (<>
      <Dot x={x - 4.5} y={y + 1} r={2} /><Dot x={x + 4.5} y={y + 1} r={2} />
      <Line d={`M${x - 7} ${y - 3} L${x - 2.5} ${y - 5} M${x + 2.5} ${y - 5} L${x + 7} ${y - 3}`} w={1.6} />
      <Line d={`M${x - 2.5} ${y + 8} Q${x} ${y + 5} ${x + 2.5} ${y + 8}`} w={1.8} />
    </>);
    case 'angry': return (<>
      <Line d={`M${x - 7.5} ${y - 4} L${x - 2} ${y - 1} M${x + 7.5} ${y - 4} L${x + 2} ${y - 1}`} w={2} />
      <Dot x={x - 4.5} y={y + 2} r={1.8} /><Dot x={x + 4.5} y={y + 2} r={1.8} />
      <Line d={`M${x - 3.5} ${y + 8} L${x - 1.2} ${y + 5.5} L${x + 1.2} ${y + 8} L${x + 3.5} ${y + 5.5}`} w={1.6} />
    </>);
    case 'tired': return (<>
      <Line d={`M${x - 7} ${y} L${x - 2} ${y} M${x + 2} ${y} L${x + 7} ${y}`} w={2} />
      <Line d={`M${x - 6.5} ${y + 3.5} Q${x - 4.5} ${y + 5} ${x - 2.5} ${y + 3.5} M${x + 2.5} ${y + 3.5} Q${x + 4.5} ${y + 5} ${x + 6.5} ${y + 3.5}`} w={1.2} c="#8E7CC3" />
      <Line d={`M${x - 3} ${y + 8} Q${x - 1.5} ${y + 6.5} ${x} ${y + 8} Q${x + 1.5} ${y + 9.5} ${x + 3} ${y + 8}`} w={1.6} />
    </>);
    default: return (<>
      <Dot x={x - 4.5} y={y} r={2} /><Dot x={x + 4.5} y={y} r={2} />
      <Line d={`M${x - 2.5} ${y + 5} Q${x} ${y + 8} ${x + 2.5} ${y + 5}`} w={1.8} />
    </>);
  }
}

function Sprout({ e, sigh }: { e: MascotExpression; sigh: string }) {
  const look = SPROUT[e];
  const [tx, ty] = look.top;
  const shape = look.shape ?? LEAF;
  const leaf = (sx: number, angle: number) => (
    <g className={s.leaf} style={{ '--a': angle, '--amp': sx > 0 ? look.amp : -look.amp, transformOrigin: `${tx}px ${ty}px` } as CSSProperties}>
      <path className={s.o} d={mk(shape, tx, ty, sx)} fill={look.col} />
      <Line d={mk(VEIN, tx, ty, sx)} w={2.2} c={look.vein} />
    </g>
  );
  const bolt = (x: number, y: number, k: number, dl: number) => (
    <path className={cls(s.zap, s.fb)} style={delay(dl)} fill="#FFD23F" stroke={INK} strokeWidth={2} strokeLinejoin="round" d={mk(BOLT, x, y, 1, k)} />
  );
  return (<>
    <Shadow />
    <g className={s.char}>
      <path d={look.stem} fill="none" stroke={INK} strokeWidth={26} strokeLinecap="round" />
      <path d={look.stem} fill="none" stroke={look.col} strokeWidth={20} strokeLinecap="round" />
      <SproutFace e={e} x={look.face[0]} y={look.face[1]} />
      {leaf(-1, look.aL)}{leaf(1, look.aR)}
      {e === 'happy' && (<>
        <Star x={28} y={76} r={8} dl={0} /><Star x={174} y={70} r={7} dl={.35} /><Star x={34} y={114} r={6} dl={.7} />
        <Star x={168} y={110} r={8} dl={.2} /><Star x={100} y={52} r={6} dl={.55} />
      </>)}
      {e === 'sad' && (<>
        <Tear d="M156 134 Q151 142 156 146 Q161 142 156 134Z" />
        <Tear d="M38 134 Q33 142 38 146 Q43 142 38 134Z" delay={-.9} />
      </>)}
      {e === 'angry' && (<>
        <g className={cls(s.cloud, s.fb)}>
          <ellipse className={s.o} cx={100} cy={36} rx={26} ry={11} fill="#6B6F80" /><circle className={s.o} cx={82} cy={32} r={11} fill="#6B6F80" />
          <circle className={s.o} cx={104} cy={26} r={13} fill="#7B8092" /><circle className={s.o} cx={122} cy={34} r={10} fill="#6B6F80" />
          <ellipse cx={100} cy={40} rx={24} ry={7} fill="#6B6F80" />
        </g>
        {bolt(100, 62, 1.1, 0)}{bolt(30, 86, 1.3, -.35)}{bolt(172, 88, 1.3, -.7)}{bolt(48, 54, .9, -.2)}{bolt(154, 52, .9, -.55)}
      </>)}
      {e === 'tired' && sigh && <text className={s.say} x={6} y={62}>{sigh}</text>}
    </g>
    <path className={s.o} d="M58 174 Q64 158 100 158 Q136 158 142 174 Q100 186 58 174Z" fill="#A0703F" />
    <ellipse cx={80} cy={170} rx={5} ry={2.4} fill="#7B5230" /><ellipse cx={116} cy={173} rx={6} ry={2.4} fill="#7B5230" /><ellipse cx={100} cy={166} rx={3} ry={1.6} fill="#7B5230" />
  </>);
}

/* ---------- Coffee cup ---------- */
const CUP_BODY = 'M48 98 C48 98 50 158 68 170 C84 180 116 180 132 170 C150 158 152 98 152 98 Z';
const CUP_RING: Record<MascotExpression, string> = { normal: '#B27646', happy: '#B27646', sad: '#A9703F', angry: '#4A2A18', tired: '#A9703F' };
const CUP_FOAM: Record<MascotExpression, string> = { normal: '#F6E7CF', happy: '#FBEBD2', sad: '#F1E0C6', angry: '#5E3822', tired: '#E7D2B0' };

function CupFace({ e }: { e: MascotExpression }) {
  switch (e) {
    case 'happy': return (<>
      <Line d="M81 98 Q87 90 93 98 M107 98 Q113 90 119 98" /><Line d="M92 101 Q100 110 108 101" w={2.8} />
      <ellipse cx={78} cy={102} rx={5} ry={3} fill="#F28B82" opacity={.75} /><ellipse cx={122} cy={102} rx={5} ry={3} fill="#F28B82" opacity={.75} />
    </>);
    case 'sad': return (<>
      <Dot x={88} y={97} r={2.6} /><Dot x={112} y={97} r={2.6} />
      <Line d="M80 94 L93 90 M107 90 L120 94" w={2.4} /><Line d="M93 105 Q100 99 107 105" w={2.6} />
      <Tear d="M82 101 Q78.5 106 82 109 Q85.5 106 82 101Z" w={1.6} />
    </>);
    case 'angry': {
      const light = '#F6E7CF'; // the dark foam needs a light face
      return (<>
        <Line d="M79 90 L94 96 M121 90 L106 96" c={light} /><Dot x={90} y={99.5} r={2.2} c={light} /><Dot x={110} y={99.5} r={2.2} c={light} />
        <Line d="M92 106 Q100 101 108 106" w={2.8} c={light} />
      </>);
    }
    case 'tired': return (<>
      <Line d="M82 96 L94 96 M106 96 L118 96" /><Line d="M83 100 Q88 103 93 100 M107 100 Q112 103 117 100" w={1.8} c="#8E7CC3" />
      <Line d="M93 105 Q96.5 102 100 105 Q103.5 108 107 105" w={2.4} />
    </>);
    default: return (<><Dot x={88} y={96} r={2.6} /><Dot x={112} y={96} r={2.6} /><Line d="M95 103 L105 103" w={2.4} /></>);
  }
}

function Wisp({ x, dl, col = '#C9C2B8', w = 3.5 }: { x: number; dl: number; col?: string; w?: number }) {
  return <path className={s.steam} style={delay(dl)} fill="none" stroke={col} strokeWidth={w} strokeLinecap="round" d={`M${x} 76 Q${x - 7} 66 ${x} 56 Q${x + 7} 46 ${x} 36`} />;
}

function Cup({ e, clipId }: { e: MascotExpression; clipId: string }) {
  const CR = '#FFF6E4', SH = '#EADBBA';
  const foam = CUP_FOAM[e];
  return (<>
    <Shadow />
    <g className={s.char}>
      <path d="M148 112 C182 104 184 152 146 156" fill="none" stroke={INK} strokeWidth={19} strokeLinecap="round" />
      <path d="M148 112 C182 104 184 152 146 156" fill="none" stroke={CR} strokeWidth={12} strokeLinecap="round" />
      <clipPath id={clipId}><path d={CUP_BODY} /></clipPath>
      <path d={CUP_BODY} fill={CR} />
      <path clipPath={`url(#${clipId})`} d="M132 90 L160 90 L160 190 L108 190 Q140 150 132 90Z" fill={SH} />
      <path className={s.o} d={CUP_BODY} fill="none" />
      <path d="M58 114 Q58 142 64 156" fill="none" stroke="#fff" strokeWidth={4} strokeLinecap="round" opacity={.85} />
      <ellipse className={s.o} cx={100} cy={98} rx={53} ry={18} fill={CR} />
      <ellipse cx={100} cy={98} rx={46} ry={14.5} fill={CUP_RING[e]} />
      <ellipse cx={100} cy={98} rx={39} ry={11.5} fill={foam} />
      <CupFace e={e} />
      {e === 'tired' && <path d="M118 112 Q118 130 125 130 Q132 130 132 112" fill={foam} stroke={INK} strokeWidth={2.4} strokeLinecap="round" strokeLinejoin="round" />}
      {e === 'normal' && (<><Wisp x={86} dl={0} /><Wisp x={114} dl={-1.1} /></>)}
      {e === 'happy' && (<>
        <Wisp x={82} dl={0} /><Wisp x={118} dl={-.6} />
        <Heart x={62} y={60} k={1.1} dl={0} /><Heart x={140} y={54} k={.9} dl={-.7} /><Heart x={100} y={42} k={.7} dl={-1.3} />
      </>)}
      {e === 'sad' && <Wisp x={100} dl={0} col="#B9B5C4" />}
      {e === 'angry' && (<>
        <Wisp x={80} dl={0} col="#E08A6A" w={4} /><Wisp x={100} dl={-.25} col="#E08A6A" w={4} /><Wisp x={120} dl={-.5} col="#E08A6A" w={4} />
        <Anger x={158} y={60} k={.75} />
      </>)}
      {e === 'tired' && (<>
        <Wisp x={106} dl={0} col="#D6D0C6" w={3} />
        <text className={s.zz} x={136} y={72} fontSize={13}>z</text>
        <text className={s.zz} style={delay(-.4)} x={146} y={58} fontSize={17}>Z</text>
        <text className={s.zz} style={delay(-.8)} x={158} y={42} fontSize={21}>z</text>
      </>)}
    </g>
  </>);
}

type Props = { mascot: MascotId; expression: MascotExpression; sigh: string; clipId: string };

/** Children of the mascot <svg>; `clipId` must be unique on the page (the cup clips its shading). */
export function MascotArt({ mascot, expression, sigh, clipId }: Props) {
  if (mascot === 'cat') return <Cat e={expression} sigh={sigh} />;
  if (mascot === 'coffee') return <Cup e={expression} clipId={clipId} />;
  return <Sprout e={expression} sigh={sigh} />;
}
