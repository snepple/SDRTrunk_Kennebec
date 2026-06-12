# Ease-of-Setup & Adaptive Reception — Automation Opportunities (June 2026)

Goal: a user with limited radio knowledge gets the app receiving audio with minimal steps, and the
app keeps reception optimal on its own as channels move between tuners and conditions change
(weather, antenna, temperature, RF environment).

This builds on the June 2026 review (`CODE_REVIEW_2026-06.md`) and the uptime/automation work
merged in PR #718 / #720.

---

## 1. Where the setup journey stands today

**First-time wizard** (`gui/wizard/FirstTimeWizard.java`) covers environment only: JMBE build, PPM
calibration, Windows optimizations, Gemini key, GPU, memory. It ends with **zero channels, no
audio check, no RadioReference login, no streaming** — the user lands in an empty app.

**RadioReference import** (`gui/playlist/radioreference/`) is the strongest existing automation:
it auto-detects decoder type from the RR system flavor, fills control frequencies from site data,
and creates talkgroup aliases. But every channel it creates is **inert by default**:
no auto-start, no recording, no alert config, no streaming assignment
(`SiteEditor.java:494-606`, `Channel.java:75-104`).

**Tuner selection** is already automatic at runtime (no preferred tuner → `TunerManager.getSource()`
picks the first tuner that covers the frequency), so the often-feared "assign a tuner" step is
optional — but nothing tells the user that, and there is no load balancing across tuners.

**Fresh install → hearing local P25 audio** is roughly 15–20 steps today, including knowing your
system exists on RadioReference and navigating Country → State → County → System.

## 2. Highest-impact opportunity: "Hear something in 3 clicks"

**S-1. Location-first guided setup (extend FirstTimeWizard).** Add a final wizard page:
"Find my local radio systems" → user enters zip code / picks county once → app uses the existing
RadioReference API to list nearby trunked systems ranked by proximity → one click imports the
system **with live defaults** (auto-start on, alias list auto-created, inactivity alert + auto-
restart on, recording optional toggle). The RR cascade editors already contain all the pieces;
this is a re-sequencing with better defaults, not new infrastructure. RR's `getZipcodeInfo`-style
county lookup makes location-first search feasible.

**S-2. Live-by-default imported channels.** Change `SiteEditor` channel creation to set:
`autoStart=true` (first/control site), inactivity alert enabled with auto-restart (the A-1 feature
from PR #720), and alias list auto-created and named after the system if none selected. Today every
one of these is a separate manual discovery for the user.

**S-3. Setup validation panel ("Why am I not hearing anything?").** A single status card that
checks, in order: tuner detected → tuner enabled → channel configured → channel processing →
control channel sync acquired → recent decode activity → audio device present → audio unmuted.
Each red item links to the fix. All of the underlying state already exists
(`TunerManager`, `ChannelProcessingManager`, `SystemHealthMonitor`, the new `/health` aggregator);
this is the same data pointed at a novice instead of a log file.

## 3. Adaptive reception engine (gain / squelch / drift) — the core ask

Reception quality is governed by per-tuner gain, per-channel squelch (analog), and frequency
accuracy. Today: gain is a **static default** (`R8xTunerConfiguration.java:29-32` ships fixed
GAIN_327 with no feedback loop), squelch auto-track exists but is buried, PPM now self-corrects
and persists (PR #720). The missing piece is a closed loop driven by the quality metrics the app
already produces.

**Available control inputs (already in the codebase):**
- Per-channel power: `SourceEvent.NOTIFICATION_CHANNEL_POWER` (`SourceEvent.java:34,418`)
- Squelch control bus: `REQUEST_CHANGE_SQUELCH_THRESHOLD`, `REQUEST_CHANGE_SQUELCH_AUTO_TRACK`
  (`SourceEvent.java:56-58`) feeding `AdaptiveSquelch` (noise-floor tracking, 5s adjust period)
- Decoder quality: BER in P25/DMR decoders, sync-loss tracking in `SystemHealthMonitor`
  (300k-bit threshold), SNR/BER history persisted to SQLite by `SignalHealthPredictionLogger`
- Gain actuators: every tuner editor sets gain through the tuner controller; RTL R8x exposes
  `MasterGain.AUTOMATIC` (hardware AGC, `R8xEmbeddedTuner.java:670`), Airspy has LNA/Mixer AGC
  flags (default off, `AirspyTunerConfiguration.java:33-34`)

**A-9. Closed-loop gain controller (per tuner).** A `TunerGainOptimizer` that, for each tuner:
1. Aggregates quality across **all channels currently using that tuner** (gain is a shared
   resource — optimize for the weakest active channel without overloading the strongest).
2. Detects "gain too high": noise floor rise / clipping / BER worsening as power increases.
   Detects "gain too low": low channel power + sync losses + BER high.
3. Steps gain one notch at a time with hysteresis and a settling period (e.g., evaluate over
   60s windows), bounded to safe ranges, and **only between calls** (never mid-transmission).
4. Persists the converged gain per tuner serial (same pattern as the auto-PPM persistence in
   `TunerPPMCorrectedEvent`), so the next start is pre-optimized.
5. Re-evaluates continuously — this is what handles weather, antenna moves, and seasonal drift,
   and re-optimizes when the channel mix on a tuner changes (channels start/stop → recompute).
Hardware AGC (RTL `MasterGain.AUTOMATIC`, Airspy AGC) is the zero-effort fallback for analog
listening, but for digital decoding a slow software loop driven by BER outperforms hardware AGC,
which is tuned for audio, not symbol decisions. Suggested default: software loop on, hardware AGC
off, with a "Manual" override in the tuner editor that disables the loop.

**A-10. Squelch: automatic by default.** `AdaptiveSquelch` already tracks the noise floor and
re-adjusts every 5 seconds — exactly the "adjusts as reception changes" behavior wanted — but it
is opt-in per channel. Make auto-track the default for new NBFM/AM channels, label the editor
control "Automatic (recommended)", and seed the initial threshold from the first 5 seconds of
noise-floor measurement instead of a fixed dB value. Digital channels (P25/DMR) need no squelch —
hide the concept from users entirely there.

**A-11. Reception quality score per channel (drives both UI and the loop).** Distill power + BER +
sync-rate into a 0–100 score shown as a simple colored dot next to each Now Playing channel, with
plain-language hints ("Weak signal — check antenna", "Overloaded — gain reduced automatically").
The same score is the objective function for A-9 and the trigger for A-12.

**A-12. Degradation response ladder.** When a channel's score stays poor: (1) gain loop nudges,
(2) re-run PPM correction, (3) if multiple tuners can cover the frequency, migrate the channel to
the tuner with the better noise figure / lower load, (4) notify with the measured cause. Steps 1–2
exist or are A-9; step 3 reuses `restartChannelsForTuner` mechanics with a preferred-tuner swap.

## 4. Streaming setup friction

**A-13. Channel-level stream assignment.** Today streaming is tagged **per alias**
(`StreamAliasSelectionEditor.java`) — for a 500-talkgroup system that is 500 clicks. Add
"Stream this channel → [config]" which bulk-tags every alias in the channel's list and auto-tags
future aliases created by `DynamicAliasPopulator` for that list. This is the single biggest
streaming pain point.

**A-14. Streaming step in import flow.** After RR import, offer "Stream this system?" with the
user's existing broadcast configs (or a create-new shortcut) and apply A-13 in one click.

## 5. Suggested sequencing

| Priority | Item | Effort |
|---|---|---|
| 1 | S-2 live-by-default imported channels | Small |
| 2 | A-10 squelch automatic by default | Small |
| 3 | A-13 channel-level stream assignment | Medium |
| 4 | A-11 reception quality score + plain-language hints | Medium |
| 5 | A-9 closed-loop gain controller with persistence | Medium-Large |
| 6 | S-1 location-first guided setup | Medium-Large |
| 7 | S-3 setup validation panel | Medium |
| 8 | A-12 degradation ladder (incl. tuner migration) | Large |
| 9 | A-14 streaming in import flow | Small (after A-13) |

Items 1–2 are default changes a novice never sees but immediately benefits from. Items 3–5 remove
the two biggest knowledge requirements (streaming wiring, gain tuning). Items 6–8 complete the
"3 clicks to audio, self-maintaining" vision.
