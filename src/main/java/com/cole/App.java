package com.cole;

import com.cole.Service.GraduationService;
import com.cole.util.DBInit;
import com.cole.util.PasswordUtil;
import com.cole.util.DBUtil;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            DBInit.initializeDatabase();
            // Run graduation check after DB init
            GraduationService graduationService = new GraduationService();
            graduationService.checkAndUpdateGraduationFlags();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            java.net.URL fxmlLocation = App.class.getResource("/fxml/login.fxml");
            if (fxmlLocation == null) {
                throw new RuntimeException("Cannot find /fxml/login.fxml. Please check the path.");
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Scene scene = new Scene(loader.load());
            stage.setTitle("Student Tracker - Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen(); // <-- Add this line to center the window
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