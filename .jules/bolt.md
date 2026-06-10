## 2026-05-03 - NBFM Geographic Talkgroup Generator Architecture Bounds
**Learning:** Refactoring core properties like `Talkgroup` IDs from `int` to `long` in an established application like SDRTrunk involves an extreme risk of cascading regressions across parsing protocols (P25, LTR, AM, NBFM, etc.), playlist loading rules, memory persistence structures (JAXB / XML), and UI components.
**Action:** When a user requests IDs larger than 32-bit `MAX_VALUE`, rather than undertaking a massive breaking architecture change to `long`, it is safer to handle numeric manipulation in String form (for the UI) and let the integer gracefully fallback natively, or advise the user to accept constraints unless a full-scale refactor is mandated.

## 2024-05-09 - Automated libvolk Installation in Batch Script
**Learning:** For users relying on a custom batch script for building on Windows, native dependencies like `libvolk` might not be pre-installed and can halt the build process.
**Action:** Always inject dependency checks into bootstrap scripts (e.g. `update_sdrtrunk.bat`), leveraging PowerShell for downloads and `cmake` for configuration, to minimize environment setup friction.

## 2026-05-15 - Single-pass string sanitization vs String.replace()
**Learning:** When performing multi-character sanitization in SDRTrunk, repeated `String.replace()` calls create significant O(N*M) string and builder allocation overhead and GC pressure. This is especially true for high-frequency operations like file path sanitization during audio recording creation.
**Action:** Prefer a single-pass `StringBuilder` that only allocates if a change is needed over repeated `String.replace()` calls to prevent unnecessary intermediate array allocations. Use a `switch` statement for O(1) character checking.
## 2026-05-08 - Formatting 32-bit Unsigned Talkgroups
**Learning:** Talkgroups, especially in NBFM, can exceed the bounds of a signed 32-bit integer. The talkgroup field accepts up to 4,294,967,295 (unsigned `int` max). If we use `String.valueOf(talkgroup)` directly, it prints as a negative number when it exceeds 2,147,483,647.
**Action:** Use `Integer.toUnsignedString()` when fetching talkgroup IDs for display instead of `String.valueOf()` to correctly display values larger than `Integer.MAX_VALUE`.
## 2024-05-10 - Lift Regex Compilation Out of Predicates
**Learning:** Compiling regex patterns (`Pattern.compile()`) inside `FilteredList.setPredicate` causes O(N) regex compilations for every single item on every keystroke, leading to significant UI lag for large datasets in JavaFX applications.
**Action:** Always hoist invariant operations like regex compilation and string lowercasing outside of the lambda/predicate passed to filtering or mapping functions.

## 2026-06-09 - Eliminate Autoboxing Churn in DSP Code
**Learning:** Utilizing generic boxed collections like `ArrayList<Float>` in high-frequency DSP processing loops creates enormous amounts of silent memory allocations due to autoboxing and unboxing, thrashing the GC.
**Action:** Always prefer primitive arrays (`float[]`) or dedicated primitive collection libraries for high-performance circular buffers and numeric processing to eliminate GC pressure and CPU overhead.
## 2026-05-18 - Spectrum Analyzer and Waterfall Drawing Bugs
**Learning:** Mathematical sign errors when refactoring audio spectrum math (e.g. log scaling `mDBScale`) can result in out-of-bounds metrics resulting in entire charts clamping to Y=0 or skipping the last bin (`update.length - 1`). Dynamic range (in decibels) is a negative value `20 * log10(1 / (2 ^ (bit_depth - 1)))`.
**Action:** When updating spectrum code, ensure loop index logic accurately bounds the array size (`update.length`) rather than off-by-one offsets. Ensure that dynamically scaled bounds evaluate to their true negative values so UI scaling metrics (like `scalor = height / -mDBScale`) correctly map into positive view bounds.
## 2025-02-18 - Avoid Math.pow for squaring in DSP
**Learning:** Math.pow(val, 2.0f) is heavily optimized for general exponentiation but is significantly slower than direct multiplication (val * val) when computing squares. This is particularly problematic in high-frequency DSP code paths like PowerMonitor.java where calculations occur on every sample buffer.
**Action:** Always use direct multiplication (val * val) for squaring instead of Math.pow() in performance-critical code.
