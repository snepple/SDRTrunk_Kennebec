import sys
import re

def fix():
    file_path = "src/main/java/io/github/dsheirer/source/tuner/rtl/e4k/E4KTunerEditor.java"
    with open(file_path, "r") as f:
        content = f.read()

    old_code = """                    if(gain == E4KGain.MANUAL)
                    {
                        getMixerGainCombo().setValue(getEmbeddedTuner().getMixerGain(true));
                        getMixerGainCombo().setDisable(!(true));
                        getLNAGainCombo().setValue(getEmbeddedTuner().getLNAGain(true));
                        getLNAGainCombo().setDisable(!(true));
                    }"""

    new_code = """                    if(gain == E4KGain.MANUAL)
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

    if old_code in content:
        content = content.replace(old_code, new_code)
        with open(file_path, "w") as f:
            f.write(content)
        print("Fixed UsbException in E4KTunerEditor.java")
    else:
        print("Could not find the exact code block")

if __name__ == "__main__":
    fix()
