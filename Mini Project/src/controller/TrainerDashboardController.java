package com.gymmanagementsystem.controller;

import com.gymmanagementsystem.util.SessionManager;
import com.gymmanagementsystem.dao.ClassDAO;
import com.gymmanagementsystem.model.GymClass;
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
import java.util.ResourceBundle;

public class TrainerDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private TableView<GymClass> classesTable;
    @FXML private TableColumn<GymClass, String> classNameColumn;
    @FXML private TableColumn<GymClass, String> startTimeColumn;
    @FXML private TableColumn<GymClass, String> endTimeColumn;
    @FXML private TableColumn<GymClass, Integer> currentBookingsColumn;
    @FXML private TableColumn<GymClass, Integer> maxCapacityColumn;
    @FXML private TableColumn<GymClass, String> statusColumn;

    private ClassDAO classDAO = new ClassDAO();
    private ObservableList<GymClass> classes = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String userName = SessionManager.getInstance().getCurrentUser().getFullName();
        welcomeLabel.setText("Welcome, " + userName + " (Trainer)");
        
        setupTable();
        loadTrainerClasses();
    }

    private void setupTable() {
        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        currentBookingsColumn.setCellValueFactory(new PropertyValueFactory<>("currentBookings"));
        maxCapacityColumn.setCellValueFactory(new PropertyValueFactory<>("maxCapacity"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        classesTable.setItems(classes);
    }

    private void loadTrainerClasses() {
        // Note: You would need to implement a way to get trainer ID from user ID
        // For now, we'll load all classes - in a real implementation, 
        // you'd filter by the logged-in trainer's ID
        classes.clear();
        classes.addAll(classDAO.getAllClasses());
    }

    @FXML
    private void handleClassScheduling() {
        loadScene("/fxml/class-scheduling.fxml", "Class Scheduling");
    }

    @FXML
    private void handleRefresh() {
        loadTrainerClasses();
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        loadScene("/fxml/login.fxml", "Gym Management System");
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
            e.printStackTrace();
        }
    }
}
