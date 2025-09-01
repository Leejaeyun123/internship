// 사용자의 로그인 및 회원가입 요청을 처리하고, 로그인 성공 시 채팅방 화면으로 전환하는 역할을 함

package com.mycompany.chat;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    @FXML
    private TextField idField;
    @FXML
    private PasswordField passwordField;

    // 데이터베이스 연결 정보를 상수로 정의하여 관리 용이성을 높임
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?serverTimezone=UTC";
    private static final String DB_USER = "your_user";
    private static final String DB_PASSWORD = "your_password";

    @FXML
    protected void handleLogInButtonAction() {
        String username = idField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("아이디와 비밀번호를 모두 입력하세요.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password_hash");
                // 사용자가 입력한 비밀번호와 DB의 해시된 비밀번호를 비교
                if (BCrypt.checkpw(password, hashedPassword)) {
                    System.out.println("로그인 성공!");
                    // TODO: 로그인 성공 시 채팅방 화면으로 전환하는 로직 구현
                    loadChatRoom(username); // 로그인 성공 시 호출
                } else {
                    System.out.println("로그인 실패: 비밀번호가 일치하지 않습니다.");
                }
            } else {
                System.out.println("로그인 실패: 사용자 ID가 존재하지 않습니다.");
            }
        } catch (SQLException e) {
            System.out.println("데이터베이스 오류가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleSignUpButtonAction() {
        String username = idField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("아이디와 비밀번호를 모두 입력하세요.");
            return;
        }

        // 비밀번호를 안전하게 해시화
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            System.out.println("회원가입 성공!");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // MySQL Duplicate entry error code
                System.out.println("회원가입 실패: 이미 존재하는 아이디입니다.");
            } else {
                System.out.println("데이터베이스 오류가 발생했습니다: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadChatRoom(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat_room.fxml"));
            Parent chatRoomRoot = loader.load();
            
            ChatRoomController controller = loader.getController();
            
            // ChatClient 객체 생성 및 ChatRoomController에 전달
            ChatClient client = new ChatClient("localhost", 8000, controller);
            controller.setClient(client);

            Stage stage = (Stage) idField.getScene().getWindow();
            stage.setScene(new Scene(chatRoomRoot));
            stage.setTitle("채팅방 - " + username);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: UI에 오류 메시지 표시
        }
    }
}