# GPU Acceleration (Aparapi OpenCL)

SDRTrunk Kennebec features experimental support for hardware-accelerated digital signal processing by offloading complex mathematical operations—specifically Fast Fourier Transforms (FFT) and magnitude calculations—to your Graphics Processing Unit (GPU).

This feature is unique to the Kennebec fork and utilizes **Aparapi** (A PARallel API) to dynamically translate Java bytecode into OpenCL kernels at runtime.

## What it does

In modern SDR applications, decoding multiple high-bandwidth digital radio channels (such as P25 Phase II or DMR) simultaneously requires massive amounts of parallel math. While modern CPUs handle this well using SIMD Vector instructions, maxing out your CPU can lead to audio stuttering or dropped frames.

By enabling GPU acceleration:
* **FFT Math is Offloaded:** The heavy lifting for spectral processing is moved to your graphics card.
* **CPU Relief:** Frees up your CPU to handle higher-level tasks like audio routing, JMBE decoding, UI rendering, and AI transcription.
* **Higher Channel Count:** Allows you to monitor significantly more concurrent channels on the same machine without bogging down the system.

## Supported Hardware

Because SDRTrunk Kennebec uses OpenCL rather than proprietary APIs like NVIDIA CUDA, the feature is highly hardware-agnostic. 

You need a GPU that supports **OpenCL 1.2 or higher**. This includes almost all hardware released in the last decade:

* **AMD Radeon:** Any dedicated RX-series desktop GPU, or modern Ryzen APUs with integrated Radeon graphics.
* **NVIDIA:** Any dedicated GPU from the GTX 900-series onward (including all RTX cards).
* **Intel:** Intel Iris Xe, Intel Arc dedicated GPUs, and Intel UHD integrated graphics.

*Note: You do not need a high-end gaming GPU. Entry-level dedicated cards or robust integrated graphics are more than capable of handling the FFT workload.*

## How to Enable GPU Acceleration

1. Open SDRTrunk Kennebec and navigate to the **User Preferences** menu.
2. Select the **Advanced** tab.
3. Locate the **Hardware Acceleration (OpenCL)** section.
4. Check the box to **Enable GPU FFT Processing**.
5. Restart SDRTrunk Kennebec.

Upon restart, you can verify that the GPU was successfully detected by checking your application logs. You should see a startup message similar to:
`Hardware Acceleration Detector: Found OpenCL compatible GPU [Your GPU Name]`

## Troubleshooting

If you experience issues, graphical glitches in the waterfall, or complete decoder failure after enabling the feature, your OpenCL drivers may be out of date.
* Ensure you have installed the latest stable graphics drivers from AMD, NVIDIA, or Intel.
* If the application crashes upon startup with GPU acceleration enabled, you can manually disable it by opening the `SDRTrunk.properties` file located in your `SDRTrunk` user directory and setting `gpu_acceleration_enabled=false`.
