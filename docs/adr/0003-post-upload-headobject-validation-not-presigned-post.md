# Validate uploaded video size/type via a post-upload S3 HeadObject, not a presigned POST policy

Presigned `PUT` URLs — what `CreateAnalysisUseCase` already generates — can't enforce a size or content-type
limit at the S3 layer: S3 doesn't check `Content-Length` server-side for a plain `PUT`, even if it's a signed
header. The AWS-native way to get real, non-bypassable enforcement is a presigned **POST** policy
(`content-length-range` + `Content-Type` conditions, checked by S3 itself before the bytes are even accepted).
We didn't switch to that: the AWS SDK for Java v2 has no built-in support for generating presigned POST
policies (a long-standing, still-open gap in the SDK), so it would mean hand-rolling the SigV4 policy-document
signing ourselves — a meaningfully bigger, more fragile piece of infrastructure than the alternative.

Instead, `ConfirmVideoUploadUseCase` `HeadObject`s the uploaded key before transitioning the analysis out of
`CREATED`: if the real `Content-Length`/`Content-Type` violates `analysis.upload.max-size-bytes` /
`allowed-content-type`, the object is deleted and the request is rejected (`413`/`415`) before the analysis
ever reaches the CV pipeline. This still fully blocks bad uploads from being processed — it just checks a
moment later (after the PUT succeeds) instead of rejecting the PUT itself — using the AWS SDK client already
wired into `S3VideoStorage`, with no new signing logic.
