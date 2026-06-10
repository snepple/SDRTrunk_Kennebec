/*
 * Comprehensive tests for DSP and Spectrum subsystems.
 * Covers: ComplexDftProcessor, WaterfallColorModel, SpectrumUtils, and Smoothing Filters.
 */
package io.github.dsheirer.spectrum;

import io.github.dsheirer.dsp.filter.smoothing.GaussianSmoothingFilter;
import io.github.dsheirer.dsp.filter.smoothing.NoSmoothingFilter;
import io.github.dsheirer.dsp.filter.smoothing.RectangularSmoothingFilter;
import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter;
import io.github.dsheirer.dsp.filter.smoothing.TriangularSmoothingFilter;
import io.github.dsheirer.spectrum.converter.DFTResultsConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the DSP and Spectrum subsystems.
 */
@DisplayName("Spectrum Subsystem Tests")
class SpectrumSubsystemTest
{
    // ========================================================================================
    // ComplexDftProcessor Tests
    // ========================================================================================

    @Nested
    @DisplayName("ComplexDftProcessor Tests")
    class ComplexDftProcessorTests
    {
        @Test
        @DisplayName("Should start and stop without errors")
        void testStartAndStop()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                assertTrue(processor.isRunning(), "Processor should be running after construction");
                processor.stop();
                assertFalse(processor.isRunning(), "Processor should not be running after stop()");
                processor.start();
                assertTrue(processor.isRunning(), "Processor should be running after start()");
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Should not be running after dispose")
        void testDisposeStopsProcessor()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            assertTrue(processor.isRunning(), "Processor should be running after construction");
            processor.dispose();
            assertFalse(processor.isRunning(), "Processor should not be running after dispose()");
        }

        @Test
        @DisplayName("Should handle restart correctly")
        void testRestart()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                assertTrue(processor.isRunning());
                processor.restart();
                assertTrue(processor.isRunning(), "Processor should be running after restart()");
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Should have default DFT size of 4096")
        void testDefaultDftSize()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                assertEquals(DFTSize.FFT04096, processor.getDFTSize(),
                    "Default DFT size should be FFT04096");
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Should accept DFT size change request")
        void testSetDftSize()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                processor.setDFTSize(DFTSize.FFT08192);
                // The setDFTSize queues the change; getDFTSize returns the current (old) size
                // until the executor picks it up. This is by design.
                // We verify at minimum that setDFTSize doesn't throw.
                assertNotNull(processor.getDFTSize(), "DFT size should not be null");
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Should accept all DFTSize enum values")
        void testSetAllDftSizes()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                for(DFTSize size : DFTSize.values())
                {
                    assertDoesNotThrow(() -> processor.setDFTSize(size),
                        "Setting DFT size to " + size + " should not throw");
                }
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Should register DFTResultsConverter without errors")
        void testAddConverter()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                // Create a minimal concrete DFTResultsConverter for testing
                DFTResultsConverter converter = new DFTResultsConverter()
                {
                    @Override
                    public void receive(float[] results)
                    {
                        // no-op for test
                    }
                };

                assertDoesNotThrow(() -> processor.addConverter(converter),
                    "Adding a converter should not throw");
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Should register multiple converters without errors")
        void testAddMultipleConverters()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                for(int i = 0; i < 5; i++)
                {
                    DFTResultsConverter converter = new DFTResultsConverter()
                    {
                        @Override
                        public void receive(float[] results)
                        {
                        }
                    };
                    assertDoesNotThrow(() -> processor.addConverter(converter));
                }
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Should return positive frame rate")
        void testGetFrameRate()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                assertTrue(processor.getFrameRate() > 0,
                    "Frame rate should be positive, got: " + processor.getFrameRate());
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Should clear buffer without errors")
        void testClearBuffer()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                assertDoesNotThrow(processor::clearBuffer,
                    "clearBuffer() should not throw");
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Multiple stop() calls should not throw")
        void testMultipleStopCalls()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                processor.stop();
                assertDoesNotThrow(processor::stop,
                    "Calling stop() on a stopped processor should not throw");
            }
            finally
            {
                processor.dispose();
            }
        }

        @Test
        @DisplayName("Multiple start() calls should not throw")
        void testMultipleStartCalls()
        {
            ComplexDftProcessor processor = new ComplexDftProcessor();
            try
            {
                // Already running from constructor
                assertDoesNotThrow(processor::start,
                    "Calling start() on a running processor should not throw");
                assertTrue(processor.isRunning());
            }
            finally
            {
                processor.dispose();
            }
        }
    }

    // ========================================================================================
    // WaterfallColorModel Tests
    // ========================================================================================

    @Nested
    @DisplayName("WaterfallColorModel Tests")
    class WaterfallColorModelTests
    {
        @Test
        @DisplayName("ARGB color map should produce exactly 256 entries")
        void testColorMapSize()
        {
            int[] argb = WaterfallColorModel.getARGBColorMap();
            assertEquals(256, argb.length,
                "Color map should have exactly 256 entries");
        }

        @Test
        @DisplayName("All ARGB color map entries should have alpha = 0xFF")
        void testColorMapAlphaChannel()
        {
            int[] argb = WaterfallColorModel.getARGBColorMap();
            for(int i = 0; i < argb.length; i++)
            {
                int alpha = (argb[i] >> 24) & 0xFF;
                assertEquals(0xFF, alpha,
                    "Entry " + i + " should have alpha=0xFF, got: 0x" + Integer.toHexString(alpha));
            }
        }

        @Test
        @DisplayName("ARGB color map entries should be valid packed ARGB values")
        void testColorMapValidARGB()
        {
            int[] argb = WaterfallColorModel.getARGBColorMap();
            for(int i = 0; i < argb.length; i++)
            {
                // Verify alpha is always 0xFF (fully opaque)
                int alpha = (argb[i] >> 24) & 0xFF;
                int red = (argb[i] >> 16) & 0xFF;
                int green = (argb[i] >> 8) & 0xFF;
                int blue = argb[i] & 0xFF;

                assertEquals(0xFF, alpha,
                    "Alpha should be 0xFF for entry " + i);
                assertTrue(red >= 0 && red <= 255,
                    "Red should be 0-255 for entry " + i + ", got: " + red);
                assertTrue(green >= 0 && green <= 255,
                    "Green should be 0-255 for entry " + i + ", got: " + green);
                assertTrue(blue >= 0 && blue <= 255,
                    "Blue should be 0-255 for entry " + i + ", got: " + blue);
            }
        }

        @Test
        @DisplayName("Background noise entries (0-15) should be dark blue")
        void testBackgroundNoiseEntriesLow()
        {
            int[] argb = WaterfallColorModel.getARGBColorMap();
            for(int i = 0; i < 16; i++)
            {
                int red = (argb[i] >> 16) & 0xFF;
                int green = (argb[i] >> 8) & 0xFF;
                int blue = argb[i] & 0xFF;

                assertEquals(0, red, "Red for entry " + i + " should be 0");
                assertEquals(0, green, "Green for entry " + i + " should be 0");
                assertEquals(127, blue, "Blue for entry " + i + " should be 127");
            }
        }

        @Test
        @DisplayName("Background noise entries (16-31) should be brighter blue")
        void testBackgroundNoiseEntriesHigh()
        {
            int[] argb = WaterfallColorModel.getARGBColorMap();
            for(int i = 16; i < 32; i++)
            {
                int red = (argb[i] >> 16) & 0xFF;
                int green = (argb[i] >> 8) & 0xFF;
                int blue = argb[i] & 0xFF;

                assertEquals(0, red, "Red for entry " + i + " should be 0");
                assertEquals(0, green, "Green for entry " + i + " should be 0");
                assertEquals(191, blue, "Blue for entry " + i + " should be 191");
            }
        }

        @Test
        @DisplayName("High-signal entries (188-255) should be pure red")
        void testHighSignalEntries()
        {
            int[] argb = WaterfallColorModel.getARGBColorMap();
            for(int i = 188; i < 256; i++)
            {
                int red = (argb[i] >> 16) & 0xFF;
                int green = (argb[i] >> 8) & 0xFF;
                int blue = argb[i] & 0xFF;

                assertEquals(255, red, "Red for entry " + i + " should be 255");
                assertEquals(0, green, "Green for entry " + i + " should be 0");
                assertEquals(0, blue, "Blue for entry " + i + " should be 0");
            }
        }

        @Test
        @DisplayName("IndexColorModel should have 256 map size")
        void testDefaultColorModelSize()
        {
            java.awt.image.IndexColorModel model = WaterfallColorModel.getDefaultColorModel();
            assertEquals(256, model.getMapSize(),
                "Default color model should have 256 entries");
        }

        @Test
        @DisplayName("IndexColorModel should have 8-bit pixel depth")
        void testDefaultColorModelBitDepth()
        {
            java.awt.image.IndexColorModel model = WaterfallColorModel.getDefaultColorModel();
            assertEquals(8, model.getPixelSize(),
                "Default color model should have 8-bit pixel depth");
        }
    }

    // ========================================================================================
    // SpectrumUtils Tests
    // ========================================================================================

    @Nested
    @DisplayName("SpectrumUtils Tests")
    class SpectrumUtilsTests
    {
        @Test
        @DisplayName("Zoom level 0 should produce multiplier 1")
        void testZoomLevel0()
        {
            assertEquals(1, SpectrumUtils.getZoomMultiplier(0));
        }

        @Test
        @DisplayName("Zoom level 1 should produce multiplier 2")
        void testZoomLevel1()
        {
            assertEquals(2, SpectrumUtils.getZoomMultiplier(1));
        }

        @Test
        @DisplayName("Zoom level 2 should produce multiplier 4")
        void testZoomLevel2()
        {
            assertEquals(4, SpectrumUtils.getZoomMultiplier(2));
        }

        @Test
        @DisplayName("Zoom level 3 should produce multiplier 8")
        void testZoomLevel3()
        {
            assertEquals(8, SpectrumUtils.getZoomMultiplier(3));
        }

        @Test
        @DisplayName("Zoom level 4 should produce multiplier 16")
        void testZoomLevel4()
        {
            assertEquals(16, SpectrumUtils.getZoomMultiplier(4));
        }

        @Test
        @DisplayName("Zoom level 5 should produce multiplier 32")
        void testZoomLevel5()
        {
            assertEquals(32, SpectrumUtils.getZoomMultiplier(5));
        }

        @Test
        @DisplayName("Zoom level 6 should produce multiplier 64")
        void testZoomLevel6()
        {
            assertEquals(64, SpectrumUtils.getZoomMultiplier(6));
        }

        @Test
        @DisplayName("All zoom levels 0-6 should produce correct power-of-2 multipliers")
        void testAllZoomLevels()
        {
            int[] expected = {1, 2, 4, 8, 16, 32, 64};
            for(int zoom = 0; zoom <= 6; zoom++)
            {
                assertEquals(expected[zoom], SpectrumUtils.getZoomMultiplier(zoom),
                    "Zoom level " + zoom + " should produce multiplier " + expected[zoom]);
            }
        }

        @Test
        @DisplayName("Zoom multiplier follows 2^zoom formula")
        void testZoomMultiplierFormula()
        {
            for(int zoom = 0; zoom <= 6; zoom++)
            {
                int expected = (int) Math.pow(2, zoom);
                assertEquals(expected, SpectrumUtils.getZoomMultiplier(zoom),
                    "getZoomMultiplier(" + zoom + ") should equal 2^" + zoom + " = " + expected);
            }
        }
    }

    // ========================================================================================
    // Smoothing Filter Tests
    // ========================================================================================

    @Nested
    @DisplayName("Smoothing Filter Tests")
    class SmoothingFilterTests
    {
        private static final float[] TEST_DATA = {
            1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f,
            11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f, 18.0f, 19.0f, 20.0f
        };

        // ---------- NoSmoothingFilter Tests ----------

        @Nested
        @DisplayName("NoSmoothingFilter Tests")
        class NoSmoothingFilterTests
        {
            @Test
            @DisplayName("Should pass data through unchanged")
            void testPassThrough()
            {
                NoSmoothingFilter filter = new NoSmoothingFilter();
                float[] input = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
                float[] result = filter.filter(input);
                assertSame(input, result,
                    "NoSmoothingFilter should return the exact same array reference");
            }

            @Test
            @DisplayName("Should return NONE smoothing type")
            void testSmoothingType()
            {
                NoSmoothingFilter filter = new NoSmoothingFilter();
                assertEquals(SmoothingFilter.SmoothingType.NONE, filter.getSmoothingType());
            }

            @Test
            @DisplayName("Should report default point size")
            void testPointSize()
            {
                NoSmoothingFilter filter = new NoSmoothingFilter();
                assertEquals(SmoothingFilter.SMOOTHING_DEFAULT, filter.getPointSize(),
                    "NoSmoothingFilter should report the default smoothing point size");
            }

            @Test
            @DisplayName("Should pass large array through unchanged")
            void testLargeArrayPassThrough()
            {
                NoSmoothingFilter filter = new NoSmoothingFilter();
                float[] result = filter.filter(TEST_DATA);
                assertSame(TEST_DATA, result);
            }

            @Test
            @DisplayName("Should pass empty array through without error")
            void testEmptyArrayPassThrough()
            {
                NoSmoothingFilter filter = new NoSmoothingFilter();
                float[] empty = new float[0];
                float[] result = filter.filter(empty);
                assertSame(empty, result);
            }
        }

        // ---------- GaussianSmoothingFilter Tests ----------

        @Nested
        @DisplayName("GaussianSmoothingFilter Tests")
        class GaussianSmoothingFilterTests
        {
            @Test
            @DisplayName("Should produce valid output with default point size")
            void testDefaultFilter()
            {
                GaussianSmoothingFilter filter = new GaussianSmoothingFilter();
                float[] result = filter.filter(TEST_DATA);
                assertNotNull(result, "Result should not be null");
                assertEquals(TEST_DATA.length, result.length,
                    "Result should have same length as input");
            }

            @Test
            @DisplayName("Should return GAUSSIAN smoothing type")
            void testSmoothingType()
            {
                GaussianSmoothingFilter filter = new GaussianSmoothingFilter();
                assertEquals(SmoothingFilter.SmoothingType.GAUSSIAN, filter.getSmoothingType());
            }

            @Test
            @DisplayName("Default point size should be 3")
            void testDefaultPointSize()
            {
                GaussianSmoothingFilter filter = new GaussianSmoothingFilter();
                assertEquals(3, filter.getPointSize(),
                    "Default Gaussian filter point size should be 3");
            }

            @Test
            @DisplayName("Should produce output values that are different from input (smoothed)")
            void testOutputDiffersFromInput()
            {
                GaussianSmoothingFilter filter = new GaussianSmoothingFilter();
                // Use non-linear data (spike signal) since symmetric filters preserve linear ramps exactly
                float[] spikeData = {0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                float[] result = filter.filter(spikeData);
                // The middle values around the spike should be smoothed and differ from original
                boolean anyDifferent = false;
                for(int i = 1; i < spikeData.length - 1; i++)
                {
                    if(Math.abs(result[i] - spikeData[i]) > 0.001f)
                    {
                        anyDifferent = true;
                        break;
                    }
                }
                assertTrue(anyDifferent,
                    "Gaussian filter should produce values that differ from the input");
            }

            @Test
            @DisplayName("Should accept point size configuration for all valid odd sizes")
            void testSetPointSize()
            {
                GaussianSmoothingFilter filter = new GaussianSmoothingFilter();
                int[] validSizes = {3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29};
                for(int size : validSizes)
                {
                    assertDoesNotThrow(() -> filter.setPointSize(size),
                        "Setting point size to " + size + " should not throw");
                }
            }

            @Test
            @DisplayName("Should change point size after setPointSize and filter call")
            void testPointSizeChangeApplied()
            {
                GaussianSmoothingFilter filter = new GaussianSmoothingFilter();
                assertEquals(3, filter.getPointSize());
                filter.setPointSize(9);
                // Point size changes are applied lazily on next filter() call
                filter.filter(TEST_DATA);
                assertEquals(9, filter.getPointSize(),
                    "Point size should be 9 after calling setPointSize(9) and filter()");
            }

            @Test
            @DisplayName("Should throw for invalid point size")
            void testInvalidPointSize()
            {
                GaussianSmoothingFilter filter = new GaussianSmoothingFilter();
                assertThrows(IllegalArgumentException.class, () -> filter.setPointSize(4),
                    "Setting even point size should throw IllegalArgumentException");
            }

            @Test
            @DisplayName("Should produce finite output values")
            void testOutputFinite()
            {
                GaussianSmoothingFilter filter = new GaussianSmoothingFilter();
                float[] result = filter.filter(TEST_DATA);
                for(int i = 0; i < result.length; i++)
                {
                    assertTrue(Float.isFinite(result[i]),
                        "Output at index " + i + " should be finite, got: " + result[i]);
                }
            }

            @Test
            @DisplayName("Available point sizes should include standard odd values 3-29")
            void testAvailablePointSizes()
            {
                GaussianSmoothingFilter filter = new GaussianSmoothingFilter();
                var sizes = filter.getPointSizeList();
                assertTrue(sizes.contains(3), "Should contain 3");
                assertTrue(sizes.contains(9), "Should contain 9");
                assertTrue(sizes.contains(29), "Should contain 29");
            }
        }

        // ---------- RectangularSmoothingFilter Tests ----------

        @Nested
        @DisplayName("RectangularSmoothingFilter Tests")
        class RectangularSmoothingFilterTests
        {
            @Test
            @DisplayName("Should produce valid output with default point size")
            void testDefaultFilter()
            {
                RectangularSmoothingFilter filter = new RectangularSmoothingFilter();
                float[] result = filter.filter(TEST_DATA);
                assertNotNull(result);
                assertEquals(TEST_DATA.length, result.length);
            }

            @Test
            @DisplayName("Should return RECTANGLE smoothing type")
            void testSmoothingType()
            {
                RectangularSmoothingFilter filter = new RectangularSmoothingFilter();
                assertEquals(SmoothingFilter.SmoothingType.RECTANGLE, filter.getSmoothingType());
            }

            @Test
            @DisplayName("Default point size should be 3")
            void testDefaultPointSize()
            {
                RectangularSmoothingFilter filter = new RectangularSmoothingFilter();
                assertEquals(3, filter.getPointSize());
            }

            @Test
            @DisplayName("Rectangular coefficients should all be equal (1/N)")
            void testRectangularCoefficients()
            {
                float[] coeffs = RectangularSmoothingFilter.getCoefficients(5);
                assertNotNull(coeffs);
                assertEquals(5, coeffs.length);
                float expected = 1.0f / 5.0f;
                for(float c : coeffs)
                {
                    assertEquals(expected, c, 0.0001f,
                        "All rectangular coefficients should be 1/N");
                }
            }

            @Test
            @DisplayName("Rectangular coefficients should sum to 1.0")
            void testCoefficientsSumToOne()
            {
                for(int size = 3; size <= 31; size += 2)
                {
                    float[] coeffs = RectangularSmoothingFilter.getCoefficients(size);
                    float sum = 0;
                    for(float c : coeffs)
                    {
                        sum += c;
                    }
                    assertEquals(1.0f, sum, 0.001f,
                        "Coefficients for size " + size + " should sum to 1.0");
                }
            }

            @Test
            @DisplayName("Should accept point size configuration for all valid sizes")
            void testSetPointSize()
            {
                RectangularSmoothingFilter filter = new RectangularSmoothingFilter();
                int[] validSizes = {3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29};
                for(int size : validSizes)
                {
                    assertDoesNotThrow(() -> filter.setPointSize(size),
                        "Setting point size to " + size + " should not throw");
                }
            }

            @Test
            @DisplayName("Should produce finite output values")
            void testOutputFinite()
            {
                RectangularSmoothingFilter filter = new RectangularSmoothingFilter();
                float[] result = filter.filter(TEST_DATA);
                for(int i = 0; i < result.length; i++)
                {
                    assertTrue(Float.isFinite(result[i]),
                        "Output at index " + i + " should be finite");
                }
            }

            @Test
            @DisplayName("Should throw for invalid point size")
            void testInvalidPointSize()
            {
                RectangularSmoothingFilter filter = new RectangularSmoothingFilter();
                assertThrows(IllegalArgumentException.class, () -> filter.setPointSize(6),
                    "Setting invalid point size should throw IllegalArgumentException");
            }
        }

        // ---------- TriangularSmoothingFilter Tests ----------

        @Nested
        @DisplayName("TriangularSmoothingFilter Tests")
        class TriangularSmoothingFilterTests
        {
            @Test
            @DisplayName("Should produce valid output with default point size")
            void testDefaultFilter()
            {
                TriangularSmoothingFilter filter = new TriangularSmoothingFilter();
                float[] result = filter.filter(TEST_DATA);
                assertNotNull(result);
                assertEquals(TEST_DATA.length, result.length);
            }

            @Test
            @DisplayName("Should return TRIANGLE smoothing type")
            void testSmoothingType()
            {
                TriangularSmoothingFilter filter = new TriangularSmoothingFilter();
                assertEquals(SmoothingFilter.SmoothingType.TRIANGLE, filter.getSmoothingType());
            }

            @Test
            @DisplayName("Default point size should be 3")
            void testDefaultPointSize()
            {
                TriangularSmoothingFilter filter = new TriangularSmoothingFilter();
                assertEquals(3, filter.getPointSize());
            }

            @Test
            @DisplayName("Should accept point size configuration for all valid sizes")
            void testSetPointSize()
            {
                TriangularSmoothingFilter filter = new TriangularSmoothingFilter();
                int[] validSizes = {3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29};
                for(int size : validSizes)
                {
                    assertDoesNotThrow(() -> filter.setPointSize(size),
                        "Setting point size to " + size + " should not throw");
                }
            }

            @Test
            @DisplayName("Should produce finite output values")
            void testOutputFinite()
            {
                TriangularSmoothingFilter filter = new TriangularSmoothingFilter();
                float[] result = filter.filter(TEST_DATA);
                for(int i = 0; i < result.length; i++)
                {
                    assertTrue(Float.isFinite(result[i]),
                        "Output at index " + i + " should be finite");
                }
            }

            @Test
            @DisplayName("Triangular filter with 3-point should produce weighted average")
            void testTriangular3PointWeighting()
            {
                TriangularSmoothingFilter filter = new TriangularSmoothingFilter();
                // With coefficients [0.25, 0.5, 0.25] and input [1,2,3,4,5]
                float[] input = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
                float[] result = filter.filter(input);

                // Middle value (index 1): 0.25*1 + 0.5*2 + 0.25*3 = 0.25 + 1.0 + 0.75 = 2.0
                assertEquals(2.0f, result[1], 0.001f,
                    "3-point triangular at index 1 should be weighted average");
                // Middle value (index 2): 0.25*2 + 0.5*3 + 0.25*4 = 0.5 + 1.5 + 1.0 = 3.0
                assertEquals(3.0f, result[2], 0.001f,
                    "3-point triangular at index 2 should be weighted average");
                // Middle value (index 3): 0.25*3 + 0.5*4 + 0.25*5 = 0.75 + 2.0 + 1.25 = 4.0
                assertEquals(4.0f, result[3], 0.001f,
                    "3-point triangular at index 3 should be weighted average");
            }

            @Test
            @DisplayName("Should throw for invalid point size")
            void testInvalidPointSize()
            {
                TriangularSmoothingFilter filter = new TriangularSmoothingFilter();
                assertThrows(IllegalArgumentException.class, () -> filter.setPointSize(10),
                    "Setting invalid point size should throw IllegalArgumentException");
            }
        }

        // ---------- Cross-filter Tests ----------

        @Nested
        @DisplayName("Cross-filter Comparison Tests")
        class CrossFilterTests
        {
            @Test
            @DisplayName("All filter types should produce same-length output as input")
            void testAllFiltersSameLength()
            {
                SmoothingFilter[] filters = {
                    new NoSmoothingFilter(),
                    new GaussianSmoothingFilter(),
                    new RectangularSmoothingFilter(),
                    new TriangularSmoothingFilter()
                };

                for(SmoothingFilter filter : filters)
                {
                    float[] result = filter.filter(TEST_DATA);
                    assertEquals(TEST_DATA.length, result.length,
                        filter.getSmoothingType() + " filter output length should match input");
                }
            }

            @Test
            @DisplayName("Smoothing filters should produce different output than NoSmoothingFilter")
            void testSmoothingVsNoSmoothing()
            {
                NoSmoothingFilter noSmooth = new NoSmoothingFilter();
                GaussianSmoothingFilter gaussian = new GaussianSmoothingFilter();
                RectangularSmoothingFilter rectangular = new RectangularSmoothingFilter();
                TriangularSmoothingFilter triangular = new TriangularSmoothingFilter();

                // Use non-linear data (spike) since symmetric filters preserve linear ramps exactly
                float[] spikeData = {0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                float[] noResult = noSmooth.filter(spikeData);
                float[] gResult = gaussian.filter(spikeData);
                float[] rResult = rectangular.filter(spikeData);
                float[] tResult = triangular.filter(spikeData);

                assertFalse(Arrays.equals(noResult, gResult),
                    "Gaussian result should differ from unfiltered data");
                assertFalse(Arrays.equals(noResult, rResult),
                    "Rectangular result should differ from unfiltered data");
                assertFalse(Arrays.equals(noResult, tResult),
                    "Triangular result should differ from unfiltered data");
            }

            @Test
            @DisplayName("Different filter types should produce different output")
            void testDifferentFilterOutputs()
            {
                GaussianSmoothingFilter gaussian = new GaussianSmoothingFilter();
                RectangularSmoothingFilter rectangular = new RectangularSmoothingFilter();
                TriangularSmoothingFilter triangular = new TriangularSmoothingFilter();

                float[] gResult = gaussian.filter(TEST_DATA);
                float[] rResult = rectangular.filter(TEST_DATA);
                float[] tResult = triangular.filter(TEST_DATA);

                // At least one pair should be different
                boolean grDiff = !Arrays.equals(gResult, rResult);
                boolean gtDiff = !Arrays.equals(gResult, tResult);
                boolean rtDiff = !Arrays.equals(rResult, tResult);

                assertTrue(grDiff || gtDiff || rtDiff,
                    "At least two of three filter types should produce different outputs");
            }

            @Test
            @DisplayName("All smoothing filters should handle constant input gracefully")
            void testConstantInput()
            {
                float[] constant = new float[20];
                Arrays.fill(constant, 5.0f);

                SmoothingFilter[] filters = {
                    new GaussianSmoothingFilter(),
                    new RectangularSmoothingFilter(),
                    new TriangularSmoothingFilter()
                };

                for(SmoothingFilter filter : filters)
                {
                    float[] result = filter.filter(constant);
                    // Smoothing a constant signal should produce roughly the same constant
                    for(int i = 1; i < result.length - 1; i++)
                    {
                        assertEquals(5.0f, result[i], 0.01f,
                            filter.getSmoothingType() + " filter on constant input at index " + i +
                                " should be ~5.0");
                    }
                }
            }

            @Test
            @DisplayName("SmoothingFilter constants should have valid range")
            void testSmoothingConstants()
            {
                assertEquals(3, SmoothingFilter.SMOOTHING_MINIMUM);
                assertEquals(29, SmoothingFilter.SMOOTHING_MAXIMUM);
                assertEquals(9, SmoothingFilter.SMOOTHING_DEFAULT);
                assertTrue(SmoothingFilter.SMOOTHING_MINIMUM < SmoothingFilter.SMOOTHING_DEFAULT);
                assertTrue(SmoothingFilter.SMOOTHING_DEFAULT < SmoothingFilter.SMOOTHING_MAXIMUM);
            }
        }
    }

    // ========================================================================================
    // DFTSize Enum Tests
    // ========================================================================================

    @Nested
    @DisplayName("DFTSize Enum Tests")
    class DFTSizeTests
    {
        @Test
        @DisplayName("All DFTSize values should be powers of 2")
        void testDftSizesArePowersOfTwo()
        {
            for(DFTSize size : DFTSize.values())
            {
                int s = size.getSize();
                assertTrue(s > 0 && (s & (s - 1)) == 0,
                    size.name() + " size " + s + " should be a power of 2");
            }
        }

        @Test
        @DisplayName("DFTSize enum should have expected number of values")
        void testDftSizeCount()
        {
            assertEquals(7, DFTSize.values().length,
                "Should have 7 DFT sizes from 512 to 32768");
        }

        @Test
        @DisplayName("DFTSize labels should match their size values")
        void testDftSizeLabels()
        {
            for(DFTSize size : DFTSize.values())
            {
                assertEquals(String.valueOf(size.getSize()), size.getLabel(),
                    "Label for " + size.name() + " should match its size");
            }
        }

        @Test
        @DisplayName("DFTSize should include specific known sizes")
        void testSpecificSizes()
        {
            assertEquals(512, DFTSize.FFT00512.getSize());
            assertEquals(1024, DFTSize.FFT01024.getSize());
            assertEquals(2048, DFTSize.FFT02048.getSize());
            assertEquals(4096, DFTSize.FFT04096.getSize());
            assertEquals(8192, DFTSize.FFT08192.getSize());
            assertEquals(16384, DFTSize.FFT16384.getSize());
            assertEquals(32768, DFTSize.FFT32768.getSize());
        }
    }
}
