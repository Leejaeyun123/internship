// node json_parser2

const jsonString = '{"name": "홍길동", "age": 30}';
const data = JSON.parse(jsonString);

console.log(data.name);