// 로그인 화면의 UI를 제어하고, 로그인 및 회원가입 로직을 구현
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField idField;
    @FXML private TextField nicknameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "ljy";

    // 회원가입 화면으로 전환하는 메서드
    @FXML
    protected void handleSignUpView() {
        try {
            Parent signUpRoot = FXMLLoader.load(getClass().getResource("/signup.fxml"));
            Stage stage = (Stage) idField.getScene().getWindow();
            stage.setScene(new Scene(signUpRoot));
            stage.setTitle("회원가입");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 로그인 화면으로 전환하는 메서드
    @FXML
    protected void handleLogInView() {
        try {
            Parent logInRoot = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(new Scene(logInRoot));
            stage.setTitle("로그인");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleLogIn() {
        String id = idField.getText();
        String password = passwordField.getText();

        if (id.isEmpty() || password.isEmpty()) {
            statusLabel.setText("아이디와 비밀번호를 모두 입력하세요.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT password_hash, nickname FROM users WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password_hash");
                String nickname = rs.getString("nickname");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    statusLabel.setText("로그인 성공! 자동 로그인 중...");
                    loadChatRoom(id, nickname);
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
    protected void handleSignUp() {
        String id = idField.getText();
        String password = passwordField.getText();
        String nickname = nicknameField.getText();

        if (id.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
            statusLabel.setText("아이디, 비밀번호, 닉네임을 모두 입력하세요.");
            return;
        }

        if (password.length() < 4) {
            statusLabel.setText("비밀번호는 최소 4자리 이상이어야 합니다.");
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO users (id, password_hash, nickname) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, nickname);
            pstmt.executeUpdate();
            statusLabel.setText("회원가입 성공! 자동 로그인 중...");
            loadChatRoom(id, nickname);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                statusLabel.setText("이미 존재하는 아이디 또는 닉네임입니다.");
            } else {
                statusLabel.setText("DB 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadChatRoom(String id, String nickname) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat_room.fxml"));
            Parent chatRoomRoot = loader.load();
            ChatRoomController controller = loader.getController();

            ChatClient client = new ChatClient("localhost", 8000, controller, nickname);
            controller.setClient(client);

            Stage stage = (Stage) idField.getScene().getWindow();
            stage.setScene(new Scene(chatRoomRoot));
            stage.setTitle("채팅방 - " + nickname);
            stage.show();
        } catch (IOException e) {
            statusLabel.setText("채팅방을 불러오는 중 오류 발생.");
            e.printStackTrace();
        }
    }
}
