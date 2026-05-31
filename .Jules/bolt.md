## 2026-05-03 - NBFM Geographic Talkgroup Generator Architecture Bounds
**Learning:** Refactoring core properties like `Talkgroup` IDs from `int` to `long` in an established application like SDRTrunk involves an extreme risk of cascading regressions across parsing protocols (P25, LTR, AM, NBFM, etc.), playlist loading rules, memory persistence structures (JAXB / XML), and UI components.
**Action:** When a user requests IDs larger than 32-bit `MAX_VALUE`, rather than undertaking a massive breaking architecture change to `long`, it is safer to handle numeric manipulation in String form (for the UI) and let the integer gracefully fallback natively, or advise the user to accept constraints unless a full-scale refactor is mandated.

## 2024-05-09 - Automated libvolk Installation in Batch Script
**Learning:** For users relying on a custom batch script for building on Windows, native dependencies like `libvolk` might not be pre-installed and can halt the build process.
**Action:** Always inject dependency checks into bootstrap scripts (e.g. `update_sdrtrunk.bat`), leveraging PowerShell for downloads and `cmake` for configuration, to minimize environment setup friction.

## 2026-05-18 - Spectrum Analyzer and Waterfall Drawing Bugs
**Learning:** Mathematical sign errors when refactoring audio spectrum math (e.g. log scaling `mDBScale`) can result in out-of-bounds metrics resulting in entire charts clamping to Y=0 or skipping the last bin (`update.length - 1`). Dynamic range (in decibels) is a negative value `20 * log10(1 / (2 ^ (bit_depth - 1)))`.
**Action:** When updating spectrum code, ensure loop index logic accurately bounds the array size (`update.length`) rather than off-by-one offsets. Ensure that dynamically scaled bounds evaluate to their true negative values so UI scaling metrics (like `scalor = height / -mDBScale`) correctly map into positive view bounds.

## 2026-05-18 - Lazy Buffer Evaluation in Channelizer Views
**Learning:** Using `FloatNativeBuffer` with eagerly evaluated `toInterleaved()` calls on `ComplexSamples` in GUI channelizer views causes massive memory churn from unused arrays when frames are dropped by the `NativeBufferManager`.
**Action:** Always use `ComplexSamplesNativeBufferAdapter` to lazily defer array allocations until explicitly pulled by consumers to conserve memory during high-speed spectrum updates.

## 2026-05-31 - FloatFIRFilter ArrayList Performance Overhead
**Learning:** Using `ArrayList<Float>` for DSP circular buffers causes massive performance degradation due to the boxing/unboxing overhead of primitive floats and object memory churn during high-frequency sample processing loops.
**Action:** Always use primitive arrays like `float[]` instead of Collections for high-frequency numerical DSP operations to eliminate boxing overhead and improve cache locality.
