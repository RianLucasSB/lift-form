import { apiRequest } from '@/lib/api/httpClient'
import type { CreateAnalysisPayload, CreateAnalysisResponse, GetAnalysisResponse } from '../types'

export const analysisApi = {
  create(payload: CreateAnalysisPayload) {
    return apiRequest<CreateAnalysisResponse>('/analysis/create', {
      method: 'POST',
      body: payload,
      authenticated: true,
    })
  },
  confirmUpload(analysisId: number) {
    return apiRequest<void>(`/analysis/videos/${analysisId}/upload-complete`, {
      method: 'POST',
      authenticated: true,
    })
  },
  get(analysisId: number) {
    return apiRequest<GetAnalysisResponse>(`/analysis/${analysisId}`, {
      authenticated: true,
    })
  },
}
