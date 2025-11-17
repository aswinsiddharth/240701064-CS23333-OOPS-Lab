package com.gymmanagementsystem.dao;

import com.gymmanagementsystem.model.Payment;
import com.gymmanagementsystem.model.Member;
import com.gymmanagementsystem.model.User;
import com.gymmanagementsystem.model.MembershipPlan;
import com.gymmanagementsystem.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentDAO {

    private static final Logger LOGGER = Logger.getLogger(PaymentDAO.class.getName());

    // Create payment with extended membership
    public boolean createPayment(Payment payment) {
        String sql = "INSERT INTO payments (member_id, transaction_id, amount, discount, final_amount, " +
                "payment_method, payment_type, status, description, invoice_number, coupon_code, processed_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setInt(1, payment.getMemberId());
            stmt.setString(2, payment.getTransactionId());
            stmt.setBigDecimal(3, payment.getAmount());
            stmt.setBigDecimal(4, payment.getDiscount());
            stmt.setBigDecimal(5, payment.getFinalAmount());
            stmt.setString(6, payment.getPaymentMethod());
            stmt.setString(7, payment.getPaymentType());
            stmt.setString(8, payment.getStatus());
            stmt.setString(9, payment.getDescription());
            stmt.setString(10, payment.getInvoiceNumber());
            stmt.setString(11, payment.getCouponCode());
            stmt.setInt(12, payment.getProcessedBy());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    payment.setId(generatedKeys.getInt(1));
                }

                if ("MEMBERSHIP".equals(payment.getPaymentType()) ||
                        "RENEWAL".equals(payment.getPaymentType())) {
                    extendMembership(conn, payment.getMemberId());
                }

                conn.commit();
                LOGGER.log(Level.INFO, "✅ Payment created: {0}, Status: {1}, Amount: {2}",
                        new Object[]{payment.getTransactionId(), payment.getStatus(), payment.getFinalAmount()});
                return true;
            }

            conn.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating payment", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }

        return false;
    }

    private void extendMembership(Connection conn, int memberId) throws SQLException {
        String sql = "UPDATE members SET " +
                "membership_end_date = CASE " +
                "   WHEN membership_end_date > CURRENT_DATE THEN " +
                "       DATE_ADD(membership_end_date, INTERVAL " +
                "           (SELECT duration_in_months FROM membership_plans WHERE id = members.membership_plan_id) MONTH) " +
                "   ELSE " +
                "       DATE_ADD(CURRENT_DATE, INTERVAL " +
                "           (SELECT duration_in_months FROM membership_plans WHERE id = members.membership_plan_id) MONTH) " +
                "END, " +
                "membership_status = 'ACTIVE' " +
                "WHERE id = ?";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, memberId);
        stmt.executeUpdate();
    }

    public List<Payment> getAllPayments() {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT p.*, u.first_name, u.last_name, u.email, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM payments p " +
                "JOIN members m ON p.member_id = m.id " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "ORDER BY p.payment_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Payment payment = extractPaymentFromResultSet(rs);
                payments.add(payment);
            }

            LOGGER.log(Level.INFO, "Retrieved {0} payments", payments.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving payments", e);
        }

        return payments;
    }

    public List<Payment> getPaymentsByMember(int memberId) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT p.*, u.first_name, u.last_name, u.email, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM payments p " +
                "JOIN members m ON p.member_id = m.id " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "WHERE p.member_id = ? " +
                "ORDER BY p.payment_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Payment payment = extractPaymentFromResultSet(rs);
                payments.add(payment);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving payments for member: " + memberId, e);
        }

        return payments;
    }

    public Payment getPaymentByTransactionId(String transactionId) {
        String sql = "SELECT p.*, u.first_name, u.last_name, u.email, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM payments p " +
                "JOIN members m ON p.member_id = m.id " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "WHERE p.transaction_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transactionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractPaymentFromResultSet(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving payment by transaction ID: " + transactionId, e);
        }

        return null;
    }

    public boolean updatePaymentStatus(int paymentId, String status) {
        String sql = "UPDATE payments SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, paymentId);

            boolean updated = stmt.executeUpdate() > 0;
            if (updated) {
                LOGGER.log(Level.INFO, "Payment status updated: ID {0} -> {1}", new Object[]{paymentId, status});
            }
            return updated;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating payment status", e);
        }

        return false;
    }

    public boolean processRefund(int paymentId, BigDecimal refundAmount, String reason) {
        String sql = "UPDATE payments SET refund_amount = ?, refund_reason = ?, " +
                "refund_date = CURRENT_TIMESTAMP, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, refundAmount);
            stmt.setString(2, reason);
            stmt.setString(3, "REFUNDED");
            stmt.setInt(4, paymentId);

            boolean refunded = stmt.executeUpdate() > 0;
            if (refunded) {
                LOGGER.log(Level.INFO, "Refund processed: ID {0}, Amount: {1}",
                        new Object[]{paymentId, refundAmount});
            }
            return refunded;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error processing refund", e);
        }

        return false;
    }

    // COMPLETELY REWRITTEN - ULTRA SIMPLE AND BULLETPROOF
    public PaymentStats getPaymentStatistics() {
        System.out.println("\n🔄 ========== LOADING PAYMENT STATISTICS ==========");
        PaymentStats stats = new PaymentStats();

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            System.out.println("✅ Database connection established");

            // STEP 1: Get total count
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM payments");
            if (rs.next()) {
                stats.totalPayments = rs.getInt("total");
                System.out.println("📊 Total payments in database: " + stats.totalPayments);
            }
            rs.close();
            stmt.close();

            // STEP 2: Get completed payments and revenue
            stmt = conn.createStatement();
            rs = stmt.executeQuery(
                    "SELECT COUNT(*) as count, SUM(final_amount) as revenue " +
                            "FROM payments WHERE status = 'COMPLETED'"
            );
            if (rs.next()) {
                stats.completedCount = rs.getInt("count");
                BigDecimal revenue = rs.getBigDecimal("revenue");
                stats.completedRevenue = (revenue != null) ? revenue : BigDecimal.ZERO;
                stats.totalRevenue = stats.completedRevenue;

                System.out.println("✅ Completed payments: " + stats.completedCount);
                System.out.println("💰 Completed revenue: ₹" + stats.completedRevenue);
            }
            rs.close();
            stmt.close();

            // STEP 3: Get pending payments
            stmt = conn.createStatement();
            rs = stmt.executeQuery(
                    "SELECT COUNT(*) as count, SUM(final_amount) as revenue " +
                            "FROM payments WHERE status = 'PENDING'"
            );
            if (rs.next()) {
                stats.pendingCount = rs.getInt("count");
                BigDecimal revenue = rs.getBigDecimal("revenue");
                stats.pendingRevenue = (revenue != null) ? revenue : BigDecimal.ZERO;

                System.out.println("⏳ Pending payments: " + stats.pendingCount);
                System.out.println("💵 Pending revenue: ₹" + stats.pendingRevenue);
            }
            rs.close();
            stmt.close();

            // STEP 4: Get failed payments
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM payments WHERE status = 'FAILED'");
            if (rs.next()) {
                stats.failedCount = rs.getInt("count");
                System.out.println("❌ Failed payments: " + stats.failedCount);
            }
            rs.close();
            stmt.close();

            // STEP 5: Get refunded payments and amount
            stmt = conn.createStatement();
            rs = stmt.executeQuery(
                    "SELECT COUNT(*) as count, SUM(refund_amount) as total_refunds " +
                            "FROM payments WHERE status = 'REFUNDED'"
            );
            if (rs.next()) {
                stats.refundedCount = rs.getInt("count");
                BigDecimal refunds = rs.getBigDecimal("total_refunds");
                stats.totalRefunds = (refunds != null) ? refunds : BigDecimal.ZERO;

                System.out.println("🔄 Refunded payments: " + stats.refundedCount);
                System.out.println("💸 Total refunds: ₹" + stats.totalRefunds);
            }

            System.out.println("\n✅ ========== STATISTICS LOADED SUCCESSFULLY ==========");
            System.out.println("📈 SUMMARY:");
            System.out.println("   Total Revenue: ₹" + stats.totalRevenue);
            System.out.println("   Completed: " + stats.completedCount + " payments");
            System.out.println("   Pending: " + stats.pendingCount + " payments");
            System.out.println("   Refunds: ₹" + stats.totalRefunds);
            System.out.println("=====================================================\n");

        } catch (SQLException e) {
            System.err.println("❌ ========== SQL ERROR ==========");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            System.err.println("==================================\n");

            LOGGER.log(Level.SEVERE, "Error retrieving payment statistics", e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing resources", e);
            }
        }

        return stats;
    }

    public List<Payment> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT p.*, u.first_name, u.last_name, u.email, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM payments p " +
                "JOIN members m ON p.member_id = m.id " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "WHERE DATE(p.payment_date) BETWEEN ? AND ? " +
                "ORDER BY p.payment_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Payment payment = extractPaymentFromResultSet(rs);
                payments.add(payment);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving payments by date range", e);
        }

        return payments;
    }

    public List<Payment> searchPayments(String searchTerm) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT p.*, u.first_name, u.last_name, u.email, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM payments p " +
                "JOIN members m ON p.member_id = m.id " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "WHERE u.first_name LIKE ? OR u.last_name LIKE ? " +
                "OR p.transaction_id LIKE ? OR p.invoice_number LIKE ? " +
                "ORDER BY p.payment_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + searchTerm + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Payment payment = extractPaymentFromResultSet(rs);
                payments.add(payment);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching payments", e);
        }

        return payments;
    }

    public boolean deletePayment(int paymentId) {
        String sql = "UPDATE payments SET status = 'CANCELLED' WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, paymentId);
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) {
                LOGGER.log(Level.INFO, "Payment cancelled: ID {0}", paymentId);
            }
            return deleted;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting payment", e);
        }

        return false;
    }

    private Payment extractPaymentFromResultSet(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setId(rs.getInt("id"));
        payment.setMemberId(rs.getInt("member_id"));
        payment.setTransactionId(rs.getString("transaction_id"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setDiscount(rs.getBigDecimal("discount") != null ? rs.getBigDecimal("discount") : BigDecimal.ZERO);
        payment.setFinalAmount(rs.getBigDecimal("final_amount"));
        payment.setPaymentMethod(rs.getString("payment_method"));
        payment.setPaymentType(rs.getString("payment_type"));
        payment.setStatus(rs.getString("status"));
        payment.setPaymentDate(rs.getTimestamp("payment_date"));
        payment.setDescription(rs.getString("description"));
        payment.setInvoiceNumber(rs.getString("invoice_number"));
        payment.setCouponCode(rs.getString("coupon_code"));
        payment.setRefundAmount(rs.getBigDecimal("refund_amount"));
        payment.setRefundDate(rs.getTimestamp("refund_date"));
        payment.setRefundReason(rs.getString("refund_reason"));
        payment.setProcessedBy(rs.getInt("processed_by"));

        Member member = new Member();
        member.setId(rs.getInt("member_id"));
        User user = new User();
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        member.setUser(user);
        payment.setMember(member);

        String planName = rs.getString("plan_name");
        if (planName != null) {
            MembershipPlan plan = new MembershipPlan();
            plan.setPlanName(planName);
            plan.setPrice(rs.getBigDecimal("plan_price"));
            payment.setMembershipPlan(plan);
        }

        return payment;
    }

    public static class PaymentStats {
        public int totalPayments;
        public BigDecimal totalRevenue;
        public BigDecimal completedRevenue;
        public BigDecimal pendingRevenue;
        public BigDecimal totalRefunds;
        public int completedCount;
        public int pendingCount;
        public int failedCount;
        public int refundedCount;

        public PaymentStats() {
            this.totalPayments = 0;
            this.totalRevenue = BigDecimal.ZERO;
            this.completedRevenue = BigDecimal.ZERO;
            this.pendingRevenue = BigDecimal.ZERO;
            this.totalRefunds = BigDecimal.ZERO;
            this.completedCount = 0;
            this.pendingCount = 0;
            this.failedCount = 0;
            this.refundedCount = 0;
        }
    }
    /**
     * Get count of completed payments for specific month
     */
    public int getCompletedPaymentsCount(int month, int year) throws SQLException {
        String query = "SELECT COUNT(*) as total FROM payments " +
                "WHERE MONTH(payment_date) = ? AND YEAR(payment_date) = ? " +
                "AND status = 'COMPLETED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, month);
            stmt.setInt(2, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting completed payments count", e);
            return 0;
        }
    }

    /**
     * Get monthly revenue for specific month and year
     */
    public double getMonthlyRevenue(int month, int year) throws SQLException {
        String query = "SELECT COALESCE(SUM(final_amount), 0) as total FROM payments " +
                "WHERE MONTH(payment_date) = ? AND YEAR(payment_date) = ? " +
                "AND status = 'COMPLETED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, month);
            stmt.setInt(2, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
                return 0.0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting monthly revenue", e);
            return 0.0;
        }
    }

    /**
     * Get count of failed payments for specific month
     */
    public int getFailedPaymentsCount(int month, int year) throws SQLException {
        String query = "SELECT COUNT(*) as total FROM payments " +
                "WHERE MONTH(payment_date) = ? AND YEAR(payment_date) = ? " +
                "AND status = 'FAILED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, month);
            stmt.setInt(2, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting failed payments count", e);
            return 0;
        }
    }

    /**
     * Get count of refunded payments for specific month
     */
    public int getRefundedPaymentsCount(int month, int year) throws SQLException {
        String query = "SELECT COUNT(*) as total FROM payments " +
                "WHERE MONTH(payment_date) = ? AND YEAR(payment_date) = ? " +
                "AND status = 'REFUNDED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, month);
            stmt.setInt(2, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting refunded payments count", e);
            return 0;
        }
    }

    /**
     * Get total refund amount for specific month
     */
    public double getTotalRefundsForMonth(int month, int year) throws SQLException {
        String query = "SELECT COALESCE(SUM(refund_amount), 0) as total FROM payments " +
                "WHERE MONTH(payment_date) = ? AND YEAR(payment_date) = ? " +
                "AND status = 'REFUNDED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, month);
            stmt.setInt(2, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
                return 0.0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total refunds", e);
            return 0.0;
        }
    }

    /**
     * Get payment count by method for specific month
     */
    public int getPaymentCountByMethod(String method, int month, int year) throws SQLException {
        String query = "SELECT COUNT(*) as total FROM payments " +
                "WHERE payment_method = ? " +
                "AND MONTH(payment_date) = ? AND YEAR(payment_date) = ? " +
                "AND status = 'COMPLETED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, method);
            stmt.setInt(2, month);
            stmt.setInt(3, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting payment count by method", e);
            return 0;
        }
    }
    /**
     * Get count of pending payments (all pending, not just current month)
     */
    public int getPendingPaymentsCount() throws SQLException {
        String query = "SELECT COUNT(*) as total FROM payments WHERE status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting pending payments count", e);
            return 0;
        }
    }
}