package io.github.dsheirer.source.tuner.usb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class USBTunerControllerTest
{
    @Test
    void zeroLengthTransfersDoNotRestartBeforeMinimumDuration()
    {
        assertFalse(USBTunerController.shouldRestartForZeroLengthTransfers(
            USBTunerController.ZERO_LENGTH_TRANSFER_ERROR_THRESHOLD, 15_000L));
    }

    @Test
    void zeroLengthTransfersRestartAfterSustainedNoSampleInterval()
    {
        assertTrue(USBTunerController.shouldRestartForZeroLengthTransfers(
            USBTunerController.ZERO_LENGTH_TRANSFER_ERROR_THRESHOLD,
            USBTunerController.ZERO_LENGTH_TRANSFER_ERROR_MIN_DURATION_MS));
    }

    @Test
    void zeroLengthTransfersNeedMinimumCountAndDuration()
    {
        assertFalse(USBTunerController.shouldRestartForZeroLengthTransfers(
            USBTunerController.ZERO_LENGTH_TRANSFER_ERROR_THRESHOLD - 1,
            USBTunerController.ZERO_LENGTH_TRANSFER_ERROR_MIN_DURATION_MS));
    }
}
