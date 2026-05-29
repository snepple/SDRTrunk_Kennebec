## 2023-10-27 - Performance Optimization
**Learning:** `NativeRealFIRFilterTest` failing with `UnsatisfiedLinkError` is expected during tests, as noted in the system prompt memories.
**Action:** Ignore this specific test failure when verifying changes.

## 2023-10-27 - FastMath vs Math for Math.sqrt and FMA
**Learning:** `FastMath.sqrt` and `Math.sqrt` perform similarly enough natively for large loops that replacing them doesn't yield measurable benefits. For vector operations, `FMA` isn't universally faster than `mul().add()` due to current vector instruction mappings on JVMs. Re-using buffers in `MagnitudeCalculator` implementations to eliminate rapid allocations per `calculate` invocation yields a massive improvement (3000ms -> 175ms for 100k iterations of 8k array).
**Action:** Prioritize reusing buffers to alleviate GC pressure rather than substituting math algorithms if not strictly necessary.

## 2023-10-27 - FastMath vs Math for Math.sqrt and FMA
**Learning:** `FastMath.sqrt` and `Math.sqrt` perform similarly enough natively for large loops that replacing them doesn't yield measurable benefits. For vector operations, `FMA` isn't universally faster than `mul().add()` due to current vector instruction mappings on JVMs. Re-using buffers in `MagnitudeCalculator` implementations to eliminate rapid allocations per `calculate` invocation yields a massive improvement (3000ms -> 175ms for 100k iterations of 8k array).
**Action:** Prioritize reusing buffers to alleviate GC pressure rather than substituting math algorithms if not strictly necessary.

## 2024-05-24 - Inner Loop Memory Store Optimization
**Learning:** Accumulating directly into array indices inside inner convolution loops incurs repeated bounds checking and memory store penalties.
**Action:** Extract a local primitive accumulator variable outside inner loops, update the primitive, and perform a single array assignment per outer loop iteration.
