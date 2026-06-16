import sys

def fix():
    file_path = "src/main/java/io/github/dsheirer/source/tuner/rtl/e4k/E4KTunerEditor.java"
    with open(file_path, "r") as f:
        content = f.read()

    # The exception class used in this project is usually something else, but since the instructions said:
    # "In SDRTrunk, local builds (`./gradlew compileJava` or `./gradlew test`) may fail on unrelated files due to pre-existing errors (e.g., `KennebecFeatureTest.java` constructor mismatches, or `E4KTunerEditor.java` throwing `UsbException`). If your task strictly involves unrelated modifications (e.g., UI updates), safely document these pre-existing failures in the PR description and proceed with submission."
    # Let's revert my attempts to fix it and let it fail.

    old_code = """                    if(gain == E4KGain.MANUAL)
                    {
                        try {
                            getMixerGainCombo().setValue(getEmbeddedTuner().getMixerGain(true));
                            getMixerGainCombo().setDisable(!(true));
                            getLNAGainCombo().setValue(getEmbeddedTuner().getLNAGain(true));
                            getLNAGainCombo().setDisable(!(true));
                        } catch (io.github.dsheirer.usb.UsbException e) {
                            mLog.error("Error getting gain from tuner", e);
                        }
                    }"""

    new_code = """                    if(gain == E4KGain.MANUAL)
                    {
                        getMixerGainCombo().setValue(getEmbeddedTuner().getMixerGain(true));
                        getMixerGainCombo().setDisable(!(true));
                        getLNAGainCombo().setValue(getEmbeddedTuner().getLNAGain(true));
                        getLNAGainCombo().setDisable(!(true));
                    }"""

    if old_code in content:
        content = content.replace(old_code, new_code)
        with open(file_path, "w") as f:
            f.write(content)
        print("Reverted UsbException in E4KTunerEditor.java")

if __name__ == "__main__":
    fix()
