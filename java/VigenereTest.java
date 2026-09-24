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
        test("crack recovers keys of different lengths", VigenereTest::crackKeys);
        test("crack does not return a repeated key", VigenereTest::crackNoRepeats);
        test("crack respects the maximum key length", VigenereTest::crackMaxKeyLength);
        test("findKey with a known length", VigenereTest::crackFindKey);
        test("crack rejects bad input", VigenereTest::crackErrors);
        test("cli crack", VigenereTest::cliCrack);
        test("cli crack errors", VigenereTest::cliCrackErrors);

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

    /** Opening of Alice's Adventures in Wonderland (public domain), about 600 letters. */
    private static final String ALICE = """
        ALICE WAS BEGINNING TO GET VERY TIRED OF SITTING BY HER SISTER ON THE BANK, AND OF HAVING \
        NOTHING TO DO: ONCE OR TWICE SHE HAD PEEPED INTO THE BOOK HER SISTER WAS READING, BUT IT HAD \
        NO PICTURES OR CONVERSATIONS IN IT, AND WHAT IS THE USE OF A BOOK, THOUGHT ALICE, WITHOUT \
        PICTURES OR CONVERSATIONS? SO SHE WAS CONSIDERING IN HER OWN MIND (AS WELL AS SHE COULD, FOR \
        THE HOT DAY MADE HER FEEL VERY SLEEPY AND STUPID), WHETHER THE PLEASURE OF MAKING A \
        DAISY-CHAIN WOULD BE WORTH THE TROUBLE OF GETTING UP AND PICKING THE DAISIES, WHEN SUDDENLY A \
        WHITE RABBIT WITH PINK EYES RAN CLOSE BY HER. THERE WAS NOTHING SO VERY REMARKABLE IN THAT; \
        NOR DID ALICE THINK IT SO VERY MUCH OUT OF THE WAY TO HEAR THE RABBIT SAY TO ITSELF, OH DEAR! \
        OH DEAR! I SHALL BE LATE!""";

    private static void crackKeys() {
        Vigenere v = new Vigenere(UPPER);
        // VIGENERE's repeated Es make length 4 look partly right; an early version fell for it.
        for (String key : new String[] {"Q", "LEMON", "VIGENERE", "WONDERLAND", "CHESHIRECAT"}) {
            var result = VigenereCracker.crack(v.encrypt(ALICE, key));
            assertEquals(key, result.key());
            assertEquals(ALICE, result.plaintext());
        }
    }

    private static void crackNoRepeats() {
        Vigenere v = new Vigenere(UPPER);
        // LEMONLEMON encrypts exactly like LEMON, and LEMON is the simpler answer.
        assertEquals("LEMON", VigenereCracker.crack(v.encrypt(ALICE, "LEMONLEMON")).key());
    }

    private static void crackMaxKeyLength() {
        Vigenere v = new Vigenere(UPPER);
        String code = v.encrypt(ALICE, "WONDERLAND");
        assertTrue(VigenereCracker.crack(code, 5).key().length() <= 5, "key longer than the maximum");
        assertEquals("WONDERLAND", VigenereCracker.crack(code, 10).key());
    }

    private static void crackFindKey() {
        Vigenere v = new Vigenere(UPPER);
        assertEquals("RABBIT", VigenereCracker.findKey(v.encrypt(ALICE, "RABBIT"), 6));
    }

    private static void crackErrors() {
        assertThrows(() -> VigenereCracker.crack(null));
        assertThrows(() -> VigenereCracker.crack("TOO SHORT"));
        assertThrows(() -> VigenereCracker.crack(ALICE, 0));
        assertThrows(() -> VigenereCracker.findKey(ALICE, 0));
    }

    private static void cliCrack() {
        String code = new Vigenere(UPPER).encrypt(ALICE, "LEMON");
        assertEquals("0|Key: LEMON\n" + ALICE + "\n|", cli(code, "crack").toString());
        assertEquals("0|Key: LEMON\n" + ALICE + "\n|", cli("", "crack", code).toString());
        Cli lower = cli(code.toLowerCase(), "crack", "--caps");
        assertEquals("0|Key: LEMON\n" + ALICE + "\n|", lower.toString());
        assertTrue(cli(code, "crack", "--max-key-length", "3").out.startsWith("Key: "),
            "crack should accept --max-key-length");
    }

    private static void cliCrackErrors() {
        Cli lower = cli("", "crack", "the quick brown fox jumps over the lazy dog");
        assertTrue(lower.code == 1 && lower.err.contains("--caps"), "should suggest --caps: " + lower.err);
        assertEquals("2", String.valueOf(cli("x", "crack", "--base64").code));
        assertEquals("2", String.valueOf(cli("x", "crack", "--alphabet", "AB").code));
        assertEquals("2", String.valueOf(cli("x", "crack", "--max-key-length", "0").code));
        assertEquals("2", String.valueOf(cli("x", "crack", "--max-key-length", "many").code));
        assertEquals("2", String.valueOf(cli("x", "encrypt", "K", "--max-key-length", "3").code));
    }

    private record Cli(int code, String out, String err) {
        @Override
        public String toString() {
            return code + "|" + out + "|" + err;
        }
    }

    private static Cli cli(String stdin, String... args) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int code = Vigenere.run(args,
            new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Cli(code, out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n"),
            err.toString(StandardCharsets.UTF_8));
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
