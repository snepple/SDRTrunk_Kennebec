## 2024-05-18 - String Replace Performance
**Learning:** Calling `String.replace()` inside a loop over an array of multiple search strings allocates intermediate strings for every call, resulting in significant memory churn and CPU overhead (O(N*M) where N is string length and M is number of replacements) even if no replacements are made.
**Action:** When performing multi-character sanitization, use a single-pass implementation using a `StringBuilder` or regex pattern matching. With `StringBuilder`, only allocate it if an actual change needs to be made.
## 2024-05-18 - String Replace Performance
**Learning:** Calling `String.replace()` inside a loop over an array of multiple search strings allocates intermediate strings for every call, resulting in significant memory churn and CPU overhead (O(N*M) where N is string length and M is number of replacements) even if no replacements are made.
**Action:** When performing multi-character sanitization, use a single-pass implementation using a `StringBuilder` or regex pattern matching. With `StringBuilder`, only allocate it if an actual change needs to be made.
