public class test3 {
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
    public static String formatOctetAsHex(int value) {
        char[] chars = new char[2];
        chars[0] = HEX_CHARS[(value >> 4) & 0xF];
        chars[1] = HEX_CHARS[value & 0xF];
        return new String(chars);
    }
    public static void main(String[] args) {
        System.out.println(formatOctetAsHex(255));
        System.out.println(formatOctetAsHex(0));
        System.out.println(formatOctetAsHex(10));
    }
}
