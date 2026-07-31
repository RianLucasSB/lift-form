import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api/httpClient'
import { analysisApi } from '../api/analysisApi'
import { uploadToS3 } from '../api/uploadToS3'
import { useCreateAnalysisFlow } from './useCreateAnalysisFlow'

vi.mock('../api/analysisApi')
vi.mock('../api/uploadToS3')

const file = new File(['fake video bytes'], 'squat.mp4', { type: 'video/mp4' })

beforeEach(() => {
  vi.resetAllMocks()
})

describe('useCreateAnalysisFlow', () => {
  it('starts in idle stage with no error', () => {
    const onSuccess = vi.fn()
    const { result } = renderHook(() => useCreateAnalysisFlow(onSuccess))

    expect(result.current.stage).toBe('idle')
    expect(result.current.error).toBeNull()
    expect(result.current.uploadProgress).toBe(0)
  })

  it('creates, uploads, and confirms the analysis, calling onSuccess with the analysisId', async () => {
    vi.mocked(analysisApi.create).mockResolvedValue({ analysisId: 1, uploadUrl: 'https://s3.example/upload', expiresIn: 900 })
    vi.mocked(uploadToS3).mockResolvedValue(undefined)
    vi.mocked(analysisApi.confirmUpload).mockResolvedValue(undefined)
    const onSuccess = vi.fn()

    const { result } = renderHook(() => useCreateAnalysisFlow(onSuccess))
    await act(() => result.current.start(file))

    expect(analysisApi.create).toHaveBeenCalledWith({ exerciseType: 'SQUAT', fileName: 'squat.mp4' })
    expect(uploadToS3).toHaveBeenCalledWith('https://s3.example/upload', file, expect.any(Function))
    expect(analysisApi.confirmUpload).toHaveBeenCalledWith(1)
    expect(onSuccess).toHaveBeenCalledWith(1)
    expect(result.current.error).toBeNull()
  })

  it('reports upload progress while the S3 upload is in flight', async () => {
    vi.mocked(analysisApi.create).mockResolvedValue({ analysisId: 1, uploadUrl: 'https://s3.example/upload', expiresIn: 900 })
    vi.mocked(uploadToS3).mockImplementation(async (_url, _file, onProgress) => {
      onProgress(42)
    })
    vi.mocked(analysisApi.confirmUpload).mockResolvedValue(undefined)

    const { result } = renderHook(() => useCreateAnalysisFlow(vi.fn()))
    await act(() => result.current.start(file))

    expect(result.current.uploadProgress).toBe(42)
  })

  it('shows a friendly error when create fails, and retry restarts the whole flow', async () => {
    vi.mocked(analysisApi.create).mockRejectedValueOnce(new ApiError(500, ['boom']))
    const { result } = renderHook(() => useCreateAnalysisFlow(vi.fn()))

    await act(() => result.current.start(file))
    expect(result.current.error).toBe("Couldn't start the analysis. Please try again.")

    vi.mocked(analysisApi.create).mockResolvedValueOnce({ analysisId: 2, uploadUrl: 'https://s3.example/upload', expiresIn: 900 })
    vi.mocked(uploadToS3).mockResolvedValueOnce(undefined)
    vi.mocked(analysisApi.confirmUpload).mockResolvedValueOnce(undefined)

    await act(() => result.current.retry())

    expect(analysisApi.create).toHaveBeenCalledTimes(2)
    expect(result.current.error).toBeNull()
  })

  it('shows a friendly error when the S3 upload fails, and retry restarts the whole flow', async () => {
    vi.mocked(analysisApi.create).mockResolvedValue({ analysisId: 1, uploadUrl: 'https://s3.example/upload', expiresIn: 900 })
    vi.mocked(uploadToS3).mockRejectedValueOnce(new Error('network error'))

    const { result } = renderHook(() => useCreateAnalysisFlow(vi.fn()))
    await act(() => result.current.start(file))

    expect(result.current.error).toBe('Upload failed. Please try again.')

    vi.mocked(uploadToS3).mockResolvedValueOnce(undefined)
    vi.mocked(analysisApi.confirmUpload).mockResolvedValueOnce(undefined)

    await act(() => result.current.retry())

    // a fresh analysis is created rather than reusing the failed upload's id
    expect(analysisApi.create).toHaveBeenCalledTimes(2)
    expect(result.current.error).toBeNull()
  })

  it('on a generic confirm-upload failure, retry only re-confirms the same analysisId', async () => {
    vi.mocked(analysisApi.create).mockResolvedValue({ analysisId: 7, uploadUrl: 'https://s3.example/upload', expiresIn: 900 })
    vi.mocked(uploadToS3).mockResolvedValue(undefined)
    vi.mocked(analysisApi.confirmUpload).mockRejectedValueOnce(new ApiError(500, ['boom']))

    const { result } = renderHook(() => useCreateAnalysisFlow(vi.fn()))
    await act(() => result.current.start(file))

    expect(result.current.error).toBe("Couldn't confirm the upload. Please try again.")

    vi.mocked(analysisApi.confirmUpload).mockResolvedValueOnce(undefined)
    await act(() => result.current.retry())

    expect(analysisApi.create).toHaveBeenCalledTimes(1)
    expect(uploadToS3).toHaveBeenCalledTimes(1)
    expect(analysisApi.confirmUpload).toHaveBeenCalledTimes(2)
    expect(analysisApi.confirmUpload).toHaveBeenLastCalledWith(7)
    expect(result.current.error).toBeNull()
  })

  it.each([413, 415])(
    'on a %d confirm-upload failure, retry restarts the whole flow instead of re-confirming',
    async (status) => {
      vi.mocked(analysisApi.create).mockResolvedValue({ analysisId: 7, uploadUrl: 'https://s3.example/upload', expiresIn: 900 })
      vi.mocked(uploadToS3).mockResolvedValue(undefined)
      vi.mocked(analysisApi.confirmUpload).mockRejectedValueOnce(new ApiError(status, ['too big']))

      const { result } = renderHook(() => useCreateAnalysisFlow(vi.fn()))
      await act(() => result.current.start(file))

      expect(result.current.error).toBe('File exceeds the upload limits. Please start over with a different file.')

      vi.mocked(analysisApi.confirmUpload).mockResolvedValueOnce(undefined)
      await act(() => result.current.retry())

      expect(analysisApi.create).toHaveBeenCalledTimes(2)
      expect(uploadToS3).toHaveBeenCalledTimes(2)
      expect(result.current.error).toBeNull()
    }
  )
})
