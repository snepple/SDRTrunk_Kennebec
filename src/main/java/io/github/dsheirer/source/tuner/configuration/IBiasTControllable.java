package io.github.dsheirer.source.tuner.configuration;

/**
 * Interface for tuner configurations and controllers that support Bias-T control.
 */
public interface IBiasTControllable {
    
    /**
     * Indicates if Bias-T power is currently enabled.
     * @return true if enabled, false otherwise
     */
    boolean isBiasT();

    /**
     * Sets the Bias-T power state.
     * @param enabled true to enable Bias-T power, false to disable
     */
    void setBiasT(boolean enabled);
}
