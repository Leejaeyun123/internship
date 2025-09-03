package com.mycompany.chat;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML
    private TextField idField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel; // 로그인/회원가입 피드백 표시용

    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

    @FXML
    protected void handleLogInButtonAction() {
        String username = idField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("아이디와 비밀번호를 모두 입력하세요.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password_hash");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    statusLabel.setText("로그인 성공!");
                    loadChatRoom(username);
                } else {
                    statusLabel.setText("비밀번호가 일치하지 않습니다.");
                }
            } else {
                statusLabel.setText("존재하지 않는 사용자입니다.");
            }
        } catch (SQLException e) {
            statusLabel.setText("데이터베이스 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleSignUpButtonAction() {
        String username = idField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("아이디와 비밀번호를 모두 입력하세요.");
            return;
        }

        if (password.length() < 4) {
            statusLabel.setText("비밀번호는 최소 4자리 이상이어야 합니다.");
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();

            statusLabel.setText("회원가입 성공! 자동 로그인 중...");
            // 회원가입 성공 후 자동 로그인 시도
            handleLogInButtonAction();

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry
                statusLabel.setText("이미 존재하는 아이디입니다.");
            } else {
                statusLabel.setText("DB 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 로그인 성공 시 채팅방 화면 로딩
    private void loadChatRoom(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat_room.fxml"));
            Parent chatRoomRoot = loader.load();

            ChatRoomController controller = loader.getController();
            ChatClient client = new ChatClient("localhost", 8000, controller, username);
            controller.setClient(client);

            Stage stage = (Stage) idField.getScene().getWindow();
            stage.setScene(new Scene(chatRoomRoot));
            stage.setTitle("채팅방 - " + username);
            stage.show();
        } catch (IOException e) {
            statusLabel.setText("채팅방을 불러오는 중 오류 발생.");
            e.printStackTrace();
        }
    }
}
