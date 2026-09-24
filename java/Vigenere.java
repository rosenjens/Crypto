import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Vigenere extends VigenereBase{

    public Vigenere(String alphabet) {
        super(alphabet);
    }

    public String encrypt(String str, String key, boolean toCapitals, boolean trim) {
        if (toCapitals) {
            key = key.toUpperCase();
        }
        return encrypt(prepare(str, toCapitals, trim), key);
    }

    public String decrypt(String code, String key, boolean toCapitals, boolean trim) {
        if (toCapitals) {
            key = key.toUpperCase();
        }
        return decrypt(prepare(code, toCapitals, trim), key);
    }

    private String prepare(String str, boolean toCapitals, boolean trim) {
        if (toCapitals) {
            str = str.toUpperCase();
        }
        if (trim) {
            var newStr = new StringBuilder(str.length());
            for (char c : str.toCharArray()) {
                if (getAlphabet().indexOf(c) != -1) {
                    newStr.append(c);
                }
            }
            str = newStr.toString();
        }
        return str;
    }

    public String encryptBase64(String str, String key){
        if (str == null){
            throw new IllegalArgumentException();
        }
        String code = Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
        return encrypt(code, key);
    }

    public String decryptBase64(String code, String key){
        return new String(Base64.getDecoder().decode(decrypt(code, key)), StandardCharsets.UTF_8);
    }

    public static final String DEFAULT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String BASE64_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    private static final String USAGE = """
        Usage: java Vigenere <encrypt|decrypt> <key> [text...] [options]
               java Vigenere crack [text...] [--caps] [--max-key-length <n>]

        Reads the text from standard input if none is given.

        crack finds the key of English text encrypted with the default A-Z
        alphabet, without knowing it, and prints the key and the plaintext.
        It needs roughly 30 letters of ciphertext per key letter.

        Options:
          --alphabet <chars>      alphabet to use (default A-Z, or the Base64
                                  characters with --base64)
          --caps                  uppercase the text and key first
          --trim                  drop characters that are not in the alphabet
          --base64                Base64-wrap the text so any Unicode text works
          --max-key-length <n>    longest key crack tries (default 20)
          -h, --help              show this help""";

    public static void main(String[] args) {
        // Match the UTF-8 used for standard input, whatever the platform default.
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        System.exit(run(args, System.in, out, err));
    }

    /** Runs the command line tool and returns the exit code. */
    static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
        String alphabet = null;
        boolean caps = false, trim = false, base64 = false;
        Integer maxKeyLength = null;
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h", "--help" -> {
                    out.println(USAGE);
                    return 0;
                }
                case "--alphabet" -> {
                    if (++i >= args.length) {
                        return usageError(err, "--alphabet needs a value");
                    }
                    alphabet = args[i];
                }
                case "--max-key-length" -> {
                    if (++i >= args.length) {
                        return usageError(err, "--max-key-length needs a value");
                    }
                    try {
                        maxKeyLength = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        maxKeyLength = 0;
                    }
                    if (maxKeyLength < 1) {
                        return usageError(err, "--max-key-length must be a positive number");
                    }
                }
                case "--caps" -> caps = true;
                case "--trim" -> trim = true;
                case "--base64" -> base64 = true;
                case String a when a.startsWith("--") -> {
                    return usageError(err, "unknown option " + a);
                }
                case String a -> positional.add(a);
            }
        }

        if (positional.isEmpty()) {
            return usageError(err, "missing mode");
        }
        String mode = positional.get(0);
        boolean crack = mode.equals("crack");
        if (!crack && !mode.equals("encrypt") && !mode.equals("decrypt")) {
            return usageError(err, "mode must be encrypt, decrypt or crack, not " + mode);
        }
        if (crack && (alphabet != null || base64 || trim)) {
            return usageError(err, "crack only supports --caps and --max-key-length");
        }
        if (!crack && maxKeyLength != null) {
            return usageError(err, "--max-key-length only applies to crack");
        }
        if (!crack && positional.size() < 2) {
            return usageError(err, "missing key");
        }
        if (base64 && (caps || trim)) {
            return usageError(err, "--base64 cannot be combined with --caps or --trim");
        }
        String key = crack ? null : positional.get(1);
        int textStart = crack ? 1 : 2;

        String text;
        if (positional.size() > textStart) {
            text = String.join(" ", positional.subList(textStart, positional.size()));
        } else {
            try {
                text = readAll(in);
            } catch (IOException e) {
                err.println("Error: could not read input: " + e.getMessage());
                return 1;
            }
        }

        if (crack) {
            try {
                var result = VigenereCracker.crack(caps ? text.toUpperCase() : text,
                    maxKeyLength != null ? maxKeyLength : VigenereCracker.DEFAULT_MAX_KEY_LENGTH);
                out.println("Key: " + result.key());
                out.println(result.plaintext());
                return 0;
            } catch (IllegalArgumentException e) {
                boolean lowercase = !caps && text.chars().anyMatch(c -> c >= 'a' && c <= 'z');
                err.println("Error: " + e.getMessage() + (lowercase ? " (try --caps for lowercase text)" : ""));
                return 1;
            }
        }

        if (alphabet == null) {
            alphabet = base64 ? BASE64_ALPHABET : DEFAULT_ALPHABET;
        }

        try {
            var v = new Vigenere(alphabet);
            boolean enc = mode.equals("encrypt");
            String result;
            if (base64 && enc) {
                result = v.encryptBase64(text, key);
            } else if (base64) {
                try {
                    result = v.decryptBase64(text, key);
                } catch (IllegalArgumentException e) {
                    err.println("Error: could not decrypt; check the key, alphabet and ciphertext");
                    return 1;
                }
            } else {
                result = enc ? v.encrypt(text, key, caps, trim) : v.decrypt(text, key, caps, trim);
            }
            out.println(result);
            return 0;
        } catch (IllegalArgumentException e) {
            err.println("Error: " + (e.getMessage() != null ? e.getMessage() : "invalid argument"));
            return 1;
        }
    }

    private static int usageError(PrintStream err, String message) {
        err.println("Error: " + message);
        err.println(USAGE);
        return 2;
    }

    /** Reads all of the stream as UTF-8, dropping one trailing newline. */
    private static String readAll(InputStream in) throws IOException {
        String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        if (s.endsWith("\r\n")) {
            return s.substring(0, s.length() - 2);
        }
        if (s.endsWith("\n")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
