# Kennebec Version Fork - AI Instructions

The following rules must be followed for all future tasks in this repository:

## Task Approval and Completion
You must make it clear that you will implement the recommended changes if approved by the user. Once approved and all coding is complete, the version update must be the absolute final step executed immediately before preparing the Pull Request.

## Just-In-Time Version Check
Do not check the version at the beginning of the task. Immediately before creating the commit and PR, you must fetch and inspect the `gradle.properties` or `build.gradle` file from the `master` branch to determine the most currently accepted, baseline version.

## Versioning Format
The version number strictly follows the custom format `K.XX.YYY`.

## Format Validation
If the baseline version found in the `master` branch does not match the `K.XX.YYY` format, halt the update process and explicitly ask the user for the correct current version.

## Incrementing Logic
If valid, take this newly fetched baseline version from master and increment the `YYY` section (the patch number) by exactly one.

## Zero-Padding
The `YYY` section must always remain exactly three digits long, using leading zeros as needed (e.g., `K.01.099` becomes `K.01.100`). Apply this new version to your current working branch.

## Temporary Development Files

1. **Dynamic Naming for Concurrency**: Whenever you need to generate, download, or utilize a `versions.png` file during development, you must append the current date and precise time (hours, minutes, and seconds) to the filename. For example, use a format like `versions_20260430_153957.png`. This is critical to prevent file locking or overwriting conflicts when multiple tasks or agents are operating simultaneously.
2. **Strict Cleanup Protocol**: This image is a temporary artifact. As soon as you have finished extracting the necessary information or completing the specific step that required it, you must delete the timestamped versions image from the local filesystem.
3. **Commit Prevention**: Under no circumstances should this temporary versions file be included in a commit or a Pull Request.
