package com.mycompany.chat; // 이 클래스가 속한 패키지(네임스페이스) 선언

import javafx.collections.FXCollections; // 관찰 가능한 리스트 생성 유틸(FX 컬렉션 팩토리)
import javafx.collections.ObservableList; // UI와 자동 동기화되는 리스트 타입
import javafx.fxml.FXML; // FXML에서 주입될 필드/메서드를 표시하는 어노테이션
import javafx.scene.control.*; // JavaFX UI 컨트롤들(ListView, TextArea, Button 등)
import javafx.scene.input.MouseButton; // 마우스 버튼(좌/우/중) 상수

import java.util.*; // 컬렉션(Set, HashSet, Arrays 등) 사용

public class ChatMainController { // 메인 채팅 화면을 제어하는 컨트롤러 클래스 시작

    // 상단 // 섹션 설명 주석
    @FXML private ComboBox<String> statusCombo; // 상태(활동 중/자리 비움) 선택 콤보박스(FXML 주입)
    @FXML private Label myNickLabel; // 내 닉네임 표시 라벨(FXML 주입)

    // 좌측: 방 목록 + 새 방 만들기 // 섹션 설명 주석
    @FXML private ListView<String> roomListView; // 채팅방 목록을 보여주는 리스트뷰(FXML 주입)
    @FXML private TextField newRoomField; // 새 방 이름 입력 필드(FXML 주입)

    // 하단: 전체 접속자 // 실제 레이아웃상 좌측 섹션 안에 배치되어 있지만 의미상 '전체 접속자' 블록
    @FXML private ListView<String> allUsersListView; // 서버 전체 접속자 목록 리스트뷰(FXML 주입)
    @FXML private Label allUsersTitle; // "전체 접속자 (N명)" 라벨(FXML 주입)

    // 중앙: 채팅 영역 + 입력창 // 섹션 설명 주석
    @FXML private TextArea chatArea; // 채팅 로그 표시 영역(FXML 주입)
    @FXML private TextField messageField; // 메시지 입력 필드(FXML 주입)
    @FXML private Button sendBtn; // 전송 버튼(FXML 주입)

    // 우측: 현재(활성) 방 참가자 // 섹션 설명 주석
    @FXML private ListView<String> roomUsersListView; // 활성 방의 참가자 목록 리스트뷰(FXML 주입)
    @FXML private Label roomUsersTitle; // "참가자 (N명)" 라벨(FXML 주입)

    /** 방 “원본 이름” 목록 */ // 각 방의 원래 이름만 담는 아이템 리스트(셀 렌더링에서 표시문구 조합)
    private final ObservableList<String> roomItems = FXCollections.observableArrayList(); // 방 목록(관찰 가능, UI 자동 반영)
    private final ObservableList<String> allUserItems = FXCollections.observableArrayList(); // 전체 접속자 목록(관찰 가능)
    private final ObservableList<String> roomUserItems = FXCollections.observableArrayList(); // 활성 방 참가자 목록(관찰 가능)

    /** 내가 가입한 방들 */ // 로그인 사용자가 현재 가입(join)해 있는 방들의 집합
    private final Set<String> myRooms = new HashSet<>(); // 중복 없이 보관하기 위해 Set 사용
    /** 현재 활성 방(메시지 전송 대상) */ // sendMessage 시 대상이 되는 방
    private String activeRoom = null; // 서버에서 내려주는 roomactive:로 갱신
    /** 활성 방에서 타이핑 중인지 */ // 입력창에 글자가 있으면 true
    private boolean typingActive = false; // 타이핑 상태 플래그(셀 렌더링 시 "채팅 중" 표시용)

    private ChatClient client; // 서버와 통신 담당 클라이언트 객체(초기화 시 주입)
    private String myNickname; // 내 닉네임(상단 라벨 표시 및 필요 시 로깅)

    /* ---------------- 초기화/주입 ---------------- */ // 초기 세팅 관련 메서드 구역

    public void init(String nickname, ChatClient client) { // 로그인 직후 호출되어 닉네임/클라이언트를 주입
        this.myNickname = nickname; // 닉네임 저장
        this.client = client; // 클라이언트 저장

        if (myNickLabel != null) myNickLabel.setText(nickname); // 상단 라벨에 내 닉네임 표시
        if (chatArea != null) chatArea.setWrapText(true); // 채팅 영역 줄바꿈(오버플로 방지)

        if (statusCombo != null) { // 상태 콤보박스가 주입되어 있다면
            statusCombo.setItems(FXCollections.observableArrayList("활동 중", "자리 비움")); // 두 상태값 세팅
            statusCombo.getSelectionModel().select("활동 중"); // 기본 선택을 "활동 중"으로
        } // if 끝

        if (roomListView != null) { // 방 리스트뷰가 주입되어 있다면
            roomListView.setItems(roomItems); // 방 목록 데이터 소스로 roomItems 연결
            // 셀에 "입장 중 / 채팅 중" 표시 // 커스텀 셀 팩토리로 각 줄의 표시문구를 동적으로 구성
            roomListView.setCellFactory(lv -> new ListCell<>() { // 익명 ListCell 구현
                @Override protected void updateItem(String item, boolean empty) { // 각 셀의 렌더링 로직
                    super.updateItem(item, empty); // 기본 처리
                    if (empty || item == null) { setText(null); return; } // 비어있으면 표시 없음
                    String label = item; // 기본은 방 이름 자체
                    if (myRooms.contains(item)) label += " - 입장 중"; // 내가 가입한 방이면 표시 추가
                    if (typingActive && item.equals(activeRoom)) label += " - 채팅 중"; // 활성 방 + 타이핑 중이면 추가 표시
                    setText(label); // 조합된 라벨 텍스트 적용
                } // updateItem 끝
            }); // setCellFactory 끝
            // 더블 클릭 → 참가(+활성화) // 방을 더블클릭하면 join 요청 보내고 활성 방으로 전환
            roomListView.setOnMouseClicked(ev -> { // 마우스 클릭 이벤트 핸들러 등록
                if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) { // 좌클릭 더블클릭 판정
                    joinRoomAction(); // 방 참가 액션 호출(FXML 이벤트 핸들러 재사용)
                } // if 끝
            }); // setOnMouseClicked 끝
        } // if 끝
        if (allUsersListView != null) allUsersListView.setItems(allUserItems); // 전체 접속자 리스트뷰 데이터 연결
        if (roomUsersListView != null) roomUsersListView.setItems(roomUserItems); // 활성 방 참가자 리스트뷰 데이터 연결

        // 메시지 입력 상태 → 타이핑 플래그 갱신 // 입력창 내용 변화를 감지하여 "채팅 중" 배지 갱신
        if (messageField != null) { // 입력 필드가 주입되어 있다면
            messageField.textProperty().addListener((obs, ov, nv) -> { // 텍스트 변경 리스너 등록
                boolean now = nv != null && !nv.isBlank(); // 현재 타이핑 중인지 판정(공백/빈문자 제외)
                if (now != typingActive) { // 이전 상태와 달라졌다면만
                    typingActive = now; // 상태 갱신
                    if (roomListView != null) roomListView.refresh(); // 방 리스트 셀 재렌더링(배지 갱신)
                } // if 끝
            }); // addListener 끝
        } // if 끝

        // 최초 목록 요청 // 로그인 직후 서버의 스냅샷 요청(방 목록/내 가입 방/활성 방/참가자/전체 사용자 등)
        if (client != null) client.sendRaw("room:list"); // 서버에 "room:list" 커맨드 송신
    } // init 끝

    /* ---------------- 서버 수신 처리 ---------------- */ // 서버에서 온 프로토콜 라인을 분기 처리

    public void displayMessage(String line) { // ChatClient 수신 스레드가 UI 스레드로 호출
        if (line == null) return; // null 방어

        if (line.startsWith("chat:")) { // 일반 채팅 포맷: "chat:닉네임: 메시지"
            appendChat(line.substring("chat:".length())); // "chat:" 접두어 제거 후 본문 출력

        } else if (line.startsWith("system:")) { // 시스템 메시지 포맷: "system:메시지"
            appendSystem(line.substring("system:".length())); // 시스템 메시지로 출력

        } else if (line.startsWith("roomlist:")) { // 방 목록 포맷: "roomlist:방1,방2,방3"
            updateRoomList(line.substring("roomlist:".length())); // 방 목록 갱신

        } else if (line.startsWith("myrooms:")) { // 내 가입 방 목록: "myrooms:방1,방2"
            updateMyRooms(line.substring("myrooms:".length())); // 내 방 세트 갱신

        } else if (line.startsWith("roomactive:")) { // 활성 방 알림: "roomactive:방이름"
            activeRoom = line.substring("roomactive:".length()); // 활성 방 이름 저장
            if (roomListView != null) roomListView.refresh(); // 셀 표시(채팅 중 배지 등) 갱신

        } else if (line.startsWith("userlist:")) { // 참가자 목록: "userlist:<room>:nick|status,nick|status"
            // 형식: "userlist:<room>:nick|status,nick|status" // 상세 형식 주석
            String body = line.substring("userlist:".length()); // "userlist:" 접두어 제거
            String room = body; // 기본값으로 전체 body를 room으로 두고
            String csv = ""; // 참가자 CSV 기본값은 빈 문자열
            int idx = body.indexOf(':'); // 첫 번째 ':' 위치 찾기
            if (idx >= 0) { // ':'가 있다면
                room = body.substring(0, idx); // 앞부분은 방 이름
                csv  = body.substring(idx + 1); // 뒷부분은 참가자 CSV
            } // if 끝
            // 우측 패널은 "활성 방"의 참가자만 표시 // 다른 방 참가자는 표시하지 않음
            if (room != null && room.equals(activeRoom)) { // 수신된 방이 현재 활성 방이면
                updateRoomUsers(csv); // 참가자 패널 갱신
            } // if 끝

        } else if (line.startsWith("allusers:")) { // 전체 접속자: "allusers:nick|status,nick|status"
            updateAllUsers(line.substring("allusers:".length())); // 전체 접속자 리스트 갱신

        } else { // 그 외(디버그/미정 의도 메세지)
            if (chatArea != null) chatArea.appendText(line + "\n"); // 원문을 로그 영역에 출력
        } // if-else 체인 끝
    } // displayMessage 끝

    private void appendChat(String body) { // 채팅 로그에 한 줄 추가
        if (chatArea != null) chatArea.appendText(body + "\n"); // 본문 출력 후 줄바꿈
    } // appendChat 끝
    public void appendSystem(String msg) { // 시스템 메시지를 로그에 추가
        if (chatArea != null) chatArea.appendText("== " + msg + " ==\n"); // 구분선 스타일로 출력
    } // appendSystem 끝

    private void updateRoomList(String csv) { // 서버에서 받은 방 목록 CSV로 리스트 갱신
        roomItems.clear(); // 기존 방 목록 비우기
        if (csv != null && !csv.isBlank()) { // 유효한 CSV라면
            roomItems.addAll(Arrays.asList(csv.split(","))); // 콤마로 분리하여 추가
        } // if 끝
        if (roomListView != null) roomListView.refresh(); // 커스텀 셀 재렌더링
        // 선택이 없으면 첫 항목만 선택(참가 아님) // 시각적 포커스만, 실제 join은 아님
        if (!roomItems.isEmpty() && roomListView.getSelectionModel().isEmpty()) { // 방이 있고 아직 선택이 없다면
            roomListView.getSelectionModel().selectFirst(); // 첫 번째 항목 선택
        } // if 끝
    } // updateRoomList 끝

    private void updateMyRooms(String csv) { // 내 가입 방 목록 갱신
        myRooms.clear(); // 기존 세트 초기화
        if (csv != null && !csv.isBlank()) { // 유효한 CSV라면
            myRooms.addAll(Arrays.asList(csv.split(","))); // 콤마 분리 후 Set에 저장
        } // if 끝
        if (roomListView != null) roomListView.refresh(); // 셀 렌더링 갱신(입장 중 배지 반영)
    } // updateMyRooms 끝

    private void updateRoomUsers(String csv) { // 활성 방 참가자 목록 갱신
        roomUserItems.clear(); // 기존 참가자 리스트 초기화
        int count = 0; // 카운터 초기화(라벨에 N명 표시용)
        if (csv != null && !csv.isBlank()) { // 유효한 CSV라면
            for (String token : csv.split(",")) { // 각 참가자 토큰 반복("nick|status")
                String[] parts = token.split("\\|", 2); // 닉네임과 상태를 '|'로 분리(최대 2개)
                String nick = parts[0]; // 닉네임
                String st = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : "활동 중"; // 상태 기본값 처리
                roomUserItems.add(nick + " (" + st + ")"); // "닉네임 (상태)" 형식으로 추가
                count++; // 카운트 증가
            } // for 끝
        } // if 끝
        if (roomUsersTitle != null) roomUsersTitle.setText("참가자 (" + count + "명)"); // 우측 라벨에 인원 반영
    } // updateRoomUsers 끝

    private void updateAllUsers(String csv) { // 전체 접속자 목록 갱신
        allUserItems.clear(); // 기존 목록 초기화
        int count = 0; // 카운트 초기화
        if (csv != null && !csv.isBlank()) { // 유효한 CSV라면
            for (String token : csv.split(",")) { // 각 사용자 토큰 반복("nick|status")
                String[] parts = token.split("\\|", 2); // 닉네임/상태 분리
                String nick = parts[0]; // 닉네임
                String st = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : "활동 중"; // 상태 기본값
                allUserItems.add(nick + " (" + st + ")"); // "닉네임 (상태)" 형식으로 추가
                count++; // 카운트 증가
            } // for 끝
        } // if 끝
        if (allUsersTitle != null) allUsersTitle.setText("전체 접속자 (" + count + "명)"); // 라벨에 총원 반영
    } // updateAllUsers 끝

    /* ---------------- FXML 이벤트 ---------------- */ // FXML에서 onAction 등으로 연결되는 이벤트 핸들러

    @FXML private void changeStatusAction() { // 상태 콤보 변경 시 호출
        if (client == null || statusCombo == null) return; // 주입 확인
        String st = statusCombo.getValue(); // 선택된 상태값 읽기
        if (st != null && !st.isBlank()) client.sendStatus(st); // 서버에 상태 변경 통지
    } // changeStatusAction 끝

    @FXML private void createRoomAction() { // "만들기" 버튼 클릭 시 호출
        if (client == null || newRoomField == null) return; // 주입 확인
        String room = safeTrim(newRoomField.getText()); // 입력값 트리밍
        if (room.isEmpty()) return; // 빈 값이면 무시
        client.sendCreateRoom(room); // 서버에 방 생성 요청
        // 만들면 바로 참가+활성화 // UX상 생성 직후 해당 방으로 들어가도록 처리
        client.sendJoinRoom(room); // 생성한 방에 가입 및 활성화
        newRoomField.clear(); // 입력창 비우기
    } // createRoomAction 끝

    @FXML private void joinRoomAction() { // "참가"(또는 방 더블클릭) 시 호출
        if (client == null || roomListView == null) return; // 주입 확인
        String room = roomListView.getSelectionModel().getSelectedItem(); // 현재 선택된 방 이름
        if (room == null || room.isBlank()) return; // 유효성 확인
        client.sendJoinRoom(room);   // 가입 + 활성화 요청
    } // joinRoomAction 끝

    @FXML private void leaveRoomAction() { // "나가기" 버튼 클릭 시 호출
        if (client == null || roomListView == null) return; // 주입 확인
        String room = roomListView.getSelectionModel().getSelectedItem(); // 선택된 방
        if (room == null || room.isBlank()) return; // 유효성 확인
        client.sendLeaveRoom(room); // 서버에 방 탈퇴 요청
    } // leaveRoomAction 끝

    @FXML private void sendMessageAction() { // 메시지 전송 버튼/엔터 입력 시 호출
        if (client == null || messageField == null) return; // 주입 확인
        String msg = safeTrim(messageField.getText()); // 입력 텍스트 트리밍
        if (msg.isEmpty()) return; // 빈 메시지는 전송하지 않음
        client.sendMessage(msg); // 서버에 일반 채팅 전송(활성 방으로 브로드캐스트됨)
        messageField.clear(); // → typingActive=false 로 바뀌며 목록 갱신됨 // 클리어하면 textProperty 리스너가 타이핑 상태 해제 처리
    } // sendMessageAction 끝

    /* ---------------- 유틸 ---------------- */ // 작은 헬퍼 함수들

    private static String safeTrim(String s) { // null-safe trim 함수
        return (s == null) ? "" : s.trim(); // null이면 빈 문자열, 아니면 앞뒤 공백 제거
    } // safeTrim 끝
} // ChatMainController 클래스 끝
