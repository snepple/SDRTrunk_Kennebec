package io.github.dsheirer.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteUtilTest {
    @Test
    public void testToHexStringIntArray() {
        int[] input = {0x00, 0x1A, 0xFF, 0x7B};
        String result = ByteUtil.toHexString(input);
        assertEquals("001AFF7B", result);
    }

    @Test
    public void testToHexStringByteArray() {
        byte[] input = {(byte)0x00, (byte)0x1A, (byte)0xFF, (byte)0x7B};
        String result = ByteUtil.toHexString(input);
        assertEquals("001AFF7B", result);
    }
}
