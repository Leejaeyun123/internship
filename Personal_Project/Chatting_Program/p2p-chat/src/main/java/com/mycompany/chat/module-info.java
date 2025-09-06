module com.mycompany.chat {
    // JavaFX 모듈
    requires javafx.controls;
    requires javafx.fxml;

    // 표준 Java 모듈
    requires java.sql;
    requires java.naming;

    // 외부 라이브러리 (자동 모듈)
    requires mysql.connector.java;
    requires jbcrypt;
    requires com.google.protobuf;

    opens com.mycompany.chat to javafx.fxml;
    exports com.mycompany.chat;
}
