package com.mycompany.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
<<<<<<< HEAD
        // Scene의 너비와 높이를 명시적으로 지정하여 초기 창 크기를 고정합니다.
        scene = new Scene(loadFXML("login"), 400, 400); 
        stage.setTitle("로그인");
        stage.setScene(scene);
=======
        scene = new Scene(loadFXML("login"), 600, 400);
        stage.setScene(scene);
        stage.setTitle("로그인");
>>>>>>> 37db516 (09-07)
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
<<<<<<< HEAD
        // FXML 파일을 클래스패스 루트에서 찾도록 경로를 수정했습니다.
=======
        // FXML 파일을 클래스 패스 루트에서 찾도록 경로를 수정
>>>>>>> 37db516 (09-07)
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
