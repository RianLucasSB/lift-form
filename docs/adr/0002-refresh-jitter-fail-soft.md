# Jittered proactive refresh with fail-soft on 401, not cross-tab coordination

Refresh tokens rotate on every use (the old one is revoked, a new one issued in the same call), so two
browser tabs racing to refresh at the same instant will have one of them get a `401` on an already-revoked
token. This isn't rare: tabs sharing a login compute their proactive-refresh timer from the same
`expiresIn`, so their timers target the same wall-clock instant by default.

Rather than building cross-tab coordination (e.g. a `BroadcastChannel` leader election) to eliminate the
race, we accept it: the proactive refresh timer is jittered (±15s around a fixed 60s-before-expiry buffer)
so simultaneous firing is rare, and a `401` from a *proactive* refresh never triggers a logout — it's
silently skipped. The losing tab's next request (its next proactive cycle, or a real user-triggered request)
reads whatever refresh-token cookie is current in the browser's jar at send time and succeeds transparently,
since cookies aren't cached per-tab. Only a `401` from the *reactive* (request-triggered) refresh-and-retry
path in `httpClient.ts` counts as session-over and logs the user out.
