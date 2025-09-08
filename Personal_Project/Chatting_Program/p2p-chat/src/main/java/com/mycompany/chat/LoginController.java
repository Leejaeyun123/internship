package com.mycompany.chat; // 이 클래스가 속한 패키지 선언. 동일 패키지의 클래스들이 함께 묶여 관리됨.

import javafx.fxml.FXML; // FXML에서 컨트롤러 필드/메서드를 주입하기 위한 애너테이션을 사용하기 위해 임포트.
import javafx.fxml.FXMLLoader; // FXML 파일을 로드(파싱)하여 UI 트리를 생성하는 로더 클래스 임포트.
import javafx.scene.Parent; // Scene의 루트 컨테이너 타입(모든 노드의 공통 조상) 임포트.
import javafx.scene.Scene; // JavaFX의 장면(화면)을 나타내는 클래스 임포트.
import javafx.scene.control.Label; // 라벨 UI 컨트롤 임포트.
import javafx.scene.control.PasswordField; // 비밀번호 입력용 텍스트 필드(UI 컨트롤) 임포트.
import javafx.scene.control.TextField; // 일반 텍스트 입력 필드(UI 컨트롤) 임포트.
import javafx.scene.control.Alert; // 경고/정보용 팝업 다이얼로그(Alert) 임포트.
import javafx.scene.control.ButtonType; // Alert에 들어가는 버튼 타입(OK 등) 임포트.
import javafx.stage.Stage; // 최상위 윈도우(스테이지) 클래스를 임포트.
import org.mindrot.jbcrypt.BCrypt; // 비밀번호 해시/검증을 위한 BCrypt 라이브러리 임포트.

import java.io.IOException; // 입출력 예외 처리용 예외 클래스 임포트(FXML 로드 등에서 사용).
import java.sql.*; // JDBC 연결/쿼리 수행을 위한 표준 SQL 인터페이스 임포트(DriverManager, Connection, PreparedStatement 등).

public class LoginController { // 로그인/회원가입 화면을 제어하는 JavaFX 컨트롤러 클래스 선언.

    @FXML private TextField idField; // FXML에서 주입되는 아이디 입력 필드(로그인/회원가입 공용).
    @FXML private TextField nicknameField;   // 회원가입 화면에서만 존재하는 별명 입력 필드(로그인 화면일 때는 null일 수 있음).
    @FXML private PasswordField passwordField; // 비밀번호 입력 필드(로그인/회원가입 공용).
    @FXML private Label statusLabel; // 상태 메시지를 사용자에게 보여줄 라벨(FXML에서 주입).

    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC"; // MySQL 접속 URL(스키마 chat_app, 타임존 지정).
    private static final String DB_USER = "root"; // DB 접속 아이디.
    private static final String DB_PASSWORD = "ljy"; // DB 접속 비밀번호.

    /* ===================== 화면 전환 ===================== */ // 로그인/회원가입 화면 간 전환 관련 메서드 구역 주석.

    @FXML // FXML 버튼 onAction 등에서 호출되도록 표시.
    protected void handleSignUpView() { // "회원가입" 화면으로 전환하는 핸들러.
        try { // FXML 로딩 중 발생 가능한 IOException을 처리.
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/signup.fxml")); // 리소스 경로에서 signup.fxml을 읽어올 FXMLLoader 생성.
            Parent signUpRoot = fxmlLoader.load(); // FXML 파싱 및 UI 트리 생성, 루트 노드 반환.
            Stage stage = (Stage) idField.getScene().getWindow(); // 현재 컨트롤이 올라간 Scene의 Window(=Stage)를 가져옴.
            stage.setScene(new Scene(signUpRoot)); // 새로 로드한 루트를 바탕으로 Scene 생성 후 Stage에 세팅.
            stage.setTitle("회원가입"); // 윈도우 타이틀을 "회원가입"으로 설정.
            stage.show(); // 변경된 Scene을 사용자에게 표시.
        } catch (IOException e) { // FXML 로드 실패 등의 예외 처리.
            e.printStackTrace(); // 디버그를 위해 스택 트레이스 출력.
            statusLabel.setText("회원가입 화면을 불러오는 중 오류 발생."); // 사용자에게 오류 메시지 표시.
        }
    }

    @FXML // FXML에서 호출 가능하도록 표시.
    protected void handleLogInView() { // "로그인" 화면으로 전환하는 핸들러.
        try { // FXML 로딩 예외 처리 블록.
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/login.fxml")); // login.fxml 리소스를 읽어올 로더 생성.
            Parent logInRoot = fxmlLoader.load(); // FXML 파싱 및 UI 트리 생성.
            Stage stage = (Stage) statusLabel.getScene().getWindow(); // 현재 라벨이 포함된 Stage 객체 획득.
            stage.setScene(new Scene(logInRoot)); // 새로운 Scene을 생성하여 스테이지에 설정.
            stage.setTitle("로그인"); // 윈도우 타이틀을 "로그인"으로 설정.
            stage.show(); // 장면 표시.
        } catch (IOException e) { // 로딩 실패 시 예외 처리.
            e.printStackTrace(); // 디버그 로그 출력.
            statusLabel.setText("로그인 화면을 불러오는 중 오류 발생."); // 사용자에게 안내 메시지 표시.
        }
    }

    /* ===================== 로그인/회원가입 ===================== */ // 인증 로직(로그인/회원가입) 영역 구분 주석.

    @FXML // FXML 버튼 onAction 등에서 호출될 수 있게 지정.
    protected void handleLogIn() { // 로그인 버튼 클릭 시 호출되는 메서드.
        String id = safeTrim(idField.getText()); // 아이디 입력값을 읽고 null/양끝 공백을 안전하게 제거.
        String password = passwordField.getText(); // 비밀번호 입력값을 읽음(공백 유지: 해시 비교 시 그대로 사용).

        if (id.isEmpty() || password.isEmpty()) { // 아이디 또는 비밀번호가 비어있으면
            statusLabel.setText("아이디와 비밀번호를 모두 입력하세요."); // 사용자에게 안내 메시지 출력.
            return; // 더 진행하지 않고 종료.
        }

        String sql = "SELECT password_hash, nickname FROM users WHERE id = ?"; // 해당 id 사용자의 해시/닉네임을 조회하는 SQL(프리페어드 스테이트먼트).
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); // try-with-resources: 연결을 열고 블록 종료 시 자동 닫힘.
             PreparedStatement pstmt = conn.prepareStatement(sql)) { // SQL을 미리 컴파일한 PreparedStatement 생성.

            pstmt.setString(1, id); // 첫 번째 바인딩 파라미터(?)에 아이디 값 세팅(인젝션 방지).
            try (ResultSet rs = pstmt.executeQuery()) { // 쿼리 실행 후 결과 집합을 받음(try-with-resources로 자동 close).
                if (rs.next()) { // 결과가 존재하면(해당 id 사용자가 있으면)
                    String hashedPassword = rs.getString("password_hash"); // 저장된 해시 문자열을 가져옴.
                    String nickname = rs.getString("nickname"); // 저장된 닉네임을 가져옴.
                    if (BCrypt.checkpw(password, hashedPassword)) { // 입력 비밀번호와 저장된 해시를 BCrypt로 검증.
                        statusLabel.setText("로그인 성공! 자동 로그인 중..."); // 성공 메시지 표시.
                        loadChatRoom(id, nickname);  // 멀티탭 메인(채팅 메인 화면)으로 전환 및 클라이언트 연결 시도.
                    } else { // 비밀번호가 틀린 경우
                        statusLabel.setText("비밀번호가 일치하지 않습니다."); // 오류 안내.
                    }
                } else { // 결과가 없으면(해당 id 사용자 없음)
                    statusLabel.setText("존재하지 않는 사용자입니다."); // 존재하지 않음을 안내.
                }
            }
        } catch (SQLException e) { // JDBC 처리 중 발생한 예외 처리.
            e.printStackTrace(); // 디버그 로그 출력.
            statusLabel.setText("데이터베이스 오류가 발생했습니다."); // 사용자 안내.
        }
    }

    @FXML // FXML 버튼에서 호출되도록 표시.
    protected void handleSignUp() { // 회원가입 버튼 클릭 시 호출되는 메서드.
        String id = safeTrim(idField.getText()); // 아이디 입력값을 읽고 안전하게 trim.
        String password = passwordField.getText(); // 비밀번호 입력값을 읽음.
        String nickname = safeTrim(nicknameField != null ? nicknameField.getText() : null); // 회원가입 화면에서만 있는 닉네임 필드 값을 안전하게 trim(null 보호).

        if (id.isEmpty() || password.isEmpty() || nickname.isEmpty()) { // 세 값 중 하나라도 비어있으면
            statusLabel.setText("아이디, 비밀번호, 닉네임을 모두 입력하세요."); // 사용자 안내.
            return; // 더 진행하지 않음.
        }
        if (password.length() < 4) { // 비밀번호 최소 길이 정책(예: 4자) 점검.
            statusLabel.setText("비밀번호는 최소 4자리 이상이어야 합니다."); // 사용자 안내.
            return; // 진행 중단.
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt()); // 입력 비밀번호를 BCrypt로 솔트 포함 해싱(보안 저장용).
        String sql = "INSERT INTO users (id, password_hash, nickname) VALUES (?, ?, ?)"; // 사용자 정보를 저장할 INSERT SQL.

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); // DB 연결을 열고 자동 닫기 설정.
             PreparedStatement pstmt = conn.prepareStatement(sql)) { // INSERT용 PreparedStatement 준비.

            pstmt.setString(1, id); // 1번째 파라미터: id 바인딩.
            pstmt.setString(2, hashedPassword); // 2번째 파라미터: 해시된 비밀번호 바인딩.
            pstmt.setString(3, nickname); // 3번째 파라미터: 닉네임 바인딩.
            pstmt.executeUpdate(); // INSERT 실행(영향받은 행 수 반환되지만 여기서는 사용하지 않음).

            statusLabel.setText("회원가입 성공! 자동 로그인 중..."); // 성공 메시지 표시.
            loadChatRoom(id, nickname); // 즉시 로그인 처리처럼 채팅 메인으로 전환.

        } catch (SQLException e) { // INSERT 과정에서 발생할 수 있는 예외 처리.
            if (e.getErrorCode() == 1062) { // MySQL 에러 코드 1062: 중복 키(아이디 또는 닉네임 중복).
                statusLabel.setText("이미 존재하는 아이디 또는 닉네임입니다."); // 사용자에게 중복 안내.
            } else { // 그 외 DB 예외
                e.printStackTrace(); // 디버그 로그 출력.
                statusLabel.setText("데이터베이스 오류가 발생했습니다."); // 일반 오류 안내.
            }
        }
    }

    /* ===================== 채팅 메인(멀티탭) 로드 ===================== */ // 채팅 메인 화면(FXML) 로드 + 서버 연결 로직 구역.

    private void loadChatRoom(String id, String nickname) { // 로그인/회원가입 성공 시 채팅 메인 화면을 띄우는 내부 메서드.
        Parent root; // 로드한 FXML의 루트 노드를 담을 변수.
        ChatMainController controller; // 로드한 FXML에 연결된 컨트롤러 참조를 담을 변수.

        // 1) FXML 로드 (화면 구성 오류와 서버 연결 오류를 분리)
        try { // 화면(FXML) 로드만 먼저 시도하여 UI 문제를 네트워크 문제와 분리해서 처리.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat_main.fxml")); // chat_main.fxml을 찾는 로더 생성.
            root = loader.load(); // FXML 파싱하여 UI 트리 생성 및 루트 노드 획득.
            controller = loader.getController(); // 해당 FXML과 매핑된 ChatMainController 인스턴스 획득.
        } catch (Exception e) { // 로드 실패 시(리소스 경로/구문 오류 등)
            e.printStackTrace(); // 디버그 로그 출력.
            statusLabel.setText("화면 로드 실패: " + e.getClass().getSimpleName() + " - " + String.valueOf(e.getMessage())); // 원인 요약 표시.
            return; // 더 진행하지 않고 종료(서버 연결도 시도하지 않음).
        }

        // 2) 화면 먼저 보여주고
        Stage stage = (Stage) idField.getScene().getWindow(); // 현재 로그인 화면의 Stage 가져오기.
        Scene scene = new Scene(root); // 로드한 루트 노드로 새로운 Scene 생성.
        stage.setScene(scene); // Stage에 Scene 적용(화면 전환).
        stage.setTitle("채팅 - " + nickname); // 윈도우 타이틀에 닉네임 표시.

        // 창 크기/최소 사이즈(버튼 줄임표 방지)
        stage.setMinWidth(900); // 최소 가로 폭 설정(너무 작은 창에서 레이아웃 무너짐 방지).
        stage.setMinHeight(560); // 최소 세로 높이 설정.
        stage.setWidth(1000); // 초기 가로 크기 설정.
        stage.setHeight(640); // 초기 세로 크기 설정.
        stage.centerOnScreen(); // 화면 중앙으로 창 위치 이동.

        stage.show(); // 새로운 장면을 사용자에게 표시.

        // 3) 그 다음 서버 연결(연결 실패해도 화면은 뜸)
        try { // 네트워크 연결은 화면 표시 뒤에 시도(실패해도 UI는 살아있도록).
            ChatClient client = new ChatClient("localhost", 8000, controller, nickname); // 채팅 서버(로컬호스트:8000)에 연결하는 클라이언트 생성 + 수신 스레드 시작.
            controller.init(nickname, client); // 컨트롤러에 닉네임/클라이언트 주입 및 UI 초기화(목록 요청 등).
        } catch (IOException e) { // 서버가 꺼져있거나 연결 실패 시 예외 처리.
            e.printStackTrace(); // 디버그 로그 출력.
            new Alert(Alert.AlertType.ERROR, // 사용자에게 알림 창 표시.
                    "서버 연결 실패: " + e.getMessage(), // 실패 원인 메시지 구성.
                    ButtonType.OK).show(); // OK 버튼 하나로 보여주기.
            // 필요하면 여기서 controller 쪽에 "오프라인 상태" 표시 메서드 호출 가능 // 추가 UX 개선 여지에 대한 주석.
        }
    }

    /* ===================== 유틸 ===================== */ // 공통 유틸리티 메서드 영역.

    private static String safeTrim(String s) { // null 안전하게 문자열 공백 제거하는 헬퍼 메서드.
        return s == null ? "" : s.trim(); // s가 null이면 빈 문자열, 아니면 trim 결과 반환.
    }
}
