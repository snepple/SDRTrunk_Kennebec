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

## 2024-05-18 - FilteredList Predicate Invariant Hoisting
**Learning:** In JavaFX `FilteredList` implementations, executing expensive invariant operations (like compiling regular expressions or converting search strings to lowercase) inside the `setPredicate` lambda causes severe performance bottlenecks. For a list of N items, these operations execute N times per UI update/keypress, leading to O(N) redundant object allocations and UI freezes, particularly when catching exceptions for invalid inputs.
**Action:** Always hoist invariant transformations outside the `setPredicate` lambda block. Compile patterns and prepare filter strings once, assign them to `final` or effectively final variables, and then reference those variables within the predicate to ensure they are evaluated only once per update cycle.
## 2024-05-12 - Memory allocation in DSP Pipeline Iterators
**Learning:** `FloatNativeBuffer` iterator processes buffers in small chunks, repeatedly calling `Arrays.copyOfRange` followed by `SampleUtils.deinterleave`. This creates an array, copies data to it, and then passes it to `deinterleave` which creates two more arrays, allocating 3 arrays for every small chunk of elements processed, resulting in high memory churn.
**Action:** `SampleUtils.deinterleave` was extended to support an offset and length argument, allowing the caller to bypass `Arrays.copyOfRange` and directly parse the correct subset of the interleaved samples array.
## 2024-05-12 - Memory allocation in DSP Pipeline Iterators
**Learning:** `FloatNativeBuffer` iterator processes buffers in small chunks, repeatedly calling `Arrays.copyOfRange` followed by `SampleUtils.deinterleave`. This creates an array, copies data to it, and then passes it to `deinterleave` which creates two more arrays, allocating 3 arrays for every small chunk of elements processed, resulting in high memory churn. Also need to pad with zeroes if short.
**Action:** `SampleUtils.deinterleave` was extended to support an offset and length argument, allowing the caller to bypass `Arrays.copyOfRange` and directly parse the correct subset of the interleaved samples array while padding with zeros to ensure the output buffers are always the requested length.
## 2025-02-12 - Invariant Object Creation in JavaFX FilteredList Predicates
**Learning:** In JavaFX `FilteredList` implementations mapping to large UI tables, compiling regex patterns or calling `.toLowerCase()` on the search string *inside* the `setPredicate` lambda causes redundant string allocations and evaluations for every row during every keystroke, leading to O(N) complexity for an operation that should be O(1) and causing severe UI lag.
**Action:** Always hoist invariant transformations (like `toLowerCase()` on search text) or `Pattern.compile()` outside the predicate lambda so they are only evaluated once per keystroke instead of N times.
## 2025-02-12 - Recompiling Regexes and Redundant Matching
**Learning:** Checking `String.matches()` internally compiles a new `Pattern` every time. Doing `Pattern.compile()` right after `matches()` doubles the work.
**Action:** When matching regex patterns multiple times, declare a `static final Pattern` variable and use `pattern.matcher(string).find()` to avoid redundant `Pattern.compile` allocations and matching invocations.
## 2024-05-18 - PlaylistUpdater Regex Compilation
**Learning:** Checking `String.matches()` internally compiles a new `Pattern` every time. Calling this within a loop parsing playlist aliases with large files creates significant redundant object allocations and CPU load parsing Regex strings.
**Action:** When matching regex patterns multiple times inside loops (such as verifying AliasID text formats), extract the Regex string to `static final Pattern` instances and use `pattern.matcher(string).matches()` to prevent redundant regex compilations and save CPU time.
