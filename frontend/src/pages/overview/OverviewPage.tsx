import { Wordmark } from '@/components/Wordmark.tsx'

export function OverviewPage() {
  return (
    <div className="flex min-h-svh flex-col">
      <header className="mx-auto flex w-full max-w-6xl items-center px-6 py-5">
        <Wordmark />
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 px-6 pb-24">
        <h1 className="font-stretch-expanded text-2xl font-black uppercase">Overview</h1>
        <p className="mt-3 text-sm text-muted-foreground">
          Your lifts will show up here once you upload your first video.
        </p>
      </main>
    </div>
  )
}
