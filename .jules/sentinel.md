## 2026-04-26 - Prevent Command Injection in ScriptAction
**Vulnerability:** Command injection / Execution of arbitrary non-file strings via ProcessBuilder in ScriptAction.
**Learning:** The ScriptAction class accepted any string and passed it directly to ProcessBuilder, allowing execution of potentially malicious commands if the string wasn't an actual file path.
**Prevention:** Use java.nio.file.Files.isRegularFile() to validate that the provided script path actually points to an existing file before executing it.
