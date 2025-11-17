package com.gymmanagementsystem.controller;
import com.gymmanagementsystem.util.PaymentReportGenerator;
import javafx.stage.Stage;

import com.gymmanagementsystem.dao.PaymentDAO;
import com.gymmanagementsystem.dao.MemberDAO;
import com.gymmanagementsystem.model.Payment;
import com.gymmanagementsystem.model.Member;
import com.gymmanagementsystem.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentManagementController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(PaymentManagementController.class.getName());

    @FXML private TableView<Payment> paymentsTable;
    @FXML private TableColumn<Payment, String> memberColumn;
    @FXML private TableColumn<Payment, String> transactionIdColumn;
    @FXML private TableColumn<Payment, BigDecimal> amountColumn;
    @FXML private TableColumn<Payment, BigDecimal> discountColumn;
    @FXML private TableColumn<Payment, BigDecimal> finalAmountColumn;
    @FXML private TableColumn<Payment, String> paymentMethodColumn;
    @FXML private TableColumn<Payment, String> paymentTypeColumn;
    @FXML private TableColumn<Payment, String> statusColumn;
    @FXML private TableColumn<Payment, Timestamp> paymentDateColumn;

    @FXML private ComboBox<Member> memberComboBox;
    @FXML private TextField amountField;
    @FXML private TextField discountField;
    @FXML private TextField finalAmountField;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private ComboBox<String> paymentTypeComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField couponCodeField;
    @FXML private TextArea descriptionField;

    @FXML private Label totalRevenueLabel;
    @FXML private Label completedPaymentsLabel;
    @FXML private Label pendingPaymentsLabel;
    @FXML private Label refundsLabel;

    @FXML private TextField searchField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Button addButton;
    @FXML private Button updateStatusButton;
    @FXML private Button refundButton;
    @FXML private Button deleteButton;

    private PaymentDAO paymentDAO = new PaymentDAO();
    private MemberDAO memberDAO = new MemberDAO();
    private ObservableList<Payment> payments = FXCollections.observableArrayList();
    private ObservableList<Member> members = FXCollections.observableArrayList();
    private Payment selectedPayment = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("🔄 Initializing Payment Management...");

        setupTable();
        setupComboBoxes();
        loadData();
        loadStatistics();
        setupListeners();

        System.out.println("✅ Payment Management initialized successfully");
    }

    private void setupTable() {
        memberColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getMember() != null &&
                    cellData.getValue().getMember().getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getMember().getUser().getFullName());
            }
            return new javafx.beans.property.SimpleStringProperty("Unknown");
        });

        transactionIdColumn.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discount"));
        finalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("finalAmount"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        paymentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));

        // Style status column
        statusColumn.setCellFactory(column -> new TableCell<Payment, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "COMPLETED":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "PENDING":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case "FAILED":
                        case "REFUNDED":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        paymentsTable.setItems(payments);
    }

    private void setupComboBoxes() {
        // Setup member combo box
        memberComboBox.setItems(members);
        memberComboBox.setCellFactory(listView -> new ListCell<Member>() {
            @Override
            protected void updateItem(Member member, boolean empty) {
                super.updateItem(member, empty);
                if (empty || member == null || member.getUser() == null) {
                    setText(null);
                } else {
                    setText(member.getUser().getFullName() + " (ID: " + member.getId() + ")");
                }
            }
        });
        memberComboBox.setButtonCell(new ListCell<Member>() {
            @Override
            protected void updateItem(Member member, boolean empty) {
                super.updateItem(member, empty);
                if (empty || member == null || member.getUser() == null) {
                    setText(null);
                } else {
                    setText(member.getUser().getFullName());
                }
            }
        });

        // Setup payment method combo box
        paymentMethodComboBox.getItems().addAll("CASH", "CARD", "ONLINE", "UPI", "WALLET");
        paymentMethodComboBox.setValue("CASH");

        // Setup payment type combo box
        paymentTypeComboBox.getItems().addAll("MEMBERSHIP", "RENEWAL", "CLASS", "OTHER");
        paymentTypeComboBox.setValue("MEMBERSHIP");

        // Setup status combo box
        statusComboBox.getItems().addAll("PENDING", "COMPLETED", "FAILED");
        statusComboBox.setValue("COMPLETED");
    }

    private void setupListeners() {
        // Table selection listener
        paymentsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        populateFields(newValue);
                        selectedPayment = newValue;
                        updateStatusButton.setDisable(false);
                        refundButton.setDisable(!newValue.isRefundable());
                        deleteButton.setDisable(false);
                        addButton.setDisable(true);
                    } else {
                        clearFields();
                        selectedPayment = null;
                        updateStatusButton.setDisable(true);
                        refundButton.setDisable(true);
                        deleteButton.setDisable(true);
                        addButton.setDisable(false);
                    }
                });

        // Amount and discount calculation listeners
        amountField.textProperty().addListener((obs, oldVal, newVal) -> calculateFinalAmount());
        discountField.textProperty().addListener((obs, oldVal, newVal) -> calculateFinalAmount());

        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
    }

    private void calculateFinalAmount() {
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            BigDecimal discount = discountField.getText().trim().isEmpty() ?
                    BigDecimal.ZERO : new BigDecimal(discountField.getText().trim());
            BigDecimal finalAmount = amount.subtract(discount);
            finalAmountField.setText(finalAmount.toString());
        } catch (NumberFormatException e) {
            finalAmountField.setText("0.00");
        }
    }

    private void loadData() {
        System.out.println("🔄 Loading payments and members...");

        payments.clear();
        payments.addAll(paymentDAO.getAllPayments());
        System.out.println("✅ Loaded " + payments.size() + " payments");

        members.clear();
        members.addAll(memberDAO.getAllMembers());
        System.out.println("✅ Loaded " + members.size() + " members");
    }

    private void loadStatistics() {
        System.out.println("🔄 Loading payment statistics...");

        try {
            // Try to get statistics from DAO
            PaymentDAO.PaymentStats stats = paymentDAO.getPaymentStatistics();

            if (stats != null) {
                // Use the statistics from DAO
                String totalRevenue = stats.completedRevenue != null ?
                        String.format("%.2f", stats.completedRevenue) : "0.00";

                String totalRefunds = stats.totalRefunds != null ?
                        String.format("%.2f", stats.totalRefunds) : "0.00";

                totalRevenueLabel.setText("₹" + totalRevenue);
                completedPaymentsLabel.setText(String.valueOf(stats.completedCount));
                pendingPaymentsLabel.setText(String.valueOf(stats.pendingCount));
                refundsLabel.setText("₹" + totalRefunds);

                System.out.println("✅ Statistics loaded from DAO:");
                System.out.println("   Total Revenue: ₹" + totalRevenue);
                System.out.println("   Completed: " + stats.completedCount);
                System.out.println("   Pending: " + stats.pendingCount);
                System.out.println("   Refunds: ₹" + totalRefunds);
            } else {
                // Fallback: Calculate manually from loaded payments
                System.out.println("⚠️ DAO statistics null, calculating manually...");
                calculateStatisticsManually();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading statistics from DAO", e);
            System.err.println("❌ Error loading statistics: " + e.getMessage());
            e.printStackTrace();

            // Fallback: Calculate manually
            try {
                calculateStatisticsManually();
            } catch (Exception e2) {
                LOGGER.log(Level.SEVERE, "Error in manual calculation", e2);
                setDefaultStatistics();
            }
        }
    }

    /**
     * FALLBACK METHOD: Manually calculate statistics from loaded payments
     * This ensures statistics are displayed even if DAO method fails
     */
    private void calculateStatisticsManually() {
        System.out.println("📊 Calculating statistics manually from " + payments.size() + " payments...");

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;
        int completedCount = 0;
        int pendingCount = 0;

        for (Payment payment : payments) {
            if (payment.getStatus() != null) {
                switch (payment.getStatus()) {
                    case "COMPLETED":
                        completedCount++;
                        if (payment.getFinalAmount() != null) {
                            totalRevenue = totalRevenue.add(payment.getFinalAmount());
                        }
                        break;
                    case "PENDING":
                        pendingCount++;
                        break;
                    case "REFUNDED":
                        if (payment.getRefundAmount() != null) {
                            totalRefunds = totalRefunds.add(payment.getRefundAmount());
                        }
                        break;
                }
            }
        }

        // Update UI
        totalRevenueLabel.setText("₹" + String.format("%.2f", totalRevenue));
        completedPaymentsLabel.setText(String.valueOf(completedCount));
        pendingPaymentsLabel.setText(String.valueOf(pendingCount));
        refundsLabel.setText("₹" + String.format("%.2f", totalRefunds));

        System.out.println("✅ Manual statistics calculated:");
        System.out.println("   Total Revenue: ₹" + String.format("%.2f", totalRevenue));
        System.out.println("   Completed: " + completedCount);
        System.out.println("   Pending: " + pendingCount);
        System.out.println("   Refunds: ₹" + String.format("%.2f", totalRefunds));
    }

    private void setDefaultStatistics() {
        totalRevenueLabel.setText("₹0.00");
        completedPaymentsLabel.setText("0");
        pendingPaymentsLabel.setText("0");
        refundsLabel.setText("₹0.00");
    }

    @FXML
    private void handleAdd() {
        System.out.println("🔄 Adding new payment...");

        if (validateInput()) {
            try {
                Payment payment = new Payment(
                        memberComboBox.getValue().getId(),
                        new BigDecimal(amountField.getText().trim()),
                        paymentMethodComboBox.getValue(),
                        paymentTypeComboBox.getValue(),
                        descriptionField.getText()
                );

                // Set discount if provided
                if (!discountField.getText().trim().isEmpty()) {
                    payment.setDiscount(new BigDecimal(discountField.getText().trim()));
                }

                // Set coupon code if provided
                if (!couponCodeField.getText().trim().isEmpty()) {
                    payment.setCouponCode(couponCodeField.getText().trim());
                }

                payment.setStatus(statusComboBox.getValue());
                payment.setProcessedBy(SessionManager.getInstance().getCurrentUser().getId());

                if (paymentDAO.createPayment(payment)) {
                    System.out.println("✅ Payment created successfully");

                    showAlert("Payment Recorded!",
                            "Payment recorded successfully!\n" +
                                    "Transaction ID: " + payment.getTransactionId() + "\n" +
                                    "Invoice Number: " + payment.getInvoiceNumber(),
                            Alert.AlertType.INFORMATION);

                    clearFields();
                    loadData();
                    loadStatistics(); // This will now work with fallback
                } else {
                    System.err.println("❌ Failed to create payment");
                    showAlert("Error", "Failed to record payment.", Alert.AlertType.ERROR);
                }
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid number format", e);
                showAlert("Invalid Input", "Please enter valid amount and discount values.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleUpdateStatus() {
        if (selectedPayment != null) {
            String newStatus = statusComboBox.getValue();
            if (paymentDAO.updatePaymentStatus(selectedPayment.getId(), newStatus)) {
                showAlert("Success", "Payment status updated successfully!", Alert.AlertType.INFORMATION);
                clearFields();
                loadData();
                loadStatistics();
            } else {
                showAlert("Error", "Failed to update payment status.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleRefund() {
        if (selectedPayment != null && selectedPayment.isRefundable()) {
            // Create refund dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Process Refund");
            dialog.setHeaderText("Refund Payment: " + selectedPayment.getTransactionId());

            ButtonType refundButtonType = new ButtonType("Process Refund", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(refundButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField refundAmountField = new TextField();
            refundAmountField.setPromptText("Refund Amount");
            refundAmountField.setText(selectedPayment.getFinalAmount().toString());

            TextArea refundReasonArea = new TextArea();
            refundReasonArea.setPromptText("Reason for refund");
            refundReasonArea.setPrefRowCount(3);

            Label maxRefundLabel = new Label("Max Refund: ₹" + selectedPayment.getFinalAmount());

            grid.add(new Label("Refund Amount:"), 0, 0);
            grid.add(refundAmountField, 1, 0);
            grid.add(maxRefundLabel, 1, 1);
            grid.add(new Label("Reason:"), 0, 2);
            grid.add(refundReasonArea, 1, 2);

            dialog.getDialogPane().setContent(grid);

            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == refundButtonType) {
                try {
                    BigDecimal refundAmount = new BigDecimal(refundAmountField.getText().trim());
                    String reason = refundReasonArea.getText().trim();

                    if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 ||
                            refundAmount.compareTo(selectedPayment.getFinalAmount()) > 0) {
                        showAlert("Invalid Amount", "Refund amount must be between 0 and " +
                                selectedPayment.getFinalAmount(), Alert.AlertType.ERROR);
                        return;
                    }

                    if (reason.isEmpty()) {
                        showAlert("Missing Reason", "Please provide a reason for the refund.",
                                Alert.AlertType.ERROR);
                        return;
                    }

                    if (paymentDAO.processRefund(selectedPayment.getId(), refundAmount, reason)) {
                        showAlert("Refund Processed",
                                "Refund of ₹" + refundAmount + " processed successfully!",
                                Alert.AlertType.INFORMATION);
                        clearFields();
                        loadData();
                        loadStatistics();
                    } else {
                        showAlert("Error", "Failed to process refund.", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Invalid Amount", "Please enter a valid refund amount.", Alert.AlertType.ERROR);
                }
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedPayment != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete Payment");
            confirmation.setContentText("Are you sure you want to cancel this payment?\n" +
                    "Transaction ID: " + selectedPayment.getTransactionId());

            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (paymentDAO.deletePayment(selectedPayment.getId())) {
                    showAlert("Success", "Payment cancelled successfully!", Alert.AlertType.INFORMATION);
                    clearFields();
                    loadData();
                    loadStatistics();
                } else {
                    showAlert("Error", "Failed to cancel payment.", Alert.AlertType.ERROR);
                }
            }
        }
    }

    @FXML
    private void handleGenerateReceipt() {
        if (selectedPayment != null) {
            // Create receipt dialog
            Alert receiptAlert = new Alert(Alert.AlertType.INFORMATION);
            receiptAlert.setTitle("Payment Receipt");
            receiptAlert.setHeaderText("Receipt Details");

            String receiptContent = generateReceiptContent(selectedPayment);
            receiptAlert.setContentText(receiptContent);

            receiptAlert.getDialogPane().setMinWidth(500);
            receiptAlert.showAndWait();
        } else {
            showAlert("No Selection", "Please select a payment to generate receipt.",
                    Alert.AlertType.WARNING);
        }
    }

    private String generateReceiptContent(Payment payment) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("           GYM PAYMENT RECEIPT          \n");
        receipt.append("═══════════════════════════════════════\n\n");

        receipt.append("Invoice Number: ").append(payment.getInvoiceNumber()).append("\n");
        receipt.append("Transaction ID: ").append(payment.getTransactionId()).append("\n");
        receipt.append("Date: ").append(payment.getPaymentDate()).append("\n\n");

        receipt.append("───────────────────────────────────────\n");
        receipt.append("Member: ").append(payment.getMember().getUser().getFullName()).append("\n");
        receipt.append("Email: ").append(payment.getMember().getUser().getEmail()).append("\n");
        receipt.append("Phone: ").append(payment.getMember().getUser().getPhone()).append("\n\n");

        receipt.append("───────────────────────────────────────\n");
        receipt.append("Payment Type: ").append(payment.getPaymentType()).append("\n");
        receipt.append("Payment Method: ").append(payment.getPaymentMethod()).append("\n");
        if (payment.getMembershipPlan() != null) {
            receipt.append("Membership Plan: ").append(payment.getMembershipPlan().getPlanName()).append("\n");
        }
        receipt.append("\n");

        receipt.append("Amount: ₹").append(payment.getAmount()).append("\n");
        if (payment.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Discount: -₹").append(payment.getDiscount()).append("\n");
            if (payment.getCouponCode() != null) {
                receipt.append("Coupon: ").append(payment.getCouponCode()).append("\n");
            }
        }
        receipt.append("───────────────────────────────────────\n");
        receipt.append("Final Amount: ₹").append(payment.getFinalAmount()).append("\n");
        receipt.append("Status: ").append(payment.getStatus()).append("\n");

        if (payment.getRefundAmount() != null &&
                payment.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("\nRefund Amount: ₹").append(payment.getRefundAmount()).append("\n");
            receipt.append("Refund Date: ").append(payment.getRefundDate()).append("\n");
        }

        receipt.append("\n═══════════════════════════════════════\n");
        receipt.append("        Thank you for your payment!     \n");
        receipt.append("═══════════════════════════════════════\n");

        return receipt.toString();
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadData();
        } else {
            payments.clear();
            payments.addAll(paymentDAO.searchPayments(searchTerm));
        }
        // Recalculate statistics after search
        loadStatistics();
    }

    @FXML
    private void handleFilterByDate() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                showAlert("Invalid Date Range", "Start date cannot be after end date.",
                        Alert.AlertType.ERROR);
                return;
            }
            payments.clear();
            payments.addAll(paymentDAO.getPaymentsByDateRange(startDate, endDate));
            loadStatistics(); // Recalculate statistics for filtered data
        } else {
            showAlert("Missing Dates", "Please select both start and end dates.",
                    Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        loadData();
        loadStatistics(); // Reload full statistics
    }

    @FXML
    private void handleApplyDiscount() {
        if (!amountField.getText().trim().isEmpty()) {
            TextInputDialog dialog = new TextInputDialog("10");
            dialog.setTitle("Apply Discount");
            dialog.setHeaderText("Enter Discount Percentage");
            dialog.setContentText("Discount %:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(percentage -> {
                try {
                    double discountPercent = Double.parseDouble(percentage);
                    if (discountPercent > 0 && discountPercent <= 100) {
                        BigDecimal amount = new BigDecimal(amountField.getText().trim());
                        BigDecimal discount = amount.multiply(BigDecimal.valueOf(discountPercent / 100));
                        discountField.setText(discount.toString());
                    } else {
                        showAlert("Invalid Percentage", "Discount must be between 0 and 100.",
                                Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter a valid percentage.", Alert.AlertType.ERROR);
                }
            });
        } else {
            showAlert("Missing Amount", "Please enter amount first.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleBack() {
        loadScene("/fxml/admin-dashboard.fxml", "Admin Dashboard");
    }

    private void populateFields(Payment payment) {
        // Find and select member
        for (Member member : members) {
            if (member.getId() == payment.getMemberId()) {
                memberComboBox.setValue(member);
                break;
            }
        }

        amountField.setText(payment.getAmount().toString());
        discountField.setText(payment.getDiscount().toString());
        finalAmountField.setText(payment.getFinalAmount().toString());
        paymentMethodComboBox.setValue(payment.getPaymentMethod());
        paymentTypeComboBox.setValue(payment.getPaymentType());
        statusComboBox.setValue(payment.getStatus());
        couponCodeField.setText(payment.getCouponCode() != null ? payment.getCouponCode() : "");
        descriptionField.setText(payment.getDescription());
    }

    private void clearFields() {
        memberComboBox.setValue(null);
        amountField.clear();
        discountField.clear();
        finalAmountField.clear();
        paymentMethodComboBox.setValue("CASH");
        paymentTypeComboBox.setValue("MEMBERSHIP");
        statusComboBox.setValue("COMPLETED");
        couponCodeField.clear();
        descriptionField.clear();
    }

    private boolean validateInput() {
        if (memberComboBox.getValue() == null) {
            showAlert("Missing Member", "Please select a member.", Alert.AlertType.WARNING);
            return false;
        }

        if (amountField.getText().trim().isEmpty()) {
            showAlert("Missing Amount", "Please enter payment amount.", Alert.AlertType.WARNING);
            return false;
        }

        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Invalid Amount", "Amount must be greater than zero.", Alert.AlertType.WARNING);
                return false;
            }

            if (!discountField.getText().trim().isEmpty()) {
                BigDecimal discount = new BigDecimal(discountField.getText().trim());
                if (discount.compareTo(amount) > 0) {
                    showAlert("Invalid Discount", "Discount cannot be greater than amount.",
                            Alert.AlertType.WARNING);
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter valid numeric values.", Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            Stage stage = (Stage) paymentsTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading scene", e);
            e.printStackTrace();
        }
    }
    /**
     * Handle Generate Report button click
     */
    @FXML
    private void handleGenerateReport() {
        System.out.println("🔄 Generating monthly report...");

        try {
            // Get current stage
            Stage stage = (Stage) paymentsTable.getScene().getWindow();

            // Create report generator
            PaymentReportGenerator reportGenerator = new PaymentReportGenerator();

            // Generate report
            boolean success = reportGenerator.generateMonthlyReport(stage);

            if (success) {
                System.out.println("✅ Report generated successfully!");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating report", e);
            showAlert("Report Generation Failed",
                    "Unable to generate report: " + e.getMessage(),
                    Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
}