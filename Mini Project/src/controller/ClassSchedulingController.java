package com.gymmanagementsystem.controller;

import com.gymmanagementsystem.dao.ClassDAO;
import com.gymmanagementsystem.dao.TrainerDAO;
import com.gymmanagementsystem.model.GymClass;
import com.gymmanagementsystem.model.Trainer;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Enhanced ClassSchedulingController with improved UX, validation, and features
 */
public class ClassSchedulingController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ClassSchedulingController.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Table and columns
    @FXML private TableView<GymClass> classesTable;
    @FXML private TableColumn<GymClass, String> classNameColumn;
    @FXML private TableColumn<GymClass, String> trainerColumn;
    @FXML private TableColumn<GymClass, Timestamp> startTimeColumn;
    @FXML private TableColumn<GymClass, Timestamp> endTimeColumn;
    @FXML private TableColumn<GymClass, Integer> currentBookingsColumn;
    @FXML private TableColumn<GymClass, Integer> maxCapacityColumn;
    @FXML private TableColumn<GymClass, String> statusColumn;

    // Form fields
    @FXML private TextField classNameField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<Trainer> trainerComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startTimeField;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endTimeField;
    @FXML private TextField maxCapacityField;
    @FXML private ComboBox<String> statusComboBox;

    // Buttons
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;

    // Search and filter
    @FXML private TextField searchField;
    @FXML private DatePicker filterDatePicker;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private Button refreshButton;

    // Labels for feedback
    @FXML private Label feedbackLabel;
    @FXML private Label durationLabel;
    @FXML private Label conflictWarningLabel;

    // Data
    private ClassDAO classDAO = new ClassDAO();
    private TrainerDAO trainerDAO = new TrainerDAO();
    private ObservableList<GymClass> classes = FXCollections.observableArrayList();
    private ObservableList<Trainer> trainers = FXCollections.observableArrayList();
    private FilteredList<GymClass> filteredClasses;
    private GymClass selectedClass = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupComboBoxes();
        setupSearchAndFilter();
        setupValidation();
        loadData();
        setupListeners();

        // Auto-update statuses periodically
        startAutoUpdate();
    }

    /**
     * Setup table columns and formatting
     */
    private void setupTable() {
        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));

        // Trainer column with name formatting
        trainerColumn.setCellValueFactory(cellData -> {
            GymClass gc = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(gc.getTrainerName());
        });

        // Format start time
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startTimeColumn.setCellFactory(column -> new TableCell<GymClass, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                            .format(item.toLocalDateTime()));
                }
            }
        });

        // Format end time
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        endTimeColumn.setCellFactory(column -> new TableCell<GymClass, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                            .format(item.toLocalDateTime()));
                }
            }
        });

        // Bookings with visual indicator
        currentBookingsColumn.setCellValueFactory(new PropertyValueFactory<>("currentBookings"));
        currentBookingsColumn.setCellFactory(column -> new TableCell<GymClass, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    GymClass gc = getTableView().getItems().get(getIndex());
                    setText(item + "/" + gc.getMaxCapacity());

                    // Color code based on capacity
                    double occupancy = gc.getOccupancyRate();
                    if (occupancy >= 90) {
                        setStyle("-fx-background-color: #ffcccc;"); // Red
                    } else if (occupancy >= 70) {
                        setStyle("-fx-background-color: #fff3cd;"); // Yellow
                    } else {
                        setStyle("-fx-background-color: #d4edda;"); // Green
                    }
                }
            }
        });

        maxCapacityColumn.setCellValueFactory(new PropertyValueFactory<>("maxCapacity"));

        // Status with color coding
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new TableCell<GymClass, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "SCHEDULED":
                            setStyle("-fx-text-fill: #0066cc; -fx-font-weight: bold;");
                            break;
                        case "IN_PROGRESS":
                            setStyle("-fx-text-fill: #ff9900; -fx-font-weight: bold;");
                            break;
                        case "COMPLETED":
                            setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                            break;
                        case "CANCELLED":
                            setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                            break;
                        case "FULL":
                            setStyle("-fx-text-fill: #6c757d; -fx-font-weight: bold;");
                            break;
                    }
                }
            }
        });

        classesTable.setItems(classes);

        // Enable row selection
        classesTable.setRowFactory(tv -> {
            TableRow<GymClass> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleRowDoubleClick(row.getItem());
                }
            });
            return row;
        });
    }

    /**
     * Setup combo boxes
     */
    private void setupComboBoxes() {
        // Trainer combo box
        trainerComboBox.setItems(trainers);
        trainerComboBox.setCellFactory(listView -> new ListCell<Trainer>() {
            @Override
            protected void updateItem(Trainer trainer, boolean empty) {
                super.updateItem(trainer, empty);
                if (empty || trainer == null || trainer.getUser() == null) {
                    setText(null);
                } else {
                    setText(trainer.getUser().getFullName() + " - " + trainer.getSpecialization());
                }
            }
        });
        trainerComboBox.setButtonCell(new ListCell<Trainer>() {
            @Override
            protected void updateItem(Trainer trainer, boolean empty) {
                super.updateItem(trainer, empty);
                if (empty || trainer == null || trainer.getUser() == null) {
                    setText(null);
                } else {
                    setText(trainer.getUser().getFullName());
                }
            }
        });

        // Status combo box
        statusComboBox.getItems().addAll(
                GymClass.STATUS_SCHEDULED,
                GymClass.STATUS_IN_PROGRESS,
                GymClass.STATUS_COMPLETED,
                GymClass.STATUS_CANCELLED
        );
        statusComboBox.setValue(GymClass.STATUS_SCHEDULED);

        // Filter status combo box (if exists)
        if (filterStatusComboBox != null) {
            filterStatusComboBox.getItems().addAll(
                    "All",
                    GymClass.STATUS_SCHEDULED,
                    GymClass.STATUS_IN_PROGRESS,
                    GymClass.STATUS_COMPLETED,
                    GymClass.STATUS_CANCELLED,
                    GymClass.STATUS_FULL
            );
            filterStatusComboBox.setValue("All");
        }
    }

    /**
     * Setup search and filter functionality
     */
    private void setupSearchAndFilter() {
        // Wrap observable list in filtered list
        filteredClasses = new FilteredList<>(classes, p -> true);

        // Search field listener
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterClasses();
            });
        }

        // Date filter listener
        if (filterDatePicker != null) {
            filterDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
                filterClasses();
            });
        }

        // Status filter listener
        if (filterStatusComboBox != null) {
            filterStatusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                filterClasses();
            });
        }

        // Bind sorted list to table
        SortedList<GymClass> sortedClasses = new SortedList<>(filteredClasses);
        sortedClasses.comparatorProperty().bind(classesTable.comparatorProperty());
        classesTable.setItems(sortedClasses);
    }

    /**
     * Apply filters to class list
     */
    private void filterClasses() {
        filteredClasses.setPredicate(gymClass -> {
            // Search filter
            if (searchField != null && !searchField.getText().isEmpty()) {
                String searchLower = searchField.getText().toLowerCase();
                boolean matchesSearch = gymClass.getClassName().toLowerCase().contains(searchLower)
                        || (gymClass.getDescription() != null && gymClass.getDescription().toLowerCase().contains(searchLower))
                        || gymClass.getTrainerName().toLowerCase().contains(searchLower);

                if (!matchesSearch) return false;
            }

            // Date filter
            if (filterDatePicker != null && filterDatePicker.getValue() != null) {
                LocalDate filterDate = filterDatePicker.getValue();
                LocalDate classDate = gymClass.getStartTime().toLocalDateTime().toLocalDate();
                if (!classDate.equals(filterDate)) return false;
            }

            // Status filter
            if (filterStatusComboBox != null && !"All".equals(filterStatusComboBox.getValue())) {
                if (!gymClass.getStatus().equals(filterStatusComboBox.getValue())) return false;
            }

            return true;
        });
    }

    /**
     * Setup real-time validation
     */
    private void setupValidation() {
        // Time field validation
        startTimeField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateTimeField(startTimeField);
            calculateDuration();
            checkTrainerConflict();
        });

        endTimeField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateTimeField(endTimeField);
            calculateDuration();
            checkTrainerConflict();
        });

        // Date picker validation
        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            calculateDuration();
            checkTrainerConflict();
        });

        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            calculateDuration();
            checkTrainerConflict();
        });

        // Capacity validation
        maxCapacityField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                maxCapacityField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // Trainer selection
        trainerComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            checkTrainerConflict();
        });
    }

    /**
     * Calculate and display class duration
     */
    private void calculateDuration() {
        if (durationLabel == null) return;

        try {
            if (startDatePicker.getValue() != null && endDatePicker.getValue() != null
                    && !startTimeField.getText().isEmpty() && !endTimeField.getText().isEmpty()) {

                LocalDateTime start = LocalDateTime.of(
                        startDatePicker.getValue(),
                        LocalTime.parse(startTimeField.getText(), TIME_FORMATTER)
                );
                LocalDateTime end = LocalDateTime.of(
                        endDatePicker.getValue(),
                        LocalTime.parse(endTimeField.getText(), TIME_FORMATTER)
                );

                if (end.isAfter(start)) {
                    long minutes = java.time.Duration.between(start, end).toMinutes();
                    long hours = minutes / 60;
                    long mins = minutes % 60;

                    String duration = hours > 0 ? hours + "h " + mins + "m" : mins + "m";
                    durationLabel.setText("Duration: " + duration);
                    durationLabel.setTextFill(Color.GREEN);
                } else {
                    durationLabel.setText("End time must be after start time");
                    durationLabel.setTextFill(Color.RED);
                }
            } else {
                durationLabel.setText("");
            }
        } catch (DateTimeParseException e) {
            durationLabel.setText("Invalid time format");
            durationLabel.setTextFill(Color.RED);
        }
    }

    /**
     * Check for trainer scheduling conflicts
     */
    private void checkTrainerConflict() {
        if (conflictWarningLabel == null) return;

        try {
            if (trainerComboBox.getValue() != null
                    && startDatePicker.getValue() != null && endDatePicker.getValue() != null
                    && !startTimeField.getText().isEmpty() && !endTimeField.getText().isEmpty()) {

                LocalDateTime start = LocalDateTime.of(
                        startDatePicker.getValue(),
                        LocalTime.parse(startTimeField.getText(), TIME_FORMATTER)
                );
                LocalDateTime end = LocalDateTime.of(
                        endDatePicker.getValue(),
                        LocalTime.parse(endTimeField.getText(), TIME_FORMATTER)
                );

                Integer excludeId = selectedClass != null ? selectedClass.getId() : null;
                boolean hasConflict = classDAO.hasTrainerConflict(
                        trainerComboBox.getValue().getId(),
                        Timestamp.valueOf(start),
                        Timestamp.valueOf(end),
                        excludeId
                );

                if (hasConflict) {
                    conflictWarningLabel.setText("⚠ Trainer has conflicting schedule!");
                    conflictWarningLabel.setTextFill(Color.RED);
                    conflictWarningLabel.setVisible(true);
                } else {
                    conflictWarningLabel.setVisible(false);
                }
            } else {
                conflictWarningLabel.setVisible(false);
            }
        } catch (Exception e) {
            conflictWarningLabel.setVisible(false);
        }
    }

    /**
     * Validate time field format
     */
    private void validateTimeField(TextField field) {
        String text = field.getText();
        if (text.isEmpty()) return;

        try {
            LocalTime.parse(text, TIME_FORMATTER);
            field.setStyle("-fx-border-color: green; -fx-border-width: 2px;");
        } catch (DateTimeParseException e) {
            field.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }
    }

    /**
     * Setup event listeners
     */
    private void setupListeners() {
        classesTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        populateFields(newValue);
                        selectedClass = newValue;
                        updateButton.setDisable(false);
                        deleteButton.setDisable(false);
                        addButton.setDisable(true);
                    } else {
                        selectedClass = null;
                        updateButton.setDisable(true);
                        deleteButton.setDisable(true);
                        addButton.setDisable(false);
                    }
                });
    }

    /**
     * Load data from database
     */
    private void loadData() {
        classes.clear();
        classes.addAll(classDAO.getAllClasses());

        trainers.clear();
        trainers.addAll(trainerDAO.getAllTrainers());

        LOGGER.info("Loaded " + classes.size() + " classes and " + trainers.size() + " trainers");
    }

    /**
     * Handle add class button
     */
    @FXML
    private void handleAdd() {
        if (!validateInput()) return;

        try {
            GymClass gymClass = createGymClassFromForm();

            if (classDAO.createClass(gymClass)) {
                showFeedback("✓ Class created successfully!", "success");
                clearFields();
                loadData();
            } else {
                showFeedback("✗ Failed to create class. Check trainer schedule.", "error");
            }
        } catch (Exception e) {
            showFeedback("✗ Error: " + e.getMessage(), "error");
            LOGGER.severe("Error creating class: " + e.getMessage());
        }
    }

    /**
     * Handle update class button
     */
    @FXML
    private void handleUpdate() {
        if (selectedClass == null || !validateInput()) return;

        try {
            updateGymClassFromForm(selectedClass);

            if (classDAO.updateClass(selectedClass)) {
                showFeedback("✓ Class updated successfully!", "success");
                clearFields();
                loadData();
            } else {
                showFeedback("✗ Failed to update class. Check trainer schedule.", "error");
            }
        } catch (Exception e) {
            showFeedback("✗ Error: " + e.getMessage(), "error");
            LOGGER.severe("Error updating class: " + e.getMessage());
        }
    }

    /**
     * Handle delete class button
     */
    @FXML
    private void handleDelete() {
        if (selectedClass == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Class");
        confirmation.setContentText("Are you sure you want to delete '" + selectedClass.getClassName() + "'?\n\n" +
                "Current bookings: " + selectedClass.getCurrentBookings() + "\n" +
                "This action cannot be undone.");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            if (classDAO.deleteClass(selectedClass.getId())) {
                showFeedback("✓ Class deleted successfully!", "success");
                clearFields();
                loadData();
            } else {
                showFeedback("✗ Failed to delete class.", "error");
            }
        }
    }

    /**
     * Handle refresh button
     */
    @FXML
    private void handleRefresh() {
        // Update class statuses
        classDAO.updateClassStatuses();
        loadData();
        showFeedback("✓ Data refreshed!", "success");
    }

    /**
     * Handle clear button
     */
    @FXML
    private void handleClear() {
        clearFields();
        classesTable.getSelectionModel().clearSelection();
    }

    /**
     * Handle back button
     */
    @FXML
    private void handleBack() {
        loadScene("/fxml/admin-dashboard.fxml", "Admin Dashboard");
    }

    /**
     * Handle row double click
     */
    private void handleRowDoubleClick(GymClass gymClass) {
        // Could open detailed view or booking management
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Class Details");
        info.setHeaderText(gymClass.getClassName());

        String details = String.format(
                "Trainer: %s\n" +
                        "Date: %s\n" +
                        "Time: %s - %s\n" +
                        "Duration: %s\n" +
                        "Capacity: %s\n" +
                        "Occupancy: %.1f%%\n" +
                        "Status: %s\n\n" +
                        "Description:\n%s",
                gymClass.getTrainerName(),
                gymClass.getDateOnly(),
                gymClass.getStartTimeOnly(),
                gymClass.getEndTimeOnly(),
                gymClass.getFormattedDuration(),
                gymClass.getBookingSummary(),
                gymClass.getOccupancyRate(),
                gymClass.getStatus(),
                gymClass.getDescription() != null ? gymClass.getDescription() : "No description"
        );

        info.setContentText(details);
        info.showAndWait();
    }

    /**
     * Create GymClass from form fields
     */
    private GymClass createGymClassFromForm() {
        LocalDateTime startDateTime = LocalDateTime.of(
                startDatePicker.getValue(),
                LocalTime.parse(startTimeField.getText(), TIME_FORMATTER)
        );
        LocalDateTime endDateTime = LocalDateTime.of(
                endDatePicker.getValue(),
                LocalTime.parse(endTimeField.getText(), TIME_FORMATTER)
        );

        GymClass gymClass = new GymClass(
                classNameField.getText().trim(),
                descriptionField.getText().trim(),
                trainerComboBox.getValue().getId(),
                Timestamp.valueOf(startDateTime),
                Timestamp.valueOf(endDateTime),
                Integer.parseInt(maxCapacityField.getText())
        );
        gymClass.setStatus(statusComboBox.getValue());

        return gymClass;
    }

    /**
     * Update existing GymClass from form fields
     */
    private void updateGymClassFromForm(GymClass gymClass) {
        LocalDateTime startDateTime = LocalDateTime.of(
                startDatePicker.getValue(),
                LocalTime.parse(startTimeField.getText(), TIME_FORMATTER)
        );
        LocalDateTime endDateTime = LocalDateTime.of(
                endDatePicker.getValue(),
                LocalTime.parse(endTimeField.getText(), TIME_FORMATTER)
        );

        gymClass.setClassName(classNameField.getText().trim());
        gymClass.setDescription(descriptionField.getText().trim());
        gymClass.setTrainerId(trainerComboBox.getValue().getId());
        gymClass.setStartTime(Timestamp.valueOf(startDateTime));
        gymClass.setEndTime(Timestamp.valueOf(endDateTime));
        gymClass.setMaxCapacity(Integer.parseInt(maxCapacityField.getText()));
        gymClass.setStatus(statusComboBox.getValue());
    }

    /**
     * Populate form fields from GymClass
     */
    private void populateFields(GymClass gymClass) {
        classNameField.setText(gymClass.getClassName());
        descriptionField.setText(gymClass.getDescription());

        // Find and select trainer
        for (Trainer trainer : trainers) {
            if (trainer.getId() == gymClass.getTrainerId()) {
                trainerComboBox.setValue(trainer);
                break;
            }
        }

        if (gymClass.getStartTime() != null) {
            LocalDateTime startDateTime = gymClass.getStartTime().toLocalDateTime();
            startDatePicker.setValue(startDateTime.toLocalDate());
            startTimeField.setText(startDateTime.toLocalTime().format(TIME_FORMATTER));
        }

        if (gymClass.getEndTime() != null) {
            LocalDateTime endDateTime = gymClass.getEndTime().toLocalDateTime();
            endDatePicker.setValue(endDateTime.toLocalDate());
            endTimeField.setText(endDateTime.toLocalTime().format(TIME_FORMATTER));
        }

        maxCapacityField.setText(String.valueOf(gymClass.getMaxCapacity()));
        statusComboBox.setValue(gymClass.getStatus());

        calculateDuration();
    }

    /**
     * Clear all form fields
     */
    private void clearFields() {
        classNameField.clear();
        descriptionField.clear();
        trainerComboBox.setValue(null);
        startDatePicker.setValue(null);
        startTimeField.clear();
        endDatePicker.setValue(null);
        endTimeField.clear();
        maxCapacityField.clear();
        statusComboBox.setValue(GymClass.STATUS_SCHEDULED);

        // Reset styling
        startTimeField.setStyle("");
        endTimeField.setStyle("");

        if (durationLabel != null) durationLabel.setText("");
        if (conflictWarningLabel != null) conflictWarningLabel.setVisible(false);
    }

    /**
     * Validate form input
     */
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Class name validation
        if (classNameField.getText().trim().isEmpty()) {
            errors.append("• Class name is required\n");
        } else if (classNameField.getText().trim().length() < 3) {
            errors.append("• Class name must be at least 3 characters\n");
        }

        // Trainer validation
        if (trainerComboBox.getValue() == null) {
            errors.append("• Trainer must be selected\n");
        }

        // Date validation
        if (startDatePicker.getValue() == null) {
            errors.append("• Start date is required\n");
        }
        if (endDatePicker.getValue() == null) {
            errors.append("• End date is required\n");
        }

        // Time validation
        if (startTimeField.getText().trim().isEmpty()) {
            errors.append("• Start time is required\n");
        } else {
            try {
                LocalTime.parse(startTimeField.getText(), TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                errors.append("• Start time must be in HH:MM format (e.g., 09:00)\n");
            }
        }

        if (endTimeField.getText().trim().isEmpty()) {
            errors.append("• End time is required\n");
        } else {
            try {
                LocalTime.parse(endTimeField.getText(), TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                errors.append("• End time must be in HH:MM format (e.g., 10:30)\n");
            }
        }

        // Time range validation
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null
                && !startTimeField.getText().isEmpty() && !endTimeField.getText().isEmpty()) {
            try {
                LocalDateTime start = LocalDateTime.of(
                        startDatePicker.getValue(),
                        LocalTime.parse(startTimeField.getText(), TIME_FORMATTER)
                );
                LocalDateTime end = LocalDateTime.of(
                        endDatePicker.getValue(),
                        LocalTime.parse(endTimeField.getText(), TIME_FORMATTER)
                );

                if (!end.isAfter(start)) {
                    errors.append("• End time must be after start time\n");
                }

                if (start.isBefore(LocalDateTime.now()) && selectedClass == null) {
                    errors.append("• Start time cannot be in the past\n");
                }
            } catch (DateTimeParseException e) {
                // Already handled above
            }
        }

        // Capacity validation
        if (maxCapacityField.getText().trim().isEmpty()) {
            errors.append("• Max capacity is required\n");
        } else {
            try {
                int capacity = Integer.parseInt(maxCapacityField.getText());
                if (capacity <= 0) {
                    errors.append("• Max capacity must be greater than 0\n");
                } else if (capacity > 100) {
                    errors.append("• Max capacity cannot exceed 100\n");
                }

                // Check if reducing capacity below current bookings
                if (selectedClass != null && capacity < selectedClass.getCurrentBookings()) {
                    errors.append("• Cannot reduce capacity below current bookings (" +
                            selectedClass.getCurrentBookings() + ")\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Max capacity must be a valid number\n");
            }
        }

        // Trainer conflict check
        if (trainerComboBox.getValue() != null && startDatePicker.getValue() != null
                && endDatePicker.getValue() != null && !startTimeField.getText().isEmpty()
                && !endTimeField.getText().isEmpty()) {
            try {
                LocalDateTime start = LocalDateTime.of(
                        startDatePicker.getValue(),
                        LocalTime.parse(startTimeField.getText(), TIME_FORMATTER)
                );
                LocalDateTime end = LocalDateTime.of(
                        endDatePicker.getValue(),
                        LocalTime.parse(endTimeField.getText(), TIME_FORMATTER)
                );

                Integer excludeId = selectedClass != null ? selectedClass.getId() : null;
                boolean hasConflict = classDAO.hasTrainerConflict(
                        trainerComboBox.getValue().getId(),
                        Timestamp.valueOf(start),
                        Timestamp.valueOf(end),
                        excludeId
                );

                if (hasConflict) {
                    errors.append("• Trainer has a conflicting class at this time\n");
                }
            } catch (Exception e) {
                // Ignore validation if dates/times are invalid
            }
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Please correct the following errors:");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    /**
     * Show feedback message with auto-hide
     */
    private void showFeedback(String message, String type) {
        if (feedbackLabel == null) {
            // Fallback to alert if label not available
            Alert alert = new Alert(
                    "success".equals(type) ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR
            );
            alert.setContentText(message);
            alert.show();
            return;
        }

        feedbackLabel.setText(message);

        switch (type) {
            case "success":
                feedbackLabel.setTextFill(Color.GREEN);
                break;
            case "error":
                feedbackLabel.setTextFill(Color.RED);
                break;
            case "warning":
                feedbackLabel.setTextFill(Color.ORANGE);
                break;
            default:
                feedbackLabel.setTextFill(Color.BLACK);
        }

        feedbackLabel.setVisible(true);

        // Auto-hide after 3 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> feedbackLabel.setVisible(false));
        pause.play();
    }

    /**
     * Start auto-update timer for class statuses
     */
    private void startAutoUpdate() {
        // Update statuses every 5 minutes
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.minutes(5),
                        event -> {
                            classDAO.updateClassStatuses();
                            loadData();
                            LOGGER.info("Auto-updated class statuses");
                        }
                )
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Load scene helper
     */
    private void loadScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load());

            // Try to load CSS
            try {
                scene.getStylesheets().add(
                        getClass().getResource("/css/style.css").toExternalForm()
                );
            } catch (Exception e) {
                LOGGER.warning("Could not load CSS file");
            }

            Stage stage = (Stage) classesTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            LOGGER.severe("Error loading scene: " + e.getMessage());
            e.printStackTrace();
        }
    }
}