import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Wordmark } from '@/components/Wordmark.tsx'
import { KneeAngleTrace } from './KneeAngleTrace.tsx'

const STEPS = [
  {
    number: '01',
    title: 'Record',
    body: 'Film your squat set from the side. A phone propped against a plate works fine.',
  },
  {
    number: '02',
    title: 'Upload',
    body: 'Your video goes straight to storage and into the analysis pipeline — pose tracking, rep detection, feature extraction.',
  },
  {
    number: '03',
    title: 'Score',
    body: 'Get a form score with per-dimension feedback for the whole set, in minutes.',
  },
]

const DIMENSIONS = [
  { label: 'Depth', body: 'how far below parallel you break' },
  { label: 'Back angle', body: 'torso lean through the rep' },
  { label: 'Tempo', body: 'eccentric and concentric timing' },
  { label: 'Lockout', body: 'full extension at the top' },
  { label: 'Range of motion', body: 'complete reps, every rep' },
  { label: 'Consistency', body: 'rep-to-rep variance across the set' },
]

export function LandingPage() {
  return (
    <div className="min-h-svh">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
        <Wordmark />
        <Link
          to="/login"
          className="rounded-sm font-mono text-sm text-muted-foreground underline-offset-4 hover:text-foreground hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
        >
          Sign in
        </Link>
      </header>

      <main>
        <section className="mx-auto grid max-w-6xl items-center gap-12 px-6 pt-14 pb-20 lg:grid-cols-[1.05fr_1fr] lg:pt-24 lg:pb-28">
          <div>
            <p className="font-mono text-xs tracking-[0.2em] text-plate-blue uppercase">
              Squat analysis · 0–1 form score
            </p>
            <h1 className="mt-5 font-stretch-expanded text-[clamp(2.4rem,5.4vw,4.3rem)] leading-[0.95] font-black tracking-tight uppercase">
              Form,
              <br />
              measured
              <br />
              in&nbsp;degrees.
            </h1>
            <p className="mt-6 max-w-md text-lg text-muted-foreground">
              Upload a set of squats. Liftform tracks your joints frame by frame, finds every rep,
              and scores your form — feedback in degrees and seconds, not vibes.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <Button asChild className="h-11 px-6 text-base">
                <Link to="/login">Analyze my squat</Link>
              </Button>
              <Link
                to="/register"
                className="rounded-sm text-sm font-medium text-foreground underline underline-offset-4 hover:text-plate-red focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
              >
                Create an account
              </Link>
            </div>
            <p className="mt-5 font-mono text-xs text-muted-foreground">
              Squats today · more lifts on the way
            </p>
          </div>
          <KneeAngleTrace />
        </section>

        <section className="border-t border-border">
          <div className="mx-auto grid max-w-6xl gap-10 px-6 py-16 md:grid-cols-3">
            {STEPS.map((step) => (
              <div key={step.number}>
                <p className="font-mono text-xs tracking-[0.2em] text-plate-red">
                  {step.number}
                  <span className="ml-3 text-foreground uppercase">{step.title}</span>
                </p>
                <p className="mt-3 max-w-xs text-sm text-muted-foreground">{step.body}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="border-t border-border">
          <div className="mx-auto max-w-6xl px-6 py-16">
            <h2 className="font-mono text-xs tracking-[0.2em] text-muted-foreground uppercase">
              Scored across six dimensions
            </h2>
            <dl className="mt-8 grid grid-cols-2 gap-x-8 gap-y-8 md:grid-cols-3 lg:grid-cols-6">
              {DIMENSIONS.map((dim) => (
                <div key={dim.label}>
                  <dt className="border-t-2 border-foreground pt-2.5 text-sm font-semibold">
                    {dim.label}
                  </dt>
                  <dd className="mt-1.5 text-xs text-muted-foreground">{dim.body}</dd>
                </div>
              ))}
            </dl>
          </div>
        </section>
      </main>

      <footer className="border-t border-border">
        <div className="mx-auto flex max-w-6xl items-baseline justify-between px-6 py-8 font-mono text-xs text-muted-foreground">
          <span>LIFTFORM — form, measured.</span>
          <span>© 2026</span>
        </div>
      </footer>
    </div>
  )
}
