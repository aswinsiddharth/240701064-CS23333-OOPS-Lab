package com.gymmanagementsystem.controller;

import com.gymmanagementsystem.dao.MemberDAO;
import com.gymmanagementsystem.dao.UserDAO;
import com.gymmanagementsystem.model.Member;
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
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Enhanced Member Management Controller with search, filtering, and better UX
 */
public class MemberManagementController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MemberManagementController.class.getName());

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Phone validation pattern (flexible for various formats)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9\\s\\-+()]{7,20}$"
    );

    @FXML private TableView<Member> membersTable;
    @FXML private TableColumn<Member, String> nameColumn;
    @FXML private TableColumn<Member, String> emailColumn;
    @FXML private TableColumn<Member, String> phoneColumn;
    @FXML private TableColumn<Member, String> membershipStatusColumn;
    @FXML private TableColumn<Member, Date> membershipEndDateColumn;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emergencyContactField;
    @FXML private TextArea medicalConditionsField;
    @FXML private ComboBox<String> membershipStatusComboBox;
    @FXML private DatePicker membershipEndDatePicker;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    // NEW: Search and filter controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;

    private MemberDAO memberDAO = new MemberDAO();
    private UserDAO userDAO = new UserDAO();
    private ObservableList<Member> members = FXCollections.observableArrayList();
    private ObservableList<Member> allMembers = FXCollections.observableArrayList();
    private Member selectedMember = null;

    // For search debouncing
    private PauseTransition searchDelay;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupComboBoxes();
        setupSearchAndFilter();
        loadMembers();
        updateStatistics();

        // Add selection listener
        membersTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        populateFields(newValue);
                        selectedMember = newValue;
                        updateButton.setDisable(false);
                        deleteButton.setDisable(false);
                        addButton.setDisable(true);
                        updateStatusLabel("Selected: " + newValue.getUser().getFullName(), "info");
                    } else {
                        clearFields();
                        selectedMember = null;
                        updateButton.setDisable(true);
                        deleteButton.setDisable(true);
                        addButton.setDisable(false);
                        updateStatusLabel("Ready", "info");
                    }
                });

        // Auto-update expired memberships on load
        updateExpiredMemberships();
    }

    private void setupTable() {
        // Name column with null safety
        nameColumn.setCellValueFactory(cellData -> {
            Member member = cellData.getValue();
            if (member != null && member.getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        member.getUser().getFullName());
            }
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });

        // Email column with null safety
        emailColumn.setCellValueFactory(cellData -> {
            Member member = cellData.getValue();
            if (member != null && member.getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        member.getUser().getEmail());
            }
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });

        // Phone column with null safety
        phoneColumn.setCellValueFactory(cellData -> {
            Member member = cellData.getValue();
            if (member != null && member.getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        member.getUser().getPhone());
            }
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });

        // Status column with color coding
        membershipStatusColumn.setCellValueFactory(new PropertyValueFactory<>("membershipStatus"));
        membershipStatusColumn.setCellFactory(column -> new TableCell<Member, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    // Color code based on status
                    switch (status) {
                        case "ACTIVE":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "EXPIRED":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        case "SUSPENDED":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case "EXPIRING_SOON":
                            setStyle("-fx-text-fill: #FFA500; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        // End date column with formatting
        membershipEndDateColumn.setCellValueFactory(new PropertyValueFactory<>("membershipEndDate"));
        membershipEndDateColumn.setCellFactory(column -> new TableCell<Member, Date>() {
            @Override
            protected void updateItem(Date date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.toString());
                    // Highlight if expiring soon
                    LocalDate endDate = date.toLocalDate();
                    LocalDate today = LocalDate.now();
                    long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);

                    if (daysRemaining < 0) {
                        setStyle("-fx-text-fill: red;");
                    } else if (daysRemaining <= 7) {
                        setStyle("-fx-text-fill: orange;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        membersTable.setItems(members);

        // Enable multi-selection for batch operations
        membersTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void setupComboBoxes() {
        // Membership status options
        membershipStatusComboBox.getItems().addAll("ACTIVE", "EXPIRED", "SUSPENDED");
        membershipStatusComboBox.setValue("ACTIVE");

        // Filter options
        if (filterStatusComboBox != null) {
            filterStatusComboBox.getItems().addAll("All", "ACTIVE", "EXPIRED", "SUSPENDED", "EXPIRING_SOON");
            filterStatusComboBox.setValue("All");
            filterStatusComboBox.setOnAction(e -> applyFilter());
        }
    }

    private void setupSearchAndFilter() {
        // Setup search with debouncing (wait 500ms after typing stops)
        if (searchField != null) {
            searchDelay = new PauseTransition(Duration.millis(500));
            searchDelay.setOnFinished(e -> performSearch());

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                searchDelay.playFromStart();
            });
        }
    }

    private void loadMembers() {
        try {
            allMembers.clear();
            allMembers.addAll(memberDAO.getAllMembers());
            members.clear();
            members.addAll(allMembers);
            updateStatusLabel("Loaded " + members.size() + " members", "success");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading members", e);
            updateStatusLabel("Error loading members", "error");
            showAlert("Failed to load members: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAdd() {
        if (!validateInput(true)) {
            return;
        }

        try {
            // Check for duplicate username/email
            if (isDuplicateUser()) {
                showAlert("Username or email already exists!", Alert.AlertType.WARNING);
                return;
            }

            // Create user first
            User user = new User(
                    usernameField.getText().trim(),
                    passwordField.getText(),
                    emailField.getText().trim(),
                    "MEMBER",
                    firstNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    phoneField.getText().trim()
            );

            if (userDAO.createUser(user)) {
                // Create member
                Member member = new Member(
                        user.getId(),
                        emergencyContactField.getText().trim(),
                        medicalConditionsField.getText().trim(),
                        1, // Default plan (you can add plan selection)
                        new Date(System.currentTimeMillis()),
                        Date.valueOf(membershipEndDatePicker.getValue())
                );
                member.setMembershipStatus(membershipStatusComboBox.getValue());

                if (memberDAO.createMember(member)) {
                    showAlert("Member '" + user.getFullName() + "' added successfully!",
                            Alert.AlertType.INFORMATION);
                    clearFields();
                    loadMembers();
                    updateStatistics();
                    updateStatusLabel("Member added successfully", "success");
                } else {
                    userDAO.deleteUser(user.getId()); // Rollback
                    showAlert("Failed to create member record.", Alert.AlertType.ERROR);
                    updateStatusLabel("Failed to add member", "error");
                }
            } else {
                showAlert("Failed to create user account.", Alert.AlertType.ERROR);
                updateStatusLabel("Failed to create user account", "error");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding member", e);
            showAlert("Error: " + e.getMessage(), Alert.AlertType.ERROR);
            updateStatusLabel("Error adding member", "error");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedMember == null) {
            showAlert("Please select a member to update.", Alert.AlertType.WARNING);
            return;
        }

        if (!validateInput(false)) {
            return;
        }

        try {
            // Get fresh user data from database to ensure we have the correct user ID
            User user = userDAO.getUserById(selectedMember.getUserId());

            if (user == null) {
                LOGGER.severe("User not found for member ID: " + selectedMember.getId());
                showAlert("User record not found. Please contact administrator.", Alert.AlertType.ERROR);
                updateStatusLabel("User record not found", "error");
                return;
            }

            LOGGER.log(Level.INFO, "=== UPDATE MEMBER START ===");
            LOGGER.log(Level.INFO, "Member ID: {0}, User ID: {1}",
                    new Object[]{selectedMember.getId(), user.getId()});

            // Update user information
            user.setFirstName(firstNameField.getText().trim());
            user.setLastName(lastNameField.getText().trim());
            user.setEmail(emailField.getText().trim());
            user.setPhone(phoneField.getText().trim());
            user.setUsername(usernameField.getText().trim());

            // Update password only if provided
            String newPassword = passwordField.getText().trim();
            if (!newPassword.isEmpty()) {
                LOGGER.info("Updating password");
                user.setPassword(newPassword);
            } else {
                LOGGER.info("Keeping existing password");
                // Set password to null so updateUser knows not to update it
                user.setPassword(null);
            }

            LOGGER.info("Attempting to update user in database...");
            boolean userUpdated = userDAO.updateUser(user);
            LOGGER.log(Level.INFO, "User update result: {0}", userUpdated);

            if (userUpdated) {
                // Update member information
                selectedMember.setEmergencyContact(emergencyContactField.getText().trim());
                selectedMember.setMedicalConditions(medicalConditionsField.getText().trim());
                selectedMember.setMembershipStatus(membershipStatusComboBox.getValue());
                selectedMember.setMembershipEndDate(Date.valueOf(membershipEndDatePicker.getValue()));

                LOGGER.info("Attempting to update member in database...");
                boolean memberUpdated = memberDAO.updateMember(selectedMember);
                LOGGER.log(Level.INFO, "Member update result: {0}", memberUpdated);

                if (memberUpdated) {
                    showAlert("Member '" + user.getFullName() + "' updated successfully!",
                            Alert.AlertType.INFORMATION);
                    clearFields();
                    loadMembers();
                    updateStatistics();
                    updateStatusLabel("Member updated successfully", "success");
                    LOGGER.info("=== UPDATE MEMBER SUCCESS ===");
                } else {
                    showAlert("Failed to update member information.", Alert.AlertType.ERROR);
                    updateStatusLabel("Failed to update member", "error");
                    LOGGER.warning("Member update failed");
                }
            } else {
                showAlert("Failed to update user information.", Alert.AlertType.ERROR);
                updateStatusLabel("Failed to update user", "error");
                LOGGER.warning("User update failed");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "=== EXCEPTION DURING UPDATE ===");
            LOGGER.log(Level.SEVERE, "Error updating member: " + e.getMessage(), e);
            showAlert("Error: " + e.getMessage(), Alert.AlertType.ERROR);
            updateStatusLabel("Error updating member", "error");
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedMember == null) {
            showAlert("Please select a member to delete.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete Member: " + selectedMember.getUser().getFullName());
            confirmation.setContentText("Are you sure you want to delete this member?\nThis action cannot be undone!");

            if (confirmation.showAndWait().get() == ButtonType.OK) {
                if (memberDAO.deleteMember(selectedMember.getId())) {
                    userDAO.deleteUser(selectedMember.getUserId());
                    showAlert("Member deleted successfully!", Alert.AlertType.INFORMATION);
                    clearFields();
                    loadMembers();
                    updateStatistics();
                    updateStatusLabel("Member deleted", "success");
                } else {
                    showAlert("Failed to delete member.", Alert.AlertType.ERROR);
                    updateStatusLabel("Failed to delete member", "error");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting member", e);
            showAlert("Error: " + e.getMessage(), Alert.AlertType.ERROR);
            updateStatusLabel("Error deleting member", "error");
        }
    }

    @FXML
    private void handleRefresh() {
        loadMembers();
        updateStatistics();
        clearFields();
        if (searchField != null) searchField.clear();
        if (filterStatusComboBox != null) filterStatusComboBox.setValue("All");
        updateStatusLabel("Data refreshed", "success");
    }

    @FXML
    private void handleClear() {
        clearFields();
        membersTable.getSelectionModel().clearSelection();
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
            members.setAll(allMembers);
            applyFilter();
            return;
        }

        try {
            List<Member> searchResults = memberDAO.searchMembers(searchTerm);
            members.setAll(searchResults);
            updateStatusLabel("Found " + searchResults.size() + " member(s)", "info");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching members", e);
            updateStatusLabel("Search error", "error");
        }
    }

    // NEW: Filter functionality
    private void applyFilter() {
        if (filterStatusComboBox == null) return;

        String selectedStatus = filterStatusComboBox.getValue();

        if ("All".equals(selectedStatus)) {
            if (searchField != null && !searchField.getText().trim().isEmpty()) {
                performSearch();
            } else {
                members.setAll(allMembers);
            }
        } else {
            try {
                List<Member> filteredMembers = memberDAO.getMembersByStatus(selectedStatus);
                members.setAll(filteredMembers);
                updateStatusLabel("Filtered: " + filteredMembers.size() + " " + selectedStatus + " member(s)", "info");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error filtering members", e);
                updateStatusLabel("Filter error", "error");
            }
        }
    }

    // NEW: Update expired memberships
    private void updateExpiredMemberships() {
        try {
            int updated = memberDAO.updateExpiredMemberships();
            if (updated > 0) {
                LOGGER.log(Level.INFO, "Updated {0} expired memberships", updated);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not update expired memberships", e);
        }
    }

    // NEW: Update statistics display
    private void updateStatistics() {
        if (statsLabel == null) return;

        try {
            MemberDAO.MembershipStats stats = memberDAO.getMembershipStats();
            statsLabel.setText(String.format(
                    "Total: %d | Active: %d | Expired: %d | Suspended: %d",
                    stats.getTotal(), stats.getActive(), stats.getExpired(), stats.getSuspended()
            ));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not update statistics", e);
        }
    }

    private void populateFields(Member member) {
        if (member == null) return;

        try {
            if (member.getUser() != null) {
                firstNameField.setText(member.getUser().getFirstName());
                lastNameField.setText(member.getUser().getLastName());
                emailField.setText(member.getUser().getEmail());
                phoneField.setText(member.getUser().getPhone());
                usernameField.setText(member.getUser().getUsername());
            }

            emergencyContactField.setText(member.getEmergencyContact() != null ?
                    member.getEmergencyContact() : "");
            medicalConditionsField.setText(member.getMedicalConditions() != null ?
                    member.getMedicalConditions() : "");
            membershipStatusComboBox.setValue(member.getMembershipStatus());

            if (member.getMembershipEndDate() != null) {
                membershipEndDatePicker.setValue(member.getMembershipEndDate().toLocalDate());
            }

            passwordField.clear(); // Don't show password
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
        emergencyContactField.clear();
        medicalConditionsField.clear();
        membershipStatusComboBox.setValue("ACTIVE");
        membershipEndDatePicker.setValue(null);
        passwordField.setPromptText("Password");
    }

    private boolean validateInput(boolean isNewMember) {
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

        // Password validation (only for new members)
        if (isNewMember) {
            if (passwordField.getText().trim().isEmpty()) {
                errors.append("• Password is required\n");
            } else if (passwordField.getText().length() < 6) {
                errors.append("• Password must be at least 6 characters\n");
            }
        } else {
            // For updates, validate only if password is provided
            if (!passwordField.getText().trim().isEmpty() && passwordField.getText().length() < 6) {
                errors.append("• Password must be at least 6 characters\n");
            }
        }

        // Emergency contact validation
        String emergencyContact = emergencyContactField.getText().trim();
        if (!emergencyContact.isEmpty() && !PHONE_PATTERN.matcher(emergencyContact).matches()) {
            errors.append("• Invalid emergency contact format\n");
        }

        // Membership end date validation
        if (membershipEndDatePicker.getValue() == null) {
            errors.append("• Membership end date is required\n");
        } else if (membershipEndDatePicker.getValue().isBefore(LocalDate.now())) {
            errors.append("• Membership end date cannot be in the past\n");
        }

        if (errors.length() > 0) {
            showAlert("Please correct the following errors:\n\n" + errors.toString(),
                    Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    private boolean isDuplicateUser() {
        // This would need implementation in UserDAO
        // For now, return false
        return false;
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

            // Apply CSS if available
            try {
                scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not load stylesheet", e);
            }

            Stage stage = (Stage) membersTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading scene: " + fxmlFile, e);
            showAlert("Failed to load screen: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}