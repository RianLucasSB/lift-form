import { z } from 'zod'

export const MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024 // 500MB
export const ALLOWED_CONTENT_TYPE = 'video/mp4'

// Mirrors the server's post-upload validation (analysis.upload.max-size-bytes /
// allowed-content-type, enforced in ConfirmVideoUploadUseCase via S3 HeadObject)
// so the form rejects an oversized or wrong-type file before a presigned URL is
// ever requested, instead of failing later with a 413/415 after the upload.
export const uploadSchema = z.object({
  file: z
    .instanceof(FileList, { message: 'Select a video file' })
    .refine((files) => files.length === 1, 'Select a video file')
    .refine((files) => files[0]?.type === ALLOWED_CONTENT_TYPE, 'Please upload an MP4 video.')
    .refine((files) => (files[0]?.size ?? 0) <= MAX_FILE_SIZE_BYTES, 'File is too large (max 500MB).'),
})

export type UploadFormValues = z.infer<typeof uploadSchema>

export function getSelectedFile(values: UploadFormValues): File {
  return values.file[0]
}
