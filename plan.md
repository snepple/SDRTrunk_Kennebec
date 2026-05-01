1. **Update `TunerEditor.java`:** Add `protected String getUsbInfo()` which checks if `getDiscoveredTuner()` is an instance of `DiscoveredUSBTuner` and returns formatted USB Bus and Port string (e.g. ` (USB Bus: X Port: Y)`).
2. **Update all Subclasses of `TunerEditor`:** Modify where `getTunerIdLabel().setText(...)` is called, primarily in `tunerStatusUpdated()`. Append `getUsbInfo()` to the tuner's ID label text so the USB information is displayed.
3. **Run tests:** `./gradlew test` to make sure changes are correct.
4. **Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.**
5. **Submit.**
