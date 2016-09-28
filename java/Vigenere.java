class Vigenere {
    private String alphabet;

    public Vigenere(String alphabet){
        if (alphabet == null){
            throw new IllegalArgumentException();
        }
        this.alphabet = alphabet;
    }

    public void changeAlphabet(String alphabet){
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
        String str = "";

        if (key == "" || alphabet == ""){
            str = s;
        }else{
            for (int i = 0; i < s.length(); i++){
                int c = alphabet.indexOf(s.charAt(i));
                if (c != -1){
                    if (sign == '+'){
                        str += alphabet.charAt((c + alphabet.indexOf(key.charAt(i%key.length()))) % alphabet.length());
                    }else{
                        str += alphabet.charAt((c - alphabet.indexOf(key.charAt(i%key.length())) + alphabet.length()) % alphabet.length());
                    }
                }else{
                    str += s.charAt(i);
                }
            }
        }

        return str;

    }

}

