# Analysis result view shows `classification`, not `feedback.overall_label`

A completed analysis carries two parallel quality signals derived from the same `overallScore`:
`classification` (`GOOD`/`FAIR`/`POOR`, computed on the Java side by `Classification.fromScore(...)`) and
`feedback.overall_label` (`excellent`/`good`/`fair`/`poor`/`bad`, computed by the Python scoring worker for
its own internal use — see `squat_predictor.py`). The two use different score thresholds and are not
interchangeable, so showing both risked visibly contradicting each other (e.g. `classification: FAIR` next
to `overall_label: good`).

The result page's headline badge is driven by `classification` only; `feedback.overall_label` is not
rendered anywhere in the frontend. `classification` is the field the backend explicitly treats as the one
source of truth for "what counts as a good score," specifically to avoid that rule drifting between
languages — `overall_label` is a worker-internal artifact, not a second opinion meant to reach end users.
