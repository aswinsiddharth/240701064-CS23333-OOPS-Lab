package com.gymmanagementsystem.util;

import com.gymmanagementsystem.dao.MemberDAO;
import com.gymmanagementsystem.dao.PaymentDAO;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generate comprehensive payment and membership reports
 *
 * @author GymPulse Team
 * @version 1.0
 */
public class PaymentReportGenerator {

    private final MemberDAO memberDAO;
    private final PaymentDAO paymentDAO;
    private final NumberFormat currencyFormatter;
    private final DateTimeFormatter dateFormatter;

    public PaymentReportGenerator() {
        this.memberDAO = new MemberDAO();
        this.paymentDAO = new PaymentDAO();
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        this.dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    }

    /**
     * Generate comprehensive monthly report
     *
     * @param stage Parent stage for file chooser
     * @return true if report generated successfully
     */
    public boolean generateMonthlyReport(Stage stage) {
        try {
            // Get current and previous month data
            ReportData currentMonth = getMonthlyData(YearMonth.now());
            ReportData previousMonth = getMonthlyData(YearMonth.now().minusMonths(1));

            // Let user choose save location
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Monthly Report");
            fileChooser.setInitialFileName("Monthly_Report_" +
                    YearMonth.now().format(DateTimeFormatter.ofPattern("MMM_yyyy")) + ".txt");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                // Generate report content
                String reportContent = generateReportContent(currentMonth, previousMonth);

                // Write to file
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(reportContent);
                }

                showSuccess("Report generated successfully!\nSaved to: " + file.getAbsolutePath());
                return true;
            }

        } catch (Exception e) {
            showError("Failed to generate report", e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get monthly statistics data
     */
    private ReportData getMonthlyData(YearMonth month) throws SQLException {
        ReportData data = new ReportData();

        // Get member statistics
        data.newMembers = memberDAO.getNewMembersForMonth(month.getMonthValue(), month.getYear());
        data.totalActiveMembers = memberDAO.getActiveMembersCount();
        data.totalMembers = memberDAO.getTotalMembersCount();

        // Get payment statistics
        data.totalRevenue = paymentDAO.getMonthlyRevenue(month.getMonthValue(), month.getYear());
        data.completedPayments = paymentDAO.getCompletedPaymentsCount(month.getMonthValue(), month.getYear());
        data.pendingPayments = paymentDAO.getPendingPaymentsCount();
        data.failedPayments = paymentDAO.getFailedPaymentsCount(month.getMonthValue(), month.getYear());
        data.refundedPayments = paymentDAO.getRefundedPaymentsCount(month.getMonthValue(), month.getYear());
        data.totalRefunds = paymentDAO.getTotalRefundsForMonth(month.getMonthValue(), month.getYear());

        // Payment method breakdown
        data.cashPayments = paymentDAO.getPaymentCountByMethod("CASH", month.getMonthValue(), month.getYear());
        data.cardPayments = paymentDAO.getPaymentCountByMethod("CARD", month.getMonthValue(), month.getYear());
        data.onlinePayments = paymentDAO.getPaymentCountByMethod("ONLINE", month.getMonthValue(), month.getYear());

        return data;
    }

    /**
     * Generate formatted report content
     */
    private String generateReportContent(ReportData current, ReportData previous) {
        StringBuilder report = new StringBuilder();

        // Header
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                 GYM MASTER MANAGEMENT SYSTEM\n");
        report.append("                    MONTHLY PAYMENT REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        report.append("Report Generated: ").append(LocalDate.now().format(dateFormatter)).append("\n");
        report.append("Report Period: ").append(YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))).append("\n\n");

        // MEMBERSHIP STATISTICS
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                    MEMBERSHIP STATISTICS\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        report.append(String.format("%-40s : %d\n", "New Members This Month", current.newMembers));
        report.append(String.format("%-40s : %d\n", "New Members Last Month", previous.newMembers));
        report.append(String.format("%-40s : %+d\n\n", "Month-over-Month Change",
                current.newMembers - previous.newMembers));

        report.append(String.format("%-40s : %d\n", "Total Active Members", current.totalActiveMembers));
        report.append(String.format("%-40s : %d\n", "Total Members (All Status)", current.totalMembers));
        report.append(String.format("%-40s : %.1f%%\n\n", "Active Member Rate",
                (current.totalActiveMembers * 100.0 / current.totalMembers)));

        // REVENUE STATISTICS
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                     REVENUE STATISTICS\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        report.append(String.format("%-40s : %s\n", "Total Revenue (Current Month)",
                currencyFormatter.format(current.totalRevenue)));
        report.append(String.format("%-40s : %s\n", "Total Revenue (Previous Month)",
                currencyFormatter.format(previous.totalRevenue)));
        report.append(String.format("%-40s : %s\n\n", "Revenue Change",
                currencyFormatter.format(current.totalRevenue - previous.totalRevenue)));

        if (current.completedPayments > 0) {
            report.append(String.format("%-40s : %s\n\n", "Average Payment Amount",
                    currencyFormatter.format(current.totalRevenue / current.completedPayments)));
        }

        // PAYMENT STATISTICS
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                    PAYMENT STATISTICS\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        report.append(String.format("%-40s : %d\n", "Completed Payments", current.completedPayments));
        report.append(String.format("%-40s : %d\n", "Pending Payments", current.pendingPayments));
        report.append(String.format("%-40s : %d\n", "Failed Payments", current.failedPayments));
        report.append(String.format("%-40s : %d\n\n", "Refunded Payments", current.refundedPayments));

        int totalPayments = current.completedPayments + current.pendingPayments +
                current.failedPayments + current.refundedPayments;

        if (totalPayments > 0) {
            report.append(String.format("%-40s : %.1f%%\n", "Success Rate",
                    (current.completedPayments * 100.0 / totalPayments)));
            report.append(String.format("%-40s : %.1f%%\n\n", "Failure Rate",
                    (current.failedPayments * 100.0 / totalPayments)));
        }

        report.append(String.format("%-40s : %s\n\n", "Total Refunds Issued",
                currencyFormatter.format(current.totalRefunds)));

        // PAYMENT METHOD BREAKDOWN
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                 PAYMENT METHOD BREAKDOWN\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        report.append(String.format("%-40s : %d payments\n", "Cash Payments", current.cashPayments));
        report.append(String.format("%-40s : %d payments\n", "Card Payments", current.cardPayments));
        report.append(String.format("%-40s : %d payments\n\n", "Online Payments", current.onlinePayments));

        int methodTotal = current.cashPayments + current.cardPayments + current.onlinePayments;
        if (methodTotal > 0) {
            report.append("Payment Method Distribution:\n");
            report.append(String.format("  Cash   : %.1f%%\n", (current.cashPayments * 100.0 / methodTotal)));
            report.append(String.format("  Card   : %.1f%%\n", (current.cardPayments * 100.0 / methodTotal)));
            report.append(String.format("  Online : %.1f%%\n\n", (current.onlinePayments * 100.0 / methodTotal)));
        }

        // GROWTH ANALYSIS
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                      GROWTH ANALYSIS\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        // Member Growth
        if (previous.newMembers > 0) {
            double memberGrowth = ((current.newMembers - previous.newMembers) * 100.0) / previous.newMembers;
            report.append(String.format("%-40s : %+.1f%%\n", "Member Growth Rate", memberGrowth));
        }

        // Revenue Growth
        if (previous.totalRevenue > 0) {
            double revenueGrowth = ((current.totalRevenue - previous.totalRevenue) * 100.0) / previous.totalRevenue;
            report.append(String.format("%-40s : %+.1f%%\n", "Revenue Growth Rate", revenueGrowth));
        }

        report.append("\n");

        // SUMMARY & INSIGHTS
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                   SUMMARY & INSIGHTS\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        // Generate insights
        if (current.newMembers > previous.newMembers) {
            report.append("✓ Member acquisition is improving! ");
            report.append(String.format("%d more members joined this month.\n",
                    current.newMembers - previous.newMembers));
        } else if (current.newMembers < previous.newMembers) {
            report.append("⚠ Member acquisition declined. ");
            report.append("Consider marketing initiatives.\n");
        }

        if (current.totalRevenue > previous.totalRevenue) {
            report.append("✓ Revenue is growing! ");
            report.append(String.format("Increased by %s this month.\n",
                    currencyFormatter.format(current.totalRevenue - previous.totalRevenue)));
        }

        if (current.pendingPayments > 5) {
            report.append("⚠ High number of pending payments. ");
            report.append("Follow up with members.\n");
        }

        if (current.failedPayments > 0) {
            report.append(String.format("⚠ %d failed payments this month. ", current.failedPayments));
            report.append("Review payment processing.\n");
        }

        report.append("\n");

        // Footer
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                     END OF REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n");

        return report.toString();
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Report Generated");
        alert.setHeaderText("Success!");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class to hold report data
     */
    private static class ReportData {
        int newMembers;
        int totalActiveMembers;
        int totalMembers;
        double totalRevenue;
        int completedPayments;
        int pendingPayments;
        int failedPayments;
        int refundedPayments;
        double totalRefunds;
        int cashPayments;
        int cardPayments;
        int onlinePayments;
    }
}