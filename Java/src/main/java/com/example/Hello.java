package com.example;

public class Hello {
    static void myMethod(String a, int b) {
        System.out.println(a + " is " + b);
    }

    public static void main(String[] args) {
        myMethod("A", 15);
        myMethod("B", 16);
        myMethod("C", 17);
    }
}
