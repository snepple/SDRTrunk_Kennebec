# Using Airspy and HackRF One with SDRTrunk Kennebec

> Setup guide for Airspy Mini, Airspy R2, HackRF One, Jawbreaker, and RAD1O in SDRTrunk Kennebec, covering driver installation and gain configuration.

SDRTrunk Kennebec supports Airspy Mini/R2 and all three HackRF variants (HackRF One, Jawbreaker, and RAD1O) as USB tuners. Both device families communicate through libusb and share the same driver setup requirements as RTL-SDR dongles, but offer wider tuning ranges, higher sample rates, and additional gain controls.

> **Info:**
  Both Airspy and HackRF devices are communicated with using libusb. On Windows, you must install the WinUSB driver via Zadig before SDRTrunk Kennebec can claim the device. On Linux, you may need to add a udev rule.

## Driver installation

  ### Windows

      ### Download Zadig
        Download Zadig from [zadig.akeo.ie](https://zadig.akeo.ie) and run it as administrator.


      ### Select your device
        Open **Options > List All Devices**. Select your Airspy or HackRF device from the dropdown.


      ### Install WinUSB
        Confirm the target driver is **WinUSB**, then click **Replace Driver** (or **Install Driver** for a new device). Wait for the installation to complete.


      ### Reconnect and launch
        Unplug and re-plug the device, then launch SDRTrunk Kennebec. The tuner should appear in the **Tuners** panel.




  ### Linux

      ### Add a udev rule for Airspy
        ```bash theme={null}
        echo 'SUBSYSTEM=="usb", ATTRS{idVendor}=="1d50", MODE="0666"' | sudo tee /etc/udev/rules.d/99-airspy.rules
        sudo udevadm control --reload-rules
        ```


      ### Add a udev rule for HackRF
        ```bash theme={null}
        echo 'SUBSYSTEM=="usb", ATTRS{idVendor}=="1d50", ATTRS{idProduct}=="6089", MODE="0666"' | sudo tee /etc/udev/rules.d/99-hackrf.rules
        sudo udevadm control --reload-rules
        ```


      ### Re-plug and launch
        Unplug and re-plug the device, then start SDRTrunk Kennebec.




  ### macOS
    macOS does not load a conflicting kernel driver for Airspy or HackRF devices. Plug in the device and launch SDRTrunk Kennebec — the tuner manager claims the device through libusb automatically.

    > **Note:**
      On macOS Tahoe (version 26), there is a known compatibility issue with libusb. If detection fails, try installing the HEAD build of libusb via Homebrew: `brew install libusb --HEAD`



***

## Airspy Mini and Airspy R2

The Airspy tuner controller identifies itself with `TunerType.AIRSPY_R820T`. The tunable range is **24 MHz to 1800 MHz** with 90% usable bandwidth at each selected sample rate.

### Sample rate

The Airspy reports its supported sample rates directly from the device firmware. SDRTrunk Kennebec reads these rates at startup and populates the **Sample Rate** dropdown in the tuner editor. The default fallback rate is **10.00 MHz**.

> **Tip:**
  Higher sample rates give you a wider instantaneous view of the spectrum and allow more channels to be decoded simultaneously from a single tuner. Choose the highest rate your CPU can sustain without dropping samples.

### Gain mode

The Airspy tuner editor provides three gain modes, selectable from the **Gain Mode** dropdown:

  ### Linearity

    Optimizes for maximum dynamic range. A single master slider selects from 22 linearity gain steps. This is the recommended starting point for dense signal environments.


  ### Sensitivity

    Optimizes for weak-signal reception. A single master slider selects from 22 sensitivity gain steps.


  ### Custom

    Exposes individual **LNA Gain** (0–14), **Mixer Gain** (0–15), and **IF Gain** (0–15) sliders for manual control.


| Gain control       | Range  | Default |
| ------------------ | ------ | ------- |
| LNA Gain           | 0 – 14 | 7       |
| Mixer Gain         | 0 – 15 | 9       |
| IF Gain            | 0 – 15 | 9       |
| Linearity master   | 1 – 22 | 14      |
| Sensitivity master | 1 – 22 | 10      |

You can also enable or disable **LNA AGC** and **Mixer AGC** using the corresponding checkboxes in the tuner editor. When AGC is enabled for a stage, manual gain control for that stage is bypassed.

***

## HackRF One, Jawbreaker, and RAD1O

The HackRF tuner controller detects the specific board variant by querying the board ID from the firmware, mapping it to the appropriate `TunerType` enum value (`HACKRF_ONE`, `HACKRF_JAWBREAKER`, or `HACKRF_RAD1O`). All three share the same tuner editor and configuration options.

The tunable range is **10 MHz to 6000 MHz** with 90% usable bandwidth.

### Sample rate

Select a sample rate from the **Sample Rate** dropdown. The available rates and their associated baseband filters are:

| Label                     | Rate       | Baseband filter |
| ------------------------- | ---------- | --------------- |
| 1.750 MHz                 | 1,750,000  | 1.75 MHz        |
| 2.500 MHz                 | 2,500,000  | 2.50 MHz        |
| 3.500 MHz                 | 3,500,000  | 3.50 MHz        |
| **5.000 MHz** *(default)* | 5,000,000  | 5.00 MHz        |
| 5.500 MHz                 | 5,500,000  | 5.50 MHz        |
| 6.000 MHz                 | 6,000,000  | 6.00 MHz        |
| 7.000 MHz                 | 7,000,000  | 7.00 MHz        |
| 8.000 MHz                 | 8,000,000  | 8.00 MHz        |
| 9.000 MHz                 | 9,000,000  | 9.00 MHz        |
| 10.000 MHz                | 10,000,000 | 10.00 MHz       |
| 12.000 MHz                | 12,000,000 | 12.00 MHz       |
| 14.000 MHz                | 14,000,000 | 14.00 MHz       |
| 15.000 MHz                | 15,000,000 | 15.00 MHz       |
| 20.000 MHz                | 20,000,000 | 20.00 MHz       |

### LNA and VGA gain

The HackRF tuner editor exposes separate **LNA Gain** and **VGA Gain** dropdowns. Adjust these to balance sensitivity against overload for your signal environment.

### Amplifier

The HackRF One includes an internal RF amplifier that you can toggle on or off with the **Amplifier** button in the tuner editor. The amplifier applies approximately 11 dB of gain before the LNA stage.

> **Warning:**
  Enabling the internal amplifier on strong local signals can cause overload and intermodulation. Start with the amplifier off and enable it only if you need additional gain for weak signals.

***

## Troubleshooting

  ### The tuner shows Error or does not appear at startup
    Verify the WinUSB driver is installed (Windows) or udev rules are applied (Linux). If the device was recently plugged in, try unplugging and re-plugging it. Check the application log for the specific error message — "access denied" indicates a driver or permissions issue, while "in use by another application" means another process has claimed the device.


  ### Sample rates are not populating for Airspy
    SDRTrunk Kennebec queries supported sample rates directly from the Airspy firmware during startup. If the device fails to enumerate fully, the dropdown may remain empty. Unplug the device, wait a few seconds, and re-plug it before launching the application.


  ### The tuner shows status Recovering
    The application detected a USB transfer error and is automatically attempting to restart the tuner. See [Tuner self-healing and automatic reset](/hardware/tuner-self-healing) for details on the recovery process.