import java.util.Random;

/**
 * Recovers a grid for English text encrypted with the Playfair cipher, without knowing the key.
 *
 * Letter counting cannot break Playfair, because it encrypts pairs of letters, so this searches the
 * grids themselves with simulated annealing. Starting from a random grid, it makes one small change
 * at a time, such as swapping two letters or two rows, and keeps the change when the decryption looks
 * more like English. To avoid getting stuck on a grid that is only partly right, it sometimes keeps a
 * change for the worse too, less and less often as the search cools down.
 *
 * How much a text looks like English is the average log-probability of its runs of three letters,
 * from {@link EnglishTrigrams}. A search that ends far from English starts again from a new random grid,
 * up to {@link #MAX_ATTEMPTS} times, and the best grid of all the attempts wins.
 *
 * Rotating a grid's rows or columns, or swapping its rows with its columns, does not change how it
 * encrypts, so the grid found may be one of these variations of the original. The search is random but
 * starts from a fixed seed, so the same ciphertext always gives the same result.
 *
 * It needs a reasonable amount of text: roughly 300 letters.
 */
public class PlayfairCracker {
    /** Minimum number of letters needed to attempt a crack. */
    public static final int MIN_LETTERS = 100;

    /** Most searches before giving up and returning the best grid found. */
    public static final int MAX_ATTEMPTS = 10;

    /** Only this many letters are used to search, which is plenty and keeps long texts fast. */
    private static final int SEARCH_LETTERS = 600;

    private static final int STEPS = 100;
    private static final int CHANGES_PER_STEP = 10_000;
    private static final double START_TEMPERATURE = 0.04;

    /**
     * A score English text easily reaches, and a wrong grid does not. With enough text, the right grid
     * scores around -3.3, and grids that are far from right score below -4.
     */
    private static final double ENGLISH_SCORE = -3.8;

    private static final long SEED = 1;

    private static final double[] TRIGRAMS = EnglishTrigrams.logProbabilities();

    public record Result(String key, String plaintext) {}

    /**
     * Guesses a grid and decrypts the ciphertext. The key is the grid's 25 letters, row by row, and
     * works as a key for {@link Playfair}.
     */
    public static Result crack(String ciphertext) {
        // Rejects ciphertext that Playfair could not have produced, whatever the key.
        Playfair.decrypt(ciphertext, "");
        String letters = Playfair.letters(ciphertext);
        if (letters.length() < MIN_LETTERS) {
            throw new IllegalArgumentException(
                "Need at least " + MIN_LETTERS + " letters A-Z to crack, got " + letters.length());
        }
        int[] text = letters.chars().limit(SEARCH_LETTERS).map(c -> c - 'A').toArray();
        var search = new PlayfairCracker(text);
        var random = new Random(SEED);
        for (int attempt = 0; attempt < MAX_ATTEMPTS && search.bestScore < ENGLISH_SCORE; attempt++) {
            search.anneal(random);
        }
        var key = new StringBuilder(25);
        for (int c : search.best) {
            key.append((char) ('A' + c));
        }
        return new Result(key.toString(), Playfair.decrypt(ciphertext, key.toString()));
    }

    private final int[] text;
    private final int[] plain;
    private final int[] position = new int[26];
    private int[] best;
    private double bestScore = Double.NEGATIVE_INFINITY;

    private PlayfairCracker(int[] text) {
        this.text = text;
        this.plain = new int[text.length];
    }

    /** One search from a random grid, cooling from the start temperature to zero. */
    private void anneal(Random random) {
        int[] grid = new int[25];
        for (int c = 0, n = 0; c < 26; c++) {
            if (c != 'J' - 'A') {
                grid[n++] = c;
            }
        }
        for (int i = 24; i > 0; i--) {
            swap(grid, i, random.nextInt(i + 1));
        }
        double score = score(grid);
        if (score > bestScore) {
            bestScore = score;
            best = grid.clone();
        }
        int[] next = new int[25];
        for (int step = 0; step < STEPS; step++) {
            double temperature = START_TEMPERATURE * (STEPS - step) / STEPS;
            for (int i = 0; i < CHANGES_PER_STEP; i++) {
                change(grid, next, random);
                double nextScore = score(next);
                double gain = nextScore - score;
                if (gain >= 0 || random.nextDouble() < StrictMath.exp(gain / temperature)) {
                    int[] t = grid;
                    grid = next;
                    next = t;
                    score = nextScore;
                    if (score > bestScore) {
                        bestScore = score;
                        best = grid.clone();
                    }
                }
            }
        }
    }

    /**
     * Copies the grid into {@code next} with one random change: usually two letters swapped, and
     * sometimes two rows or columns swapped, or the grid flipped, to move many letters at once.
     */
    private static void change(int[] grid, int[] next, Random random) {
        int kind = random.nextInt(50);
        if (kind >= 5) {
            System.arraycopy(grid, 0, next, 0, 25);
            swap(next, random.nextInt(25), random.nextInt(25));
            return;
        }
        int a = random.nextInt(5), b = random.nextInt(5);
        for (int i = 0; i < 25; i++) {
            int row = i / 5, col = i % 5;
            switch (kind) {
                case 0 -> row = row == a ? b : row == b ? a : row;   // swap two rows
                case 1 -> col = col == a ? b : col == b ? a : col;   // swap two columns
                case 2 -> row = 4 - row;                             // flip top to bottom
                case 3 -> col = 4 - col;                             // flip left to right
                default -> { row = 4 - row; col = 4 - col; }       // turn upside down
            }
            next[i] = grid[row * 5 + col];
        }
    }

    /** Decrypts the text with the grid and returns the average log10-probability of its trigrams. */
    private double score(int[] grid) {
        for (int i = 0; i < 25; i++) {
            position[grid[i]] = i;
        }
        for (int i = 0; i < text.length; i += 2) {
            int a = position[text[i]], b = position[text[i + 1]];
            int rowA = a / 5, colA = a % 5, rowB = b / 5, colB = b % 5;
            if (rowA == rowB) {
                colA = (colA + 4) % 5;
                colB = (colB + 4) % 5;
            } else if (colA == colB) {
                rowA = (rowA + 4) % 5;
                rowB = (rowB + 4) % 5;
            } else {
                int t = colA;
                colA = colB;
                colB = t;
            }
            plain[i] = grid[rowA * 5 + colA];
            plain[i + 1] = grid[rowB * 5 + colB];
        }
        double sum = 0;
        for (int i = 2; i < plain.length; i++) {
            sum += TRIGRAMS[(plain[i - 2] * 26 + plain[i - 1]) * 26 + plain[i]];
        }
        return sum / (plain.length - 2);
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
