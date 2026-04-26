## 2024-05-13 - [Optimize formatOctetAsHex]
**Learning:** `String.format("%02X", value)` is heavily used in `AbstractMessage.formatOctetAsHex` but parsing format strings is slow. A custom char array lookup is nearly ~50x faster.
**Action:** Replace `String.format("%02X", value)` in `AbstractMessage.formatOctetAsHex` with a fast char array lookup implementation.
