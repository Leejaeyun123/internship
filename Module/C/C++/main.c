// 함수 호출
// gcc -o math main.c math_functions.c -> ./math

#include <stdio.h>
#include "math_functions.h"

int main() {
    int result = add(5, 3);
    printf("5 + 3 = %d\n", result);
    return 0;
}