# Connect and Configure an RTL-SDR Tuner in SDRTrunk Kennebec

RTL-SDR dongles built on the RTL2832U chip are the most widely used SDR hardware with SDRTrunk Kennebec. Each dongle contains an embedded tuner IC — typically a Rafael Micro R820T or R828D, an Elonics E4000, or a Fitipower FC0012/FC0013 — that the application identifies automatically at startup. Once the correct driver is in place and the device is plugged in, SDRTrunk Kennebec detects and configures the tuner without any further steps from you.

## Driver setup

SDRTrunk Kennebec communicates with RTL-SDR dongles using **libusb**, not the default DVB-T driver that ships with most operating systems. You must replace or bypass the default driver on your platform before the application can claim the device.

<Tabs>
  <Tab title="Windows">
    Windows ships a generic DVB-T driver for RTL2832 devices that blocks libusb access. Use **Zadig** to replace it with the WinUSB driver.


      **1. Download and run Zadig**

        Download Zadig from [zadig.akeo.ie](https://zadig.akeo.ie) and run it as administrator.

      **2. Select your RTL-SDR device**

        Open the **Options** menu and enable **List All Devices**. Select your RTL-SDR dongle from the dropdown — the device name often starts with "Bulk-In" or "RTL2838".

      **3. Install the WinUSB driver**

        Confirm the target driver shown is **WinUSB**, then click **Replace Driver** (or **Install Driver** on a first install). Wait for the installation to complete.

      **4. Reconnect and launch**

        Unplug and re-plug the dongle, then start SDRTrunk Kennebec. The tuner should appear in the **Tuners** panel with status `Enabled`.



    > [!WARNING]
>
      Replacing the driver with Zadig removes the ability to use the dongle as a DVB-T receiver in other software. You can re-run Zadig to restore the original driver if needed.

  </Tab>
  <Tab title="Linux">
    Linux loads the `dvb_usb_rtl28xxu` kernel module automatically for RTL2832 devices, which conflicts with libusb. Blacklist the module and add a udev rule to grant non-root access.


      **5. Blacklist the DVB kernel module**

        Create a blacklist file to prevent the DVB driver from loading at boot:

        ```bash
        echo "blacklist dvb_usb_rtl28xxu" | sudo tee /etc/modprobe.d/rtlsdr.conf
        ```

      **6. Add a udev rule**

        Create a udev rule so SDRTrunk Kennebec can open the device without root privileges:

        ```bash
        echo 'SUBSYSTEM=="usb", ATTRS{idVendor}=="0bda", MODE="0666"' | sudo tee /etc/udev/rules.d/99-rtlsdr.rules
        sudo udevadm control --reload-rules
        ```

      **7. Reload the module or reboot**

        Unload the conflicting module and re-plug the dongle, or reboot to apply both changes:

        ```bash
        sudo modprobe -r dvb_usb_rtl28xxu
        ```

      **8. Launch SDRTrunk Kennebec**

        Start the application. The tuner should appear in the **Tuners** panel with status `Enabled`.


  </Tab>
  <Tab title="macOS">
    macOS does not load a conflicting kernel driver for RTL2832 devices. Plug in the dongle and launch SDRTrunk Kennebec — the tuner manager claims the device through libusb directly, with no driver installation required.

    > [!NOTE]
>
      On macOS Tahoe (version 26), there is a known compatibility issue between libusb and the operating system. If USB tuner detection fails on Tahoe, install the latest HEAD build of libusb via Homebrew:

      ```bash
      brew install libusb --HEAD
      ```

  </Tab>
</Tabs>

## How auto-detection works

When SDRTrunk Kennebec starts, the tuner manager enumerates all USB devices and compares each device's vendor ID (VID) and product ID (PID) against its list of known RTL2832 tuner types. Matching devices start automatically.

During initialization, the application performs a test communication with the device. If the test fails — indicating the device may need a USB reset — it issues a reset and retries before continuing. It then reads the device EEPROM for the serial number and identifies the embedded tuner IC to determine which configuration options to display.

> [!NOTE]
>
Hot-plug is supported on platforms where libusb provides that capability. Plugging in an RTL-SDR dongle while SDRTrunk Kennebec is already running adds it to the tuner list automatically — no restart required.

## Configuration options

Open the tuner editor by clicking the tuner name in the **Tuners** panel. The options available depend on which embedded tuner IC is inside your dongle.

### Sample rate

The default sample rate for RTL2832-based devices is **2.400 MHz**. Select a different rate from the **Sample Rate** dropdown in the tuner editor.

| Label | Rate |
|---|---|
| 0.230 MHz | 230,400 samples/sec |
| 0.240 MHz | 240,000 samples/sec |
| 0.256 MHz | 256,000 samples/sec |
| 0.288 MHz | 288,000 samples/sec |
| 0.300 MHz | 300,000 samples/sec |
| 0.960 MHz | 960,000 samples/sec |
| 1.024 MHz | 1,024,000 samples/sec |
| 1.200 MHz | 1,200,000 samples/sec |
| 1.440 MHz | 1,440,000 samples/sec |
| 1.600 MHz | 1,600,000 samples/sec |
| 1.800 MHz | 1,800,000 samples/sec |
| 1.920 MHz | 1,920,000 samples/sec |
| 2.048 MHz | 2,048,000 samples/sec |
| 2.304 MHz | 2,304,000 samples/sec |
| **2.400 MHz** *(default)* | 2,400,000 samples/sec |
| 2.560 MHz | 2,560,000 samples/sec |
| 2.880 MHz | 2,880,000 samples/sec |

> [!WARNING]
>
The sample rate is locked while any decoding channels are assigned to the tuner. Disable or remove all channels before changing the sample rate.

### Tuner gain

For R820T/R828D-based dongles, the tuner editor exposes separate **LNA Gain** and **Mixer Gain** dropdowns. Adjust these to find a level that avoids receiver overload while maintaining adequate sensitivity for your signal environment.

### PPM frequency correction

Clock crystals in RTL-SDR dongles vary slightly from their nominal frequency. Use the **Frequency Correction (PPM)** spinner in the tuner editor to enter a static correction value in parts per million.

You can also enable **Auto PPM Correction**, which allows SDRTrunk Kennebec to apply dynamic frequency error correction automatically based on decoded signals.

> [!TIP]
>
To find the correct PPM value for your dongle, tune to a known-accurate reference signal — such as a strong broadcast FM station — and adjust the PPM spinner until the signal centers correctly in the spectrum display.

### Bias-T

R820T/R828D-based dongles that include a bias-T circuit expose a **Bias-T** toggle in the tuner editor. Enabling this applies a DC voltage to the antenna connector to power active antenna preamplifiers or inline LNA modules.

> [!WARNING]
>
Only enable Bias-T if your antenna or feed line is designed to carry DC voltage. Enabling Bias-T on passive antennas or unprotected equipment may cause damage.

## Troubleshooting

### Tuner appears with status `Error`

Check the application log for the specific error message. Common causes include:

- **Access denied** — The WinUSB driver is not installed (Windows) or the udev rules are not in place (Linux). Re-run Zadig or add the udev rule as described above.
- **Device in use by another application** — Close any other SDR software (such as SDR#, GQRX, or CubicSDR) that may have claimed the device.
- **Unable to reset USB device** — The device failed the startup reset. Unplug the dongle, wait a few seconds, and re-plug it.

### Tuner shows status `Recovering`

SDRTrunk Kennebec detected a USB transfer error and is attempting to restart the tuner automatically. The application makes up to 5 recovery attempts at 3-minute intervals before marking the tuner as permanently failed. See [Tuner Self-Healing](/hardware/tuner-self-healing) for full details.

### Audio is distorted or channels fail to decode

Input overload is the most common cause. Lower the **LNA Gain** or **Mixer Gain**, or add an RF attenuator between the antenna and the dongle. Also confirm that your PPM correction value is accurate for your specific device.
