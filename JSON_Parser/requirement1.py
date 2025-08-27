# .json 파일을 가져오는 방법

import json

with open('requirement.json', 'r') as file:       # 'requirement.json' 파일을 읽기 모드('r')로 엶
    parsed_data = json.load(file)                 # json.load()를 사용하여 파일의 내용을 직접 parsing

print(parsed_data['Title'])
print(parsed_data['MessageSequence'][0]['label']) # [0] : 첫 번째 MessageSequence / ['label'] : 첫 번째 MessageSequence의 'label'에 해당하는 값