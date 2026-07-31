export type ExerciseType = 'SQUAT'

export interface CreateAnalysisPayload {
  exerciseType: ExerciseType
  fileName: string
}

export interface CreateAnalysisResponse {
  analysisId: number
  uploadUrl: string
  expiresIn: number
}

export type VideoAnalysisStatus = 'CREATED' | 'UPLOADED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'EXPIRED'

export type Classification = 'GOOD' | 'FAIR' | 'POOR'

export type FeedbackRating = 'good' | 'fair' | 'poor'

export interface FeedbackDimension {
  rating: FeedbackRating
  detail: string
}

// Keys produced by score-analyzer-worker's SquatScorePredictor._generate_feedback
// (see workers/score_analyzer_worker/inference/squat_predictor.py). overall_label
// is a separate, Python-internal 5-bucket label and is deliberately not surfaced —
// classification (below) is the one source of truth for the headline result,
// see docs/adr/0003-result-shows-classification-not-overall-label.md.
export interface AnalysisFeedback {
  overall_label?: string
  depth?: FeedbackDimension
  back?: FeedbackDimension
  tempo?: FeedbackDimension
  lockout?: FeedbackDimension
  rom?: FeedbackDimension
  consistency?: FeedbackDimension
}

export interface AnalysisResultData {
  modelVersion: string
  overallScore: number
  classification: Classification
  feedback: AnalysisFeedback
  scoredAt: string
}

export interface GetAnalysisResponse {
  status: VideoAnalysisStatus
  result: AnalysisResultData | null
  errors: string[]
}
