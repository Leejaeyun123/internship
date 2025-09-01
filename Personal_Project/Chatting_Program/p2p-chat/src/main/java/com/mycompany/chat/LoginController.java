// src/main/java/com/mycompany/chat/LoginController.java
package com.mycompany.chat;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private TextField idField;
    @FXML
    private PasswordField passwordField;

    @FXML
    protected void handleLogInButtonAction() {
        // 로그인 로직 구현
        System.out.println("로그인 시도: " + idField.getText());
    }

    @FXML
    protected void handleSignUpButtonAction() {
        // 회원가입 로직 구현
        System.out.println("회원가입 시도: " + idField.getText());
    }
}