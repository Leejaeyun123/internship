package com.mycompany.chat;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML private TextField idField;
    @FXML private TextField nicknameField;   // 회원가입 화면에서만 존재(로그인 화면에서는 null일 수 있음)
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

    /* ===================== 화면 전환 ===================== */

    @FXML
    protected void handleSignUpView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/signup.fxml"));
            Parent signUpRoot = fxmlLoader.load();
            Stage stage = (Stage) idField.getScene().getWindow();
            stage.setScene(new Scene(signUpRoot));
            stage.setTitle("회원가입");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("회원가입 화면을 불러오는 중 오류 발생.");
        }
    }

    @FXML
    protected void handleLogInView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent logInRoot = fxmlLoader.load();
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(new Scene(logInRoot));
            stage.setTitle("로그인");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("로그인 화면을 불러오는 중 오류 발생.");
        }
    }

    /* ===================== 로그인/회원가입 ===================== */

    @FXML
    protected void handleLogIn() {
        String id = safeTrim(idField.getText());
        String password = passwordField.getText();

        if (id.isEmpty() || password.isEmpty()) {
            statusLabel.setText("아이디와 비밀번호를 모두 입력하세요.");
            return;
        }

        String sql = "SELECT password_hash, nickname FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password_hash");
                    String nickname = rs.getString("nickname");
                    if (BCrypt.checkpw(password, hashedPassword)) {
                        statusLabel.setText("로그인 성공! 자동 로그인 중...");
                        loadChatRoom(id, nickname);  // 멀티탭 메인으로 진입
                    } else {
                        statusLabel.setText("비밀번호가 일치하지 않습니다.");
                    }
                } else {
                    statusLabel.setText("존재하지 않는 사용자입니다.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("데이터베이스 오류가 발생했습니다.");
        }
    }

    @FXML
    protected void handleSignUp() {
        String id = safeTrim(idField.getText());
        String password = passwordField.getText();
        String nickname = safeTrim(nicknameField != null ? nicknameField.getText() : null);

        if (id.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
            statusLabel.setText("아이디, 비밀번호, 닉네임을 모두 입력하세요.");
            return;
        }
        if (password.length() < 4) {
            statusLabel.setText("비밀번호는 최소 4자리 이상이어야 합니다.");
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (id, password_hash, nickname) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, nickname);
            pstmt.executeUpdate();

            statusLabel.setText("회원가입 성공! 자동 로그인 중...");
            loadChatRoom(id, nickname);

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // 중복 키
                statusLabel.setText("이미 존재하는 아이디 또는 닉네임입니다.");
            } else {
                e.printStackTrace();
                statusLabel.setText("데이터베이스 오류가 발생했습니다.");
            }
        }
    }

    /* ===================== 채팅 메인(멀티탭) 로드 ===================== */

    private void loadChatRoom(String id, String nickname) {
        Parent root;
        ChatMainController controller;

        // 1) FXML 로드 (화면 구성 오류와 서버 연결 오류를 분리)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat_main.fxml"));
            root = loader.load();
            controller = loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("화면 로드 실패: " + e.getClass().getSimpleName() + " - " + String.valueOf(e.getMessage()));
            return;
        }

        // 2) 화면 먼저 보여주고
        Stage stage = (Stage) idField.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("채팅 - " + nickname);

        // 창 크기/최소 사이즈(버튼 줄임표 방지)
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setWidth(1000);
        stage.setHeight(640);
        stage.centerOnScreen();

        stage.show();

        // 3) 그 다음 서버 연결(연결 실패해도 화면은 뜸)
        try {
            ChatClient client = new ChatClient("localhost", 8000, controller, nickname);
            controller.init(nickname, client);
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "서버 연결 실패: " + e.getMessage(),
                    ButtonType.OK).show();
            // 필요하면 여기서 controller 쪽에 "오프라인 상태" 표시 메서드 호출 가능
        }
    }

    /* ===================== 유틸 ===================== */

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
