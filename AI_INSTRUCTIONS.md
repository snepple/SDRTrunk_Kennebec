# Kennebec Version Fork - AI Instructions

The following rules must be followed for all future tasks in this repository:

## Task Completion Protocol
Every time you successfully complete a coding task or bug fix, you must update the project version number as your final step before committing or preparing a Pull Request.

## Versioning Format
The version number strictly follows the custom format `K.XX.YYY`.

## Incrementing Logic
You must increment the `YYY` section (the patch number) by exactly one.

## Zero-Padding
The `YYY` section must always remain exactly three digits long, using leading zeros as needed. For example, `K.01.001` becomes `K.01.002`, and `K.01.099` becomes `K.01.100`.

## Temporary Development Files

1. **Dynamic Naming for Concurrency**: Whenever you need to generate, download, or utilize a `versions.png` file during development, you must append the current date and precise time (hours, minutes, and seconds) to the filename. For example, use a format like `versions_20260430_153957.png`. This is critical to prevent file locking or overwriting conflicts when multiple tasks or agents are operating simultaneously.
2. **Strict Cleanup Protocol**: This image is a temporary artifact. As soon as you have finished extracting the necessary information or completing the specific step that required it, you must delete the timestamped versions image from the local filesystem.
3. **Commit Prevention**: Under no circumstances should this temporary versions file be included in a commit or a Pull Request.
