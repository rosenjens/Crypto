import java.util.Base64;
import java.io.UnsupportedEncodingException;

public class Vigenere extends VigenereBase{

    public Vigenere(String alphabet) {
        super(alphabet);
    }

    public String encrypt(String str, String key, boolean toCapitals, boolean trim) {
        if (toCapitals) {
            str = str.toUpperCase();
            key = key.toUpperCase();
        }
        if (trim) {
            StringBuilder newStr = new StringBuilder(str.length());
            for (char c : str.toCharArray())
                if (getAlphabet().indexOf(c) != -1)
                    newStr.append(c);
            str = newStr.toString();
        }
        return encrypt(str, key);
    }

    public String encryptBase64(String str, String key){
        String code = "";
        try {
            code = Base64.getEncoder().encodeToString(str.getBytes("utf-8"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encrypt(code,key) ;
    }

    public String decryptBase64(String code, String key){
        String str = "";
        try {
            str = new String(Base64.getDecoder().decode(decrypt(code,key)), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }
}
