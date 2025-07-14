package com.cole;

import com.cole.util.DBInit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            DBInit.initializeDatabase();

            java.net.URL fxmlLocation = App.class.getResource("/fxml/login.fxml");
            if (fxmlLocation == null) {
                throw new RuntimeException("Cannot find /fxml/login.fxml. Please check the path.");
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Scene scene = new Scene(loader.load());
            stage.setTitle("Student Tracker - Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {    
        // Register the AuthService to handle authentication
        // This is a test registration, you can remove it later
        launch(args);
    }

}