## 2024-05-06 - [VOLK JNI DSP Integration] **Learning:** [Using VOLK for dot product natively provides significant speedup over pure Java (0.054 ms vs 0.288 ms) utilizing JNI with DirectByteBuffers.] **Action:** [When needing fast mathematical processing like DSP, utilize JNI + VOLK ensuring to use DirectByteBuffers to avoid array copying overheads.]

## 2024-05-15 - Avoid Intermediate Array Allocations
**Learning:** In utility classes like `ByteUtil`, creating intermediate arrays (like a `byte[]` from an `int[]`) just to reuse an existing formatting method causes unnecessary memory allocation and garbage collection overhead.
**Action:** When creating overloaded utility methods, implement the core logic directly on the target data type to avoid intermediate object creation, rather than chaining to another method if it requires a type conversion buffer.
