## 2024-05-28 - Removed unused `getDcSpikeHalfBandwidth` code
**Learning:** `EmbeddedTuner` and its subclasses implemented an unused method `getDcSpikeHalfBandwidth` and matching constant `DC_SPIKE_AVOID_BUFFER`. `RTL2832TunerController` called this method but passed it to an inherited method from `TunerController` which is no longer needed.
**Action:** Removed `getDcSpikeHalfBandwidth` abstract definition, overrides in `R8xEmbeddedTuner`, `FC0013EmbeddedTuner`, and `E4KEmbeddedTuner`, and the unused `setMiddleUnusableHalfBandwidth(mEmbeddedTuner.getDcSpikeHalfBandwidth());` call from `RTL2832TunerController`.
## 2026-06-15 - Replaced unclosed FileInputStream with try-with-resources in ZipUtility
**Learning:** Missing .close() on BufferedInputStream(new FileInputStream(...)) inside ZipUtility.zipDirectory and ZipUtility.zipFile could lead to file handle leaks, specifically during error states in file writing.
**Action:** Added try-with-resources to ZipUtility.java methods zipDirectory and zipFile to guarantee deterministic closure of file handles.
