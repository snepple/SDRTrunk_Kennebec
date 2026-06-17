# Airspy Mini, R2, HF+ and HackRF Setup in SDRTrunk Kennebec

SDRTrunk Kennebec supports the Airspy Mini, Airspy R2, Airspy HF+, HackRF One, HackRF Jawbreaker, and HackRF RAD1O as USB tuners. All of these devices communicate through libusb and share the same driver prerequisites as RTL-SDR dongles, but offer wider tuning ranges, higher maximum sample rates, and more granular gain controls than a typical RTL2832 dongle. Both families are detected automatically at startup once the driver is in place.

> **Note:**
>
Both Airspy and HackRF devices use libusb for communication. On Windows you must install the WinUSB driver via Zadig. On Linux you must add a udev rule to grant non-root access. macOS requires no driver changes.

## Driver installation

### Windows

  **1. Download Zadig**

    Download Zadig from [zadig.akeo.ie](https://zadig.akeo.ie) and run it as administrator.

  **2. Select your device**

    Open **Options > List All Devices** and select your Airspy or HackRF device from the dropdown.

  **3. Install WinUSB**

    Confirm the target driver shown is **WinUSB**, then click **Replace Driver** (or **Install Driver** for a new device). Wait for the installation to complete.

  **4. Reconnect and launch**

    Unplug and re-plug the device, then start SDRTrunk Kennebec. The tuner should appear in the **Tuners** panel.

### Linux

  **1. Add a udev rule for Airspy**

    ```bash
    echo 'SUBSYSTEM=="usb", ATTRS{idVendor}=="1d50", MODE="0666"' | sudo tee /etc/udev/rules.d/99-airspy.rules
    sudo udevadm control --reload-rules
    ```

  **2. Add a udev rule for HackRF**

    ```bash
    echo 'SUBSYSTEM=="usb", ATTRS{idVendor}=="1d50", ATTRS{idProduct}=="6089", MODE="0666"' | sudo tee /etc/udev/rules.d/99-hackrf.rules
    sudo udevadm control --reload-rules
    ```

  **3. Re-plug and launch**

    Unplug and re-plug the device, then start SDRTrunk Kennebec. The tuner should appear in the **Tuners** panel with status `Enabled`.

### macOS

macOS does not load a conflicting kernel driver for Airspy or HackRF devices. Plug in the device and launch SDRTrunk Kennebec — the tuner manager claims the device through libusb automatically.

> **Note:**
>
  On macOS Tahoe (version 26), there is a known compatibility issue with libusb. If device detection fails, install the HEAD build of libusb via Homebrew:

  ```bash
  brew install libusb --HEAD
  ```

---

## Airspy Mini and Airspy R2

The Airspy Mini and R2 both use the R820T tuner IC and are identified in SDRTrunk Kennebec with the `TunerType` value `AIRSPY_R820T`. The tunable range is **24 MHz to 1800 MHz** with 90% usable bandwidth at each selected sample rate. Compared to RTL-SDR dongles, the Airspy line offers significantly higher dynamic range and more flexible gain control, making it well-suited to congested frequency environments.

### Sample rate

The Airspy reports its supported sample rates directly from the device firmware. SDRTrunk Kennebec reads these rates at startup and populates the **Sample Rate** dropdown in the tuner editor. The default fallback rate is **10.00 MHz**.

> **Tip:**
>
Higher sample rates give you a wider instantaneous view of the spectrum and allow more channels to be decoded simultaneously from a single tuner. Choose the highest rate your CPU can sustain without dropping samples.

### Gain mode

The Airspy tuner editor provides three gain modes from the **Gain Mode** dropdown. Each mode targets a different use case.

### Linearity

Optimizes for maximum dynamic range. A single master slider selects from 22 linearity gain steps. This is the recommended starting point for dense signal environments where strong and weak signals coexist.

### Sensitivity

Optimizes for weak-signal reception. A single master slider selects from 22 sensitivity gain steps. Use this when you need to pull in marginal signals at the expense of some dynamic range.

### Custom

Exposes individual **LNA Gain**, **Mixer Gain**, and **IF Gain** sliders so you can tune each stage independently for your specific environment.

The table below summarizes the range and default for each gain control:

| Gain control | Range | Default |
|---|---|---|
| LNA Gain | 0 – 14 | 7 |
| Mixer Gain | 0 – 15 | 9 |
| IF Gain | 0 – 15 | 9 |
| Linearity master | 1 – 22 | 14 |
| Sensitivity master | 1 – 22 | 10 |

You can also enable **LNA AGC** and **Mixer AGC** using the checkboxes in the tuner editor. When AGC is enabled for a stage, the manual gain slider for that stage is bypassed.

---

## Airspy HF+

The Airspy HF+ is identified with `TunerType` value `AIRSPY_HF_PLUS`. It is designed for high-performance reception in the HF and VHF spectrum and uses USB for communication, so the same Zadig/udev driver setup applies. Consult the Airspy HF+ documentation for its specific tunable frequency range and hardware gain settings, as these differ from the R820T-based Mini and R2.

---

## HackRF One, Jawbreaker, and RAD1O

SDRTrunk Kennebec detects the specific HackRF board variant by querying the board ID from the firmware and maps it to the appropriate `TunerType` value — `HACKRF_ONE`, `HACKRF_JAWBREAKER`, or `HACKRF_RAD1O`. All three variants share the same tuner editor and configuration options in SDRTrunk Kennebec.

The tunable range is **10 MHz to 6000 MHz** with 90% usable bandwidth, making HackRF the broadest-coverage option of the supported tuner families.

### Sample rate

Select a sample rate from the **Sample Rate** dropdown. Each rate is paired with a matching baseband filter bandwidth.

| Label | Rate | Baseband filter |
|---|---|---|
| 1.750 MHz | 1,750,000 | 1.75 MHz |
| 2.500 MHz | 2,500,000 | 2.50 MHz |
| 3.500 MHz | 3,500,000 | 3.50 MHz |
| **5.000 MHz** *(default)* | 5,000,000 | 5.00 MHz |
| 5.500 MHz | 5,500,000 | 5.50 MHz |
| 6.000 MHz | 6,000,000 | 6.00 MHz |
| 7.000 MHz | 7,000,000 | 7.00 MHz |
| 8.000 MHz | 8,000,000 | 8.00 MHz |
| 9.000 MHz | 9,000,000 | 9.00 MHz |
| 10.000 MHz | 10,000,000 | 10.00 MHz |
| 12.000 MHz | 12,000,000 | 12.00 MHz |
| 14.000 MHz | 14,000,000 | 14.00 MHz |
| 15.000 MHz | 15,000,000 | 15.00 MHz |
| 20.000 MHz | 20,000,000 | 20.00 MHz |

### LNA and VGA gain

The HackRF tuner editor exposes separate **LNA Gain** and **VGA Gain** dropdowns. Adjust these to balance sensitivity against overload for your signal environment, just as you would with an RTL-SDR dongle.

### Internal amplifier

The HackRF One includes an internal RF amplifier that you can toggle with the **Amplifier** button in the tuner editor. The amplifier applies approximately 11 dB of gain before the LNA stage.

> **Warning:**
>
Enabling the internal amplifier on strong local signals can cause overload and intermodulation distortion. Start with the amplifier off and enable it only when you need additional gain for weak signals.

---

## Troubleshooting

### Tuner shows `Error` or does not appear at startup

Verify the WinUSB driver is installed (Windows) or the udev rules are applied (Linux). If the device was recently plugged in, try unplugging and re-plugging it. Check the application log for the specific error message — "access denied" indicates a driver or permissions issue, while "in use by another application" means another process has already claimed the device.

### Sample rates are not populating for Airspy

SDRTrunk Kennebec queries supported sample rates from the Airspy firmware during startup. If the device fails to enumerate fully, the dropdown may remain empty. Unplug the device, wait a few seconds, re-plug it, then launch the application again.

### Tuner shows status `Recovering`

SDRTrunk Kennebec detected a USB transfer error and is automatically attempting to restart the tuner. See [Tuner Self-Healing](/hardware/tuner-self-healing) for full details on the recovery process and what to do if the tuner does not recover.
