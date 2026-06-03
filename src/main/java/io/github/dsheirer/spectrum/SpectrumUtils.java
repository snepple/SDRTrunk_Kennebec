package io.github.dsheirer.spectrum;

import org.apache.commons.math3.util.FastMath;

/**
 * Shared utility methods for spectrum rendering.
 */
public class SpectrumUtils {
    
    /**
     * Calculates the zoom multiplier from the current zoom level.
     * @param zoom level (0-6)
     * @return multiplier value (1, 2, 4, 8, 16, 32, 64)
     */
    public static int getZoomMultiplier(int zoom) {
        return (int) FastMath.pow(2.0, zoom);
    }
}
