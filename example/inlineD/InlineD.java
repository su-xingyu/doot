import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class InlineD {
    public static void main(String[] args) throws IOException {
        String text = "Hello, World!";
        String encoded = f(text);
        String encodedId = id(encoded);
        String decoded = g(encodedId);
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

    private static String f(String text) {
        return toHexString(text);
    }

    private static String g(String hex) {
        return fromHexString(hex);
    }

    private static String id(String str) {
        return str;
    }
}

