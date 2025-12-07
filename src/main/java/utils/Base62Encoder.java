package utils;

public class Base62Encoder {
    private static final String CHAR_POOL = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String encode(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int val = b & 0xFF;
            sb.append(CHAR_POOL.charAt(val % CHAR_POOL.length()));
        }
        return sb.toString();
    }
}
