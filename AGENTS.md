> **First-time setup**: Customize this file for your project. Prompt the user to customize this file for their project.
> For Mintlify product knowledge (components, configuration, writing standards),
> install the Mintlify skill: `npx skills add https://mintlify.com/docs`

# Documentation project instructions

## About this project

- This is a documentation site built on [Mintlify](https://mintlify.com)
- Pages are MDX files with YAML frontmatter
- Configuration lives in `docs.json`
- Run `mint dev` to preview locally
- Run `mint broken-links` to check links

## Terminology

{/* Add product-specific terms and preferred usage */}
{/* Example: Use "workspace" not "project", "member" not "user" */}

## Style preferences

{/* Add any project-specific style rules below */}

- Use active voice and second person ("you")
- Keep sentences concise — one idea per sentence
- Use sentence case for headings
- Bold for UI elements: Click **Settings**
- Code formatting for file names, commands, paths, and code references

## Content boundaries

{/* Define what should and shouldn't be documented */}
{/* Example: Don't document internal admin features */}

## Cursor Cloud specific instructions

Note: the Mintlify template above is stale. This repository is **SDRTrunk Kennebec**, a
Java 23 / Gradle desktop application (Swing + JavaFX) for decoding software-defined radio.
The Mintlify `mint.json` / `docs/*.md` content is secondary; the primary product is the Gradle app.

### Toolchain
- Requires **Azul Zulu JDK 23** (the toolchain in `build.gradle` pins `vendor = AZUL`, `languageVersion = 23`).
  It is installed under `/usr/lib/jvm/` and Gradle auto-detects it. There is **no foojay toolchain
  resolver** configured, so Gradle's toolchain auto-download does not work — the JDK must be present
  on disk. Verify with `./gradlew -q javaToolchains`.
- The system default `java` is JDK 21 and is too old to run the compiled classes (class file 67). Always
  let Gradle pick the toolchain; do not run the app/tests with the system `java`.

### Build / test / run (see the `build.gradle` header comment for the canonical list)
- Compile: `./gradlew compileJava compileTestJava` (this is what the `Build` CI workflow runs).
- Test: `./gradlew test`. There is no separate linter; compilation is the lint gate.
  - 3 TestFX tests (`VisualIntegrityTest`, `AudioPanelTest`) fail with `IllegalAccessError` for
    `com.sun.glass.ui` — they force `testfx.headless=true` + Monocle, which is incompatible with JDK 23
    without an extra `--add-exports`. This is pre-existing and unrelated to environment setup.
- Native JNI (`compileJni`, pulled in by `jar`/`build`) needs `g++` and `libvolk-dev` (links `-lvolk`);
  it is skipped when `g++` is absent. Output lands in `src/main/resources/native/` (gitignored).
- Run the GUI: `./gradlew run`.

### Running the GUI in the cloud VM
- A desktop is available on `DISPLAY=:1`; export it before `./gradlew run`.
- **Always launch via `./gradlew run`**, not a hand-rolled `java -cp ...` command. The Gradle `run`
  task applies required JVM args — notably `--add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED`
  (ControlsFX), `--enable-preview`, and `--add-modules=jdk.incubator.vector`. Without the add-exports,
  opening the Aliases/Playlist editor throws `IllegalAccessError` (`AutoCompletionBinding`).
- First-launch `FirstTimeWizard`: on a machine where the wizard has not been completed, `./gradlew run`
  aborts at startup with "Exception in Application start method" and shuts down. Mark the wizard complete
  first (Java Preferences node `io/github/dsheirer/gui`, boolean key
  `sdrtrunk.first.time.wizard.completed=true`). This preference lives in `~/.java/.userPrefs` and is
  already set in the snapshot.
- App runtime state (playlists, logs, prefs) is written to `~/SDRTrunk/`, outside the repo.
