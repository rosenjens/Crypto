# Crypto

A small Java implementation of the [Vigenère cipher](https://en.wikipedia.org/wiki/Vigen%C3%A8re_cipher),
usable from the command line or as a library, with an optional Base64 mode for encrypting any Unicode text,
and a cracker that recovers the key of English ciphertext without knowing it.
It also includes the [Caesar](#caesar-cipher) and [Playfair](#playfair-cipher) ciphers, each with its own cracker.

> **Not for real security.** These are classical ciphers. Vigenère that can be broken with pen-and-paper
> frequency analysis, as the [cracker](#breaking-the-cipher) below shows. This project is for learning
> and fun. Use a modern library for anything that matters.

## How it works

Each letter of the text is shifted forward in the alphabet by the matching letter of the key
(`A` = 0, `B` = 1, …), wrapping around at the end. The key repeats as often as needed:

```
text:  ATTACK AT DAWN
key:   LEMONL EM ONLE
result LXFOPV EF RNHR
```

Characters that are not in the alphabet, such as spaces and punctuation, pass through unchanged
and do not use up a key letter. Decryption shifts backwards by the same amounts.

## Requirements

Java 25 or later. There are no dependencies and no build tool.

Run it straight from the source files, with no compile step:

```sh
cd java
java Vigenere.java encrypt LEMON "ATTACK AT DAWN"
```

Or compile once for faster start-up, and then use `java Vigenere` as in the examples below:

```sh
cd java
javac *.java
```

## Command line

```
java Vigenere <encrypt|decrypt> <key> [text...] [options]
java Vigenere crack [text...] [--cipher caesar|playfair] [--caps] [--max-key-length <n>]
```

The text can be given as arguments or piped in on standard input.

| Option | Meaning |
| --- | --- |
| `--cipher <name>` | `vigenere` (default), [`caesar`](#caesar-cipher) or [`playfair`](#playfair-cipher). |
| `--alphabet <chars>` | Alphabet to use. Default is `A`–`Z`, or the Base64 characters with `--base64`. |
| `--caps` | Uppercase the text and key first. |
| `--trim` | Remove characters that are not in the alphabet. |
| `--base64` | Base64-encode the text before encrypting, so any Unicode text works. Cannot be combined with `--caps` or `--trim`. |
| `--max-key-length <n>` | Longest key `crack` tries. Default 20. Vigenère only. |
| `-h`, `--help` | Show help. |

Examples:

```sh
$ java Vigenere encrypt LEMON "ATTACK AT DAWN"
LXFOPV EF RNHR

$ java Vigenere decrypt LEMON "LXFOPV EF RNHR"
ATTACK AT DAWN

# Lowercase input and no spaces or punctuation in the output
$ java Vigenere encrypt lemon "attack at dawn!" --caps --trim
LXFOPVEFRNHR

# Your own alphabet
$ java Vigenere encrypt 1 0110 --alphabet 01
1001

# Any Unicode text, round-tripped through a pipe
$ echo "héllo ✓" | java Vigenere encrypt Key9 --base64 | java Vigenere decrypt Key9 --base64
héllo ✓
```

Every character of the key must be in the alphabet. For example, `lemon` needs `--caps` with the default
alphabet. Output is always UTF-8, and piped input is read as UTF-8. Non-ASCII text passed as arguments is
decoded using your system locale, so on systems without a UTF-8 locale, pipe it in instead.

Exit codes: `0` success, `1` invalid key or ciphertext, `2` invalid usage.

## Breaking the cipher

`crack` finds the key of English text that was encrypted with the default `A`–`Z` alphabet,
then prints the key and the decrypted text:

```sh
$ java Vigenere encrypt WONDERLAND --caps < alice.txt > secret.txt
$ java Vigenere crack < secret.txt
Key: WONDERLAND
ALICE WAS BEGINNING TO GET VERY TIRED OF SITTING BY HER SISTER ON THE BANK, ...
```

How it works:

1. **Split into columns.** For a guessed key length *n*, every *n*th letter was shifted by the same
   key letter. So each column is just a Caesar cipher.
2. **Frequency analysis.** For each column, try all 26 shifts and keep the one whose letters look
   most like English, where `E` is common and `Z` is rare. More precisely, it keeps the shift with
   the highest likelihood under English letter frequencies.
3. **Pick the key length.** Try every length up to 20. Longer keys always fit slightly better,
   because each extra column gets its own free choice of shift, so each key letter has a cost
   (ln 26, the information needed to write it down). The length with the best fit after that cost wins.
   A multiple of the true length, such as `LEMONLEMON` for `LEMON`, fits no better but costs more, so it loses.

It needs enough text. Across 1,500 random keys of 1–12 letters, it recovered every key from
300 letters of ciphertext. From 100 letters it recovered every key of up to 5 letters. As a rule of thumb, allow
about 30 letters of ciphertext per key letter. Only uppercase `A`–`Z` is analysed, so use `--caps` for
lowercase ciphertext.

## Caesar cipher

Every letter is shifted by the same amount, so the key is a number: the shift. It is a Vigenère cipher
with a one-letter key, so it works with every option above, including `--alphabet` and `--base64`.
Negative shifts and shifts larger than the alphabet wrap around.

```sh
$ java Vigenere --cipher caesar encrypt 3 "THE QUICK BROWN FOX"
WKH TXLFN EURZQ IRA

$ java Vigenere --cipher caesar decrypt 3 "WKH TXLFN EURZQ IRA"
THE QUICK BROWN FOX
```

`crack --cipher caesar` tries all 26 shifts and keeps the one whose letters look most like English,
as for each column of a Vigenère key. It prints the shift as the key:

```sh
$ java Vigenere --cipher caesar crack "PHHW PH DW WKH VWDWLRQ DW QRRQ"
Key: 3
MEET ME AT THE STATION AT NOON
```

It needs at least 12 letters, and an ordinary sentence or two is usually enough. Very unusual text, such
as a pangram, can fool it.

## Playfair cipher

Playfair encrypts pairs of letters using a 5×5 grid. The grid holds the letters of the key, without
repeats, followed by the rest of the alphabet. There are only 25 cells, so `J` is treated as `I`.
With the key `PLAYFAIR EXAMPLE`:

```
P L A Y F
I R E X M
B C D G H
K N O Q S
T U V W Z
```

Each pair of letters is replaced by the letters to their right if they share a row, by the letters below
them if they share a column, and otherwise by the letters at the other two corners of their rectangle,
in the same rows. Rows and columns wrap around.

```sh
$ java Vigenere --cipher playfair encrypt "playfair example" "Hide the gold in the tree stump"
BMODZBXDNABEKUDMUIXMMOUVIF

$ java Vigenere --cipher playfair decrypt "playfair example" BMODZBXDNABEKUDMUIXMMOUVIF
HIDETHEGOLDINTHETREXESTUMP
```

Only the letters `A`–`Z` of the text and key are used, in either case, so `--caps` and `--trim` are implied
and `--alphabet` and `--base64` are not supported. A pair cannot hold the same letter twice, so an `X` is
put between doubled letters (`TREE` becomes `TR EX ES`), and an `X` is added to an odd number of letters
(a `Q` if the last letter is itself an `X`). Decryption cannot tell these fillers apart from real letters,
so it leaves them in, as in `TREXES` above. Ciphertext with an odd number of letters, or a pair of the same
letter, is rejected.

### Breaking Playfair

`crack --cipher playfair` finds a grid for English ciphertext without knowing the key, and prints the grid's
25 letters, row by row, and the decrypted text. The grid works as a key for `decrypt`.

```sh
$ java Vigenere --cipher playfair encrypt "playfair example" < alice.txt > secret.txt
$ java Vigenere --cipher playfair crack < secret.txt
Key: CDGHBNOQSKUVWZTLAYFPREXMI
ALICEWASBEGINXNINGTOGETVERYTIREDOFSITXTINGBYHERSISTERONTHEBANKANDOFHAVINGNOTHINGTODO...
```

That grid is the `PLAYFAIR EXAMPLE` grid shown above, rotated: it starts from the third row, and each row
starts from its second letter. Rotating the rows or columns of a grid, or swapping its rows with its columns,
does not change how it encrypts, so the cracker cannot tell these grids apart and may return any of them.
The keyword itself is lost too: only the grid it made can be found.

Counting letters does not help here, because Playfair encrypts pairs of letters, and there are far too many
grids to try them all. Instead the cracker searches the grids with
[simulated annealing](https://en.wikipedia.org/wiki/Simulated_annealing):

1. **Start anywhere.** Fill a grid with the letters in a random order.
2. **Make a small change.** Usually swap two letters, and sometimes swap two rows or columns or flip the grid.
3. **Score it.** Decrypt with the changed grid and measure how much the result looks like English, using how
   often each run of three letters (`THE`, `ING`, …) occurs in English books.
4. **Keep or undo.** Keep the change if the score went up. Also keep some changes that make it worse, so the
   search does not get stuck on a grid that is only partly right, but fewer and fewer as it goes on.
   After a million changes, the best grid seen wins.
5. **Try again if needed.** If the text still does not look like English, start again from a new random
   grid, up to 10 times.

The search is random but always starts from the same seed, so the same ciphertext gives the same answer.

It needs more text than the other crackers, and time. Across 100 random grids and passages of English
for each length, it recovered the text from 97 of 100 at 300 letters, 90 at 200 and 53 at 150, and from 99
at 600 letters. It takes a few seconds on average, and up to about 15 when it has to start again many times.
Only the first 600 letters are used to search, so longer texts take no longer. It refuses fewer than 100 letters.

The statistics come from English novels, so text that reads very differently, such as a list of names,
may defeat it.

## Library

```java
Vigenere v = new Vigenere("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

v.encrypt("ATTACK AT DAWN", "LEMON");               // "LXFOPV EF RNHR"
v.decrypt("LXFOPV EF RNHR", "LEMON");               // "ATTACK AT DAWN"
v.encrypt("attack at dawn", "lemon", true, true);   // toCapitals, trim -> "LXFOPVEFRNHR"
v.decrypt("lxfopvefrnhr", "lemon", true, false);    // "ATTACKATDAWN"

Vigenere b = new Vigenere(Vigenere.BASE64_ALPHABET);
String code = b.encryptBase64("héllo ✓", "Key9");
b.decryptBase64(code, "Key9");                      // "héllo ✓"
```

To crack ciphertext from code:

```java
var result = VigenereCracker.crack(ciphertext);   // result.key(), result.plaintext()
VigenereCracker.findKey(ciphertext, 5);           // best key when the length is known
```

Caesar has the same methods as `Vigenere`, with a number as the key, and Playfair has two static methods:

```java
Caesar c = new Caesar("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
c.encrypt("THE QUICK BROWN FOX", 3);                // "WKH TXLFN EURZQ IRA"
c.decrypt("WKH TXLFN EURZQ IRA", 3);                // "THE QUICK BROWN FOX"
CaesarCracker.crack(ciphertext);                    // result.shift(), result.plaintext()

Playfair.encrypt("Hide the gold", "playfair example");   // "BMODZBXDNAGE"
Playfair.decrypt("BMODZBXDNAGE", "PLAYFAIR EXAMPLE");    // "HIDETHEGOLDX"
PlayfairCracker.crack(ciphertext);                       // result.key(), result.plaintext()
```

`IllegalArgumentException` is thrown for a null argument, a null alphabet, a key character that is not in
the alphabet, ciphertext that does not decode as Base64, or too little text to crack. An empty key or alphabet returns the
text unchanged. Playfair also throws it for ciphertext it could not have produced.

## Running the tests

```sh
cd java
java VigenereTest.java
```

## License

[MIT](LICENSE.md)
