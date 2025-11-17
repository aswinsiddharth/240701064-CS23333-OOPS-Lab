package com.gymmanagementsystem.controller;

import com.gymmanagementsystem.dao.UserDAO;
import com.gymmanagementsystem.dao.MemberDAO;
import com.gymmanagementsystem.model.User;
import com.gymmanagementsystem.model.Member;
import com.gymmanagementsystem.util.SessionManager;
import com.gymmanagementsystem.util.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;

    // Registration fields
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private TextField regEmailField;
    @FXML private TextField regFirstNameField;
    @FXML private TextField regLastNameField;
    @FXML private TextField regPhoneField;
    @FXML private TextField regEmergencyContactField;
    @FXML private TextArea regMedicalConditionsField;
    @FXML private TabPane tabPane;

    private UserDAO userDAO = new UserDAO();
    private MemberDAO memberDAO = new MemberDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize any necessary components
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter both username and password.", false);
            return;
        }

        User user = userDAO.authenticate(username, password);
        if (user != null) {
            SessionManager.getInstance().setCurrentUser(user);
            showMessage("Login successful!", true);

            try {
                String fxmlFile = "";
                String title = "";

                switch (user.getRole()) {
                    case "ADMIN":
                        fxmlFile = "/fxml/admin-dashboard.fxml";
                        title = "Gym Management System - Admin Dashboard";
                        break;
                    case "TRAINER":
                        fxmlFile = "/fxml/trainer-dashboard.fxml";
                        title = "Gym Management System - Trainer Dashboard";
                        break;
                    case "MEMBER":
                        fxmlFile = "/fxml/member-dashboard.fxml";
                        title = "Gym Management System - Member Dashboard";
                        break;
                    default:
                        showMessage("Invalid user role.", false);
                        return;
                }

                // Load dashboard with maximized window
                loadDashboardMaximized(fxmlFile, title);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading dashboard", e);
                showMessage("Error loading dashboard.", false);
            }
        } else {
            showMessage("Invalid username or password.", false);
        }
    }

    /**
     * Load dashboard scene with maximized window
     */
    private void loadDashboardMaximized(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load());

            // Apply CSS if available
            try {
                scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not load stylesheet", e);
            }

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);

            // MAXIMIZE THE WINDOW FOR FULL SCREEN
            stage.setMaximized(true);
            stage.setResizable(true);

            // Set minimum size to prevent window from being too small
            stage.setMinWidth(1200);
            stage.setMinHeight(800);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading scene: " + fxmlFile, e);
            showMessage("Failed to load dashboard: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleRegister() {
        String username = regUsernameField.getText().trim();
        String password = regPasswordField.getText().trim();
        String email = regEmailField.getText().trim();
        String firstName = regFirstNameField.getText().trim();
        String lastName = regLastNameField.getText().trim();
        String phone = regPhoneField.getText().trim();
        String emergencyContact = regEmergencyContactField.getText().trim();
        String medicalConditions = regMedicalConditionsField.getText().trim();

        // Validate input
        if (username.isEmpty() || password.isEmpty() || email.isEmpty() ||
                firstName.isEmpty() || lastName.isEmpty()) {
            showMessage("Please fill in all required fields.", false);
            return;
        }

        // Check if username already exists
        if (userDAO.usernameExists(username)) {
            showMessage("Username already exists. Please choose another.", false);
            return;
        }

        // Create user
        User user = new User(username, password, email, "MEMBER", firstName, lastName, phone);
        if (userDAO.createUser(user)) {
            // Create member profile
            Member member = new Member(user.getId(), emergencyContact, medicalConditions,
                    1, // Default membership plan
                    new Date(System.currentTimeMillis()),
                    new Date(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))); // 30 days from now

            if (memberDAO.createMember(member)) {
                showMessage("Registration successful! Please login with your credentials.", true);
                clearRegistrationFields();
                tabPane.getSelectionModel().select(0); // Switch to login tab
            } else {
                // Rollback user creation if member creation fails
                userDAO.deleteUser(user.getId());
                showMessage("Registration failed. Please try again.", false);
            }
        } else {
            showMessage("Registration failed. Please try again.", false);
        }
    }

    private void showMessage(String message, boolean isSuccess) {
        messageLabel.setText(message);
        messageLabel.setStyle(isSuccess ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }

    private void clearRegistrationFields() {
        regUsernameField.clear();
        regPasswordField.clear();
        regEmailField.clear();
        regFirstNameField.clear();
        regLastNameField.clear();
        regPhoneField.clear();
        regEmergencyContactField.clear();
        regMedicalConditionsField.clear();
    }
}