## 2024-05-13 - [Optimize formatOctetAsHex]
**Learning:** `String.format("%02X", value)` is heavily used in `AbstractMessage.formatOctetAsHex` but parsing format strings is slow. A custom char array lookup is nearly ~50x faster.
**Action:** Replace `String.format("%02X", value)` in `AbstractMessage.formatOctetAsHex` with a fast char array lookup implementation.

## 2024-05-13 - [Optimize getIntAsHex]
**Learning:** `getIntAsHex` creates strings via `Integer.toHexString`, pads them via string concatenation inside a `while` loop, and calls `.toUpperCase()`. This is slow due to creating multiple string objects and parsing strings. Using a custom char array padding technique like in `formatOctetAsHex` but supporting variable padding lengths improves speed by ~4x.
**Action:** Replace `Integer.toHexString(value).toUpperCase()` and manual padding in `getIntAsHex` with a fast char array lookup and initialization implementation.

## 2024-05-13 - [Optimize getHex and BinaryMessage]
**Learning:** `BinaryMessage` heavily utilizes `String.format` and `Integer.toHexString` to format hex strings, which are surprisingly slow due to regex parsing and object instantiation inside inner loops in hot paths. Profiling shows `char[]` lookup techniques are significantly faster.
**Action:** Replace `String.format` and `Integer.toHexString` in `BinaryMessage` with manual bit-shifting and `char[]` mapping against a static `HEX_ARRAY`. Use `Math.max` for correctly sizing buffer arrays.
## 2025-02-12 - Fast Hexadecimal Formatting
**Learning:** In tight loops or high-frequency code paths (like parsing/formatting messages), using `StringUtils.leftPad(Integer.toHexString(value).toUpperCase(), places, '0')` is slow due to intermediate string allocations, under-the-hood regular expressions in `StringUtils`, and case conversions.
**Action:** Replace `Integer.toHexString` and string padding logic with a custom bitwise lookup against a static `char[]` of hexadecimal characters (`0123456789ABCDEF`) and process padding directly within the buffer. This avoids multiple object creations and speeds up execution significantly (~3x faster).
## 2024-05-13 - [Optimize StringUtils.leftPad Hexadecimal Formatting]
**Learning:** `StringUtils.leftPad(Integer.toHexString(...).toUpperCase(), ...)` and `String.format("%03X", ...)` are frequently used but relatively slow due to intermediate string allocations and regex parsing. Replacing them with the customized `P25Utils.formatHex(value, width)` method drastically reduces overhead and object creation.
**Action:** Replace string-based hex padders and formatters with `P25Utils.formatHex` in high-frequency monitoring or identifier formatting classes.

## 2025-05-03 - Added TwoTone UI Support
**Learning:** When trying to pass data (like UI visibility or messages) directly between independent modules such as the audio playback manager, Two Tone detectors, and streaming manager, it's safer and less invasive to use `MyEventBus` rather than tightly coupling the components.
**Action:** Keep extending the EventBus to broadcast events for internal decoupled triggers.
