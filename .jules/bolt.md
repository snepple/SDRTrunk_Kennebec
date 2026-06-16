## 2026-05-03 - NBFM Geographic Talkgroup Generator Architecture Bounds
**Learning:** Refactoring core properties like `Talkgroup` IDs from `int` to `long` in an established application like SDRTrunk involves an extreme risk of cascading regressions across parsing protocols (P25, LTR, AM, NBFM, etc.), playlist loading rules, memory persistence structures (JAXB / XML), and UI components.
**Action:** When a user requests IDs larger than 32-bit `MAX_VALUE`, rather than undertaking a massive breaking architecture change to `long`, it is safer to handle numeric manipulation in String form (for the UI) and let the integer gracefully fallback natively, or advise the user to accept constraints unless a full-scale refactor is mandated.

## 2024-05-09 - Automated libvolk Installation in Batch Script
**Learning:** For users relying on a custom batch script for building on Windows, native dependencies like `libvolk` might not be pre-installed and can halt the build process.
**Action:** Always inject dependency checks into bootstrap scripts (e.g. `update_sdrtrunk.bat`), leveraging PowerShell for downloads and `cmake` for configuration, to minimize environment setup friction.

## 2026-05-18 - Spectrum Analyzer and Waterfall Drawing Bugs
**Learning:** Mathematical sign errors when refactoring audio spectrum math (e.g. log scaling `mDBScale`) can result in out-of-bounds metrics resulting in entire charts clamping to Y=0 or skipping the last bin (`update.length - 1`). Dynamic range (in decibels) is a negative value `20 * log10(1 / (2 ^ (bit_depth - 1)))`.
**Action:** When updating spectrum code, ensure loop index logic accurately bounds the array size (`update.length`) rather than off-by-one offsets. Ensure that dynamically scaled bounds evaluate to their true negative values so UI scaling metrics (like `scalor = height / -mDBScale`) correctly map into positive view bounds.

## 2026-06-16 - String.matches Performance in Hot Paths
**Learning:** Using `String.matches()` for input validation inside hot loops or frequently called utility methods (like `BinaryMessage.load()`) incurs a massive performance penalty due to repeated `Pattern` compilation. Manual char-by-char traversal is orders of magnitude faster.
**Action:** When validating simple structures (like binary or hex strings) in performance-sensitive paths, always replace `String.matches()` with manual `charAt()` loops.

## 2026-06-16 - CI Build Failures with JDK 25 Toolchain
**Learning:** If SDRTrunk GitHub Actions CI builds fail with "Unsupported class file major version 69" (Java 25), it indicates a mismatch between the CI runner's JDK and the Gradle toolchain.
**Action:** Fix it by updating `build.gradle` to use `languageVersion = JavaLanguageVersion.of(25)` and `vendor = JvmVendorSpec.BELLSOFT`.

## 2026-06-16 - CI Build Failures with JDK 25 Toolchain
**Learning:** If SDRTrunk GitHub Actions CI builds fail with "Unsupported class file major version 69" (Java 25), it indicates a mismatch between the CI runner's JDK and the Gradle toolchain.
**Action:** Fix it by updating `build.gradle` to use `languageVersion = JavaLanguageVersion.of(25)` and `vendor = JvmVendorSpec.BELLSOFT`.
