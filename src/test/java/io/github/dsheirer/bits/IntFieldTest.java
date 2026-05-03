package io.github.dsheirer.bits;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntFieldTest {

    @Test
    public void testConstructorValid() {
        IntField field = new IntField(5, 10);
        assertEquals(5, field.start());
        assertEquals(10, field.end());
    }

    @Test
    public void testConstructorLengthTooLong() {
        // Implementation: if(end - start > 32)
        // 33 bits: 33 - 0 = 33 > 32, should throw.
        assertThrows(IllegalArgumentException.class, () -> new IntField(0, 33));
    }

    @Test
    public void testConstructorMaxLength() {
        // Implementation: if(end - start > 32)
        // Max value for (end - start) is 32.
        // If start=0, end=32, end - start = 32.
        // width() = 32 - 0 + 1 = 33 bits.
        IntField field = new IntField(0, 32);
        assertEquals(33, field.width());
    }

    @Test
    public void testConstructorBoundaryLength() {
        // width 32: end - start = 31.
        IntField field = new IntField(0, 31);
        assertEquals(32, field.width());
    }

    @Test
    public void testConstructorInvalidIndices() {
        assertThrows(IllegalArgumentException.class, () -> new IntField(10, 5));
    }

    @Test
    public void testWidth() {
        assertEquals(1, new IntField(0, 0).width());
        assertEquals(32, new IntField(0, 31).width());
        assertEquals(10, new IntField(10, 19).width());
    }

    @Test
    public void testRange() {
        IntField field = IntField.range(5, 15);
        assertEquals(5, field.start());
        assertEquals(15, field.end());
        assertEquals(11, field.width());
    }

    @Test
    public void testLength4() {
        IntField field = IntField.length4(10);
        assertEquals(10, field.start());
        assertEquals(13, field.end());
        assertEquals(4, field.width());
    }

    @Test
    public void testLength6() {
        IntField field = IntField.length6(10);
        assertEquals(10, field.start());
        assertEquals(15, field.end());
        assertEquals(6, field.width());
    }

    @Test
    public void testLength8() {
        IntField field = IntField.length8(10);
        assertEquals(10, field.start());
        assertEquals(17, field.end());
        assertEquals(8, field.width());
    }

    @Test
    public void testLength12() {
        IntField field = IntField.length12(10);
        assertEquals(10, field.start());
        assertEquals(21, field.end());
        assertEquals(12, field.width());
    }

    @Test
    public void testLength16() {
        IntField field = IntField.length16(10);
        assertEquals(10, field.start());
        assertEquals(25, field.end());
        assertEquals(16, field.width());
    }

    @Test
    public void testLength20() {
        IntField field = IntField.length20(10);
        assertEquals(10, field.start());
        assertEquals(29, field.end());
        assertEquals(20, field.width());
    }

    @Test
    public void testLength24() {
        IntField field = IntField.length24(10);
        assertEquals(10, field.start());
        assertEquals(33, field.end());
        assertEquals(24, field.width());
    }

    @Test
    public void testLength32() {
        IntField field = IntField.length32(10);
        assertEquals(10, field.start());
        assertEquals(41, field.end());
        assertEquals(32, field.width());
    }
}
