# Hardware & Tuners

SDRTrunk Kennebec receives radio signals through a **Software Defined Radio (SDR)** — a small, inexpensive USB device that turns an ordinary antenna into a wideband receiver. The SDR delivers raw radio spectrum to the application, which then decodes it into the calls you hear. This section covers choosing a tuner, reading the spectrum display, and keeping your hardware running reliably.

## Which tuner should I use?

| Tuner | Best for |
| --- | --- |
| **RTL-SDR** | The most affordable starting point. Great for a single system or trying SDRTrunk for the first time. |
| **Airspy / HackRF** | Wider bandwidth and better sensitivity for monitoring several systems at once. |

If you are unsure, start with an RTL-SDR — it works well for most single-system setups and is easy to replace or add to later.

## In this section

- **Supported Tuners** — the full list of SDR devices SDRTrunk Kennebec can use, and how to pick one.
- **RTL-SDR** — connect and configure an RTL-SDR dongle, including Windows driver setup.
- **Airspy / HackRF** — set up higher-bandwidth Airspy and HackRF tuners.
- **Spectrum & Waterfall** — read the live spectral display to find and confirm signals.
- **Waterfall Audio Controls** — control audio directly from the waterfall view.
- **Waterfall Muting** — quickly mute and unmute audio from the spectral display.
- **Tuner Waterfall Collapse** — reclaim screen space by collapsing the spectrum panel.
- **Tuner Self-Healing** — how SDRTrunk automatically revives a frozen or crashed tuner.

> **Tip:**
> On Windows, most tuners need a one-time driver install using the free **Zadig** tool before the application can see them. See the RTL-SDR guide if your device does not appear in the Tuners list.
