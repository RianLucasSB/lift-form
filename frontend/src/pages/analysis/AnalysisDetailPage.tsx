import { Loader2 } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Wordmark } from '@/components/Wordmark.tsx'
import { AnalysisResultView } from '@/features/analysis/components/AnalysisResultView'
import { useAnalysisStatusPolling } from '@/features/analysis/hooks/useAnalysisStatusPolling'

export function AnalysisDetailPage() {
  const { id } = useParams<{ id: string }>()
  const analysisId = Number(id)
  const { status, result, checkNow } = useAnalysisStatusPolling(analysisId)

  return (
    <div className="flex min-h-svh flex-col">
      <header className="mx-auto flex w-full max-w-6xl items-center px-6 py-5">
        <Wordmark />
      </header>
      <main className="mx-auto w-full max-w-2xl flex-1 px-6 pb-24">
        <h1 className="font-stretch-expanded text-2xl font-black uppercase">Analysis</h1>

        {status === 'processing' && (
          <Card className="mt-6">
            <CardContent className="flex flex-col items-center gap-4 py-6 text-center">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
              <p className="text-sm text-muted-foreground">Processing your video…</p>
            </CardContent>
          </Card>
        )}

        {status === 'completed' && result && <AnalysisResultView result={result} />}

        {status === 'failed' && (
          <div className="mt-6 space-y-4">
            <Alert variant="destructive">
              <AlertDescription>We couldn't analyze this video.</AlertDescription>
            </Alert>
            <Button asChild className="w-full">
              <Link to="/analysis/new">Try again</Link>
            </Button>
          </div>
        )}

        {status === 'timedOut' && (
          <div className="mt-6 space-y-4">
            <Alert>
              <AlertDescription>This is taking longer than expected.</AlertDescription>
            </Alert>
            <Button onClick={checkNow} className="w-full">
              Check again
            </Button>
          </div>
        )}
      </main>
    </div>
  )
}
