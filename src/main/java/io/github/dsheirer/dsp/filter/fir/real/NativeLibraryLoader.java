package io.github.dsheirer.dsp.filter.fir.real;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeLibraryLoader {
    private static final Logger log = LoggerFactory.getLogger(NativeLibraryLoader.class);
    private static boolean loaded = false;

    static {
        try {
            System.loadLibrary("library");
            loaded = true;
            log.info("Successfully loaded native DSP library.");
        } catch (UnsatisfiedLinkError e) {
            log.error("Failed to load native DSP library: " + e.getMessage());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
