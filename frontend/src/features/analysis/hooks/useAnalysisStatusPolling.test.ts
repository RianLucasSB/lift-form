import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { analysisApi } from '../api/analysisApi'
import { useAnalysisStatusPolling } from './useAnalysisStatusPolling'

vi.mock('../api/analysisApi')

beforeEach(() => {
  vi.resetAllMocks()
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('useAnalysisStatusPolling', () => {
  it('starts in the processing status and polls immediately', async () => {
    vi.mocked(analysisApi.get).mockResolvedValue({ status: 'CREATED', result: null, errors: [] })

    const { result } = renderHook(() => useAnalysisStatusPolling(1))
    await act(async () => {
      await Promise.resolve()
    })

    expect(analysisApi.get).toHaveBeenCalledWith(1)
    expect(result.current.status).toBe('processing')
  })

  it('treats CREATED, UPLOADED, and PROCESSING as one collapsed processing status', async () => {
    vi.mocked(analysisApi.get).mockResolvedValue({ status: 'UPLOADED', result: null, errors: [] })
    const { result } = renderHook(() => useAnalysisStatusPolling(1))
    await act(async () => {
      await Promise.resolve()
    })

    expect(result.current.status).toBe('processing')
  })

  it('polls every 3 seconds until the analysis completes', async () => {
    vi.mocked(analysisApi.get)
      .mockResolvedValueOnce({ status: 'PROCESSING', result: null, errors: [] })
      .mockResolvedValueOnce({ status: 'PROCESSING', result: null, errors: [] })
      .mockResolvedValueOnce({
        status: 'COMPLETED',
        result: {
          modelVersion: 'v1',
          overallScore: 0.82,
          classification: 'GOOD',
          feedback: { depth: { rating: 'good', detail: 'Good depth' } },
          scoredAt: '2026-07-27T00:00:00Z',
        },
        errors: [],
      })

    const { result } = renderHook(() => useAnalysisStatusPolling(1))
    await act(async () => {
      await Promise.resolve()
    })
    expect(analysisApi.get).toHaveBeenCalledTimes(1)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000)
    })
    expect(analysisApi.get).toHaveBeenCalledTimes(2)
    expect(result.current.status).toBe('processing')

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000)
    })
    expect(analysisApi.get).toHaveBeenCalledTimes(3)
    expect(result.current.status).toBe('completed')
    expect(result.current.result?.classification).toBe('GOOD')
  })

  it('stops polling once completed', async () => {
    vi.mocked(analysisApi.get).mockResolvedValue({
      status: 'COMPLETED',
      result: {
        modelVersion: 'v1',
        overallScore: 0.82,
        classification: 'GOOD',
        feedback: {},
        scoredAt: '2026-07-27T00:00:00Z',
      },
      errors: [],
    })

    renderHook(() => useAnalysisStatusPolling(1))
    await act(async () => {
      await Promise.resolve()
    })
    expect(analysisApi.get).toHaveBeenCalledTimes(1)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(30_000)
    })
    expect(analysisApi.get).toHaveBeenCalledTimes(1)
  })

  it('reports a failed status once the analysis fails, and stops polling', async () => {
    vi.mocked(analysisApi.get).mockResolvedValue({ status: 'FAILED', result: null, errors: ['stack trace stuff'] })

    const { result } = renderHook(() => useAnalysisStatusPolling(1))
    await act(async () => {
      await Promise.resolve()
    })

    expect(result.current.status).toBe('failed')

    await act(async () => {
      await vi.advanceTimersByTimeAsync(30_000)
    })
    expect(analysisApi.get).toHaveBeenCalledTimes(1)
  })

  it('times out after 5 minutes of still not being terminal, and stops polling', async () => {
    vi.mocked(analysisApi.get).mockResolvedValue({ status: 'PROCESSING', result: null, errors: [] })

    const { result } = renderHook(() => useAnalysisStatusPolling(1))
    await act(async () => {
      await Promise.resolve()
    })

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5 * 60 * 1000)
    })

    expect(result.current.status).toBe('timedOut')
    const callsAtTimeout = vi.mocked(analysisApi.get).mock.calls.length

    await act(async () => {
      await vi.advanceTimersByTimeAsync(30_000)
    })
    expect(analysisApi.get).toHaveBeenCalledTimes(callsAtTimeout)
  })

  it('checkNow restarts polling from a timed-out state', async () => {
    vi.mocked(analysisApi.get).mockResolvedValue({ status: 'PROCESSING', result: null, errors: [] })

    const { result } = renderHook(() => useAnalysisStatusPolling(1))
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5 * 60 * 1000)
    })
    expect(result.current.status).toBe('timedOut')

    act(() => {
      result.current.checkNow()
    })
    await act(async () => {
      await Promise.resolve()
    })

    expect(result.current.status).toBe('processing')
  })
})
