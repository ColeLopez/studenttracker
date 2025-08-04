package com.cole;

import com.cole.Service.GraduationService;
import com.cole.util.DBInit;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    /**
     * Starts the JavaFX application.
     * Initializes the database schema and sets up the main application window.
     *
     * @param stage the primary stage for this application
     * @throws Exception if an error occurs during startup
     */
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

    /**
     * The main method to launch the JavaFX application.
     * This method is used for testing purposes.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Register the AuthService to handle authentication
        // This is a test registration, you can remove it later
        launch(args);
    }

}