/**
 * The Caesar cipher: every character in the alphabet is shifted by the same amount.
 *
 * It is a Vigenère cipher whose key is the single alphabet character at position {@code shift},
 * so it supports the same alphabets, options and Base64 mode.
 */
public class Caesar {
    private final Vigenere vigenere;

    public Caesar(String alphabet) {
        vigenere = new Vigenere(alphabet);
    }

    public String getAlphabet() {
        return vigenere.getAlphabet();
    }

    public String encrypt(String str, int shift) {
        return vigenere.encrypt(str, key(shift));
    }

    public String decrypt(String code, int shift) {
        return vigenere.decrypt(code, key(shift));
    }

    public String encrypt(String str, int shift, boolean toCapitals, boolean trim) {
        return encrypt(vigenere.prepare(str, toCapitals, trim), shift);
    }

    public String decrypt(String code, int shift, boolean toCapitals, boolean trim) {
        return decrypt(vigenere.prepare(code, toCapitals, trim), shift);
    }

    public String encryptBase64(String str, int shift) {
        return vigenere.encryptBase64(str, key(shift));
    }

    public String decryptBase64(String code, int shift) {
        return vigenere.decryptBase64(code, key(shift));
    }

    /** The one-character Vigenère key for a shift. Any shift works, including negative ones. */
    private String key(int shift) {
        String alphabet = vigenere.getAlphabet();
        if (alphabet.isEmpty()) {
            return "";
        }
        return String.valueOf(alphabet.charAt(Math.floorMod(shift, alphabet.length())));
    }
}
