## 2026-05-05 - Enforcing Numeric Input with IntegerTextField
**Learning:** SDRTrunk contains many instances where a generic JavaFX `TextField` is used to collect purely numeric configuration data (like Talkgroup IDs), requiring manual string parsing and try-catch blocks to handle formatting errors on save.
**Action:** Use the existing `io.github.dsheirer.gui.control.IntegerTextField` class for these inputs. It automatically filters out non-numeric characters and provides a safe `.get()` method, preventing formatting errors before they occur and simplifying backend processing.
