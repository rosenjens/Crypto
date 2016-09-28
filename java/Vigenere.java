class Vigenere {
    private String alphabet;
    private String key;

    public Vigenere(String key, String alphabet){
        if (key == null || alphabet == null){
            throw new IllegalArgumentException();
        }
        this.alphabet = alphabet;
        this.key = key;
    }

    public void changeAlphabet(String alphabet){
        this.alphabet = alphabet;
    }
    
    public void changeKey(String key){
        this.key = key;
    }
    
    public String getAlphabet(){
        return alphabet;
    }
    
    public String getKey(){
        return key;
    }

    public String encode(String str){
        return vig(str, '+');
    }

    public String decode(String code){
        return vig(code, '-');
    }

    private String vig(String s, char sign){
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

