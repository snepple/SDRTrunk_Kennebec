# Kennebec Version Fork - AI Instructions

The following rules must be followed for all future tasks in this repository:

## Task Completion Protocol
Every time you successfully complete a coding task or bug fix, you must make it clear that you will implement the recommended changes if approved by the user. Once approved by the user, your final step before committing or preparing a Pull Request must be to update the project version number in the Gradle configuration.

## Versioning Format
The version number strictly follows the custom format `K.XX.YYY`.

## Source of Truth
Do not simply increment the version found in your current working branch. Before updating the version, you must first inspect the `build.gradle` or `gradle.properties` file from the `master` branch to determine the most currently accepted, baseline version.

## Format Validation
If the baseline version found in the `master` branch does not match the `K.XX.YYY` format, you must halt the version update process and explicitly ask the user to provide the correct current version. Do not attempt to guess or auto-correct an invalid format.

## Incrementing Logic
If the format is valid, take the baseline version from the `master` branch and increment the `YYY` section (the patch number) by exactly one. Apply this new version to your current working branch.

## Zero-Padding
The `YYY` section must always remain exactly three digits long, using leading zeros as needed. For example, if the `master` branch is at `K.01.001`, your branch becomes `K.01.002`. If `master` is at `K.01.099`, your branch becomes `K.01.100`.
