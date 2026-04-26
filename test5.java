public class test5 {
    public static void main(String[] args) {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            String s = Integer.toHexString(i).toUpperCase(java.util.Locale.ROOT);
        }
        System.out.println("toUpperCase took " + (System.currentTimeMillis() - t1) + " ms");

        long t2 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            String s = Integer.toHexString(i).toUpperCase();
        }
        System.out.println("toUpperCase (default) took " + (System.currentTimeMillis() - t2) + " ms");
    }
}
