## 2026-04-26 - Prevent Command Injection in ScriptAction
**Vulnerability:** Command injection / Execution of arbitrary non-file strings via ProcessBuilder in ScriptAction.
**Learning:** The ScriptAction class accepted any string and passed it directly to ProcessBuilder, allowing execution of potentially malicious commands if the string wasn't an actual file path.
**Prevention:** Use java.nio.file.Files.isRegularFile() to validate that the provided script path actually points to an existing file before executing it.

## 2026-04-26 - Remove hardcoded API keys
**Vulnerability:** Hardcoded API key (`c33aae37-8572-11ea-bd8b-0ecc8ab9ccec`) left in testing code for broadcast APIs (Broadcastify, OpenMHz, RdioScanner).
**Learning:** Hardcoded testing keys should not be committed to the repository because they can easily be accidentally released or exploited if they ever happen to point to production resources, or they can trigger automated secret scanning alerts.
**Prevention:** Use placeholder texts such as `YOUR_API_KEY_HERE` for testing or pull keys from an environment variable.
