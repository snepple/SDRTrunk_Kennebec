# Upstream Sync Tracking

Fork: actionpagezello/sdrtrunk (master)

## Last Check: 2026-06-26

### Last known upstream SHA: 6b2142c (Jun 19 2026)

## Sync Log

### 2026-06-26

**Upstream commits reviewed (newest to oldest):**

| SHA | Date | Title | Decision |
|-----|------|-------|----------|
| 6b2142c | Jun 19 | Clear Zello stream errors while connected | SKIP — `updateStreamErrorDetail()` already implemented in Kennebec |
| 0b98a61 | Jun 19 | Fix Zello reconnect, channel busy, and startup rate limits | SYNCED (partial) — ported startup rate limiting + channel busy cooldown |
| d8c0a71 | Jun 18 | Refactor Zello broadcasters and fix startup stagger | SYNCED (partial) — extracted functional fixes, skipped AbstractZelloBroadcaster refactor |
| 19a8a8a | May 21 | Add channel labels to CTCSS/DCS logs, remove broken occupancy check | SKIP — Kennebec independently implemented CTCSS fixes (Jun 24) |
| 484262e | May 14 | Fix silent Zello keepalive death and channel-offline reconnect | SKIP — Kennebec has more advanced keepalive implementation |
| 153260e | May 12 | Fix CTCSS tone squelch never opening during voice | SKIP — Kennebec has CTCSS improvements (Jun 24) |
| 7790ac2 | May 12 | Fix RSP power overload error spam after device removal | NOT VERIFIED — check on next run |

**What was implemented (commit 3540593):**
- `BroadcastModel.java`: Rate-limit Zello cold-starts at 9/min (batches of 9, 1s spacing, 60s pause between batches) for `ZelloConfiguration` and `ZelloConsumerConfiguration` types only
- `ZelloBroadcaster.java`: Set `mLastStreamStopTime` on `start_stream` failure to apply stream guard after rejected attempts
- `ZelloConsumerBroadcaster.java`: Same fix as above
