package com.gymmanagementsystem.controller;

import com.gymmanagementsystem.dao.TrainerDAO;
import com.gymmanagementsystem.dao.UserDAO;
import com.gymmanagementsystem.model.Trainer;
import com.gymmanagementsystem.model.User;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Enhanced Trainer Management Controller with search, filtering, and better UX
 */
public class TrainerManagementController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(TrainerManagementController.class.getName());

    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9\\s\\-+()]{7,20}$"
    );
    private static final Pattern RATE_PATTERN = Pattern.compile(
            "^\\d+(\\.\\d{1,2})?$"
    );

    @FXML private TableView<Trainer> trainersTable;
    @FXML private TableColumn<Trainer, String> nameColumn;
    @FXML private TableColumn<Trainer, String> emailColumn;
    @FXML private TableColumn<Trainer, String> phoneColumn;
    @FXML private TableColumn<Trainer, String> specializationColumn;
    @FXML private TableColumn<Trainer, BigDecimal> hourlyRateColumn;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField specializationField;
    @FXML private TextArea certificationsField;
    @FXML private TextField hourlyRateField;
    @FXML private TextArea availabilityField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    // NEW: Search and filter controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterSpecializationComboBox;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;

    private TrainerDAO trainerDAO = new TrainerDAO();
    private UserDAO userDAO = new UserDAO();
    private ObservableList<Trainer> trainers = FXCollections.observableArrayList();
    private ObservableList<Trainer> allTrainers = FXCollections.observableArrayList();
    private Trainer selectedTrainer = null;

    // For search debouncing
    private PauseTransition searchDelay;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("🔄 Initializing Trainer Management...");

        setupTable();
        setupComboBoxes();
        setupSearchAndFilter();
        loadTrainers();
        updateStatistics();

        // Add selection listener
        trainersTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        populateFields(newValue);
                        selectedTrainer = newValue;
                        updateButton.setDisable(false);
                        deleteButton.setDisable(false);
                        addButton.setDisable(true);
                        updateStatusLabel("Selected: " + newValue.getUser().getFullName(), "info");
                    } else {
                        clearFields();
                        selectedTrainer = null;
                        updateButton.setDisable(true);
                        deleteButton.setDisable(true);
                        addButton.setDisable(false);
                        updateStatusLabel("Ready", "info");
                    }
                });

        System.out.println("✅ Trainer Management initialized successfully");
    }

    private void setupTable() {
        // Name column with null safety
        nameColumn.setCellValueFactory(cellData -> {
            Trainer trainer = cellData.getValue();
            if (trainer != null && trainer.getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        trainer.getUser().getFullName());
            }
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });

        // Email column with null safety
        emailColumn.setCellValueFactory(cellData -> {
            Trainer trainer = cellData.getValue();
            if (trainer != null && trainer.getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        trainer.getUser().getEmail());
            }
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });

        // Phone column with null safety
        phoneColumn.setCellValueFactory(cellData -> {
            Trainer trainer = cellData.getValue();
            if (trainer != null && trainer.getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        trainer.getUser().getPhone());
            }
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });

        // Specialization column with styling
        specializationColumn.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        specializationColumn.setCellFactory(column -> new TableCell<Trainer, String>() {
            @Override
            protected void updateItem(String specialization, boolean empty) {
                super.updateItem(specialization, empty);
                if (empty || specialization == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(specialization);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
                }
            }
        });

        // Hourly rate column with formatting
        hourlyRateColumn.setCellValueFactory(new PropertyValueFactory<>("hourlyRate"));
        hourlyRateColumn.setCellFactory(column -> new TableCell<Trainer, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal rate, boolean empty) {
                super.updateItem(rate, empty);
                if (empty || rate == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("$" + String.format("%.2f", rate));

                    // Color code based on rate
                    if (rate.compareTo(new BigDecimal("50.00")) >= 0) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if (rate.compareTo(new BigDecimal("25.00")) >= 0) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #666666;");
                    }
                }
            }
        });

        trainersTable.setItems(trainers);
        trainersTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void setupComboBoxes() {
        if (filterSpecializationComboBox != null) {
            filterSpecializationComboBox.getItems().addAll(
                    "All Specializations",
                    "Yoga",
                    "Cardio",
                    "Strength Training",
                    "Pilates",
                    "CrossFit",
                    "Zumba",
                    "Boxing",
                    "Swimming",
                    "Personal Training"
            );
            filterSpecializationComboBox.setValue("All Specializations");
            filterSpecializationComboBox.setOnAction(e -> applyFilter());
        }
    }

    private void setupSearchAndFilter() {
        if (searchField != null) {
            searchDelay = new PauseTransition(Duration.millis(500));
            searchDelay.setOnFinished(e -> performSearch());

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                searchDelay.playFromStart();
            });
        }
    }

    private void loadTrainers() {
        try {
            System.out.println("🔄 Loading trainers from database...");

            allTrainers.clear();
            List<Trainer> trainerList = trainerDAO.getAllTrainers();
            allTrainers.addAll(trainerList);

            trainers.clear();
            trainers.addAll(allTrainers);

            // Force table refresh
            trainersTable.refresh();

            System.out.println("✅ Successfully loaded " + trainers.size() + " trainers");
            updateStatusLabel("Loaded " + trainers.size() + " trainers", "success");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading trainers", e);
            System.err.println("❌ Error loading trainers: " + e.getMessage());
            e.printStackTrace();
            updateStatusLabel("Error loading trainers", "error");
            showAlert("Failed to load trainers: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAdd() {
        System.out.println("🔄 Adding new trainer...");

        if (!validateInput(true)) {
            return;
        }

        try {
            // Create user first
            User user = new User(
                    usernameField.getText().trim(),
                    passwordField.getText(),
                    emailField.getText().trim(),
                    "TRAINER",
                    firstNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    phoneField.getText().trim()
            );

            System.out.println("Creating user: " + user.getUsername());

            if (userDAO.createUser(user)) {
                System.out.println("✅ User created with ID: " + user.getId());

                // Create trainer
                Trainer trainer = new Trainer(
                        user.getId(),
                        specializationField.getText().trim(),
                        certificationsField.getText().trim(),
                        new BigDecimal(hourlyRateField.getText().trim()),
                        availabilityField.getText().trim()
                );

                System.out.println("Creating trainer for user ID: " + user.getId());

                if (trainerDAO.createTrainer(trainer)) {
                    System.out.println("✅ Trainer created successfully!");

                    showAlert("Trainer '" + user.getFullName() + "' added successfully!",
                            Alert.AlertType.INFORMATION);

                    // Clear form first
                    clearFields();

                    // CRITICAL: Reload trainers from database
                    System.out.println("🔄 Reloading trainers table...");
                    loadTrainers();

                    // Update statistics
                    updateStatistics();

                    updateStatusLabel("Trainer added successfully", "success");

                    System.out.println("✅ Add trainer operation completed!");

                } else {
                    System.err.println("❌ Failed to create trainer record");
                    userDAO.deleteUser(user.getId()); // Rollback
                    showAlert("Failed to create trainer record.", Alert.AlertType.ERROR);
                    updateStatusLabel("Failed to add trainer", "error");
                }
            } else {
                System.err.println("❌ Failed to create user account");
                showAlert("Failed to create user account. Username or email may already exist.",
                        Alert.AlertType.ERROR);
                updateStatusLabel("Failed to create user account", "error");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding trainer", e);
            System.err.println("❌ Exception while adding trainer: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error: " + e.getMessage(), Alert.AlertType.ERROR);
            updateStatusLabel("Error adding trainer", "error");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedTrainer == null) {
            showAlert("Please select a trainer to update.", Alert.AlertType.WARNING);
            return;
        }

        if (!validateInput(false)) {
            return;
        }

        try {
            // Update user information
            User user = selectedTrainer.getUser();
            user.setFirstName(firstNameField.getText().trim());
            user.setLastName(lastNameField.getText().trim());
            user.setEmail(emailField.getText().trim());
            user.setPhone(phoneField.getText().trim());
            user.setUsername(usernameField.getText().trim());

            // Update password only if provided
            if (!passwordField.getText().trim().isEmpty()) {
                user.setPassword(passwordField.getText());
            }

            if (userDAO.updateUser(user)) {
                // Update trainer information
                selectedTrainer.setSpecialization(specializationField.getText().trim());
                selectedTrainer.setCertifications(certificationsField.getText().trim());
                selectedTrainer.setHourlyRate(new BigDecimal(hourlyRateField.getText().trim()));
                selectedTrainer.setAvailability(availabilityField.getText().trim());

                if (trainerDAO.updateTrainer(selectedTrainer)) {
                    showAlert("Trainer '" + user.getFullName() + "' updated successfully!",
                            Alert.AlertType.INFORMATION);
                    clearFields();
                    loadTrainers();
                    updateStatistics();
                    updateStatusLabel("Trainer updated successfully", "success");
                } else {
                    showAlert("Failed to update trainer information.", Alert.AlertType.ERROR);
                    updateStatusLabel("Failed to update trainer", "error");
                }
            } else {
                showAlert("Failed to update user information.", Alert.AlertType.ERROR);
                updateStatusLabel("Failed to update user", "error");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating trainer", e);
            showAlert("Error: " + e.getMessage(), Alert.AlertType.ERROR);
            updateStatusLabel("Error updating trainer", "error");
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedTrainer == null) {
            showAlert("Please select a trainer to delete.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete Trainer: " + selectedTrainer.getUser().getFullName());
            confirmation.setContentText("Are you sure you want to delete this trainer?\n" +
                    "This action cannot be undone!");

            if (confirmation.showAndWait().get() == ButtonType.OK) {
                if (trainerDAO.deleteTrainer(selectedTrainer.getId())) {
                    userDAO.deleteUser(selectedTrainer.getUserId());
                    showAlert("Trainer deleted successfully!", Alert.AlertType.INFORMATION);
                    clearFields();
                    loadTrainers();
                    updateStatistics();
                    updateStatusLabel("Trainer deleted", "success");
                } else {
                    showAlert("Failed to delete trainer.", Alert.AlertType.ERROR);
                    updateStatusLabel("Failed to delete trainer", "error");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting trainer", e);
            showAlert("Error: " + e.getMessage(), Alert.AlertType.ERROR);
            updateStatusLabel("Error deleting trainer", "error");
        }
    }

    @FXML
    private void handleRefresh() {
        System.out.println("🔄 Manual refresh triggered");
        loadTrainers();
        updateStatistics();
        clearFields();
        if (searchField != null) searchField.clear();
        if (filterSpecializationComboBox != null)
            filterSpecializationComboBox.setValue("All Specializations");
        updateStatusLabel("Data refreshed", "success");
    }

    @FXML
    private void handleClear() {
        clearFields();
        trainersTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleBack() {
        loadScene("/fxml/admin-dashboard.fxml", "Admin Dashboard");
    }

    // NEW: Search functionality
    private void performSearch() {
        if (searchField == null) return;

        String searchTerm = searchField.getText().trim();

        if (searchTerm.isEmpty()) {
            trainers.setAll(allTrainers);
            applyFilter();
            return;
        }

        try {
            List<Trainer> searchResults = trainerDAO.searchTrainers(searchTerm);
            trainers.setAll(searchResults);
            trainersTable.refresh();
            updateStatusLabel("Found " + searchResults.size() + " trainer(s)", "info");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching trainers", e);
            updateStatusLabel("Search error", "error");
        }
    }

    // NEW: Filter functionality
    private void applyFilter() {
        if (filterSpecializationComboBox == null) return;

        String selectedSpec = filterSpecializationComboBox.getValue();

        if ("All Specializations".equals(selectedSpec)) {
            if (searchField != null && !searchField.getText().trim().isEmpty()) {
                performSearch();
            } else {
                trainers.setAll(allTrainers);
                trainersTable.refresh();
            }
        } else {
            try {
                List<Trainer> filteredTrainers = trainerDAO.getTrainersBySpecialization(selectedSpec);
                trainers.setAll(filteredTrainers);
                trainersTable.refresh();
                updateStatusLabel("Filtered: " + filteredTrainers.size() + " " +
                        selectedSpec + " trainer(s)", "info");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error filtering trainers", e);
                updateStatusLabel("Filter error", "error");
            }
        }
    }

    // NEW: Update statistics display
    private void updateStatistics() {
        if (statsLabel == null) return;

        try {
            TrainerDAO.TrainerStats stats = trainerDAO.getTrainerStats();
            statsLabel.setText(String.format(
                    "Total: %d | Avg Rate: $%.2f | Min: $%.2f | Max: $%.2f",
                    stats.getTotal(),
                    stats.getAverageRate(),
                    stats.getMinRate(),
                    stats.getMaxRate()
            ));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not update statistics", e);
        }
    }

    private void populateFields(Trainer trainer) {
        if (trainer == null) return;

        try {
            if (trainer.getUser() != null) {
                firstNameField.setText(trainer.getUser().getFirstName());
                lastNameField.setText(trainer.getUser().getLastName());
                emailField.setText(trainer.getUser().getEmail());
                phoneField.setText(trainer.getUser().getPhone());
                usernameField.setText(trainer.getUser().getUsername());
            }

            specializationField.setText(trainer.getSpecialization() != null ?
                    trainer.getSpecialization() : "");
            certificationsField.setText(trainer.getCertifications() != null ?
                    trainer.getCertifications() : "");
            hourlyRateField.setText(trainer.getHourlyRate() != null ?
                    trainer.getHourlyRate().toString() : "");
            availabilityField.setText(trainer.getAvailability() != null ?
                    trainer.getAvailability() : "");

            passwordField.clear();
            passwordField.setPromptText("Leave empty to keep current password");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error populating fields", e);
        }
    }

    private void clearFields() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        usernameField.clear();
        passwordField.clear();
        specializationField.clear();
        certificationsField.clear();
        hourlyRateField.clear();
        availabilityField.clear();
        passwordField.setPromptText("Password");
    }

    private boolean validateInput(boolean isNewTrainer) {
        StringBuilder errors = new StringBuilder();

        // Required fields validation
        if (firstNameField.getText().trim().isEmpty()) {
            errors.append("• First name is required\n");
        }

        if (lastNameField.getText().trim().isEmpty()) {
            errors.append("• Last name is required\n");
        }

        // Email validation
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            errors.append("• Email is required\n");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.append("• Invalid email format\n");
        }

        // Phone validation
        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            errors.append("• Invalid phone number format\n");
        }

        // Username validation
        if (usernameField.getText().trim().isEmpty()) {
            errors.append("• Username is required\n");
        } else if (usernameField.getText().trim().length() < 3) {
            errors.append("• Username must be at least 3 characters\n");
        }

        // Password validation (only for new trainers)
        if (isNewTrainer) {
            if (passwordField.getText().trim().isEmpty()) {
                errors.append("• Password is required\n");
            } else if (passwordField.getText().length() < 6) {
                errors.append("• Password must be at least 6 characters\n");
            }
        } else {
            if (!passwordField.getText().trim().isEmpty() && passwordField.getText().length() < 6) {
                errors.append("• Password must be at least 6 characters\n");
            }
        }

        // Specialization validation
        if (specializationField.getText().trim().isEmpty()) {
            errors.append("• Specialization is required\n");
        }

        // Hourly rate validation
        String rateText = hourlyRateField.getText().trim();
        if (rateText.isEmpty()) {
            errors.append("• Hourly rate is required\n");
        } else {
            if (!RATE_PATTERN.matcher(rateText).matches()) {
                errors.append("• Hourly rate must be a valid number (e.g., 25.00)\n");
            } else {
                try {
                    BigDecimal rate = new BigDecimal(rateText);
                    if (rate.compareTo(new BigDecimal("10.00")) < 0) {
                        errors.append("• Hourly rate must be at least $10.00\n");
                    }
                    if (rate.compareTo(new BigDecimal("200.00")) > 0) {
                        errors.append("• Hourly rate cannot exceed $200.00\n");
                    }
                } catch (NumberFormatException e) {
                    errors.append("• Invalid hourly rate format\n");
                }
            }
        }

        if (errors.length() > 0) {
            showAlert("Please correct the following errors:\n\n" + errors.toString(),
                    Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    private void updateStatusLabel(String message, String type) {
        if (statusLabel == null) return;

        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-success", "status-error", "status-info");

        switch (type) {
            case "success":
                statusLabel.getStyleClass().add("status-success");
                break;
            case "error":
                statusLabel.getStyleClass().add("status-error");
                break;
            case "info":
            default:
                statusLabel.getStyleClass().add("status-info");
                break;
        }

        // Auto-clear after 5 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> statusLabel.setText("Ready"));
        pause.play();
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" :
                type == Alert.AlertType.WARNING ? "Warning" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load());

            try {
                scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not load stylesheet", e);
            }

            Stage stage = (Stage) trainersTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading scene: " + fxmlFile, e);
            showAlert("Failed to load screen: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}