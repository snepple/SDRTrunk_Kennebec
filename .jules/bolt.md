## 2024-05-18 - String Replace Performance
**Learning:** Calling `String.replace()` inside a loop over an array of multiple search strings allocates intermediate strings for every call, resulting in significant memory churn and CPU overhead (O(N*M) where N is string length and M is number of replacements) even if no replacements are made.
**Action:** When performing multi-character sanitization, use a single-pass implementation using a `StringBuilder` or regex pattern matching. With `StringBuilder`, only allocate it if an actual change needs to be made.
## 2024-05-18 - String Replace Performance
**Learning:** Calling `String.replace()` inside a loop over an array of multiple search strings allocates intermediate strings for every call, resulting in significant memory churn and CPU overhead (O(N*M) where N is string length and M is number of replacements) even if no replacements are made.
**Action:** When performing multi-character sanitization, use a single-pass implementation using a `StringBuilder` or regex pattern matching. With `StringBuilder`, only allocate it if an actual change needs to be made.
## 2024-05-06 - [VOLK JNI DSP Integration] **Learning:** [Using VOLK for dot product natively provides significant speedup over pure Java (0.054 ms vs 0.288 ms) utilizing JNI with DirectByteBuffers.] **Action:** [When needing fast mathematical processing like DSP, utilize JNI + VOLK ensuring to use DirectByteBuffers to avoid array copying overheads.]

## 2024-05-15 - Avoid Intermediate Array Allocations
**Learning:** In utility classes like `ByteUtil`, creating intermediate arrays (like a `byte[]` from an `int[]`) just to reuse an existing formatting method causes unnecessary memory allocation and garbage collection overhead.
**Action:** When creating overloaded utility methods, implement the core logic directly on the target data type to avoid intermediate object creation, rather than chaining to another method if it requires a type conversion buffer.
## 2026-05-07 - The Canvas Performer: Refactoring WaterfallPanel to direct Memory Writing
**Learning:** Rendering complex visualizations using individual JavaFX shapes or redundant Canvas clears creates object overhead and blocks the EDT or JavaFX Application Thread.
**Action:** Use a ConcurrentLinkedQueue to stream calculated pixel rows directly to a JavaFX PixelWriter and split the WritableImage drawing into top/bottom segments.

## 2026-05-15 - Single-pass string sanitization vs String.replace()
**Learning:** When performing multi-character sanitization in SDRTrunk, repeated `String.replace()` calls create significant O(N*M) string and builder allocation overhead and GC pressure. This is especially true for high-frequency operations like file path sanitization during audio recording creation.
**Action:** Prefer a single-pass `StringBuilder` that only allocates if a change is needed over repeated `String.replace()` calls to prevent unnecessary intermediate array allocations. Use a `switch` statement for O(1) character checking.
## 2026-05-08 - Formatting 32-bit Unsigned Talkgroups
**Learning:** Talkgroups, especially in NBFM, can exceed the bounds of a signed 32-bit integer. The talkgroup field accepts up to 4,294,967,295 (unsigned `int` max). If we use `String.valueOf(talkgroup)` directly, it prints as a negative number when it exceeds 2,147,483,647.
**Action:** Use `Integer.toUnsignedString()` when fetching talkgroup IDs for display instead of `String.valueOf()` to correctly display values larger than `Integer.MAX_VALUE`.
## 2024-05-10 - Lift Regex Compilation Out of Predicates
**Learning:** Compiling regex patterns (`Pattern.compile()`) inside `FilteredList.setPredicate` causes O(N) regex compilations for every single item on every keystroke, leading to significant UI lag for large datasets in JavaFX applications.
**Action:** Always hoist invariant operations like regex compilation and string lowercasing outside of the lambda/predicate passed to filtering or mapping functions.
## $(date +%Y-%m-%d) - [Primitive Array Replacement for FloatFIRFilter]
**Learning:** Using boxed collections like `ArrayList<Float>` in high-frequency DSP loops such as `FloatFIRFilter` causes excessive memory churn and boxing/unboxing overhead.
**Action:** Replaced `ArrayList<Float>` with a primitive `float[]` array for `mBuffer` to improve DSP performance and eliminate object creation in the audio pipeline.
## 2026-06-07 - [Primitive Array Replacement for FloatFIRFilter]
**Learning:** Using boxed collections like `ArrayList<Float>` in high-frequency DSP loops such as `FloatFIRFilter` causes excessive memory churn and boxing/unboxing overhead.
**Action:** Replaced `ArrayList<Float>` with a primitive `float[]` array for `mBuffer` to improve DSP performance and eliminate object creation in the audio pipeline.
