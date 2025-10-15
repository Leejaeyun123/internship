// 표준 입출력, 에러를 제공하는 클래스
// 멤버 변수는 in, out, err이고 모두 정적(static) 변수이므로 클래스를 따로 정의 X
// java.lang 패키지(Java에 필요한 가장 기본적인 클래스들이 모여 있는 패키지)에 포함돼 있어 import 없이 사용 가능

package com.example;

public class in_out_err {
    public static void main(String[] args) {
        int a = 10;
        int b = 20;
        char c = 'A';
        String d = "str";

        System.out.println("내용");
        System.out.println(123);
        System.out.print("내용\n");
        System.out.print("내용");
    }
}