# Create-analysis flow

Users need a page to create a new video analysis: a form to upload a video, client-side validation (size,
format), a visual loading/processing state while polling status, and either the scored result or a
user-friendly error once terminal. Derived from a `/grill-with-docs` interview; recorded here after the fact
since no spec file was created during the interview itself.

## Routes

- `/analysis/new` — file picker form. Owns create → upload → confirm-upload as one sequence. On success,
  navigates to `/analysis/:id`.
- `/analysis/:id` — owns polling and rendering the result/error. Both routes are protected
  (`ProtectedRoute`), matching the rest of the authenticated app.
- `/overview` gets a "New analysis" CTA linking to `/analysis/new`.

Exercise type is hardcoded to `SQUAT` (the only value the API's `ExerciseType` enum supports) — no selector
shown in the UI.

## Client-side file validation

- MP4 only (`video/mp4`), max 500MB. Rejected files show an inline error at the field, not a submission
  attempt.

## Upload UX

- Real byte-progress bar during the S3 `PUT` (requires `XMLHttpRequest`, not `fetch` — `fetch` has no
  upload-progress event), then an indeterminate spinner while confirming and while polling.

## Status polling

- Poll `GET /analysis/{id}` every 3 seconds, starting once `upload-complete` succeeds.
- Collapse `CREATED`/`UPLOADED`/`PROCESSING` into one "Processing your video…" UI state — the distinction
  isn't meaningful to someone watching a spinner.
- Give up after 5 minutes if neither `COMPLETED` nor `FAILED` is reached: show a "taking longer than
  expected" message with a manual "check again" action, rather than polling forever.

## Result rendering

- On `COMPLETED`: show `overallScore` and a `classification` (`GOOD`/`FAIR`/`POOR`) badge as the headline,
  plus the six per-dimension ratings from `feedback` (`depth`/`back`/`tempo`/`lockout`/`rom`/`consistency`,
  each `{rating, detail}`). `feedback.overall_label` (a separate, Python-worker-internal 5-bucket label) is
  never shown — `classification` is the one source of truth for the headline verdict.
- On `FAILED`: one generic, user-friendly message ("We couldn't analyze this video.") with a retry action —
  the raw `errors[]` strings from the API are pipeline-internal, not proofread user-facing copy, and are not
  shown.

## Error handling / retries

Each failure point gets its own friendly message:

- Client-side validation failure (file type/size): inline field error, no request sent.
- `POST /analysis/create` fails: "Couldn't start the analysis. Please try again." Retry restarts the whole
  flow (a new `POST /analysis/create` call).
- The S3 `PUT` fails: "Upload failed. Please try again." Retry restarts the whole flow (a new `analysisId`) —
  accepted as leaving a harmless orphaned `CREATED` row server-side, since there's no cancel/delete endpoint
  and this is out of scope to solve here.
- `POST .../upload-complete` fails for a reason *other than* a `413`/`415`: "Couldn't confirm the upload.
  Please try again." Retry re-calls only `upload-complete` against the same `analysisId` — the file is
  already sitting in S3, no need to re-upload it.
- `POST .../upload-complete` returns `413` (file too large) or `415` (unsupported content type) — see the
  backend section below: "File exceeds the upload limits. Please start over with a different file." Retry
  restarts the whole flow (a new `analysisId`), since retrying the same call would fail identically.
- Poll timeout (5 minutes, no terminal status): "This is taking longer than expected." with a manual
  "check again" action that resumes polling rather than restarting the analysis.

## Backend: real (non-bypassable) upload validation

Presigned `PUT` URLs (what `CreateAnalysisUseCase` already issues) cannot enforce a size or content-type
limit at the S3 layer — S3 doesn't check `Content-Length` server-side for a plain `PUT`. The AWS-native
mechanism for that (a presigned POST policy with `content-length-range`/`Content-Type` conditions) isn't
supported by the AWS SDK for Java v2 without hand-rolling SigV4 policy signing, which was judged too much
complexity for what a simpler mechanism already available in this codebase can achieve.

Instead: `ConfirmVideoUploadUseCase` calls S3 `HeadObject` on the uploaded key (only when the analysis is
still `CREATED`) before transitioning it to `UPLOADED`. If the real `Content-Length` exceeds
`analysis.upload.max-size-bytes` (configurable, default 500MB) or the real `Content-Type` doesn't match
`analysis.upload.allowed-content-type` (configurable, default `video/mp4`):

- the S3 object is deleted
- the analysis is left in `CREATED` (no status transition — same as any other abandoned/never-confirmed
  upload, an already-accepted harmless state)
- the request fails with `413 Payload Too Large` (size) or `415 Unsupported Media Type` (content type), so
  the frontend can distinguish "must restart with a different file" from a generic/transient confirm-upload
  failure purely via HTTP status, without a new response-body schema.

## Out of scope for this feature

- A history/list view of past analyses (the existing `GET /analysis` list endpoint is not used here).
- Cleanup of orphaned `CREATED` rows or abandoned S3 objects from restarted flows.
- Cancelling an in-progress upload.
- Automated cleanup/expiry of the `EXPIRED` status (it exists in the domain enum but nothing sets it,
  independent of this feature).
