// The hero graphic: the knee-angle signal Liftform actually extracts from a
// squat video, rendered for a deterministic three-rep set. Rep bottoms and the
// hysteresis entry threshold mirror the real pipeline's rep detection.

const STANDING = 172
const DURATION = 9.4
const ENTRY_THRESHOLD = 146 // ~85% of standing baseline, as in detect_reps_state_machine
const PARALLEL = 90

interface Rep {
  start: number
  bottom: number
  eccentric: number
  pause: number
  concentric: number
}

const REPS: Rep[] = [
  { start: 0.9, bottom: 87, eccentric: 1.3, pause: 0.2, concentric: 1.0 },
  { start: 3.8, bottom: 84, eccentric: 1.4, pause: 0.25, concentric: 1.05 },
  { start: 6.8, bottom: 86, eccentric: 1.25, pause: 0.2, concentric: 0.95 },
]

function angleAt(t: number): number {
  for (const rep of REPS) {
    const downEnd = rep.start + rep.eccentric
    const pauseEnd = downEnd + rep.pause
    const upEnd = pauseEnd + rep.concentric
    if (t >= rep.start && t < downEnd) {
      const p = (t - rep.start) / rep.eccentric
      return rep.bottom + (STANDING - rep.bottom) * (0.5 + 0.5 * Math.cos(Math.PI * p))
    }
    if (t >= downEnd && t < pauseEnd) {
      return rep.bottom + 0.6 * Math.sin((t - downEnd) * 9)
    }
    if (t >= pauseEnd && t < upEnd) {
      const p = (t - pauseEnd) / rep.concentric
      return rep.bottom + (STANDING - rep.bottom) * (0.5 - 0.5 * Math.cos(Math.PI * p))
    }
  }
  // standing between reps, with a little sensor wobble
  return STANDING + 0.9 * Math.sin(t * 2.1) + 0.5 * Math.sin(t * 4.7)
}

const X0 = 46
const X1 = 628
const Y0 = 18
const Y1 = 306
const A_MIN = 78
const A_MAX = 184

const x = (t: number) => X0 + (t / DURATION) * (X1 - X0)
const y = (a: number) => Y0 + ((A_MAX - a) / (A_MAX - A_MIN)) * (Y1 - Y0)

const tracePath = (() => {
  const parts: string[] = []
  for (let t = 0; t <= DURATION; t += 0.04) {
    parts.push(`${parts.length === 0 ? 'M' : 'L'}${x(t).toFixed(1)} ${y(angleAt(t)).toFixed(1)}`)
  }
  return parts.join(' ')
})()

const bottoms = REPS.map((rep) => ({
  x: x(rep.start + rep.eccentric + rep.pause / 2),
  y: y(rep.bottom),
  angle: rep.bottom,
}))

const gridAngles = [180, 135, 90]

export function KneeAngleTrace() {
  return (
    <figure className="rounded-lg border border-border bg-card shadow-sm">
      <figcaption className="flex items-baseline justify-between gap-4 border-b border-border px-4 py-2.5 font-mono text-[11px] tracking-wide text-muted-foreground">
        <span className="truncate">ANALYSIS · SQUAT_SET_014.MP4</span>
        <span className="shrink-0 text-pass-green">3 REPS DETECTED</span>
      </figcaption>
      <svg
        viewBox="0 0 640 340"
        role="img"
        aria-label="Knee angle across a three-rep squat set, with rep bottoms at 87, 84 and 86 degrees"
        className="block w-full"
      >
        {/* faint angle gridlines */}
        {gridAngles.map((a) => (
          <g key={a}>
            <line x1={X0} x2={X1} y1={y(a)} y2={y(a)} stroke="var(--border)" strokeWidth="1" />
            <text
              x={X0 - 8}
              y={y(a) + 3.5}
              textAnchor="end"
              className="fill-steel font-mono"
              fontSize="10"
            >
              {a}°
            </text>
          </g>
        ))}

        {/* hysteresis entry threshold — a rep starts below this line */}
        <line
          x1={X0}
          x2={X1}
          y1={y(ENTRY_THRESHOLD)}
          y2={y(ENTRY_THRESHOLD)}
          stroke="var(--plate-blue)"
          strokeWidth="1"
          strokeDasharray="5 4"
        />
        <text
          x={X0 + 6}
          y={y(ENTRY_THRESHOLD) - 6}
          className="fill-plate-blue font-mono"
          fontSize="10"
          stroke="var(--card)"
          strokeWidth="3.5"
          paintOrder="stroke"
        >
          rep threshold · {ENTRY_THRESHOLD}°
        </text>

        {/* parallel reference */}
        <line
          x1={X0}
          x2={X1}
          y1={y(PARALLEL)}
          y2={y(PARALLEL)}
          stroke="var(--steel)"
          strokeWidth="1"
          strokeDasharray="2 4"
        />
        <text
          x={X0 + 6}
          y={y(PARALLEL) - 6}
          className="fill-steel font-mono"
          fontSize="10"
          stroke="var(--card)"
          strokeWidth="3.5"
          paintOrder="stroke"
        >
          parallel · {PARALLEL}°
        </text>

        {/* the knee-angle signal */}
        <path
          d={tracePath}
          fill="none"
          stroke="var(--foreground)"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          pathLength={1}
          className="trace-path"
        />

        {/* detected rep bottoms */}
        {bottoms.map((b, i) => (
          <g key={b.x} className="trace-marker" style={{ animationDelay: `${0.7 + i * 0.75}s` }}>
            <circle cx={b.x} cy={b.y} r="4" fill="var(--pass-green)" />
            <text
              x={b.x}
              y={b.y + 22}
              textAnchor="middle"
              className="fill-pass-green font-mono"
              fontSize="12"
              fontWeight="500"
              stroke="var(--card)"
              strokeWidth="3.5"
              paintOrder="stroke"
            >
              {b.angle}°
            </text>
          </g>
        ))}
      </svg>
      <div className="flex items-baseline justify-between border-t border-border px-4 py-2.5 font-mono text-[11px] tracking-wide text-muted-foreground">
        <span>knee angle · left hip–knee–ankle</span>
        <span>9.4 s</span>
      </div>
    </figure>
  )
}
