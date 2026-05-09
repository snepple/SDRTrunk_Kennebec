## 2024-05-24 - AI Artifact Cleanup
**Finding:** A significant number of python scripts and java patches (`patch_*.py`, `patch_*.java`) were found checked into the root directory, alongside single image files (`verification.png`).
**Action:** When performing file editing, always ensure temporary helper scripts or files used for intermediate testing/verification are explicitly removed and untracked before final git commits.
