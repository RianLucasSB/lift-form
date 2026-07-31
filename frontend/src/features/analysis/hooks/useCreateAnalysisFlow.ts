import { useRef, useState } from 'react'
import { ApiError } from '@/lib/api/httpClient'
import { analysisApi } from '../api/analysisApi'
import { uploadToS3 } from '../api/uploadToS3'

export type CreateAnalysisStage = 'idle' | 'creating' | 'uploading' | 'confirming'

interface UseCreateAnalysisFlowResult {
  stage: CreateAnalysisStage
  uploadProgress: number
  error: string | null
  start: (file: File) => Promise<void>
  retry: () => Promise<void>
}

/**
 * Drives the create -> upload-to-S3 -> confirm sequence for a new analysis.
 * Retry behavior differs by which step failed: a 413/415 from confirm-upload
 * means the file itself is invalid, so retry restarts the whole flow (a new
 * analysisId); any other failure past the create step retries just that step
 * against the same analysisId, since the file is already sitting in S3.
 */
export function useCreateAnalysisFlow(onSuccess: (analysisId: number) => void): UseCreateAnalysisFlowResult {
  const [stage, setStage] = useState<CreateAnalysisStage>('idle')
  const [uploadProgress, setUploadProgress] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const fileRef = useRef<File | null>(null)
  const analysisIdRef = useRef<number | null>(null)
  const retryConfirmOnlyRef = useRef(false)

  async function confirm(analysisId: number) {
    setStage('confirming')
    try {
      await analysisApi.confirmUpload(analysisId)
      onSuccess(analysisId)
    } catch (err) {
      if (err instanceof ApiError && (err.status === 413 || err.status === 415)) {
        retryConfirmOnlyRef.current = false
        setError('File exceeds the upload limits. Please start over with a different file.')
      } else {
        retryConfirmOnlyRef.current = true
        setError("Couldn't confirm the upload. Please try again.")
      }
    }
  }

  async function upload(analysisId: number, uploadUrl: string, file: File) {
    setStage('uploading')
    setUploadProgress(0)
    try {
      await uploadToS3(uploadUrl, file, setUploadProgress)
    } catch {
      retryConfirmOnlyRef.current = false
      setError('Upload failed. Please try again.')
      return
    }
    await confirm(analysisId)
  }

  async function create(file: File) {
    setStage('creating')
    try {
      const { analysisId, uploadUrl } = await analysisApi.create({ exerciseType: 'SQUAT', fileName: file.name })
      analysisIdRef.current = analysisId
      await upload(analysisId, uploadUrl, file)
    } catch {
      retryConfirmOnlyRef.current = false
      setError("Couldn't start the analysis. Please try again.")
    }
  }

  async function start(file: File) {
    setError(null)
    fileRef.current = file
    retryConfirmOnlyRef.current = false
    await create(file)
  }

  async function retry() {
    setError(null)
    if (retryConfirmOnlyRef.current && analysisIdRef.current !== null) {
      await confirm(analysisIdRef.current)
      return
    }
    if (fileRef.current) {
      await create(fileRef.current)
    }
  }

  return { stage, uploadProgress, error, start, retry }
}
