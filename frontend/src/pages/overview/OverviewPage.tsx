import { AppHeader } from '@/components/AppHeader.tsx'
import { Button } from '@/components/ui/button'
import { Link } from 'react-router-dom'

export function OverviewPage() {
  return (
    <div className="flex min-h-svh flex-col">
      <AppHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-6 pb-24">
        <div className="flex items-center justify-between">
          <h1 className="font-stretch-expanded text-2xl font-black uppercase">Overview</h1>
          <Button asChild>
            <Link to="/analysis/new">New analysis</Link>
          </Button>
        </div>
        <p className="mt-3 text-sm text-muted-foreground">
          Your lifts will show up here once you upload your first video.
        </p>
      </main>
    </div>
  )
}
