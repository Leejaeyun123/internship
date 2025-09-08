package com.mycompany.chat; // 이 파일이 속한 패키지 선언(클래스의 논리적 폴더 경로 역할)

// 아래부터는 JavaFX 애플리케이션 구동과 FXML 로딩에 필요한 클래스들을 임포트한다.
import javafx.application.Application; // JavaFX 애플리케이션의 기본 추상 클래스(수명주기: init/start/stop)
import javafx.fxml.FXMLLoader;         // FXML 파일을 읽어 UI 트리를 생성해 주는 로더
import javafx.scene.Parent;            // 모든 씬 그래프 루트가 될 수 있는 최상위 노드 타입
import javafx.scene.Scene;             // 화면에 표시될 씬(장면) 객체
import javafx.stage.Stage;             // 윈도우(최상위 컨테이너) 역할을 하는 스테이지

import java.io.IOException;            // 입출력 관련 예외(FXML 로드 실패 시 사용)

/**
 * JavaFX 애플리케이션의 진입점 클래스.
 * - launch()가 호출되면 JavaFX Application Thread가 만들어지고, 그 안에서 start()가 호출된다.
 * - 여기서는 첫 화면으로 login.fxml을 로드해 Stage에 붙인다.
 */ // 클래스 설명 주석(코드 동작에는 영향 없음)
public class App extends Application { // Application을 상속받아 JavaFX 수명주기 메서드를 구현하는 클래스 선언
    private static Scene scene;        // 현재 Stage에 붙어 있는 Scene을 보관(화면 전환 시 루트만 교체하려고 static으로 유지)

    @Override                           // 부모 클래스(Application)의 start 메서드를 재정의한다는 표시
    public void start(Stage stage) throws IOException { // 앱 시작 시 JavaFX가 호출하는 메서드, Stage는 윈도우를 의미
        Parent root = loadFXML("login");  // "/login.fxml"을 로드하여 씬 그래프의 루트 노드(Parent)를 얻는다
        scene = new Scene(root);          // 얻은 루트를 기반으로 새 Scene(장면)을 생성한다
        stage.setScene(scene);            // 생성한 Scene을 Stage(윈도우)에 부착한다
        stage.setTitle("로그인");         // 윈도우 제목 표시줄의 텍스트를 설정한다
        stage.sizeToScene();              // 현재 Scene의 루트 크기에 맞춰 Stage 크기를 자동 조정한다
        stage.show();                     // Stage(윈도우)를 화면에 표시한다
    }                                     // start 메서드 끝

    static void setRoot(String fxml) throws IOException { // 동일한 Scene을 유지한 채 루트 노드만 교체하는 화면 전환 유틸
        Parent root = loadFXML(fxml);     // 전달받은 이름의 FXML을 로드하여 새 루트 노드를 만든다
        scene.setRoot(root);              // 기존 Scene의 루트만 교체(스타일/크기/윈도우는 유지되므로 전환이 매끄럽다)
    }                                     // setRoot 메서드 끝

    // fxml 파일은 /resources/ 폴더 바로 아래 위치
    private static Parent loadFXML(String fxml) throws IOException { // 클래스패스에서 FXML을 찾아 로드하는 헬퍼 메서드
        FXMLLoader fxmlLoader = new FXMLLoader( // FXML 로더 인스턴스를 생성한다
                App.class.getResource("/" + fxml + ".fxml") // 클래스패스 기준으로 "/이름.fxml" 리소스 경로를 해석한다
        );                                                  // getResource 호출 끝
        return fxmlLoader.load();      // FXML을 파싱하여 UI 트리를 생성하고 그 루트(Parent)를 반환한다(실패 시 IOException)
    }                                   // loadFXML 메서드 끝

    public static void main(String[] args) { // 일반 자바 앱의 진입점. 여기서 JavaFX 런타임을 시작한다
        launch(args);                // 내부적으로 JavaFX Application Thread를 만들고, 이후 start(Stage)를 호출한다
    }                                // main 메서드 끝
}                                    // App 클래스 정의 끝
