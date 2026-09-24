/**
 * Recovers the key of English text encrypted with the default A-Z alphabet, without knowing the key.
 *
 * For each possible key length, the ciphertext is split into columns, one for each key position.
 * Every letter in a column was shifted by the same key letter, so that column is just a Caesar
 * cipher: frequency analysis tries all 26 shifts and keeps the one whose decryption is most
 * likely to be English.
 *
 * Longer keys always fit a little better, because each extra column gets its own free choice of
 * shift. So each key letter has a cost: ln(26), the information needed to write it down. The key
 * length with the best fit after that cost wins. A multiple of the true length fits no better than
 * the true length but costs more, and a wrong length mixes several shifts in each column and so
 * fits much worse.
 *
 * Only the letters A-Z are counted, matching the cipher, which passes everything else through.
 * It needs a reasonable amount of text: roughly 30 letters per key letter.
 */
public class VigenereCracker {
    /** Relative frequency of A-Z in English text. */
    private static final double[] ENGLISH = {
        .08167, .01492, .02782, .04253, .12702, .02228, .02015, .06094, .06966, .00153, .00772, .04025, .02406,
        .06749, .07507, .01929, .00095, .05987, .06327, .09056, .02758, .00978, .02360, .00150, .01974, .00074
    };

    private static final double[] LOG_ENGLISH = new double[26];

    static {
        for (int i = 0; i < 26; i++) {
            LOG_ENGLISH[i] = Math.log(ENGLISH[i]);
        }
    }

    /** The cost of one key letter, in the same units (nats) as the log-likelihood. */
    private static final double COST_PER_KEY_LETTER = Math.log(26);

    /** Minimum number of letters needed to attempt a crack. */
    public static final int MIN_LETTERS = 12;

    public static final int DEFAULT_MAX_KEY_LENGTH = 20;

    public record Result(String key, String plaintext) {}

    /** Guesses the key and decrypts the ciphertext. */
    public static Result crack(String ciphertext) {
        return crack(ciphertext, DEFAULT_MAX_KEY_LENGTH);
    }

    public static Result crack(String ciphertext, int maxKeyLength) {
        if (maxKeyLength < 1) {
            throw new IllegalArgumentException("Maximum key length must be at least 1");
        }
        int[] letters = letters(ciphertext);
        if (letters.length < MIN_LETTERS) {
            throw new IllegalArgumentException(
                "Need at least " + MIN_LETTERS + " letters A-Z to crack, got " + letters.length);
        }
        String bestKey = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int length = 1; length <= Math.min(maxKeyLength, letters.length); length++) {
            var key = new StringBuilder(length);
            double score = -length * COST_PER_KEY_LETTER;
            for (int col = 0; col < length; col++) {
                int[] counts = columnCounts(letters, length, col);
                int shift = bestShift(counts);
                key.append((char) ('A' + shift));
                score += logLikelihood(counts, shift);
            }
            if (score > bestScore) {
                bestScore = score;
                bestKey = key.toString();
            }
        }
        String plaintext = new Vigenere(Vigenere.DEFAULT_ALPHABET).decrypt(ciphertext, bestKey);
        return new Result(bestKey, plaintext);
    }

    /** Returns the key of the given length whose decryption is most likely to be English. */
    public static String findKey(String ciphertext, int keyLength) {
        if (keyLength < 1) {
            throw new IllegalArgumentException("Key length must be at least 1");
        }
        int[] letters = letters(ciphertext);
        var key = new StringBuilder(keyLength);
        for (int col = 0; col < keyLength; col++) {
            key.append((char) ('A' + bestShift(columnCounts(letters, keyLength, col))));
        }
        return key.toString();
    }

    /** The letters A-Z in the text, as 0-25. */
    private static int[] letters(String text) {
        if (text == null) {
            throw new IllegalArgumentException();
        }
        return text.chars().filter(c -> c >= 'A' && c <= 'Z').map(c -> c - 'A').toArray();
    }

    /** Letter counts of every {@code keyLength}th letter, starting at {@code col}. */
    private static int[] columnCounts(int[] letters, int keyLength, int col) {
        int[] counts = new int[26];
        for (int i = col; i < letters.length; i += keyLength) {
            counts[letters[i]]++;
        }
        return counts;
    }

    /** The shift that makes a column's decryption most likely to be English. */
    private static int bestShift(int[] counts) {
        int best = 0;
        for (int shift = 1; shift < 26; shift++) {
            if (logLikelihood(counts, shift) > logLikelihood(counts, best)) {
                best = shift;
            }
        }
        return best;
    }

    /** Log-probability of a column, decrypted with {@code shift}, under English letter frequencies. */
    private static double logLikelihood(int[] counts, int shift) {
        double sum = 0;
        for (int c = 0; c < 26; c++) {
            sum += counts[c] * LOG_ENGLISH[(c - shift + 26) % 26];
        }
        return sum;
    }
}
