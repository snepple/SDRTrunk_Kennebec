1. **Modify AirspyTunerEditor.java**: I will use `run_in_bash_session` to run a Python script that replaces the LNA layout in `src/main/java/io/github/dsheirer/source/tuner/airspy/AirspyTunerEditor.java`.
   It will replace:
   ```java
        add(getLNAAGCCheckBox());
        add(getLNAGainSlider());
        add(getLNAGainValueLabel());
   ```
   with:
   ```java
        add(getLNAAGCCheckBox(), "split 2");
        JButton lnaHelp = createHelpIcon("?");
        lnaHelp.setToolTipText("<html><b>LNA Gain:</b> The power of the signal amplifier.<br>Increase this for distant signals, but lower it if you see a lot of static/noise.</html>");
        add(lnaHelp);
        add(getLNAGainSlider());
        add(getLNAGainValueLabel());
   ```
   And add the `createHelpIcon` method at the bottom of the class:
   ```java
    protected JButton createHelpIcon(String text) {
        JButton button = new JButton(text);
        button.setMargin(new java.awt.Insets(0, 4, 0, 4));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        return button;
    }
}
   ```
   *Verification*: I will run `grep -A 10 "getLNAAGCCheckBox(), \"split 2\"" src/main/java/io/github/dsheirer/source/tuner/airspy/AirspyTunerEditor.java` to verify.

2. **Log to Journal**: I will use `run_in_bash_session` to write the following to `.Jules/guide.md`:
   ```markdown
   ## LNA Gain
   **Technical:** Low Noise Amplifier power coefficient.
   **Simplified:** The power of the signal amplifier. Increase this for distant signals, but lower it if you see a lot of static/noise.
   ```
   *Verification*: I will run `cat .Jules/guide.md` to verify.

3. **Bump version**: I will use `run_in_bash_session` with `sed` to increment the version in `gradle.properties`.
   *Verification*: I will run `grep -i "projectversion" gradle.properties` to verify the bump.

4. **Compile**: I will use `run_in_bash_session` to run `./gradlew classes` to verify that the code compiles.

5. **Pre-commit**: Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.

6. **Submit PR**: I will use `run_in_bash_session` to run:
   ```bash
   git checkout -b guide-lna-gain
   git add src/main/java/io/github/dsheirer/source/tuner/airspy/AirspyTunerEditor.java
   git add .Jules/guide.md
   git add gradle.properties
   git commit -m "🧭 Guide: Added LNA Gain tooltip to Airspy tuner editor"
   ```
   Then I will call `submit` with branch `guide-lna-gain`, title "🧭 Guide: Added LNA Gain tooltip to Airspy tuner editor", and description containing "What: Added LNA Gain tooltip in AirspyTunerEditor. Why: Helps users understand what LNA Gain does and how to adjust it."
