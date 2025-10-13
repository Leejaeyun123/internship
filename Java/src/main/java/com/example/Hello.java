package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Hello {
    public static void main(String[] args) throws IOException {
        InputStream inputStream = System.in;
        InputStreamReader reader = new InputStreamReader(inputStream);
        
        char[] chars = new char[3];
        reader.read(chars);

        System.out.println(chars);
    }
}
