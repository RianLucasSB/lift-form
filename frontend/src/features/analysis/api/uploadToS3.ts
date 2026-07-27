/**
 * PUTs a file directly to a presigned S3 URL. Deliberately bypasses
 * `apiRequest`/`httpClient` (unauthenticated, non-JSON, no same-origin
 * cookies) and uses XMLHttpRequest instead of fetch so upload progress can be
 * reported — fetch has no upload-progress event.
 */
export function uploadToS3(uploadUrl: string, file: File, onProgress: (percent: number) => void): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', uploadUrl)
    xhr.setRequestHeader('Content-Type', file.type)

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`Upload failed with status ${xhr.status}`))
      }
    }

    xhr.onerror = () => reject(new Error('Upload failed'))

    xhr.send(file)
  })
}
