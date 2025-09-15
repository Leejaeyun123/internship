package com.example;

public class Hello {
    public static void main(String args[]) {
        String txt = "                          Hello World             ";
        System.out.println("Before: [" + txt + "]");
        System.out.println("After: [" + txt.trim() + "]");
    }
}