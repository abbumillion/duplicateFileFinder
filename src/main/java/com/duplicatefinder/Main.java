package com.duplicatefinder;

import com.duplicatefinder.controllers.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("Starting Duplicate File Finder...");

            // Create main controller which builds the UI
            MainController controller = new MainController();

            // Get the root node from controller
            Scene scene = new Scene(controller.getView(), 1080, 720);

            // Apply styles
            scene.getStylesheets().add(getClass().getResource("/com/duplicatefinder/style/style4.css").toExternalForm());

            // Setup stage
            primaryStage.setTitle("Duplicate  Finder");
            primaryStage.setScene(scene);
            primaryStage.show();

            System.out.println("Application started successfully");

            // Cleanup on close
            primaryStage.setOnCloseRequest(e -> {
                controller.cleanup();
            });

        } catch (Exception e) {
            System.err.println("ERROR starting application: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Failed to start application: " + e.getMessage());
        }
    }

    private void showErrorDialog(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Application Error");
        alert.setHeaderText("Failed to start");
        alert.setContentText(message);
        alert.showAndWait();
    }
}