package com.example.shortener;

import java.security.SecureRandom;

public class CodeGenerator {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RNG = new SecureRandom();

    public static String generate(int length){
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i ++){
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }

        return sb.toString();
    }
}
