module com.cole {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive java.sql;
    requires javafx.base;
    requires org.slf4j;

    opens com.cole to javafx.fxml;
    opens com.cole.controller to javafx.fxml;
    opens com.cole.model to java.base, javafx.base;
    exports com.cole;
    exports com.cole.controller;
    exports com.cole.model;
    
}
