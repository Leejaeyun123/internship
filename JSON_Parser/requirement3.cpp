// g++ requirement3.cpp -o requirement3 -std=c++11 / .json 파일을 가져오는 방법

#include <iostream>
#include <fstream>
#include "json.hpp"

using json = nlohmann::json;

int main() {
    // 파일을 읽기 모드로 엽니다.
    std::ifstream file("requirement.json");
    if (!file.is_open()) {
        std::cerr << "파일을 열 수 없습니다." << std::endl;
        return 1;
    }

    try {
        // 파일 스트림을 직접 파싱합니다.
        json parsed_data = json::parse(file);

        // 데이터에 접근
        std::cout << "Title: " << parsed_data["Title"] << std::endl;
        std::cout << "ClientHello label: " << parsed_data["MessageSequence"][0]["label"] << std::endl;

    } catch (json::parse_error& e) {
        std::cerr << "JSON 파싱 오류: " << e.what() << '\n';
    }

    return 0;
}