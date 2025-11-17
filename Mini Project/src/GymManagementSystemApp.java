package com.gymmanagementsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.gymmanagementsystem.util.DatabaseConnection;

public class GymManagementSystemApp extends Application {

    // Define consistent stage dimensions as constants
    public static final double STAGE_WIDTH = 1200;
    public static final double STAGE_HEIGHT = 700;
    public static final double MIN_WIDTH = 900;
    public static final double MIN_HEIGHT = 550;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Test database connection
            if (DatabaseConnection.isPoolInitialized()) {
                System.out.println("Database connection pool is ready!");
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            // Set consistent stage properties
            primaryStage.setTitle("Gym Management System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);

            // ALWAYS START MAXIMIZED - FULL SCREEN SIZE
            primaryStage.setMaximized(true);

            primaryStage.show();

            System.out.println("Application started successfully (MAXIMIZED)");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start application: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        try {
            DatabaseConnection.closeDataSource();
            System.out.println("Database connection pool closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}