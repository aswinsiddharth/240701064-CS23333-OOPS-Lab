package com.gymmanagementsystem.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneManager {

    private static final Logger LOGGER = Logger.getLogger(SceneManager.class.getName());

    // Define default window dimensions
    private static final double DEFAULT_MIN_WIDTH = 1200.0;
    private static final double DEFAULT_MIN_HEIGHT = 800.0;

    /**
     * Switch to a new scene while preserving current stage dimensions
     */
    public static void switchScene(Node source, String fxmlPath, String title) throws IOException {
        Stage stage = (Stage) source.getScene().getWindow();

        // Get screen bounds for maximum size
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Store current state
        final boolean wasMaximized = stage.isMaximized();

        LOGGER.log(Level.INFO, "Current maximized state: " + wasMaximized);

        // Load new FXML
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();

        // Create new scene with screen dimensions
        Scene newScene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());

        try {
            newScene.getStylesheets().add(
                    SceneManager.class.getResource("/css/style.css").toExternalForm()
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load stylesheet", e);
        }

        // Set minimum size constraints
        stage.setMinWidth(DEFAULT_MIN_WIDTH);
        stage.setMinHeight(DEFAULT_MIN_HEIGHT);

        // If was maximized, first set to normal with full dimensions
        if (wasMaximized) {
            stage.setMaximized(false);
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
        }

        // Set the new scene
        stage.setScene(newScene);
        stage.setTitle(title);

        // Re-maximize if it was maximized before
        if (wasMaximized) {
            stage.setMaximized(true);
        }

        LOGGER.log(Level.INFO, "Scene switched - New maximized state: " + stage.isMaximized());
    }

    /**
     * Switch to a new scene with default dimensions (for login)
     */
    public static void switchSceneWithDefaultSize(Node source, String fxmlPath, String title) throws IOException {
        Stage stage = (Stage) source.getScene().getWindow();

        // Load new FXML
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();

        // Create new scene with default login size
        Scene scene = new Scene(root, 450, 650);

        try {
            scene.getStylesheets().add(
                    SceneManager.class.getResource("/css/style.css").toExternalForm()
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load stylesheet", e);
        }

        // Un-maximize and reset to normal window
        stage.setMaximized(false);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setWidth(450);
        stage.setHeight(650);
        stage.centerOnScreen();

        LOGGER.log(Level.INFO, "Switched to login screen");
    }
}