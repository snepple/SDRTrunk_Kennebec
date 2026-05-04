## 2024-05-04 - User friendly error messages for exhausted AI quotas
**Learning:** Raw "RESOURCE_EXHAUSTED" error responses from backend APIs (like Gemini) confuse users when their quota runs out.
**Action:** Catch quota exhaustion responses (HTTP 429 or matching keywords in the response body), throw a clean user-friendly exception message ("Gemini API quota exhausted..."), and attempt to gracefully downgrade the user to a less resource-intensive fallback model to preserve availability.
