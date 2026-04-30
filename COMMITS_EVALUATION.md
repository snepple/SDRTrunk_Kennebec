# SDRTrunk Commit Comparison Evaluation

We analyzed recent commits from two external repositories (`bazineta/sdrtrunk` and `kdolan/sdrtrunk`) against the `SDRTrunk_Kennebec` base. The focus was to identify significant logic, DSP, decoding, and performance enhancements that are worth bringing into this repository, while filtering out UI/UX changes and macOS-specific platform mitigations.

## Recommended Commits from `bazineta/sdrtrunk`

The `bazineta` branch is heavily focused on audio subsystem reimplementation, DSP performance optimizations, and reducing garbage collection (GC) pressure.

1. **Performance & GC Optimization**
   - `ccf7ce6a` Use Math.log10 - Switches to standard Math library for log10 calculations (reduces heap pressure, maintains speed)
   - `fe713d6c` Reduce GC pressure - Modifies `ComplexPolyphaseChannelizerM2` to achieve an approximate 10x reduction in garbage collection overhead.
   - `30a03075` Reduce GC pressure around FFT allocations - Optimizes buffer allocations in `NativeBufferManager` and `CarrierOffsetProcessor`.
   - `f2e8b132` Reduce memory pressure in hot path
   - `d56fa3ab` Reduce channelizer float buffer churn

2. **Audio & Processing Pipeline Re-architecture**
   - `75c892e6` Reimplement audio processing layer - A major architectural shift moving from mutable to immutable state in the audio layer. Introduces `AudioCallCoordinator`, `AudioCallEvent`, and `PlayableAudioCall`.
   - `be7c1037` Audio cleanup pass - Cleans up `AudioPlaybackManager` and `AudioChannel`.
   - `1484bfe7` Address race condition in audio playback
   - `5460fa01` Refactor P2 superframe detector - Cleanup of Phase 2 superframe processing logic.

3. **DSP/Algorithm Bug Fixes**
   - `84962cfd` Fix order of operations - Resolves mathematical order of operations bug in `ComplexPolyphaseChannelizerM2`.

## Recommended Commits from `kdolan/sdrtrunk`

The `kdolan` branch contains highly specific decoding pipeline improvements, new equalizers for simulcast, and advanced baseband recording features.

1. **MDC-1200 Decoder Upgrades**
   - `9a4e13f7` MDC-1200 decoder: soft sync, FEC, CRC validation - Vastly improves MDC-1200 decoding by introducing soft sync (tolerating up to 8 preamble bit errors), convolutional FEC, and CRC-16-CCITT validation.

2. **Baseband Activity Recording**
   - `1dbf9fea` Add activity-triggered baseband recording feature - Adds a squelch-like baseband recorder that captures I/Q samples only when RF signal activity exceeds a threshold, with a circular pre-trigger buffer.

3. **Simulcast & Decode Quality Enhancements**
   - `2025c089` Add C4FM V2 decoder with Gardner TED, AFC, and adaptive thresholds - Introduces a new version of the C4FM decoder using Gardner timing error detection, automatic frequency compensation, and adaptive decision thresholds.
   - `b18f00bf` Add hybrid CMA/LMS/DD equalizer, silence detection metric, and training infrastructure - Spec 019: Introduces advanced DSP equalizers (Constant Modulus Algorithm, Least Mean Squares, Decision-Directed) for improved decoding of P25 simulcast.
   - `a03fe17e` Add simulcast audio quality improvements: BCH threshold + IMBE quality gate - Sets a hard BCH error threshold for NAC-assisted NID corrections and introduces a pre-codec IMBE quality gate to suppress bad audio frames.

4. **P25 Protocol State Bug Fixes**
   - `4a18d559` Limit consecutive DUID corrections to prevent infinite noise LDU cycle
   - `171f97c6` Fix silent calls from indefinite LDU caching on missed HDU
   - `ad1dc121` Fix false encryption detection silencing unencrypted calls
   - `416ddd6c` Fix stuck CALL state from DUID correction cycling on C4FM channels

## Conclusion

**Recommendation:**
1. From `bazineta`: Carefully evaluate the "Reimplement audio processing layer" (`75c892e6`) as it represents a massive architectural shift that requires extensive testing. The GC/memory optimizations in the channelizer (`fe713d6c`, `30a03075`) and the order of operations fix (`84962cfd`) should be merged immediately as they provide low-risk performance gains.
2. From `kdolan`: The MDC-1200 improvements (`9a4e13f7`), the P25 protocol bug fixes (`171f97c6`, `ad1dc121`), and the Activity-triggered baseband recorder (`1dbf9fea`) are highly recommended. The advanced equalizers and C4FM v2 decoder should be evaluated based on the current stability of the DSP pipeline in Kennebec.
