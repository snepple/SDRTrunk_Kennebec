public class test4 {
    public static void main(String[] args) {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            String s = String.format("%02X", i & 0xFF);
        }
        System.out.println("String.format took " + (System.currentTimeMillis() - t1) + " ms");

        long t2 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            String s = test3.formatOctetAsHex(i & 0xFF);
        }
        System.out.println("Custom hex array took " + (System.currentTimeMillis() - t2) + " ms");
    }
}
