// gcc -o requirement2 requirement2.c -ljson-c / .json 파일을 가져오는 방법

#include <stdio.h>
#include <json-c/json.h>

int main() {
    struct json_object *parsed_json;
    struct json_object *title;

    // 파일을 읽어서 JSON 객체로 파싱합니다.
    parsed_json = json_object_from_file("requirement.json");
    if (parsed_json == NULL) {
        fprintf(stderr, "파일을 읽거나 파싱할 수 없습니다.\n");
        return 1;
    }

    // "Title" 키의 값을 가져옵니다.
    json_object_object_get_ex(parsed_json, "Title", &title);
    printf("Title: %s\n", json_object_get_string(title));

    // 메모리 해제
    json_object_put(parsed_json);

    return 0;
}