package org.jdesktop.swingx.mapviewer.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MercatorUtilsTest {

    @Test
    void testYToLatZero() {
        // When y is 0, exp(0) is 1, atan(1) is PI/4.
        // 2 * PI/4 is PI/2.
        // PI/2 - PI/2 is 0.
        // Therefore, latitude should be 0.
        double result = MercatorUtils.yToLat(0, 100.0);
        assertEquals(0.0, result, 0.0001, "Latitude for y=0 should be 0.0");
    }

    @Test
    void testYToLatPositive() {
        double result = MercatorUtils.yToLat(100, 100.0);
        assertEquals(49.6049374208547, result, 0.0000000001, "Latitude for y=100, radius=100 is approx 49.605");
    }

    @Test
    void testYToLatNegative() {
        double result = MercatorUtils.yToLat(-100, 100.0);
        assertEquals(-49.60493742085471, result, 0.0000000001, "Latitude for y=-100, radius=100 is approx -49.605");
    }

    @Test
    void testYToLatExtremePositive() {
        // For very large positive Y, exp(-large) -> 0, atan(0) -> 0
        // PI/2 - 0 = PI/2 = 90 degrees
        double result = MercatorUtils.yToLat(1000000, 100.0);
        assertEquals(90.0, result, 0.0001, "Latitude for very large Y should approach 90.0");
    }

    @Test
    void testYToLatExtremeNegative() {
        // For very large negative Y, exp(large) -> infinity, atan(infinity) -> PI/2
        // PI/2 - 2 * PI/2 = -PI/2 = -90 degrees
        double result = MercatorUtils.yToLat(-1000000, 100.0);
        assertEquals(-90.0, result, 0.0001, "Latitude for very large negative Y should approach -90.0");
    }

    @Test
    void testYToLatWithLatToYInverse() {
        double radius = 256.0;
        double originalLat = 45.0;
        int y = MercatorUtils.latToY(originalLat, radius);
        double calculatedLat = MercatorUtils.yToLat(y, radius);

        // Due to int truncation in latToY, the calculated latitude will not be exactly originalLat
        // but it should be reasonably close. Let's find out how close.
        // Actually, let's test that yToLat correctly inverses the mathematical part without truncation
        // Well, we can't test latToY without int truncation since it returns int.
        // Let's just test that the value is within 1 degree
        assertEquals(originalLat, calculatedLat, 0.5, "yToLat should be approx inverse of latToY");
    }
}
