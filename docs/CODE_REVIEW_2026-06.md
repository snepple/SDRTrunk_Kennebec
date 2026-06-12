# SDRTrunk Kennebec — Application Review (June 2026)

Scope of this review, as requested:
1. **Functionality assurance** — receiving data from SDR tuners, playing audio, streaming audio.
2. **Uptime** — opportunities to keep all application functions running without human intervention.
3. **Automation** — settings and processes users currently configure manually that could be automated.

Findings reference `file:line` in the current `master` codebase. Severity reflects impact on
unattended 24/7 operation, which is this fork's stated mission.

---

## Executive summary

The Kennebec fork has real, working resilience infrastructure that upstream SDRTrunk lacks:
tuner error recovery with channel auto-restart, a Windows crash watchdog, a headless mode with a
REST health endpoint, channel inactivity alerts with Telegram/email delivery, and playlist
auto-save with atomic writes and backups. The architecture is sound.

However, the safety net has holes that matter for unattended operation:

- **Recovery gives up permanently.** Tuner recovery stops after 5 attempts (buffer errors) or
  45 minutes (disconnects). After that, reception is dead until a human restarts the app.
- **Silent failure modes are not detected.** A locked-up tuner delivering zero samples, a lost
  audio output device, and a wedged streaming connection all fail without triggering recovery.
- **The health endpoint is shallow and headless-only.** `/health` returns `UP` whenever the HTTP
  thread is alive — it does not check tuners, channels, audio, or streams, and it doesn't exist
  at all in GUI mode (how most Windows users run the app).
- **Channel restart after recovery only covers auto-start channels.** Manually started channels
  stay dead after a tuner recovers.

The highest-leverage work is closing the *detection* gaps (zero-sample watchdog, deep health
checks) and removing the *give-up* states (recovery should retry indefinitely with backoff, and
notify rather than stop). Details and concrete fixes below.

---

## Part 1 — Functionality assurance

### 1.1 Build status

`./gradlew compileJava` could not complete in the review environment because the Gradle toolchain
requires **Azul Zulu JDK 23** and the download was blocked by network policy — not a code issue.
Note for CI: `build.gradle` pins a vendor-specific toolchain fetched from foojay at build time,
which makes builds fail on any network hiccup or vendor outage. Consider allowing
`vendor=ANY` fallback or vendoring the toolchain in CI.

**There is no CI workflow in this repository.** Releases are built by hand via
`build_and_release.bat`. This is the single biggest functionality-assurance gap: nothing verifies
that a commit compiles or that the existing test suite passes before it lands on `master`.
Recommendation: add a GitHub Actions workflow that runs `./gradlew build test` on every push/PR
(matrix: Windows + Linux), and optionally builds the installer artifacts on tag.

### 1.2 Tuner reception path — verified working, with caveats

- Discovery (libusb enumeration + hotplug, SDRplay API) is solid:
  `TunerManager.java:113-175, 212-309, 358-417`.
- USB error detection catches device disconnect (`LibUsb.ERROR_NO_DEVICE`/`ERROR_PIPE`,
  `USBTunerController.java:661-664, 764-766`) and transfer-buffer exhaustion
  (`USBTunerController.java:677-681`).
- Recovery chain works end-to-end for those two error types:
  `DiscoveredTuner.setErrorMessage()` (`DiscoveredTuner.java:289-331`) schedules a recovery task →
  on success posts `TunerRecoveredEvent` → `PlaylistManager.handleTunerRecovered()`
  (`PlaylistManager.java:765-768`) → `ChannelProcessingManager.restartChannelsForTuner()`
  (`ChannelProcessingManager.java:959-970`).

Caveats are covered in Part 2 (they are uptime issues, not correctness bugs).

### 1.3 Audio playback path — works, fragile on device loss

- `AudioOutput` handles the JDK 22+ `write() <= 0` regression with a close/reopen workaround
  (`AudioOutput.java:256-264`) — good.
- On `LineUnavailableException` (USB headset unplugged, Windows default-device change), all audio
  channels are disabled with no retry or fallback (`AudioOutput.java:175-184`). Playback dies
  silently until a human changes the device in preferences. See finding U-4.

### 1.4 Streaming path — works per-platform, with one code-quality flag and several robustness gaps

- **OpenMHz `==` string comparisons** (`OpenMHzBroadcaster.java:110, 114, 177, 181`): comparisons
  like `response == "OK"` look broken, but `testConnection()` returns interned string literals
  from the same class, so they *do* work at runtime today. This is fragile — any refactor that
  builds the response string dynamically silently breaks connection detection. Fix to
  `"OK".equals(response)`. (Also: typo `"Uknown Exception"` at `OpenMHzBroadcaster.java:634`.)
- All broadcaster recording queues are **unbounded** `LinkedTransferQueue`s
  (`AudioStreamingBroadcaster.java:46`, `OpenMHzBroadcaster.java:77`,
  `BroadcastifyCallBroadcaster.java:74`). A hung or slow server grows the queue until OOM —
  which kills the whole application, not just the stream. Recordings do have a max-age purge
  (`OpenMHzBroadcaster.java:217-221`), which limits but does not eliminate the risk under
  high call volume. Use bounded queues with drop-oldest semantics.
- Temporary stream recording failures (disk full, deleted/read-only streaming directory) are
  caught, logged, and dropped (`AudioStreamingManager.java:361-377`); the path is never validated
  up front (`AudioStreamingManager.java:383-403`). A SystemHealthAlertEvent is posted, which is
  good, but there is no retry and no automatic disk-space management.
- IAmResponding UDP sender logs and continues on every send failure
  (`IAmRespondingBroadcaster.java:126-173`) — acceptable for UDP, but it resolves DNS
  (`InetAddress.getByName`) on every chunk; cache the resolved address and refresh on failure.
- Zello reconnect backoff can climb to ~15 minutes of accumulated delay and auth-failure paths
  can leave the WebSocket half-open before entering `CONFIGURATION_ERROR`
  (`ZelloBroadcaster.java:668-678, 1023-1029`).
- Real-time stream tracking maps (`AudioStreamingManager.java:264-286`) are not cleaned up if
  `stopRealTimeStreams` throws, leaking entries over long uptimes.

### 1.5 AI features (Kennebec additions) — functional, but failure handling can hurt the core pipeline

- `AIAudioOptimizer` re-throws Gemini API exceptions after logging
  (`AIAudioOptimizer.java:183-186`) and uses a 10s network timeout on a DSP-adjacent path; on
  HTTP 429 it silently downgrades the model (`AIAudioOptimizer.java:152-160`). Sustained API
  outage degrades the audio path rather than cleanly disabling itself. Recommendation: wrap all
  AI calls in a circuit breaker — after N consecutive failures, disable AI optimization, post one
  notification, and re-probe on a slow timer. **The core principle: optional AI features must
  never be able to degrade core reception/audio.**
- `AudioWatchdogService` asks Gemini whether a zero-audio condition is transient
  (`AudioWatchdogService.java:92-109`) and on API failure blindly falls back to a JMBE flush
  (`AudioWatchdogService.java:176-179`). The local fallback should be the primary heuristic, with
  AI as advisory.

---

## Part 2 — Uptime: keeping everything running without human intervention

Ordered by impact. "U-n" = uptime finding n.

### U-1 (Critical): Zero-sample tuner lockup is undetected — reception dies silently, forever

`USBTunerController.java:750-755` only dispatches transfers with `actualLength() > 0`. A stalled
tuner that keeps completing zero-length transfers (a known RTL-SDR failure mode after USB
power-state glitches) produces no error, channels keep "running" with no signal, and nothing in
the alert chain fires except — eventually — the per-channel inactivity alert (which notifies, but
does not fix). The same applies to a tuner that stops completing transfers entirely: there is no
sample-arrival watchdog anywhere (`HeartbeatManager` exists but is not used for starvation
detection).

**Fix:** count consecutive zero-length transfers and route through the existing recovery path
(`setErrorMessage("USB Error - No Sample Data")`), plus a coarse watchdog that flags any started
tuner that has delivered no samples for N seconds. This converts the worst silent-death mode into
the already-working recovery flow.

### U-2 (Critical): Recovery gives up permanently

- Buffer-exhaustion recovery: 5 attempts at 180s, then permanent
  `"Permanent USB Error"` (`DiscoveredTuner.java:398-433`).
- Disconnect recovery: 5s interval for 15 min, then 5 min interval, hard stop at 45 minutes
  (`DiscoveredTuner.java:435-477`).
- Every error string other than the two hardcoded USB messages goes straight to
  `TunerStatus.ERROR` with **no recovery at all** (`DiscoveredTuner.java:289-331`) — this
  includes all SDRplay/Airspy/HackRF error paths.

For an unattended system, "give up after 45 minutes" means an overnight USB hiccup (hub reset,
Windows power event) ends reception until morning. **Fix:** never stop retrying — back off to a
slow interval (e.g., every 10–15 min) indefinitely, send one notification via the existing
`NotificationRouter` when entering long-backoff mode, and extend recovery to all tuner error
types (a generic retry path is safe: worst case the restart fails again).

### U-3 (High): Channel restart after tuner recovery misses cases

- Only channels with `isAutoStart()` restart after recovery
  (`ChannelProcessingManager.java:964`). Manually started channels stay dead.
- The tuner→channels map is populated only when a `NOTIFICATION_ERROR_STATE` source event arrives
  (`ChannelProcessingManager.java:883-953`); a channel torn down through any other path is not
  remembered for restart.

**Fix:** remember every channel that was *processing* at the moment the tuner errored and restart
all of them on recovery, regardless of the auto-start flag.

### U-4 (High): Audio playback death on device loss

`AudioOutput.java:175-184` disables all audio channels on `LineUnavailableException` with no
retry. On Windows, a default-device change (very common: HDMI monitor sleep, USB audio re-enumeration)
permanently silences playback. **Fix:** a periodic (e.g., 10s) re-open attempt loop when in the
disabled state, with fallback to the system default device and a notification when fallback
occurs. `MixerManager` already has device enumeration to support this.

### U-5 (High): Health monitoring is shallow and absent in GUI mode

- `RestApiWatchdog` starts only in headless mode (`SDRTrunk.java:1027`); GUI deployments (the
  recommended Windows install) have no machine-readable health surface at all.
- `/health` (`RestApiWatchdog.java:47-57`) returns static `UP` — it would report healthy with
  every tuner in ERROR, all channels stopped, and every stream disconnected.
- `/restart` is unauthenticated; localhost-only mitigates remote attack, but any local process —
  including a web page issuing a cross-origin POST to `127.0.0.1:8080/restart` — can kill the
  app. Add a shared-secret token (env var / preference) and consider a non-default port; 8080 is
  the most collision-prone port on any machine.

**Fix:** start the REST API in both GUI and headless modes (preference-gated, configurable port),
and make `/health` aggregate real state: per-tuner status, per-channel processing state,
per-broadcaster `BroadcastState`, audio output state, last-sample timestamps. Return 503 when
degraded so external monitors (uptime-kuma, healthchecks.io, systemd) work out of the box.

### U-6 (Medium): Windows watchdog gaps; no watchdog on Linux/macOS

`WindowsReliabilityManager` (PowerShell `Wait-Process` + restart, `WindowsReliabilityManager.java:40-56`):
- Off by default; only detects process death — a hung (deadlocked, GC-thrashing) process is never
  restarted. The existing `DiagnosticMonitor` already detects deadlocks every 30s
  (`DiagnosticMonitor.java:104`) — wire it to trigger a self-restart instead of only logging.
- Restarts via relative path `bin\sdrtrunk.bat`; the registry Run entry it creates
  (`WindowsReliabilityManager.java:58-76`) launches with a different working directory, so the
  graceful-exit-file check (`user.dir`) and the relative restart path can both break across
  launch contexts. Use absolute paths derived from the install location.
- No crash-loop protection: an at-startup crash restarts in a tight infinite loop. Add a restart
  counter with a cooling-off period.
- Linux/macOS get nothing. Shipping a documented systemd unit (`Restart=always` +
  `WatchdogSec` integration with the REST `/health` endpoint) and a launchd plist would close
  this cheaply.

### U-7 (Medium): Streaming connections lack stuck-state timeouts

Broadcasters can sit in `CONNECTING`/`TEMPORARY_BROADCAST_ERROR` indefinitely: Icecast auth can
fail asynchronously after the session reports connected (`IcecastTCPAudioBroadcaster.java:148-273,
318-374`); Broadcastify Calls relies on a connect timeout but has no response deadline on async
uploads (`BroadcastifyCallBroadcaster.java:276-293`). **Fix:** a per-broadcaster supervisor that
forces a full teardown/reconnect if a broadcaster has not reached `CONNECTED` (or has not
successfully transmitted) within a deadline. This is one generic timer in
`AudioStreamingManager`, not per-platform work.

### U-8 (Medium): PPM drift detected but never acted on

`FrequencyErrorCorrectionManager.java:145-156` silently rejects corrections >10 PPM from
baseline. A tuner with real oscillator drift keeps "running" while decode quality collapses.
Route persistent out-of-bounds drift into the recovery/notification path. Relatedly,
`AIFrequencyStabilizer` runs hourly with a 5-call/day Gemini budget
(`AIFrequencyStabilizer.java:36, 64`) — fine as a trend tool, but it should not be the only drift
response.

### U-9 (Low): Smart Bandwidth has no rollback

`PolyphaseChannelSourceManager.autoOptimizeSampleRate()` (`PolyphaseChannelSourceManager.java:87-137`)
changes sample rate live; if the change destabilizes USB transfers there is no revert-on-failure.
Track buffer-error rate before/after a rate change and roll back if it spikes.

---

## Part 3 — Automation opportunities (reducing manual configuration)

What already exists (and is good): channel inactivity alerts with anti-spam
(`ChannelAlertMonitor.java`), Telegram/email routing (`NotificationRouter.java`), playlist
auto-save with atomic writes + `.backup` + lock file (`PlaylistManager.java:141-150`,
`PlaylistPreference.java:106-110`), Smart Bandwidth, deadlock detection and Windows
power-throttling auto-fix (`DiagnosticMonitor.java:129-199`), first-time wizard, auto-update.

Highest-value additions, in rough priority order:

| # | Opportunity | Today | Proposal |
|---|---|---|---|
| A-1 | **Self-monitoring → self-restart** | Alerts notify a human who then restarts things | Add an "auto-remediate" preference: on sustained channel inactivity / tuner permanent-error / stream stuck, automatically bounce the affected component (channel restart → tuner restart → app restart escalation ladder), with notification of what was done. All building blocks (ChannelAlertMonitor, NotificationRouter, recovery tasks, REST restart) already exist; they're just not wired to each other. |
| A-2 | **Headless/scripted provisioning** | Tuner gain/PPM, audio device, streaming credentials, notification settings all require GUI clicks; PPM calibration is GUI-interactive only (`FirstTimeWizard.java:186-200`) | Support a config file / environment variables / CLI flags for: PPM value, gain profile, streaming credentials (`BroadcastConfiguration`), notification tokens, channel auto-start. This makes Docker/systemd deployments and disaster recovery reproducible. |
| A-3 | **Auto PPM calibration** | Manual wizard against an FM broadcast signal | P25/DMR control channels are precise frequency references; `FrequencyErrorCorrectionManager` already measures error. Persist the converged PPM per tuner serial automatically and apply on startup — removes the calibration step entirely for trunked-system users. |
| A-4 | **Scheduled RadioReference sync** | One-time import wizard; talkgroup/site changes require manual re-import | Optional daily/weekly background job: re-fetch the imported system, diff talkgroups/sites/frequencies, auto-apply additive changes, notify on conflicts. Store the RR system ID with the imported channel config to make the linkage durable. |
| A-5 | **Auto tuner→channel assignment** | Channels reference a preferred tuner; multi-tuner spreading is manual | On startup, distribute channel frequency requirements across available tuners by bandwidth fit; re-balance when a tuner dies so surviving tuners cover the most-important channels (priority field already exists on channels). This also turns multi-tuner setups into automatic hardware redundancy. |
| A-6 | **Disk-space management** | Disk-full silently kills recordings/streams (see 1.4) | Background task: monitor recording/streaming directories, prune oldest recordings past a configurable cap, alert at threshold. |
| A-7 | **Config snapshot/restore** | `.backup` exists for playlist only; Java Preferences have no backup | Nightly zip of playlist + preferences + aliases to a rotating local archive; one-click/CLI restore. Java Preferences corruption is currently unrecoverable. |
| A-8 | **Watchdog enabled by default + cross-platform** | Windows watchdog opt-in; nothing for Linux/macOS | Default-on (with crash-loop guard from U-6); ship systemd unit + launchd plist in the distribution; document pairing with `/health`. |

---

## Part 4 — Recommended sequencing

1. **CI pipeline** (compile + tests on every push) — protects everything else. *(small)*
2. **U-1 zero-sample watchdog + U-2 never-give-up recovery + U-3 restart all channels** — closes
   the silent-reception-death modes; all changes sit on the existing, working recovery rails. *(medium)*
3. **U-5 deep `/health` in GUI+headless, with auth on `/restart`** — makes external monitoring
   trustworthy; prerequisite for A-1 and A-8. *(medium)*
4. **U-4 audio device retry/fallback** — biggest playback complaint-killer on Windows. *(small)*
5. **A-1 auto-remediation ladder** — converts the alert system from "tells a human" to "fixes it,
   then tells a human." *(medium)*
6. **Streaming hardening:** bounded queues, stuck-state supervisor (U-7), OpenMHz `equals`
   cleanup, AI circuit breaker. *(medium)*
7. **A-2 headless provisioning + A-8 service files** — unattended deployment story. *(medium)*
8. **A-3 auto-PPM, A-4 RR sync, A-5 auto tuner assignment** — differentiating automation
   features. *(larger)*

---

*Review performed on branch `master` @ 9863566. Static analysis only; runtime behavior with live
SDR hardware was not exercised in this environment.*
