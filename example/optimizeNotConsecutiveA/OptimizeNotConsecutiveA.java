import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class OptimizeNotConsecutiveA {
    public static void main(String[] args) throws IOException {
        String text = "Hello, World!";
        String textUpper = text.toUpperCase();
        String encoded = toHexString(textUpper);
        long currentTime = System.currentTimeMillis();
        System.out.println("Current time in milliseconds: " + currentTime);
        String decoded = fromHexString(encoded);
        System.out.println("Encoded: " + encoded);
        System.out.println("Decoded: " + decoded);
    }

    private static String toHexString(String text) {
        byte[] bytes = text.getBytes();
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static String fromHexString(String hex) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            text.append((char) Integer.parseInt(str, 16));
        }

        return text.toString();
    }
}

