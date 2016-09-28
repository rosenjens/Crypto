class Vigenere {
    private String alphabet;
    private String key;

    public Vigenere(String key, String alphabet){
        this.alphabet = alphabet;
        this.key = key;
    }

    public String encode(String str){
        String code = "";
        for (int i = 0; i < str.length(); i++){
            int c = alphabet.indexOf(str.charAt(i));
            if (c != -1)
                code += alphabet.charAt( (c + alphabet.indexOf(key.charAt(i%key.length()))) % alphabet.length());
            else
                code += str.charAt(i);
        }
        return code; 
    }

    public String decode(String str){
    
        return "";
    }

}

