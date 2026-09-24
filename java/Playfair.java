import java.util.Locale;

/**
 * The Playfair cipher, which encrypts pairs of letters using a 5x5 grid built from the key.
 *
 * The grid holds the letters of the key, without repeats, followed by the rest of the alphabet.
 * There are only 25 cells, so J is treated as I. Each pair of letters is then replaced:
 * <ul>
 *   <li>in the same row, by the letters to their right,</li>
 *   <li>in the same column, by the letters below them,</li>
 *   <li>otherwise, by the letters in the same rows at the other corners of their rectangle.</li>
 * </ul>
 * Rows and columns wrap around. Decryption goes the other way.
 *
 * Only the letters A-Z are used, in either case: the text is uppercased and everything else is
 * dropped. A pair cannot hold the same letter twice, so an X is put between doubled letters, and
 * an X is added at the end if the number of letters is odd (a Q when the letter is itself an X).
 * Decryption cannot tell these fillers apart from real letters, so it leaves them in.
 */
public class Playfair {
    private static final int SIZE = 5;

    public static String encrypt(String str, String key) {
        char[] grid = grid(key);
        String letters = letters(str);
        var pairs = new StringBuilder(letters.length() + 2);
        for (int i = 0; i < letters.length(); i++) {
            char a = letters.charAt(i);
            pairs.append(a);
            if (i + 1 < letters.length() && letters.charAt(i + 1) != a) {
                pairs.append(letters.charAt(++i));
            } else {
                pairs.append(a == 'X' ? 'Q' : 'X');
            }
        }
        return substitute(pairs, grid, 1);
    }

    public static String decrypt(String code, String key) {
        char[] grid = grid(key);
        String letters = letters(code);
        if (letters.length() % 2 != 0) {
            throw new IllegalArgumentException("Playfair ciphertext must have an even number of letters");
        }
        for (int i = 0; i < letters.length(); i += 2) {
            if (letters.charAt(i) == letters.charAt(i + 1)) {
                throw new IllegalArgumentException(
                    "Playfair ciphertext cannot contain the pair " + letters.substring(i, i + 2));
            }
        }
        return substitute(letters, grid, SIZE - 1);
    }

    /** The 5x5 grid for a key, row by row. Characters in the key that are not letters are ignored. */
    static char[] grid(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        char[] grid = new char[SIZE * SIZE];
        int n = 0;
        for (char c : (letters(key) + "ABCDEFGHIKLMNOPQRSTUVWXYZ").toCharArray()) {
            if (new String(grid, 0, n).indexOf(c) == -1) {
                grid[n++] = c;
            }
        }
        return grid;
    }

    /** The letters of the text, uppercased, with J replaced by I. */
    private static String letters(String text) {
        if (text == null) {
            throw new IllegalArgumentException();
        }
        var letters = new StringBuilder(text.length());
        for (char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                letters.append(c == 'J' ? 'I' : c);
            }
        }
        return letters.toString();
    }

    /** Replaces each pair of letters, moving {@code step} cells right or down (4 moves back by one). */
    private static String substitute(CharSequence pairs, char[] grid, int step) {
        int[] position = new int[26];
        for (int i = 0; i < grid.length; i++) {
            position[grid[i] - 'A'] = i;
        }
        var result = new StringBuilder(pairs.length());
        for (int i = 0; i < pairs.length(); i += 2) {
            int a = position[pairs.charAt(i) - 'A'];
            int b = position[pairs.charAt(i + 1) - 'A'];
            int rowA = a / SIZE, colA = a % SIZE, rowB = b / SIZE, colB = b % SIZE;
            if (rowA == rowB) {
                colA = (colA + step) % SIZE;
                colB = (colB + step) % SIZE;
            } else if (colA == colB) {
                rowA = (rowA + step) % SIZE;
                rowB = (rowB + step) % SIZE;
            } else {
                int t = colA;
                colA = colB;
                colB = t;
            }
            result.append(grid[rowA * SIZE + colA]).append(grid[rowB * SIZE + colB]);
        }
        return result.toString();
    }
}
