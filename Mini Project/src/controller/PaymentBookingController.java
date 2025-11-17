package com.gymmanagementsystem.controller;

import com.gymmanagementsystem.dao.*;
import com.gymmanagementsystem.model.*;
import com.gymmanagementsystem.util.SceneManager;
import com.gymmanagementsystem.util.SessionManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentBookingController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(PaymentBookingController.class.getName());

    // Tab Buttons
    @FXML private Button bookClassTab;
    @FXML private Button paymentManagementTab;
    @FXML private Button paymentHistoryTab;

    // Tab Contents
    @FXML private VBox bookClassContent;
    @FXML private VBox paymentManagementContent;
    @FXML private VBox paymentHistoryContent;

    // Book Class Tab Components
    @FXML private Label memberLabel;
    @FXML private Label classLabel;
    @FXML private ComboBox<Member> memberComboBox;
    @FXML private ComboBox<GymClass> classComboBox;
    @FXML private Label selectedMemberDisplay;
    @FXML private Label selectedClassDisplay;
    @FXML private VBox classDetailsPane;
    @FXML private Label durationLabel;
    @FXML private Label scheduleLabel;
    @FXML private Label classIdLabel;
    @FXML private Label availableSlotsLabel;
    @FXML private Label totalFeeLabel;
    @FXML private ToggleButton creditCardBtn;
    @FXML private ToggleButton debitCardBtn;
    @FXML private ToggleButton cashBtn;
    @FXML private ToggleButton upiBtn;  // Add this to your FXML
    @FXML private Button proceedButton;

    // QR Code Components (Add to your FXML)
    @FXML private VBox qrCodeContainer;
    @FXML private ImageView qrCodeImageView;
    @FXML private Label qrPaymentInfoLabel;

    // Payment Management Tab Components
    @FXML private TableView<Payment> paymentManagementTable;
    @FXML private TableColumn<Payment, String> pmIdColumn;
    @FXML private TableColumn<Payment, String> pmMemberColumn;
    @FXML private TableColumn<Payment, String> pmClassColumn;
    @FXML private TableColumn<Payment, String> pmAmountColumn;
    @FXML private TableColumn<Payment, String> pmMethodColumn;
    @FXML private TableColumn<Payment, String> pmStatusColumn;
    @FXML private TableColumn<Payment, String> pmDateColumn;

    // Payment History Tab Components
    @FXML private VBox paymentHistoryContainer;

    // DAOs
    private MemberDAO memberDAO;
    private ClassDAO classDAO;
    private PaymentDAO paymentDAO;

    // Data
    private ObservableList<Member> members;
    private ObservableList<GymClass> classes;
    private ObservableList<Payment> payments;

    private Member selectedMember;
    private GymClass selectedClass;
    private ToggleGroup paymentMethodGroup;

    // Class fee
    private static final double CLASS_FEE = 500.0;

    // UPI Payment Details (customize these)
    private static final String UPI_ID = "8778228414@ibl";
    private static final String MERCHANT_NAME = "Gym Management System";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            LOGGER.info("Initializing PaymentBookingController");

            // Initialize DAOs
            memberDAO = new MemberDAO();
            classDAO = new ClassDAO();
            paymentDAO = new PaymentDAO();

            // Initialize Toggle Group for payment methods
            paymentMethodGroup = new ToggleGroup();
            creditCardBtn.setToggleGroup(paymentMethodGroup);
            debitCardBtn.setToggleGroup(paymentMethodGroup);
            cashBtn.setToggleGroup(paymentMethodGroup);
            if (upiBtn != null) {
                upiBtn.setToggleGroup(paymentMethodGroup);
            }

            // Hide QR code initially
            if (qrCodeContainer != null) {
                qrCodeContainer.setVisible(false);
                qrCodeContainer.setManaged(false);
            }

            // Load data
            loadMembers();
            loadClasses();
            loadPayments();

            // Setup table columns
            setupPaymentManagementTable();

            // Add listeners
            setupListeners();

            // Show Book Class tab by default
            showBookClassTab();

            LOGGER.info("PaymentBookingController initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing PaymentBookingController", e);
            showAlert("Error", "Failed to initialize page: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadMembers() {
        try {
            LOGGER.info("Loading members");
            List<Member> memberList = memberDAO.getAllMembers();
            members = FXCollections.observableArrayList(memberList);
            memberComboBox.setItems(members);

            memberComboBox.setCellFactory(lv -> new ListCell<Member>() {
                @Override
                protected void updateItem(Member member, boolean empty) {
                    super.updateItem(member, empty);
                    if (empty || member == null) {
                        setText(null);
                    } else {
                        String name = member.getUser() != null ?
                                member.getUser().getFullName() : "Unknown";
                        String plan = member.getMembershipPlanName() != null ?
                                member.getMembershipPlanName() : "No Plan";
                        setText(name + " - " + plan);
                    }
                }
            });

            memberComboBox.setButtonCell(new ListCell<Member>() {
                @Override
                protected void updateItem(Member member, boolean empty) {
                    super.updateItem(member, empty);
                    if (empty || member == null) {
                        setText(null);
                    } else {
                        String name = member.getUser() != null ?
                                member.getUser().getFullName() : "Unknown";
                        String plan = member.getMembershipPlanName() != null ?
                                member.getMembershipPlanName() : "No Plan";
                        setText(name + " - " + plan);
                    }
                }
            });
            LOGGER.info("Loaded " + memberList.size() + " members");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading members", e);
            showAlert("Error", "Failed to load members: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadClasses() {
        try {
            LOGGER.info("Loading classes");
            List<GymClass> classList = classDAO.getUpcomingClasses();
            classes = FXCollections.observableArrayList(classList);
            classComboBox.setItems(classes);

            classComboBox.setCellFactory(lv -> new ListCell<GymClass>() {
                @Override
                protected void updateItem(GymClass gymClass, boolean empty) {
                    super.updateItem(gymClass, empty);
                    if (empty || gymClass == null) {
                        setText(null);
                    } else {
                        String trainerName = gymClass.getTrainerName();
                        setText(gymClass.getClassName() + " - " + trainerName + " - ₹" + CLASS_FEE);
                    }
                }
            });

            classComboBox.setButtonCell(new ListCell<GymClass>() {
                @Override
                protected void updateItem(GymClass gymClass, boolean empty) {
                    super.updateItem(gymClass, empty);
                    if (empty || gymClass == null) {
                        setText(null);
                    } else {
                        String trainerName = gymClass.getTrainerName();
                        setText(gymClass.getClassName() + " - " + trainerName + " - ₹" + CLASS_FEE);
                    }
                }
            });
            LOGGER.info("Loaded " + classList.size() + " classes");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading classes", e);
            showAlert("Error", "Failed to load classes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadPayments() {
        try {
            LOGGER.info("Loading payments");
            List<Payment> paymentList = paymentDAO.getAllPayments();
            paymentList.removeIf(p -> !"CLASS".equals(p.getPaymentType()));
            payments = FXCollections.observableArrayList(paymentList);
            paymentManagementTable.setItems(payments);
            displayPaymentHistory();
            LOGGER.info("Loaded " + paymentList.size() + " payments");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading payments", e);
            showAlert("Error", "Failed to load payments: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupPaymentManagementTable() {
        pmIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        pmMemberColumn.setCellValueFactory(data -> {
            Member member = data.getValue().getMember();
            String name = (member != null && member.getUser() != null) ?
                    member.getUser().getFullName() : "Unknown";
            return new SimpleStringProperty(name);
        });
        pmClassColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescription()));
        pmAmountColumn.setCellValueFactory(data ->
                new SimpleStringProperty("₹" + data.getValue().getFinalAmount()));
        pmMethodColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPaymentMethod()));
        pmStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));
        pmDateColumn.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return new SimpleStringProperty(
                    data.getValue().getPaymentDate().toLocalDateTime().format(formatter));
        });
    }

    private void setupListeners() {
        paymentMethodGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            checkIfReadyToProceed();
            updateQRCodeVisibility();
        });
    }

    @FXML
    private void onMemberSelected() {
        selectedMember = memberComboBox.getValue();
        if (selectedMember != null && selectedClass != null) {
            showSelectedItems();
        }
        checkIfReadyToProceed();
        updateQRCodeVisibility();
    }

    @FXML
    private void onClassSelected() {
        selectedClass = classComboBox.getValue();
        if (selectedClass != null) {
            displayClassDetails();
            if (selectedMember != null) {
                showSelectedItems();
            }
        }
        checkIfReadyToProceed();
        updateQRCodeVisibility();
    }

    private void updateQRCodeVisibility() {
        if (qrCodeContainer == null) return;

        Toggle selectedToggle = paymentMethodGroup.getSelectedToggle();

        if (selectedToggle != null && selectedMember != null && selectedClass != null) {
            String paymentMethod = ((ToggleButton) selectedToggle).getText();

            // Show QR code only for UPI payment
            if ("UPI".equalsIgnoreCase(paymentMethod) ||
                    (upiBtn != null && selectedToggle == upiBtn)) {
                generateAndShowQRCode();
            } else {
                hideQRCode();
            }
        } else {
            hideQRCode();
        }
    }

    private void generateAndShowQRCode() {
        try {
            // UPI Payment String Format
            String memberName = selectedMember.getUser() != null ?
                    selectedMember.getUser().getFullName() : "Member";
            String className = selectedClass.getClassName();

            // UPI Payment URL format
            String upiString = String.format(
                    "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=Class:%s-Member:%s",
                    UPI_ID,
                    MERCHANT_NAME,
                    CLASS_FEE,
                    className.replace(" ", ""),
                    memberName.replace(" ", "")
            );

            // Generate QR Code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(upiString, BarcodeFormat.QR_CODE, 250, 250);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            Image qrImage = SwingFXUtils.toFXImage(bufferedImage, null);

            // Display QR Code
            qrCodeImageView.setImage(qrImage);
            qrPaymentInfoLabel.setText(String.format(
                    "Scan QR to pay ₹%.2f\nUPI ID: %s",
                    CLASS_FEE,
                    UPI_ID
            ));

            qrCodeContainer.setVisible(true);
            qrCodeContainer.setManaged(true);

            LOGGER.info("QR Code generated successfully");
        } catch (WriterException e) {
            LOGGER.log(Level.SEVERE, "Error generating QR code", e);
            showAlert("Error", "Failed to generate QR code: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void hideQRCode() {
        if (qrCodeContainer != null) {
            qrCodeContainer.setVisible(false);
            qrCodeContainer.setManaged(false);
        }
    }

    private void showSelectedItems() {
        memberComboBox.setVisible(false);
        memberComboBox.setManaged(false);
        classComboBox.setVisible(false);
        classComboBox.setManaged(false);

        memberLabel.setText("Selected Member");
        classLabel.setText("Selected Class");

        String memberName = selectedMember.getUser() != null ?
                selectedMember.getUser().getFullName() : "Unknown";
        String memberPlan = selectedMember.getMembershipPlanName() != null ?
                selectedMember.getMembershipPlanName() : "No Plan";
        selectedMemberDisplay.setText(memberName + " - " + memberPlan);
        selectedMemberDisplay.setVisible(true);
        selectedMemberDisplay.setManaged(true);

        String trainerName = selectedClass.getTrainerName();
        selectedClassDisplay.setText(selectedClass.getClassName() + " - " + trainerName);
        selectedClassDisplay.setVisible(true);
        selectedClassDisplay.setManaged(true);
    }

    private void displayClassDetails() {
        classDetailsPane.setVisible(true);
        classDetailsPane.setManaged(true);

        durationLabel.setText(selectedClass.getFormattedDuration());
        scheduleLabel.setText(selectedClass.getFormattedStartTime());
        classIdLabel.setText(String.valueOf(selectedClass.getId()));
        availableSlotsLabel.setText(String.valueOf(selectedClass.getAvailableSpots()));
        totalFeeLabel.setText("₹" + CLASS_FEE);
    }

    private void checkIfReadyToProceed() {
        boolean isReady = selectedMember != null &&
                selectedClass != null &&
                paymentMethodGroup.getSelectedToggle() != null;
        proceedButton.setDisable(!isReady);
    }

    @FXML
    private void handleProceedToPayment() {
        if (selectedMember == null || selectedClass == null ||
                paymentMethodGroup.getSelectedToggle() == null) {
            showAlert("Error", "Please fill all fields", Alert.AlertType.ERROR);
            return;
        }

        try {
            LOGGER.info("Processing payment for member: " + selectedMember.getId() +
                    ", class: " + selectedClass.getId());

            String paymentMethod = ((ToggleButton) paymentMethodGroup.getSelectedToggle()).getText();

            if (classDAO.isClassBookedByMember(selectedClass.getId(), selectedMember.getId())) {
                showAlert("Error", "You have already booked this class!", Alert.AlertType.WARNING);
                return;
            }

            if (!selectedClass.hasAvailableSpots()) {
                showAlert("Error", "This class is full!", Alert.AlertType.WARNING);
                return;
            }

            Payment payment = new Payment();
            payment.setMemberId(selectedMember.getId());
            payment.setAmount(BigDecimal.valueOf(CLASS_FEE));
            payment.setDiscount(BigDecimal.ZERO);
            payment.setFinalAmount(BigDecimal.valueOf(CLASS_FEE));
            payment.setPaymentMethod(paymentMethod.toUpperCase());
            payment.setPaymentType("CLASS");
            payment.setStatus("COMPLETED");
            payment.setDescription("Class Booking: " + selectedClass.getClassName());
            payment.setPaymentDate(Timestamp.valueOf(LocalDateTime.now()));

            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                payment.setProcessedBy(currentUser.getId());
            }

            boolean paymentSuccess = paymentDAO.createPayment(payment);

            if (paymentSuccess) {
                boolean bookingSuccess = classDAO.bookClass(selectedClass.getId(), selectedMember.getId());

                if (bookingSuccess) {
                    String memberName = selectedMember.getUser() != null ?
                            selectedMember.getUser().getFullName() : "Unknown";

                    LOGGER.info("Booking successful for member: " + selectedMember.getId());

                    showAlert("Success", "Booking Confirmed Successfully!\n\n" +
                                    "Member: " + memberName + "\n" +
                                    "Class: " + selectedClass.getClassName() + "\n" +
                                    "Date: " + selectedClass.getFormattedStartTime() + "\n" +
                                    "Amount: ₹" + CLASS_FEE + "\n" +
                                    "Payment Method: " + paymentMethod + "\n" +
                                    "Transaction ID: " + payment.getTransactionId(),
                            Alert.AlertType.INFORMATION);

                    resetBookingForm();
                    loadClasses();
                    loadPayments();
                } else {
                    LOGGER.warning("Booking failed after payment success");
                    showAlert("Error", "Payment successful but booking failed. Please contact administrator.",
                            Alert.AlertType.ERROR);
                }
            } else {
                LOGGER.warning("Payment processing failed");
                showAlert("Error", "Payment processing failed", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing payment", e);
            showAlert("Error", "An error occurred: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void resetBookingForm() {
        selectedMember = null;
        selectedClass = null;

        memberComboBox.setValue(null);
        classComboBox.setValue(null);
        memberComboBox.setVisible(true);
        memberComboBox.setManaged(true);
        classComboBox.setVisible(true);
        classComboBox.setManaged(true);

        selectedMemberDisplay.setVisible(false);
        selectedMemberDisplay.setManaged(false);
        selectedClassDisplay.setVisible(false);
        selectedClassDisplay.setManaged(false);

        memberLabel.setText("Select Member");
        classLabel.setText("Select Class");

        classDetailsPane.setVisible(false);
        classDetailsPane.setManaged(false);

        paymentMethodGroup.selectToggle(null);
        proceedButton.setDisable(true);
        hideQRCode();
    }

    private void displayPaymentHistory() {
        paymentHistoryContainer.getChildren().clear();

        for (Payment payment : payments) {
            VBox paymentCard = createPaymentHistoryCard(payment);
            paymentHistoryContainer.getChildren().add(paymentCard);
        }
    }

    private VBox createPaymentHistoryCard(Payment payment) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label className = new Label(payment.getDescription());
        className.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        String memberName = "Unknown";
        if (payment.getMember() != null && payment.getMember().getUser() != null) {
            memberName = payment.getMember().getUser().getFullName();
        }
        Label memberLabel = new Label(memberName);
        memberLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        titleBox.getChildren().addAll(className, memberLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(payment.getStatus());
        String statusColor = payment.getStatus().equals("COMPLETED") ?
                "#d4edda" : "#fff3cd";
        String statusTextColor = payment.getStatus().equals("COMPLETED") ?
                "#155724" : "#856404";
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: " + statusTextColor + "; " +
                "-fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 11; -fx-font-weight: bold;");

        header.getChildren().addAll(titleBox, spacer, statusLabel);

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(30);
        detailsGrid.setVgap(10);

        addDetailToGrid(detailsGrid, "Amount", "₹" + payment.getFinalAmount(), 0, 0);
        addDetailToGrid(detailsGrid, "Method", payment.getPaymentMethod(), 1, 0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        addDetailToGrid(detailsGrid, "Date",
                payment.getPaymentDate().toLocalDateTime().format(formatter), 2, 0);

        card.getChildren().addAll(header, new Separator(), detailsGrid);

        return card;
    }

    private void addDetailToGrid(GridPane grid, String label, String value, int col, int row) {
        VBox box = new VBox(3);
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
        box.getChildren().addAll(labelNode, valueNode);
        grid.add(box, col, row);
    }

    @FXML
    private void showBookClassTab() {
        setActiveTab(bookClassTab);
        bookClassContent.setVisible(true);
        bookClassContent.setManaged(true);
        paymentManagementContent.setVisible(false);
        paymentManagementContent.setManaged(false);
        paymentHistoryContent.setVisible(false);
        paymentHistoryContent.setManaged(false);
    }

    @FXML
    private void showPaymentManagementTab() {
        setActiveTab(paymentManagementTab);
        bookClassContent.setVisible(false);
        bookClassContent.setManaged(false);
        paymentManagementContent.setVisible(true);
        paymentManagementContent.setManaged(true);
        paymentHistoryContent.setVisible(false);
        paymentHistoryContent.setManaged(false);
    }

    @FXML
    private void showPaymentHistoryTab() {
        setActiveTab(paymentHistoryTab);
        bookClassContent.setVisible(false);
        bookClassContent.setManaged(false);
        paymentManagementContent.setVisible(false);
        paymentManagementContent.setManaged(false);
        paymentHistoryContent.setVisible(true);
        paymentHistoryContent.setManaged(true);
    }

    private void setActiveTab(Button activeButton) {
        bookClassTab.getStyleClass().remove("active-tab");
        paymentManagementTab.getStyleClass().remove("active-tab");
        paymentHistoryTab.getStyleClass().remove("active-tab");

        activeButton.getStyleClass().add("active-tab");
    }

    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        try {
            LOGGER.info("Navigating back to dashboard");
            User currentUser = SessionManager.getInstance().getCurrentUser();

            if (currentUser != null) {
                String role = currentUser.getRole();
                Node source = (Node) event.getSource();

                if ("Admin".equalsIgnoreCase(role)) {
                    SceneManager.switchScene(source, "/fxml/admin-dashboard.fxml", "Admin Dashboard");
                } else if ("Member".equalsIgnoreCase(role)) {
                    SceneManager.switchScene(source, "/fxml/member-dashboard.fxml", "Member Dashboard");
                } else if ("Trainer".equalsIgnoreCase(role)) {
                    SceneManager.switchScene(source, "/fxml/trainer-dashboard.fxml", "Trainer Dashboard");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back to dashboard", e);
            showAlert("Error", "Failed to navigate back: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}