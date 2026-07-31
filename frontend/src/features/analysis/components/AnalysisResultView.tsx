import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { AnalysisFeedback, AnalysisResultData, Classification, FeedbackRating } from '../types'

const CLASSIFICATION_LABEL: Record<Classification, string> = {
  GOOD: 'Good',
  FAIR: 'Fair',
  POOR: 'Needs work',
}

const CLASSIFICATION_CLASSNAME: Record<Classification, string> = {
  GOOD: 'bg-pass-green/10 text-pass-green',
  FAIR: 'bg-secondary text-secondary-foreground',
  POOR: '',
}

const RATING_LABEL: Record<FeedbackRating, string> = {
  good: 'Good',
  fair: 'Fair',
  poor: 'Needs work',
}

const RATING_CLASSNAME: Record<FeedbackRating, string> = {
  good: 'bg-pass-green/10 text-pass-green',
  fair: 'bg-secondary text-secondary-foreground',
  poor: '',
}

// score-analyzer-worker's fixed feedback dimensions, in display order
// (see workers/score_analyzer_worker/inference/squat_predictor.py).
const FEEDBACK_DIMENSIONS: { key: keyof AnalysisFeedback; label: string }[] = [
  { key: 'depth', label: 'Depth' },
  { key: 'back', label: 'Back position' },
  { key: 'tempo', label: 'Tempo' },
  { key: 'lockout', label: 'Lockout' },
  { key: 'rom', label: 'Range of motion' },
  { key: 'consistency', label: 'Consistency' },
]

export function AnalysisResultView({ result }: { result: AnalysisResultData }) {
  return (
    <div className="mt-6 space-y-4">
      <Card>
        <CardHeader className="flex-row items-center justify-between">
          <CardTitle>Overall score</CardTitle>
          <Badge
            variant={result.classification === 'POOR' ? 'destructive' : undefined}
            className={CLASSIFICATION_CLASSNAME[result.classification]}
          >
            {CLASSIFICATION_LABEL[result.classification]}
          </Badge>
        </CardHeader>
        <CardContent>
          <p className="font-stretch-expanded text-3xl font-black">{Math.round(result.overallScore * 100)}%</p>
        </CardContent>
      </Card>

      {FEEDBACK_DIMENSIONS.map(({ key, label }) => {
        const dimension = result.feedback[key]
        if (!dimension || typeof dimension !== 'object') return null

        return (
          <Card key={key}>
            <CardHeader className="flex-row items-center justify-between">
              <CardTitle>{label}</CardTitle>
              <Badge
                variant={dimension.rating === 'poor' ? 'destructive' : undefined}
                className={RATING_CLASSNAME[dimension.rating]}
              >
                {RATING_LABEL[dimension.rating]}
              </Badge>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">{dimension.detail}</p>
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}
