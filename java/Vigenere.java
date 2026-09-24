import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

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
            StringBuilder newStr = new StringBuilder(str.length());
            for (char c : str.toCharArray())
                if (getAlphabet().indexOf(c) != -1)
                    newStr.append(c);
            str = newStr.toString();
        }
        return str;
    }

    public String encryptBase64(String str, String key){
        String code = "";
        try {
            code = Base64.getEncoder().encodeToString(str.getBytes("utf-8"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encrypt(code,key) ;
    }

    public String decryptBase64(String code, String key){
        String str = "";
        try {
            str = new String(Base64.getDecoder().decode(decrypt(code,key)), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    public static final String DEFAULT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String BASE64_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    private static final String USAGE =
        "Usage: java Vigenere <encrypt|decrypt> <key> [text...] [options]\n"
        + "\n"
        + "Reads the text from standard input if none is given.\n"
        + "\n"
        + "Options:\n"
        + "  --alphabet <chars>  alphabet to use (default A-Z, or the Base64\n"
        + "                      characters with --base64)\n"
        + "  --caps              uppercase the text and key first\n"
        + "  --trim              drop characters that are not in the alphabet\n"
        + "  --base64            Base64-wrap the text so any Unicode text works\n"
        + "  -h, --help          show this help";

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
        java.util.List<String> positional = new java.util.ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-h") || a.equals("--help")) {
                out.println(USAGE);
                return 0;
            } else if (a.equals("--alphabet")) {
                if (++i >= args.length) {
                    return usageError(err, "--alphabet needs a value");
                }
                alphabet = args[i];
            } else if (a.equals("--caps")) {
                caps = true;
            } else if (a.equals("--trim")) {
                trim = true;
            } else if (a.equals("--base64")) {
                base64 = true;
            } else if (a.startsWith("--")) {
                return usageError(err, "unknown option " + a);
            } else {
                positional.add(a);
            }
        }

        if (positional.size() < 2) {
            return usageError(err, "missing mode or key");
        }
        String mode = positional.get(0);
        if (!mode.equals("encrypt") && !mode.equals("decrypt")) {
            return usageError(err, "mode must be encrypt or decrypt, not " + mode);
        }
        if (base64 && (caps || trim)) {
            return usageError(err, "--base64 cannot be combined with --caps or --trim");
        }
        String key = positional.get(1);

        String text;
        if (positional.size() > 2) {
            text = String.join(" ", positional.subList(2, positional.size()));
        } else {
            try {
                text = readAll(in);
            } catch (IOException e) {
                err.println("Error: could not read input: " + e.getMessage());
                return 1;
            }
        }

        if (alphabet == null) {
            alphabet = base64 ? BASE64_ALPHABET : DEFAULT_ALPHABET;
        }

        try {
            Vigenere v = new Vigenere(alphabet);
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
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        String s = new String(buf.toByteArray(), StandardCharsets.UTF_8);
        if (s.endsWith("\r\n")) {
            return s.substring(0, s.length() - 2);
        }
        if (s.endsWith("\n")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
