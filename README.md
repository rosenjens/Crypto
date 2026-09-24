# Crypto

A small Java implementation of the [Vigenère cipher](https://en.wikipedia.org/wiki/Vigen%C3%A8re_cipher),
usable from the command line or as a library, with an optional Base64 mode for encrypting any Unicode text.

> **Not for real security.** Vigenère is a classical cipher that can be broken with pen-and-paper
> frequency analysis. This project is for learning and fun. Use a modern library for anything that matters.

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
```

The text can be given as arguments or piped in on standard input.

| Option | Meaning |
| --- | --- |
| `--alphabet <chars>` | Alphabet to use. Default is `A`–`Z`, or the Base64 characters with `--base64`. |
| `--caps` | Uppercase the text and key first. |
| `--trim` | Remove characters that are not in the alphabet. |
| `--base64` | Base64-encode the text before encrypting, so any Unicode text works. Cannot be combined with `--caps` or `--trim`. |
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

`IllegalArgumentException` is thrown for a null argument, a null alphabet, a key character that is not in
the alphabet, or ciphertext that does not decode as Base64. An empty key or alphabet returns the
text unchanged.

## Running the tests

```sh
cd java
java VigenereTest.java
```

## License

[MIT](LICENSE.md)
