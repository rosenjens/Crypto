class VigenereBase {
    private String alphabet;

    public VigenereBase(String alphabet){
        if (alphabet == null){
            throw new IllegalArgumentException();
        }
        this.alphabet = alphabet;
    }

    public void setAlphabet(String alphabet){
        if (alphabet == null){
            throw new IllegalArgumentException();
        }
        this.alphabet = alphabet;
    }

    public String getAlphabet(){
        return alphabet;
    }

    public String encrypt(String str, String key){
        return vig(str, '+', key);
    }

    public String decrypt(String code, String key){
        return vig(code, '-', key);
    }

    private String vig(String s, char sign, String key){
        if (s == null || key == null){
            throw new IllegalArgumentException();
        }
        if (key.isEmpty() || alphabet.isEmpty()){
            return s;
        }
        int[] shifts = new int[key.length()];
        for (int i = 0; i < key.length(); i++){
            shifts[i] = alphabet.indexOf(key.charAt(i));
            if (shifts[i] == -1){
                throw new IllegalArgumentException("Key character '" + key.charAt(i) + "' is not in the alphabet");
            }
        }
        int n = alphabet.length();
        StringBuilder str = new StringBuilder(s.length());
        int k = 0;
        for (int i = 0; i < s.length(); i++){
            int c = alphabet.indexOf(s.charAt(i));
            if (c != -1){
                int shift = shifts[k++ % shifts.length];
                if (sign == '+'){
                    str.append(alphabet.charAt((c + shift) % n));
                }else{
                    str.append(alphabet.charAt((c - shift + n) % n));
                }
            }else{
                str.append(s.charAt(i));
            }
        }
        return str.toString();
    }

}

