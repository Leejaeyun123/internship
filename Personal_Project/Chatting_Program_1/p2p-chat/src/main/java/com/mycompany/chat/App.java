// 로그인 화면을 띄우고, 로그인 성공 시 채팅방 화면으로 전환하는 로직을 구현

package com.mycompany.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 로그인 화면 로드
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("로그인");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}