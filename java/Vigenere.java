public class Vigenere extends VigenereBase{

    public Vigenere(String alphabet){
        super(alphabet);
    }

    public String encrypt(String str, String key, boolean toCapitals, boolean trim){
        if (toCapitals) {
            str = str.toUpperCase();
            key = key.toUpperCase();
        }
        if (trim) {
            String newStr = "";
            for (char c : str.toCharArray())
                newStr += (getAlphabet().indexOf(c) != -1 ? c : "");
            str = newStr;
        }
        return encrypt(str, key);
    }
}
