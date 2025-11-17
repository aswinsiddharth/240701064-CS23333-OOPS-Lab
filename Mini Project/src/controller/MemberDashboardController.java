package com.gymmanagementsystem.controller;

import com.gymmanagementsystem.util.SessionManager;
import com.gymmanagementsystem.dao.ClassDAO;
import com.gymmanagementsystem.dao.PaymentDAO;
import com.gymmanagementsystem.dao.MemberDAO;
import com.gymmanagementsystem.model.GymClass;
import com.gymmanagementsystem.model.Payment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MemberDashboardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MemberDashboardController.class.getName());

    @FXML private Label welcomeLabel;

    // Booked Classes Table
    @FXML private TableView<GymClass> bookedClassesTable;
    @FXML private TableColumn<GymClass, String> bookedClassNameColumn;
    @FXML private TableColumn<GymClass, String> bookedTrainerColumn;
    @FXML private TableColumn<GymClass, String> bookedStartTimeColumn;
    @FXML private TableColumn<GymClass, String> bookedEndTimeColumn;
    @FXML private TableColumn<GymClass, String> bookedStatusColumn;

    // Available Classes Table
    @FXML private TableView<GymClass> availableClassesTable;
    @FXML private TableColumn<GymClass, String> classNameColumn;
    @FXML private TableColumn<GymClass, String> trainerColumn;
    @FXML private TableColumn<GymClass, String> startTimeColumn;
    @FXML private TableColumn<GymClass, String> endTimeColumn;
    @FXML private TableColumn<GymClass, Integer> availableSpotsColumn;

    @FXML private TableView<Payment> paymentsTable;
    @FXML private TableColumn<Payment, String> paymentDateColumn;
    @FXML private TableColumn<Payment, Double> amountColumn;
    @FXML private TableColumn<Payment, String> paymentTypeColumn;
    @FXML private TableColumn<Payment, String> statusColumn;

    private ClassDAO classDAO = new ClassDAO();
    private PaymentDAO paymentDAO = new PaymentDAO();
    private MemberDAO memberDAO = new MemberDAO();
    private ObservableList<GymClass> bookedClasses = FXCollections.observableArrayList();
    private ObservableList<GymClass> availableClasses = FXCollections.observableArrayList();
    private ObservableList<Payment> payments = FXCollections.observableArrayList();

    private int currentMemberId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            String userName = SessionManager.getInstance().getCurrentUser().getFullName();
            welcomeLabel.setText("Welcome, " + userName + " (Member)");

            // Get member ID
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            currentMemberId = memberDAO.getMemberIdByUserId(userId);

            LOGGER.log(Level.INFO, "Member Dashboard initialized - User ID: {0}, Member ID: {1}",
                    new Object[]{userId, currentMemberId});

            if (currentMemberId == -1) {
                LOGGER.severe("Member ID not found for user ID: " + userId);
                showAlert("Member information not found. Please contact administrator.", Alert.AlertType.ERROR);
                return;
            }

            setupTables();
            loadData();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing Member Dashboard", e);
            showAlert("Error loading dashboard: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupTables() {
        // Booked Classes table
        bookedClassNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        bookedTrainerColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getTrainer() != null && cellData.getValue().getTrainer().getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTrainer().getUser().getFullName());
            }
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });
        bookedStartTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        bookedEndTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        bookedStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookedClassesTable.setItems(bookedClasses);

        // Available Classes table
        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        trainerColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getTrainer() != null && cellData.getValue().getTrainer().getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTrainer().getUser().getFullName());
            }
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        availableSpotsColumn.setCellValueFactory(cellData -> {
            GymClass gymClass = cellData.getValue();
            int available = gymClass.getMaxCapacity() - gymClass.getCurrentBookings();
            return new javafx.beans.property.SimpleIntegerProperty(available).asObject();
        });

        availableClassesTable.setItems(availableClasses);

        // Payments table
        paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paymentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        paymentsTable.setItems(payments);
    }

    private void loadData() {
        if (currentMemberId == -1) {
            showAlert("Member information not found. Please contact administrator.", Alert.AlertType.ERROR);
            return;
        }

        try {
            LOGGER.log(Level.INFO, "Loading data for member ID: {0}", currentMemberId);

            // Load booked classes for this member
            bookedClasses.clear();
            List<GymClass> booked = classDAO.getBookedClassesByMember(currentMemberId);
            LOGGER.log(Level.INFO, "Loaded {0} booked classes", booked.size());
            bookedClasses.addAll(booked);

            // Load available classes (not booked by this member)
            availableClasses.clear();
            List<GymClass> available = classDAO.getAvailableClassesForMember(currentMemberId);
            LOGGER.log(Level.INFO, "Loaded {0} available classes", available.size());
            availableClasses.addAll(available);

            // Load member's payments
            // Load member's payments (FIXED: filter by current member)
            payments.clear();
            List<Payment> paymentList = paymentDAO.getPaymentsByMember(currentMemberId);
            LOGGER.log(Level.INFO, "Loaded {0} payments for member ID: {1}",
                    new Object[]{paymentList.size(), currentMemberId});
            payments.addAll(paymentList);
            // Refresh table views
            bookedClassesTable.refresh();
            availableClassesTable.refresh();
            paymentsTable.refresh();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading data", e);
            showAlert("Error loading data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleBookClass() {
        GymClass selectedClass = availableClassesTable.getSelectionModel().getSelectedItem();
        if (selectedClass == null) {
            showAlert("Please select a class to book.", Alert.AlertType.WARNING);
            return;
        }

        if (!selectedClass.hasAvailableSpots()) {
            showAlert("This class is fully booked.", Alert.AlertType.WARNING);
            return;
        }

        if (currentMemberId == -1) {
            showAlert("Member information not found. Please contact administrator.", Alert.AlertType.ERROR);
            return;
        }

        try {
            LOGGER.log(Level.INFO, "Attempting to book class ID: {0} for member ID: {1}",
                    new Object[]{selectedClass.getId(), currentMemberId});

            // Book the class
            boolean success = classDAO.bookClass(selectedClass.getId(), currentMemberId);

            if (success) {
                showAlert("Class booked successfully!", Alert.AlertType.INFORMATION);
                loadData(); // Refresh data to move class from available to booked
            } else {
                showAlert("Failed to book class. You may have already booked this class, or it might be full.", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error booking class", e);
            showAlert("An error occurred while booking the class: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCancelBooking() {
        GymClass selectedClass = bookedClassesTable.getSelectionModel().getSelectedItem();
        if (selectedClass == null) {
            showAlert("Please select a booked class to cancel.", Alert.AlertType.WARNING);
            return;
        }

        if (currentMemberId == -1) {
            showAlert("Member information not found. Please contact administrator.", Alert.AlertType.ERROR);
            return;
        }

        // Confirm cancellation
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cancel Booking");
        confirmAlert.setHeaderText("Cancel Class Booking");
        confirmAlert.setContentText("Are you sure you want to cancel your booking for: " + selectedClass.getClassName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    LOGGER.log(Level.INFO, "Attempting to cancel booking - Class ID: {0}, Member ID: {1}",
                            new Object[]{selectedClass.getId(), currentMemberId});

                    boolean success = classDAO.cancelBooking(selectedClass.getId(), currentMemberId);

                    if (success) {
                        showAlert("Booking cancelled successfully!", Alert.AlertType.INFORMATION);
                        loadData(); // Refresh data to move class from booked to available
                    } else {
                        showAlert("Failed to cancel booking. Please try again.", Alert.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error cancelling booking", e);
                    showAlert("An error occurred while cancelling the booking: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        LOGGER.info("Refresh button clicked");
        loadData();
        showAlert("Data refreshed successfully!", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleLogout() {
        LOGGER.info("Logging out");
        SessionManager.getInstance().logout();
        loadScene("/fxml/login.fxml", "Gym Management System");
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading scene", e);
        }
    }
    // ===================================
// ADD THIS METHOD to MemberDashboardController.java
// ===================================

    /**
     * Navigate to Payment Booking page
     */
    @FXML
// ===================================
// Or if you want to use SceneManager (better approach):
// ===================================


    private void handleBookAndPay() {
        try {
            LOGGER.info("Navigating to Payment Booking page");

            // Close current stage
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();

            // Load payment booking scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment-booking.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Class Booking & Payment");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening payment booking page", e);
            showAlert("Failed to open booking page: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}