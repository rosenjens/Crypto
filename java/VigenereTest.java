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
