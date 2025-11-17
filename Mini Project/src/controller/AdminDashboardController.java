package com.gymmanagementsystem.controller;

import com.gymmanagementsystem.util.SessionManager;
import com.gymmanagementsystem.util.SceneManager;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Label welcomeLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String userName = SessionManager.getInstance().getCurrentUser().getFullName();
        welcomeLabel.setText("Welcome, " + userName + " (Admin)");
    }

    @FXML
    private void handleMemberManagement() {
        loadScene("/fxml/member-management.fxml", "Member Management");
    }

    @FXML
    private void handleTrainerManagement() {
        loadScene("/fxml/trainer-management.fxml", "Trainer Management");
    }

    @FXML
    private void handleClassScheduling() {
        loadScene("/fxml/class-scheduling.fxml", "Class Scheduling");
    }

    @FXML
    private void handlePaymentManagement() {
        loadScene("/fxml/payment-management.fxml", "Payment Management");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        loadSceneWithDefaultSize("/fxml/login.fxml", "Gym Management System");
    }

    // Hover effect for cards
    @FXML
    private void onCardHover(MouseEvent event) {
        VBox card = (VBox) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
        st.setToX(1.05);
        st.setToY(1.05);
        st.play();

        // Add shadow effect
        card.setStyle(card.getStyle() + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 8);");
    }

    @FXML
    private void onCardExit(MouseEvent event) {
        VBox card = (VBox) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();

        // Reset shadow effect
        String style = card.getStyle();
        style = style.replaceAll("; -fx-effect: dropshadow\\(gaussian, rgba\\(0,0,0,0\\.25\\), 20, 0, 0, 8\\);", "");
        card.setStyle(style + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);");
    }

    // Hover effect for logout button
    @FXML
    private void onLogoutHover(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 25;");
    }

    @FXML
    private void onLogoutExit(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 25;");
    }

    private void loadScene(String fxmlFile, String title) {
        try {
            // Use SceneManager to preserve current window size
            SceneManager.switchScene(welcomeLabel, fxmlFile, title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSceneWithDefaultSize(String fxmlFile, String title) {
        try {
            // Use SceneManager with default size for logout
            SceneManager.switchSceneWithDefaultSize(welcomeLabel, fxmlFile, title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}