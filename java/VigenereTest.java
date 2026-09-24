import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free tests for Vigenere. Run from the java/ directory:
 *   javac *.java && java VigenereTest
 * Exits with status 1 if any test fails.
 */
public class VigenereTest {
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String BASE64 =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    private static int passed = 0;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        test("textbook example", VigenereTest::textbookExample);
        test("round trip", VigenereTest::roundTrip);
        test("non-alphabet chars pass through without consuming key", VigenereTest::passThrough);
        test("toCapitals and trim", VigenereTest::capitalsAndTrim);
        test("toCapitals without trim keeps spaces", VigenereTest::capitalsWithoutTrim);
        test("empty key returns input unchanged", VigenereTest::emptyKey);
        test("empty alphabet returns input unchanged", VigenereTest::emptyAlphabet);
        test("key wraps around", VigenereTest::keyWraps);
        test("key char outside alphabet is rejected", VigenereTest::badKeyChar);
        test("null arguments are rejected", VigenereTest::nulls);
        test("setAlphabet changes the alphabet", VigenereTest::setAlphabet);
        test("base64 round trip with unicode", VigenereTest::base64RoundTrip);
        test("decrypt with toCapitals and trim", VigenereTest::decryptCapitalsAndTrim);
        test("cli encrypts and decrypts text arguments", VigenereTest::cliArgs);
        test("cli reads standard input", VigenereTest::cliStdin);
        test("cli options", VigenereTest::cliOptions);
        test("cli base64 round trip", VigenereTest::cliBase64);
        test("cli reports errors", VigenereTest::cliErrors);

        System.out.println(passed + " passed, " + failures.size() + " failed");
        for (String f : failures) {
            System.out.println("FAIL: " + f);
        }
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    private static void textbookExample() {
        Vigenere v = new Vigenere(UPPER);
        assertEquals("LXFOPVEFRNHR", v.encrypt("ATTACKATDAWN", "LEMON"));
        assertEquals("ATTACKATDAWN", v.decrypt("LXFOPVEFRNHR", "LEMON"));
    }

    private static void roundTrip() {
        Vigenere v = new Vigenere(UPPER);
        String plain = "THE QUICK BROWN FOX, JUMPS OVER THE LAZY DOG!";
        String code = v.encrypt(plain, "SECRET");
        assertTrue(!code.equals(plain), "ciphertext should differ from plaintext");
        assertEquals(plain, v.decrypt(code, "SECRET"));
    }

    private static void passThrough() {
        Vigenere v = new Vigenere(UPPER);
        assertEquals("LXFOPV EF RNHR", v.encrypt("ATTACK AT DAWN", "LEMON"));
        assertEquals("ATTACK AT DAWN", v.decrypt("LXFOPV EF RNHR", "LEMON"));
    }

    private static void capitalsAndTrim() {
        Vigenere v = new Vigenere(UPPER);
        assertEquals("LXFOPVEFRNHR", v.encrypt("attack at dawn!", "lemon", true, true));
    }

    private static void capitalsWithoutTrim() {
        Vigenere v = new Vigenere(UPPER);
        assertEquals("LXFOPV EF RNHR", v.encrypt("attack at dawn", "lemon", true, false));
    }

    private static void emptyKey() {
        Vigenere v = new Vigenere(UPPER);
        String runtimeEmpty = new String("");
        assertEquals("HELLO", v.encrypt("HELLO", runtimeEmpty));
        assertEquals("HELLO", v.decrypt("HELLO", runtimeEmpty));
    }

    private static void emptyAlphabet() {
        Vigenere v = new Vigenere(new String(""));
        assertEquals("HELLO", v.encrypt("HELLO", "KEY"));
    }

    private static void keyWraps() {
        Vigenere v = new Vigenere(UPPER);
        // Z + B wraps to A; decrypting A - B wraps back to Z.
        assertEquals("A", v.encrypt("Z", "B"));
        assertEquals("Z", v.decrypt("A", "B"));
        assertEquals("BCBC", v.encrypt("AAAA", "BC"));
    }

    private static void badKeyChar() {
        Vigenere v = new Vigenere(UPPER);
        assertThrows(() -> v.encrypt("HELLO", "KEY!"));
        assertThrows(() -> v.decrypt("HELLO", "key"));
    }

    private static void nulls() {
        assertThrows(() -> new Vigenere(null));
        Vigenere v = new Vigenere(UPPER);
        assertThrows(() -> v.setAlphabet(null));
        assertThrows(() -> v.encrypt(null, "KEY"));
        assertThrows(() -> v.encrypt("HELLO", null));
        assertThrows(() -> v.encryptBase64(null, "KEY"));
        assertThrows(() -> v.decryptBase64(null, "KEY"));
    }

    private static void setAlphabet() {
        Vigenere v = new Vigenere(UPPER);
        v.setAlphabet("01");
        assertEquals("01", v.getAlphabet());
        assertEquals("1001", v.encrypt("0110", "1"));
    }

    private static void base64RoundTrip() {
        Vigenere v = new Vigenere(BASE64);
        String plain = "héllo wörld ✓ 😀";
        String code = v.encryptBase64(plain, "Key9");
        assertTrue(!code.equals(plain), "ciphertext should differ from plaintext");
        assertEquals(plain, v.decryptBase64(code, "Key9"));
    }

    private static void decryptCapitalsAndTrim() {
        Vigenere v = new Vigenere(UPPER);
        assertEquals("ATTACKATDAWN", v.decrypt("lxfopv ef rnhr", "lemon", true, true));
        assertEquals("ATTACK AT DAWN", v.decrypt("lxfopv ef rnhr", "lemon", true, false));
    }

    private static void cliArgs() {
        Cli r = cli("", "encrypt", "LEMON", "ATTACK", "AT", "DAWN");
        assertEquals("0|LXFOPV EF RNHR\n|", r.toString());
        assertEquals("0|ATTACK AT DAWN\n|", cli("", "decrypt", "LEMON", "LXFOPV EF RNHR").toString());
    }

    private static void cliStdin() {
        assertEquals("0|LXFOPV EF RNHR\n|", cli("ATTACK AT DAWN\n", "encrypt", "LEMON").toString());
        assertEquals("0|LXFOPV\nEF RNHR\n|", cli("ATTACK\nAT DAWN", "encrypt", "LEMON").toString());
    }

    private static void cliOptions() {
        assertEquals("0|LXFOPVEFRNHR\n|",
            cli("", "encrypt", "lemon", "attack at dawn", "--caps", "--trim").toString());
        assertEquals("0|1001\n|", cli("", "--alphabet", "01", "encrypt", "1", "0110").toString());
        Cli help = cli("", "--help");
        assertTrue(help.code == 0 && help.out.startsWith("Usage:"), "help should print usage");
    }

    private static void cliBase64() {
        String plain = "h\u00e9llo \u2713";
        Cli enc = cli(plain, "encrypt", "Key9", "--base64");
        assertEquals("0", String.valueOf(enc.code));
        Cli dec = cli(enc.out, "decrypt", "Key9", "--base64");
        assertEquals("0|" + plain + "\n|", dec.toString());
    }

    private static void cliErrors() {
        assertEquals("2", String.valueOf(cli("", "encrypt").code));
        assertEquals("2", String.valueOf(cli("", "scramble", "KEY", "X").code));
        assertEquals("2", String.valueOf(cli("", "encrypt", "KEY", "X", "--bogus").code));
        assertEquals("2", String.valueOf(cli("", "encrypt", "KEY", "X", "--alphabet").code));
        assertEquals("2", String.valueOf(cli("", "encrypt", "KEY", "X", "--base64", "--trim").code));
        Cli bad = cli("", "encrypt", "KEY!", "HELLO");
        assertTrue(bad.code == 1 && bad.err.contains("'!'"), "bad key char should exit 1: " + bad.err);
        Cli badB64 = cli("", "decrypt", "K", "--base64", "!!x");
        assertTrue(badB64.code == 1 && badB64.err.startsWith("Error: could not decrypt"),
            "bad base64 should exit 1: " + badB64.err);
    }

    private static final class Cli {
        int code;
        String out;
        String err;

        @Override
        public String toString() {
            return code + "|" + out + "|" + err;
        }
    }

    private static Cli cli(String stdin, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Cli r = new Cli();
        r.code = Vigenere.run(args,
            new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));
        r.out = out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        r.err = err.toString(StandardCharsets.UTF_8);
        return r;
    }

    // --- minimal test harness ---

    private static void test(String name, Runnable body) {
        try {
            body.run();
            passed++;
        } catch (Throwable t) {
            failures.add(name + ": " + t);
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertThrows(Runnable body) {
        try {
            body.run();
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }
}
