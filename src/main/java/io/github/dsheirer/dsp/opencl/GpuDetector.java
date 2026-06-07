package io.github.dsheirer.dsp.opencl;

import com.aparapi.device.Device;
import com.aparapi.internal.kernel.KernelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpuDetector {
    private static final Logger mLog = LoggerFactory.getLogger(GpuDetector.class);

    /**
     * Queries Aparapi's KernelManager to determine if an OpenCL-compatible GPU is available on the system.
     */
    public static boolean isGpuAvailable() {
        try {
            Device bestDevice = KernelManager.instance().bestDevice();
            if (bestDevice != null && bestDevice.getType() == Device.TYPE.GPU) {
                mLog.info("Hardware Acceleration Detector: Found OpenCL compatible GPU [{}]", bestDevice.getShortDescription());
                return true;
            }
            mLog.info("Hardware Acceleration Detector: No compatible OpenCL GPU found.");
            return false;
        } catch (Exception e) {
            mLog.error("Hardware Acceleration Detector: Error while probing for OpenCL devices", e);
            return false;
        }
    }
}
