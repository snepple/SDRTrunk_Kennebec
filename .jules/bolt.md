## 2024-05-13 - [Optimize formatOctetAsHex]
**Learning:** `String.format("%02X", value)` is heavily used in `AbstractMessage.formatOctetAsHex` but parsing format strings is slow. A custom char array lookup is nearly ~50x faster.
**Action:** Replace `String.format("%02X", value)` in `AbstractMessage.formatOctetAsHex` with a fast char array lookup implementation.

## 2024-05-13 - [Optimize getIntAsHex]
**Learning:** `getIntAsHex` creates strings via `Integer.toHexString`, pads them via string concatenation inside a `while` loop, and calls `.toUpperCase()`. This is slow due to creating multiple string objects and parsing strings. Using a custom char array padding technique like in `formatOctetAsHex` but supporting variable padding lengths improves speed by ~4x.
**Action:** Replace `Integer.toHexString(value).toUpperCase()` and manual padding in `getIntAsHex` with a fast char array lookup and initialization implementation.
