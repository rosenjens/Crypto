/**
 * Recovers the shift of English text encrypted with the Caesar cipher and the default A-Z alphabet.
 *
 * A Caesar cipher is a Vigenère cipher with a one-letter key, so this is {@link VigenereCracker}
 * with the key length fixed at 1: it tries all 26 shifts and keeps the one whose decryption is
 * most likely to be English.
 */
public class CaesarCracker {
    public record Result(int shift, String plaintext) {}

    /** Guesses the shift and decrypts the ciphertext. */
    public static Result crack(String ciphertext) {
        var result = VigenereCracker.crack(ciphertext, 1);
        return new Result(result.key().charAt(0) - 'A', result.plaintext());
    }
}
