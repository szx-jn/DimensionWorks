import type {CSSProperties, ReactNode} from 'react';
import {Audio} from '@remotion/media';
import {
  AbsoluteFill,
  CanvasImage,
  Easing,
  Sequence,
  interpolate,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from 'remotion';

const FONT = '"Hiragino Sans GB", "PingFang SC", "Microsoft YaHei", sans-serif';
const MONO = '"SFMono-Regular", Menlo, Monaco, monospace';

const COLORS = {
  ink: '#06090d',
  panel: '#0b1116',
  line: '#25414a',
  text: '#e9f1ec',
  muted: '#8da4a0',
  cyan: '#62e4d8',
  red: '#ff5b48',
  amber: '#f1ad3d',
};

const DIMENSIONS = [
  {
    id: 'magnetic',
    name: '磁场',
    machine: '极性冶炼塔',
    fluid: '极性磁流质',
    accent: '#f1ad3d',
    x: 430,
    y: 270,
    icon: 'N/S',
  },
  {
    id: 'primordial',
    name: '原始',
    machine: '原生质生物反应器',
    fluid: '原生质原浆',
    accent: '#8fca58',
    x: 1490,
    y: 270,
    icon: 'DNA',
  },
  {
    id: 'toxic',
    name: '毒化',
    machine: '同位素衰变炉',
    fluid: '核素浆液',
    accent: '#d6d84d',
    x: 270,
    y: 610,
    icon: 'RAD',
  },
  {
    id: 'abyssal',
    name: '渊海',
    machine: '渊压萃取塔',
    fluid: '渊压卤液',
    accent: '#55d3e2',
    x: 1650,
    y: 610,
    icon: 'PSI',
  },
  {
    id: 'forlorn',
    name: '异寂',
    machine: '暗相谐振器',
    fluid: '暗相凝液',
    accent: '#b8a9d6',
    x: 600,
    y: 890,
    icon: 'VOID',
  },
  {
    id: 'candy',
    name: '糖果',
    machine: '糖晶析出器',
    fluid: '过饱和糖液',
    accent: '#f27ca5',
    x: 1320,
    y: 890,
    icon: 'CRY',
  },
] as const;

const baseText: CSSProperties = {
  color: COLORS.text,
  fontFamily: FONT,
  letterSpacing: 0,
};

const monoText: CSSProperties = {
  color: COLORS.muted,
  fontFamily: MONO,
  letterSpacing: 1.6,
  textTransform: 'uppercase',
};

const sceneFade = (
  frame: number,
  inFrames = 24,
  outAt = 9999,
  outFrames = 24,
) => {
  return interpolate(
    frame,
    [0, inFrames, outAt, outAt + outFrames],
    [0, 1, 1, 0],
    {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
      easing: [
        Easing.bezier(0.16, 1, 0.3, 1),
        Easing.linear,
        Easing.bezier(0.7, 0, 0.84, 0),
      ],
    },
  );
};

const Tick = ({children, color = COLORS.cyan}: {children: ReactNode; color?: string}) => {
  return (
    <div style={{display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14}}>
      <div style={{width: 8, height: 8, background: color, boxShadow: `0 0 16px ${color}`}} />
      <span style={{...monoText, color, fontSize: 16}}>{children}</span>
    </div>
  );
};

const TechnicalBackdrop = ({
  accent,
  gridShift = 0,
  warm = false,
}: {
  accent: string;
  gridShift?: number;
  warm?: boolean;
}) => {
  return (
    <AbsoluteFill style={{backgroundColor: COLORS.ink, overflow: 'hidden'}}>
      <div
        style={{
          position: 'absolute',
          inset: -100,
          backgroundImage: `linear-gradient(${accent}18 1px, transparent 1px), linear-gradient(90deg, ${accent}18 1px, transparent 1px)`,
          backgroundSize: '74px 74px',
          translate: `${gridShift % 74}px ${(gridShift * 0.35) % 74}px`,
          opacity: warm ? 0.62 : 0.42,
        }}
      />
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: warm
            ? 'linear-gradient(115deg, rgba(122,49,23,0.34), transparent 38%, rgba(241,173,61,0.10) 72%, transparent)'
            : 'linear-gradient(115deg, rgba(20,96,111,0.20), transparent 42%, rgba(241,173,61,0.08) 78%, transparent)',
        }}
      />
      <div
        style={{
          position: 'absolute',
          left: -180,
          top: 120,
          width: 2200,
          height: 2,
          background: accent,
          opacity: 0.28,
          rotate: '-7deg',
        }}
      />
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 0,
          height: 210,
          background: 'linear-gradient(to top, rgba(0,0,0,0.76), transparent)',
        }}
      />
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background:
            'repeating-linear-gradient(to bottom, transparent 0, transparent 5px, rgba(255,255,255,0.018) 6px)',
          opacity: 0.7,
        }}
      />
    </AbsoluteFill>
  );
};

const GearHeartScene = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const heartbeat = Math.sin(frame / 5.3) * Math.sin(frame / 17.7);
  const strike =
    frame > 85 && frame < 196
      ? Math.sin(frame * 2.41) *
        interpolate(frame, [85, 118, 196], [0, 7, 2], {
          extrapolateLeft: 'clamp',
          extrapolateRight: 'clamp',
        })
      : 0;
  const reveal = spring({
    frame,
    fps,
    config: {damping: 19, stiffness: 72},
    durationInFrames: 70,
  });
  const titleOpacity = interpolate(frame, [112, 142, 186, 210], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const dangerPulse = interpolate(Math.sin(frame / 4), [-1, 1], [0.3, 0.72]);

  return (
    <AbsoluteFill style={{...baseText, opacity: sceneFade(frame, 18, 196, 14)}}>
      <TechnicalBackdrop accent={COLORS.red} gridShift={frame * 0.35} warm />

      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: `linear-gradient(90deg, transparent 0%, rgba(255,91,72,${dangerPulse * 0.11}) 48%, transparent 100%)`,
        }}
      />

      <div style={{position: 'absolute', left: 82, top: 68, width: 390}}>
        <div style={{...monoText, color: COLORS.red, fontSize: 15, marginBottom: 24}}>
          BOOT / FAULT REPORT
        </div>
        <Tick color={COLORS.red}>RPM&nbsp;&nbsp;032 / LIMIT</Tick>
        <Tick color={COLORS.amber}>STRESS&nbsp;&nbsp;NO LINK</Tick>
        <Tick color={COLORS.red}>OUTPUT&nbsp;&nbsp;0 / 18</Tick>
        <Tick color={COLORS.muted}>NETWORK&nbsp;&nbsp;OFFLINE</Tick>
      </div>

      <div
        style={{
          position: 'absolute',
          left: 960,
          top: 536,
          width: 590,
          height: 590,
          translate: `${strike}px ${-42 - strike * 0.3}px`,
          scale: interpolate(reveal, [0, 1], [0.62, 1]) * (1 + heartbeat * 0.014),
          rotate: `${interpolate(frame, [0, 210], [-3.5, 3.8])}deg`,
          filter: `drop-shadow(0 0 ${32 + dangerPulse * 42}px rgba(255,91,72,0.50))`,
        }}
      >
        <CanvasImage
          src={staticFile('dw/gear-heart-cutout.png')}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'contain',
            imageRendering: 'pixelated',
          }}
        />
      </div>

      <div
        style={{
          position: 'absolute',
          left: 900,
          top: 834,
          width: 120,
          height: 4,
          background: COLORS.red,
          boxShadow: `0 0 20px ${COLORS.red}`,
          scale: interpolate(frame, [86, 114], [0, 1], {
            extrapolateLeft: 'clamp',
            extrapolateRight: 'clamp',
          }),
        }}
      />

      <div style={{position: 'absolute', left: 92, bottom: 92, opacity: titleOpacity}}>
        <div style={{fontSize: 70, fontWeight: 700, lineHeight: 1.15}}>机械有心</div>
        <div style={{fontSize: 70, fontWeight: 700, lineHeight: 1.15, color: COLORS.red}}>也会停摆</div>
        <div style={{...monoText, fontSize: 18, marginTop: 22}}>
          受诅咒的齿轮之心 / SEVEN ACTIVE CURSES
        </div>
      </div>

      <div style={{position: 'absolute', right: 92, bottom: 92, textAlign: 'right'}}>
        <div style={{...monoText, color: COLORS.red, fontSize: 16}}>SYSTEM HALT</div>
        <div
          style={{
            fontSize: 126,
            fontWeight: 800,
            fontVariantNumeric: 'tabular-nums',
            lineHeight: 1,
          }}
        >
          {String(Math.min(99, Math.floor(frame / 2.2))).padStart(2, '0')}%
        </div>
      </div>

      <AbsoluteFill
        style={{
          backgroundColor: '#ffffff',
          opacity: interpolate(frame, [194, 210], [0, 1], {
            extrapolateLeft: 'clamp',
            extrapolateRight: 'clamp',
          }),
        }}
      />
    </AbsoluteFill>
  );
};

const DimensionKeyScene = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const unlock = spring({
    frame: frame - 96,
    fps,
    config: {damping: 17, stiffness: 95},
    durationInFrames: 82,
  });
  const keyScale = interpolate(frame, [0, 120], [0.62, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });
  const keyRotate = interpolate(frame, [0, 92, 206], [-14, 8, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const ringScale = interpolate(unlock, [0, 1], [0.2, 5.5]);
  const ringOpacity = interpolate(unlock, [0, 0.18, 1], [0, 0.85, 0]);
  const cardOpacity = interpolate(frame, [28, 60, 238, 272], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const unlockText = interpolate(frame, [138, 178], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill style={{...baseText, opacity: sceneFade(frame, 30, 262, 24)}}>
      <TechnicalBackdrop accent={COLORS.cyan} gridShift={frame * -0.45} />

      <div
        style={{
          position: 'absolute',
          left: 960,
          top: 535,
          width: 500,
          height: 500,
          translate: '-50% -50%',
          rotate: `${frame * 0.12}deg`,
        }}
      >
        {Array.from({length: 16}).map((_, index) => (
          <div
            key={index}
            style={{
              position: 'absolute',
              left: '50%',
              top: '50%',
              width: index % 4 === 0 ? 470 : 330,
              height: 1,
              background: index % 4 === 0 ? `${COLORS.cyan}72` : `${COLORS.line}a0`,
              transformOrigin: '0 50%',
              rotate: `${index * 22.5}deg`,
              translate: '0px -50%',
            }}
          />
        ))}
      </div>

      <div
        style={{
          position: 'absolute',
          left: 960,
          top: 508,
          width: 460,
          height: 460,
          translate: '-50% -50%',
          border: `3px solid ${COLORS.cyan}`,
          scale: ringScale,
          opacity: ringOpacity,
          boxShadow: `0 0 80px ${COLORS.cyan}`,
        }}
      />

      <div
        style={{
          position: 'absolute',
          left: 960,
          top: 520,
          width: 330,
          height: 330,
          translate: '-50% -50%',
          scale: keyScale,
          rotate: `${keyRotate}deg`,
          filter: `drop-shadow(0 0 ${26 + unlock * 42}px rgba(98,228,216,0.62))`,
        }}
      >
        <CanvasImage
          src={staticFile('dw/dimension-key.png')}
          style={{width: '100%', height: '100%', imageRendering: 'pixelated'}}
        />
      </div>

      <div
        style={{
          position: 'absolute',
          left: 112,
          top: 104,
          width: 520,
          opacity: cardOpacity,
        }}
      >
        <div style={{...monoText, color: COLORS.cyan, fontSize: 17, marginBottom: 18}}>
          ONE-TIME ACCESS ITEM
        </div>
        <div style={{fontSize: 112, fontWeight: 800, lineHeight: 1}}>维度钥匙</div>
        <div style={{...monoText, color: COLORS.text, fontSize: 24, marginTop: 14}}>
          DIMENSION KEY
        </div>
        <div style={{width: 420, height: 1, background: COLORS.line, margin: '38px 0'}} />
        <div style={{fontSize: 28, lineHeight: 1.65, color: '#c3d5d1'}}>
          消耗一次，永久解锁
          <br />
          独立的中转工业维度
        </div>
      </div>

      <div
        style={{
          position: 'absolute',
          right: 110,
          top: 122,
          width: 520,
          padding: '28px 32px',
          border: `1px solid ${COLORS.line}`,
          background: 'rgba(6,9,13,0.72)',
          opacity: unlockText,
        }}
      >
        <div style={{...monoText, color: COLORS.cyan, fontSize: 15, marginBottom: 22}}>
          ACCESS GRANTED
        </div>
        <div style={{...monoText, color: COLORS.text, fontSize: 17, lineHeight: 2}}>
          TARGET&nbsp;&nbsp;&nbsp;&nbsp;dw:transfer
          <br />
          PLOT&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3 x 3 CHUNKS
          <br />
          RETURN&nbsp;&nbsp;&nbsp;&nbsp;SOURCE POSITION
          <br />
          STATUS&nbsp;&nbsp;&nbsp;&nbsp;PERMANENT
        </div>
      </div>

      <div
        style={{
          position: 'absolute',
          left: 960,
          bottom: 84,
          translate: '-50% 0',
          ...monoText,
          color: COLORS.text,
          fontSize: 20,
          opacity: unlockText,
        }}
      >
        中转维度已解锁 / PERMANENT TRANSFER ACCESS
      </div>
    </AbsoluteFill>
  );
};

const DimensionNode = ({
  dimension,
  frame,
  index,
}: {
  dimension: (typeof DIMENSIONS)[number];
  frame: number;
  index: number;
}) => {
  const {fps} = useVideoConfig();
  const appear = spring({
    frame: frame - 40 - index * 12,
    fps,
    config: {damping: 21, stiffness: 78},
    durationInFrames: 54,
  });
  const pulse = 0.5 + Math.sin(frame / 10 + index) * 0.25;

  return (
    <div
      style={{
        position: 'absolute',
        left: dimension.x - 124,
        top: dimension.y - 74,
        width: 248,
        height: 148,
        opacity: appear,
        scale: interpolate(appear, [0, 1], [0.72, 1]),
        border: `1px solid ${dimension.accent}9c`,
        background: 'rgba(7,13,17,0.88)',
        boxShadow: `inset 0 0 0 1px rgba(255,255,255,0.035), 0 0 ${18 + pulse * 20}px ${dimension.accent}26`,
        clipPath:
          'polygon(0 0, calc(100% - 24px) 0, 100% 24px, 100% 100%, 24px 100%, 0 calc(100% - 24px))',
      }}
    >
      <div
        style={{
          position: 'absolute',
          left: 18,
          top: 16,
          ...monoText,
          color: dimension.accent,
          fontSize: 13,
        }}
      >
        {dimension.icon} / 0{index + 1}
      </div>
      <div style={{position: 'absolute', left: 18, top: 43, fontSize: 35, fontWeight: 800}}>
        {dimension.name}
      </div>
      <div style={{position: 'absolute', left: 18, bottom: 17, color: '#b9cbc7', fontSize: 16}}>
        {dimension.machine}
      </div>
      <div
        style={{
          position: 'absolute',
          right: 0,
          top: 0,
          width: 7,
          height: '100%',
          background: dimension.accent,
          opacity: 0.72,
        }}
      />
    </div>
  );
};

const FlowParticle = ({
  dimension,
  index,
  frame,
}: {
  dimension: (typeof DIMENSIONS)[number];
  index: number;
  frame: number;
}) => {
  const start = 116 + index * 18;
  const progress = (frame - start) / 92;
  if (progress < 0 || progress > 1) {
    return null;
  }

  const eased = 1 - Math.pow(1 - progress, 3);
  const x = interpolate(eased, [0, 1], [dimension.x, 960]);
  const y = interpolate(eased, [0, 1], [dimension.y, 568]);
  const opacity = interpolate(progress, [0, 0.14, 0.82, 1], [0, 1, 1, 0]);

  return (
    <div
      style={{
        position: 'absolute',
        left: x - 7,
        top: y - 7,
        width: 14,
        height: 14,
        rotate: '45deg',
        background: dimension.accent,
        boxShadow: `0 0 22px ${dimension.accent}`,
        opacity,
      }}
    />
  );
};

const CentralHub = ({frame}: {frame: number}) => {
  const {fps} = useVideoConfig();
  const appear = spring({
    frame: frame - 92,
    fps,
    config: {damping: 17, stiffness: 85},
    durationInFrames: 68,
  });
  const pulse = 1 + Math.sin(frame / 7.4) * 0.018;
  const ticks = Math.min(6, Math.max(0, Math.floor((frame - 126) / 23)));

  return (
    <div
      style={{
        position: 'absolute',
        left: 960,
        top: 568,
        width: 320,
        height: 320,
        translate: '-50% -50%',
        opacity: appear,
        scale: appear * pulse,
      }}
    >
      <div
        style={{
          position: 'absolute',
          inset: 0,
          border: `3px solid ${COLORS.cyan}`,
          rotate: '45deg',
          background: 'rgba(9,19,24,0.94)',
          boxShadow: `0 0 60px rgba(98,228,216,0.24), inset 0 0 40px rgba(98,228,216,0.08)`,
        }}
      />
      <div
        style={{
          position: 'absolute',
          inset: 28,
          border: `2px solid ${COLORS.amber}cb`,
          rotate: '45deg',
          background: 'rgba(6,9,13,0.94)',
        }}
      />
      <div
        style={{
          position: 'absolute',
          left: 38,
          top: 82,
          width: 244,
          height: 156,
          padding: '17px 18px 0',
          border: `1px solid ${COLORS.line}`,
          background: 'rgba(5,10,14,0.94)',
          boxShadow: '0 18px 40px rgba(0,0,0,0.32)',
        }}
      >
        <div style={{...monoText, color: COLORS.cyan, fontSize: 12}}>TRANSFER CORE</div>
        <div style={{fontSize: 31, fontWeight: 800, marginTop: 8}}>dw:transfer</div>
        <div style={{...monoText, color: COLORS.text, fontSize: 14, marginTop: 13}}>
          MATRICES&nbsp;&nbsp;{ticks}/6
        </div>
        <div style={{...monoText, color: COLORS.amber, fontSize: 14, marginTop: 7}}>
          STABLE&nbsp;&nbsp;LINK
        </div>
        <div style={{height: 4, background: COLORS.line, marginTop: 13}}>
          <div
            style={{
              width: `${(ticks / 6) * 100}%`,
              height: '100%',
              background: COLORS.cyan,
              boxShadow: `0 0 14px ${COLORS.cyan}`,
            }}
          />
        </div>
      </div>
    </div>
  );
};

const NetworkScene = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const lineProgress = interpolate(frame, [52, 132], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });
  const titleOpacity = interpolate(frame, [10, 42, 294, 326], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const energyEnter = spring({
    frame: frame - 182,
    fps,
    config: {damping: 20, stiffness: 68},
    durationInFrames: 60,
  });
  const finalLine = interpolate(frame, [244, 280], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill style={{...baseText, opacity: sceneFade(frame, 28, 300, 30)}}>
      <TechnicalBackdrop accent={COLORS.cyan} gridShift={frame * 0.28} />

      <svg
        width="1920"
        height="1080"
        viewBox="0 0 1920 1080"
        style={{position: 'absolute', inset: 0}}
      >
        {DIMENSIONS.map((dimension, index) => {
          const individual = interpolate(
            lineProgress,
            [index * 0.08, Math.min(1, 0.44 + index * 0.08)],
            [0, 1],
            {
              extrapolateLeft: 'clamp',
              extrapolateRight: 'clamp',
            },
          );
          return (
            <line
              key={dimension.id}
              x1={dimension.x}
              y1={dimension.y}
              x2={960}
              y2={568}
              stroke={dimension.accent}
              strokeWidth={2}
              strokeDasharray="14 14"
              strokeDashoffset={(1 - individual) * 520}
              opacity={0.52 * individual}
            />
          );
        })}
        <circle
          cx="960"
          cy="568"
          r="205"
          fill="none"
          stroke={COLORS.cyan}
          strokeWidth="2"
          strokeDasharray="5 15"
          opacity={lineProgress * 0.6}
        />
        <circle
          cx="960"
          cy="568"
          r="246"
          fill="none"
          stroke={COLORS.amber}
          strokeWidth="1"
          strokeDasharray="2 26"
          opacity={lineProgress * 0.7}
        />
      </svg>

      <div style={{position: 'absolute', left: 92, top: 64, opacity: titleOpacity}}>
        <div
          style={{
            ...monoText,
            color: COLORS.cyan,
            fontSize: 17,
            marginBottom: 16,
          }}
        >
          CROSS-DIMENSION PRODUCTION NETWORK
        </div>
        <div style={{fontSize: 58, fontWeight: 800}}>六座洞穴工厂，汇入同一个工业节点</div>
      </div>

      {DIMENSIONS.map((dimension, index) => (
        <DimensionNode
          key={dimension.id}
          dimension={dimension}
          frame={frame}
          index={index}
        />
      ))}

      {DIMENSIONS.map((dimension, index) => (
        <FlowParticle
          key={`${dimension.id}-particle`}
          dimension={dimension}
          index={index}
          frame={frame}
        />
      ))}

      <CentralHub frame={frame} />

      <div
        style={{
          position: 'absolute',
          left: 750,
          bottom: 48,
          width: 420,
          opacity: energyEnter,
          borderTop: `1px solid ${COLORS.line}`,
          paddingTop: 28,
        }}
      >
        <div
          style={{
            ...monoText,
            color: COLORS.cyan,
            fontSize: 15,
            marginBottom: 20,
          }}
        >
          ME MEMORY POWER
        </div>
        <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 22}}>
          <div>
            <div style={{...monoText, color: COLORS.muted, fontSize: 14}}>DDR1</div>
            <div style={{fontSize: 25, fontWeight: 750, marginTop: 7}}>65,536 SU</div>
            <div style={{fontSize: 18, color: COLORS.muted, marginTop: 5}}>16,384 RPM</div>
          </div>
          <div>
            <div style={{...monoText, color: COLORS.amber, fontSize: 14}}>DDR5</div>
            <div style={{fontSize: 25, fontWeight: 750, marginTop: 7}}>2,097,152 SU</div>
            <div style={{fontSize: 18, color: COLORS.muted, marginTop: 5}}>262,144 RPM</div>
          </div>
        </div>
        <div style={{...monoText, color: COLORS.text, fontSize: 16, marginTop: 25}}>
          STRESS CONSERVATION / ONLINE
        </div>
      </div>

      <div
        style={{
          position: 'absolute',
          left: 94,
          bottom: 78,
          display: 'flex',
          gap: 16,
          opacity: finalLine,
        }}
      >
        {['资源采样', '效果流体', '稳定基质', '跨维封装'].map((label, index) => (
          <div
            key={label}
            style={{
              padding: '14px 18px',
              border: `1px solid ${index === 3 ? COLORS.amber : COLORS.line}`,
              color: index === 3 ? COLORS.amber : COLORS.text,
              background: 'rgba(7,13,17,0.84)',
              fontSize: 18,
            }}
          >
            {label}
          </div>
        ))}
      </div>
    </AbsoluteFill>
  );
};

const IsometricFactory = ({frame}: {frame: number}) => {
  const {fps} = useVideoConfig();
  const appear = spring({
    frame: frame - 42,
    fps,
    config: {damping: 18, stiffness: 72},
    durationInFrames: 74,
  });
  const glow = 0.7 + Math.sin(frame / 9) * 0.2;

  const block = (x: number, y: number, width: number, height: number, accent: string) => {
    return (
      <div
        key={`${x}-${y}`}
        style={{
          position: 'absolute',
          left: x,
          top: y,
          width,
          height,
          background: '#101b20',
          border: `2px solid ${accent}`,
          boxShadow: `inset 0 0 0 6px rgba(255,255,255,0.018), 0 0 ${14 * glow}px ${accent}22`,
        }}
      >
        <div style={{position: 'absolute', left: 10, top: 10, width: 8, height: 8, background: accent}} />
        <div style={{position: 'absolute', right: 10, bottom: 10, width: 26, height: 3, background: `${accent}88`}} />
      </div>
    );
  };

  return (
    <div
      style={{
        position: 'absolute',
        left: 1240,
        top: 518,
        width: 590,
        height: 430,
        translate: '-50% -50%',
        scale: interpolate(appear, [0, 1], [0.72, 1]),
        opacity: appear,
        rotate: `${interpolate(frame, [40, 180], [-5, 1.5], {
          extrapolateLeft: 'clamp',
          extrapolateRight: 'clamp',
        })}deg`,
      }}
    >
      <svg
        width="590"
        height="430"
        viewBox="0 0 590 430"
        style={{position: 'absolute', inset: 0}}
      >
        <polygon
          points="295,18 548,164 295,310 42,164"
          fill="rgba(98,228,216,0.035)"
          stroke="#2d5c62"
          strokeWidth="2"
        />
        <polygon
          points="295,72 472,174 295,276 118,174"
          fill="rgba(241,173,61,0.035)"
          stroke="#624f2e"
          strokeWidth="1"
        />
        {DIMENSIONS.map((dimension, index) => {
          const angle = (Math.PI * 2 * index) / 6 - Math.PI / 2;
          const x = 295 + Math.cos(angle) * 210;
          const y = 174 + Math.sin(angle) * 132;
          return (
            <g key={dimension.id}>
              <line
                x1="295"
                y1="174"
                x2={x}
                y2={y}
                stroke={dimension.accent}
                strokeWidth="2"
                opacity="0.48"
                strokeDasharray="5 8"
              />
              <circle cx={x} cy={y} r="7" fill={dimension.accent} />
            </g>
          );
        })}
      </svg>
      {block(242, 150, 110, 76, COLORS.cyan)}
      {block(138, 250, 96, 64, COLORS.amber)}
      {block(354, 248, 96, 64, '#8fca58')}
      {block(8, 204, 76, 52, '#d6d84d')}
      {block(506, 204, 76, 52, '#f27ca5')}
    </div>
  );
};

const TitleScene = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const titleIn = spring({
    frame: frame - 72,
    fps,
    config: {damping: 18, stiffness: 68},
    durationInFrames: 76,
  });
  const titleExit = interpolate(frame, [282, 330], [1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const underline = interpolate(frame, [110, 176], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });
  const footer = interpolate(frame, [172, 212], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const tiny = interpolate(frame, [226, 260], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill style={{...baseText, opacity: sceneFade(frame, 22, 318, 12) * titleExit}}>
      <TechnicalBackdrop accent={COLORS.amber} gridShift={frame * -0.18} warm />
      <IsometricFactory frame={frame} />

      <div
        style={{
          position: 'absolute',
          left: 96,
          top: 80,
          ...monoText,
          color: COLORS.amber,
          fontSize: 16,
        }}
      >
        DIMENSIONWORKS / INDUSTRIAL NETWORK ONLINE
      </div>

      <div
        style={{
          position: 'absolute',
          left: 100,
          right: 100,
          top: 320,
          opacity: titleIn,
          scale: interpolate(titleIn, [0, 1], [0.84, 1]),
          translate: `${(1 - titleIn) * 70}px 0px`,
        }}
      >
        <div
          style={{
            fontSize: 176,
            fontWeight: 900,
            lineHeight: 0.96,
            textShadow: '0 12px 50px rgba(0,0,0,0.62)',
          }}
        >
          维度工序
        </div>
        <div style={{display: 'flex', alignItems: 'center', gap: 24, marginTop: 22}}>
          <div
            style={{
              width: 190,
              height: 5,
              background: COLORS.amber,
              boxShadow: `0 0 20px ${COLORS.amber}`,
              scale: `${underline} 1`,
            }}
          />
          <div style={{...monoText, color: COLORS.text, fontSize: 29}}>DIMENSIONWORKS</div>
        </div>
      </div>

      <div
        style={{
          position: 'absolute',
          left: 104,
          top: 608,
          fontSize: 40,
          fontWeight: 650,
          color: '#cad9d5',
          opacity: footer,
        }}
      >
        六座洞穴工厂，一张跨维度工业网络
      </div>

      <div
        style={{
          position: 'absolute',
          left: 104,
          bottom: 80,
          display: 'flex',
          gap: 18,
          opacity: tiny,
        }}
      >
        {['CREATE 6', 'MEKANISM', 'APPLIED ENERGISTICS 2', "ALEX'S CAVES"].map((item) => (
          <div
            key={item}
            style={{
              ...monoText,
              color: COLORS.muted,
              fontSize: 14,
              borderLeft: `2px solid ${COLORS.line}`,
              paddingLeft: 14,
            }}
          >
            {item}
          </div>
        ))}
      </div>

      <div style={{position: 'absolute', right: 106, bottom: 78, textAlign: 'right', opacity: tiny}}>
        <div style={{...monoText, color: COLORS.cyan, fontSize: 15}}>
          MINECRAFT 1.20.1 / FORGE
        </div>
        <div style={{...monoText, color: COLORS.muted, fontSize: 14, marginTop: 10}}>
          FROM CAVE SAMPLE TO FACTORY NETWORK
        </div>
      </div>
    </AbsoluteFill>
  );
};

const GlobalChrome = () => {
  const frame = useCurrentFrame();
  const progress = frame / 1029;
  const chapter =
    frame < 200
      ? '01 / FAULT'
      : frame < 430
        ? '02 / KEY'
        : frame < 720
          ? '03 / NETWORK'
          : '04 / DIMENSIONWORKS';

  return (
    <AbsoluteFill style={{pointerEvents: 'none'}}>
      <div
        style={{
          position: 'absolute',
          top: 32,
          left: 38,
          ...monoText,
          color: COLORS.muted,
          fontSize: 12,
        }}
      >
        DW // OPENING SEQUENCE
      </div>
      <div
        style={{
          position: 'absolute',
          top: 32,
          right: 38,
          ...monoText,
          color: COLORS.muted,
          fontSize: 12,
        }}
      >
        {chapter}
      </div>
      <div
        style={{
          position: 'absolute',
          left: 38,
          right: 38,
          bottom: 26,
          height: 2,
          background: '#1c3135',
        }}
      >
        <div
          style={{
            width: `${progress * 100}%`,
            height: '100%',
            background: COLORS.cyan,
            boxShadow: `0 0 12px ${COLORS.cyan}`,
          }}
        />
      </div>
      <div
        style={{
          position: 'absolute',
          right: 38,
          bottom: 34,
          ...monoText,
          color: COLORS.muted,
          fontSize: 11,
        }}
      >
        {String(Math.floor(frame / 30)).padStart(2, '0')}:
        {String(frame % 30).padStart(2, '0')}
      </div>
    </AbsoluteFill>
  );
};

export const DimensionWorksOpening = () => {
  return (
    <AbsoluteFill style={{backgroundColor: COLORS.ink, ...baseText}}>
      <Sequence durationInFrames={210}>
        <GearHeartScene />
      </Sequence>
      <Sequence from={180} durationInFrames={294}>
        <DimensionKeyScene />
      </Sequence>
      <Sequence from={420} durationInFrames={330}>
        <NetworkScene />
      </Sequence>
      <Sequence from={700} durationInFrames={330}>
        <TitleScene />
      </Sequence>
      <GlobalChrome />
      <Audio src={staticFile('dw/opening-score.wav')} volume={0.68} />
    </AbsoluteFill>
  );
};
