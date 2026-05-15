## 2024-05-24 - AI Artifact Cleanup
**Finding:** A significant number of python scripts and java patches (`patch_*.py`, `patch_*.java`) were found checked into the root directory, alongside single image files (`verification.png`).
**Action:** When performing file editing, always ensure temporary helper scripts or files used for intermediate testing/verification are explicitly removed and untracked before final git commits.

## 2026-05-09 - Mintlify Docs Integrity
**Finding:** Mintlify uses the `docs` folder for its site generation, while the Help viewer inside the app uses `src/main/resources/docs`.
**Action:** Do NOT delete `.md` or `.mdx` files in the `docs` directory or `src/main/resources/docs`. They are explicitly required for the Mintlify integration and the embedded help viewer respectively. Do NOT blindly copy `docs/*.mdx` to `src/main/resources/docs/*.md` as the Java help viewer does not support MDX React components. When syncing documentation, adapt the content to standard Markdown.
