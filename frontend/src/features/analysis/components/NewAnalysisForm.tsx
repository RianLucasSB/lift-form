import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Progress } from '@/components/ui/progress'
import { useCreateAnalysisFlow } from '../hooks/useCreateAnalysisFlow'
import { getSelectedFile, uploadSchema, type UploadFormValues } from '../schemas/uploadSchema'

const STAGE_LABEL = {
  creating: 'Starting your analysis…',
  uploading: 'Uploading your video…',
  confirming: 'Finishing up…',
} as const

export function NewAnalysisForm() {
  const navigate = useNavigate()
  const { stage, uploadProgress, error, start, retry } = useCreateAnalysisFlow((analysisId) => {
    navigate(`/analysis/${analysisId}`)
  })

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<UploadFormValues>({
    resolver: zodResolver(uploadSchema),
  })

  async function onSubmit(values: UploadFormValues) {
    await start(getSelectedFile(values))
  }

  if (error) {
    return (
      <div className="mt-6 space-y-4">
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
        <Button onClick={() => void retry()} className="w-full">
          Try again
        </Button>
      </div>
    )
  }

  if (stage !== 'idle') {
    return (
      <Card className="mt-6">
        <CardContent className="flex flex-col items-center gap-4 py-6 text-center">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
          <p className="text-sm text-muted-foreground">{STAGE_LABEL[stage]}</p>
          {stage === 'uploading' && (
            <div className="w-full space-y-1.5">
              <Progress value={uploadProgress} />
              <p className="text-xs text-muted-foreground">{uploadProgress}%</p>
            </div>
          )}
        </CardContent>
      </Card>
    )
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-6 space-y-4">
      <div className="space-y-1.5">
        <Label htmlFor="file">Squat video</Label>
        <Input
          id="file"
          type="file"
          accept="video/mp4"
          aria-invalid={!!errors.file}
          {...register('file')}
        />
        <p className="text-xs text-muted-foreground">MP4 only, up to 500MB.</p>
        {errors.file && <p className="text-sm text-destructive">{errors.file.message}</p>}
      </div>

      <Button type="submit" className="w-full">
        Analyze video
      </Button>
    </form>
  )
}
