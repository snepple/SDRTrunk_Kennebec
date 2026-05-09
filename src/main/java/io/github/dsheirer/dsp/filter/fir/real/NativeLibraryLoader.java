package io.github.dsheirer.dsp.filter.fir.real;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeLibraryLoader {
    private static final Logger log = LoggerFactory.getLogger(NativeLibraryLoader.class);
    private static boolean loaded = false;

    static {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();
            String extension;
            String prefix = "lib";
            String osName;

            if (os.contains("win")) {
                extension = ".dll";
                prefix = "";
                osName = "windows";
            } else if (os.contains("mac")) {
                extension = ".dylib";
                osName = "macos";
            } else {
                extension = ".so";
                osName = "linux";
            }

            String archName;
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                archName = "aarch64";
            } else {
                archName = "x64";
            }

            String libName = prefix + "library-" + osName + "-" + archName + extension;

            java.io.InputStream in = NativeLibraryLoader.class.getResourceAsStream("/native/" + libName);
            if (in == null) {
                throw new UnsatisfiedLinkError("Native library " + libName + " not found in resources.");
            }

            java.io.File temp = java.io.File.createTempFile("library-", extension);
            temp.deleteOnExit();

            java.nio.file.Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.getAbsolutePath());

            loaded = true;
            log.info("Successfully loaded native DSP library from " + temp.getAbsolutePath());
        } catch (Throwable e) {
            log.error("Failed to load native DSP library: " + e.getMessage());
            loaded = false;
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
