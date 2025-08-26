import json

json_string = '{"name": "홍길동", "age": 30}'
data = json.loads(json_string)

print(data['name'])