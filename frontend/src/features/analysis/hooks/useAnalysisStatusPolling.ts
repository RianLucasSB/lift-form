import { useEffect, useRef, useState } from 'react'
import { analysisApi } from '../api/analysisApi'
import type { AnalysisResultData } from '../types'

const POLL_INTERVAL_MS = 3000
const MAX_POLL_ATTEMPTS = 100 // 3s * 100 = 5 minutes

export type PollingStatus = 'processing' | 'completed' | 'failed' | 'timedOut'

interface UseAnalysisStatusPollingResult {
  status: PollingStatus
  result: AnalysisResultData | null
  checkNow: () => void
}

/**
 * Polls GET /analysis/{id} every 3s, collapsing the backend's
 * CREATED/UPLOADED/PROCESSING statuses into one "processing" status (the
 * distinction isn't meaningful to someone watching a spinner) until COMPLETED
 * or FAILED. Gives up after 5 minutes with a "timedOut" status rather than
 * polling forever; checkNow restarts the loop (used by a "check again" action).
 */
export function useAnalysisStatusPolling(analysisId: number): UseAnalysisStatusPollingResult {
  const [status, setStatus] = useState<PollingStatus>('processing')
  const [result, setResult] = useState<AnalysisResultData | null>(null)
  const [generation, setGeneration] = useState(0)
  const attemptsRef = useRef(0)

  useEffect(() => {
    let cancelled = false
    let intervalId: ReturnType<typeof setInterval> | undefined

    attemptsRef.current = 0
    setStatus('processing')
    setResult(null)

    async function tick() {
      if (cancelled) return
      const response = await analysisApi.get(analysisId)
      if (cancelled) return

      if (response.status === 'COMPLETED') {
        setResult(response.result)
        setStatus('completed')
        clearInterval(intervalId)
        return
      }
      if (response.status === 'FAILED') {
        setStatus('failed')
        clearInterval(intervalId)
        return
      }

      attemptsRef.current += 1
      if (attemptsRef.current >= MAX_POLL_ATTEMPTS) {
        setStatus('timedOut')
        clearInterval(intervalId)
      }
    }

    void tick()
    intervalId = setInterval(() => void tick(), POLL_INTERVAL_MS)

    return () => {
      cancelled = true
      clearInterval(intervalId)
    }
  }, [analysisId, generation])

  return {
    status,
    result,
    checkNow: () => setGeneration((g) => g + 1),
  }
}
