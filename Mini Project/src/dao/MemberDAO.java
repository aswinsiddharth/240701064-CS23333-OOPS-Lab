package com.gymmanagementsystem.dao;

import com.gymmanagementsystem.model.Member;
import com.gymmanagementsystem.model.User;
import com.gymmanagementsystem.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced MemberDAO with better error handling, logging, and search functionality
 */
public class MemberDAO {

    private static final Logger LOGGER = Logger.getLogger(MemberDAO.class.getName());

    /**
     * Create a new member with transaction support
     */
    public boolean createMember(Member member) {
        String sql = "INSERT INTO members (user_id, emergency_contact, medical_conditions, " +
                "membership_plan_id, membership_start_date, membership_end_date, membership_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, member.getUserId());
            stmt.setString(2, member.getEmergencyContact());
            stmt.setString(3, member.getMedicalConditions());
            stmt.setInt(4, member.getMembershipPlanId());
            stmt.setDate(5, member.getMembershipStartDate());
            stmt.setDate(6, member.getMembershipEndDate());
            stmt.setString(7, member.getMembershipStatus());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        member.setId(generatedKeys.getInt(1));
                        LOGGER.log(Level.INFO, "Member created successfully with ID: {0}", member.getId());
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating member", e);
        }

        return false;
    }

    /**
     * Get all members with enhanced data including membership plan details
     */
    public List<Member> getAllMembers() {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT m.*, u.username, u.email, u.first_name, u.last_name, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM members m " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "ORDER BY u.first_name, u.last_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                members.add(extractMemberFromResultSet(rs));
            }

            LOGGER.log(Level.INFO, "Retrieved {0} members", members.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all members", e);
        }

        return members;
    }

    /**
     * Get member by ID with full details
     */
    public Member getMemberById(int memberId) {
        String sql = "SELECT m.*, u.username, u.email, u.first_name, u.last_name, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM members m " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "WHERE m.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, memberId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractMemberFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving member by ID: " + memberId, e);
        }

        return null;
    }

    /**
     * Update member information
     */
    public boolean updateMember(Member member) {
        String sql = "UPDATE members SET emergency_contact = ?, medical_conditions = ?, " +
                "membership_plan_id = ?, membership_start_date = ?, membership_end_date = ?, " +
                "membership_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, member.getEmergencyContact());
            stmt.setString(2, member.getMedicalConditions());
            stmt.setInt(3, member.getMembershipPlanId());
            stmt.setDate(4, member.getMembershipStartDate());
            stmt.setDate(5, member.getMembershipEndDate());
            stmt.setString(6, member.getMembershipStatus());
            stmt.setInt(7, member.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Member updated successfully: ID {0}", member.getId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating member: " + member.getId(), e);
        }

        return false;
    }

    /**
     * Delete member (consider soft delete in production)
     */
    public boolean deleteMember(int memberId) {
        String sql = "DELETE FROM members WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, memberId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Member deleted: ID {0}", memberId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting member: " + memberId, e);
        }

        return false;
    }

    /**
     * Get member ID by user ID
     */
    public int getMemberIdByUserId(int userId) {
        String sql = "SELECT id FROM members WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving member ID by user ID: " + userId, e);
        }

        return -1;
    }

    /**
     * NEW: Search members by name, email, or phone
     */
    public List<Member> searchMembers(String searchTerm) {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT m.*, u.username, u.email, u.first_name, u.last_name, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM members m " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "WHERE LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ? " +
                "OR LOWER(u.email) LIKE ? OR u.phone LIKE ? " +
                "ORDER BY u.first_name, u.last_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(extractMemberFromResultSet(rs));
                }
            }

            LOGGER.log(Level.INFO, "Search found {0} members for term: {1}",
                    new Object[]{members.size(), searchTerm});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching members", e);
        }

        return members;
    }

    /**
     * NEW: Get members by status
     */
    public List<Member> getMembersByStatus(String status) {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT m.*, u.username, u.email, u.first_name, u.last_name, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM members m " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "WHERE m.membership_status = ? " +
                "ORDER BY u.first_name, u.last_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(extractMemberFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving members by status: " + status, e);
        }

        return members;
    }

    /**
     * NEW: Get expiring memberships (within specified days)
     */
    public List<Member> getExpiringMemberships(int daysAhead) {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT m.*, u.username, u.email, u.first_name, u.last_name, u.phone, " +
                "mp.plan_name, mp.price as plan_price " +
                "FROM members m " +
                "JOIN users u ON m.user_id = u.id " +
                "LEFT JOIN membership_plans mp ON m.membership_plan_id = mp.id " +
                "WHERE m.membership_status = 'ACTIVE' " +
                "AND m.membership_end_date BETWEEN CURRENT_DATE AND CURRENT_DATE + ? " +
                "ORDER BY m.membership_end_date";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, daysAhead);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(extractMemberFromResultSet(rs));
                }
            }

            LOGGER.log(Level.INFO, "Found {0} expiring memberships", members.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving expiring memberships", e);
        }

        return members;
    }

    /**
     * NEW: Update expired memberships automatically
     */
    public int updateExpiredMemberships() {
        String sql = "UPDATE members SET membership_status = 'EXPIRED' " +
                "WHERE membership_status = 'ACTIVE' " +
                "AND membership_end_date < CURRENT_DATE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int rowsAffected = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Updated {0} expired memberships", rowsAffected);
            return rowsAffected;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating expired memberships", e);
        }

        return 0;
    }

    /**
     * NEW: Get membership statistics
     */
    public MembershipStats getMembershipStats() {
        String sql = "SELECT " +
                "COUNT(*) as total, " +
                "SUM(CASE WHEN membership_status = 'ACTIVE' THEN 1 ELSE 0 END) as active, " +
                "SUM(CASE WHEN membership_status = 'EXPIRED' THEN 1 ELSE 0 END) as expired, " +
                "SUM(CASE WHEN membership_status = 'SUSPENDED' THEN 1 ELSE 0 END) as suspended " +
                "FROM members";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new MembershipStats(
                        rs.getInt("total"),
                        rs.getInt("active"),
                        rs.getInt("expired"),
                        rs.getInt("suspended")
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving membership stats", e);
        }

        return new MembershipStats(0, 0, 0, 0);
    }

    /**
     * Helper method to extract Member from ResultSet
     */
    private Member extractMemberFromResultSet(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.setId(rs.getInt("id"));
        member.setUserId(rs.getInt("user_id"));
        member.setEmergencyContact(rs.getString("emergency_contact"));
        member.setMedicalConditions(rs.getString("medical_conditions"));
        member.setMembershipPlanId(rs.getInt("membership_plan_id"));
        member.setMembershipStartDate(rs.getDate("membership_start_date"));
        member.setMembershipEndDate(rs.getDate("membership_end_date"));
        member.setMembershipStatus(rs.getString("membership_status"));

        // Set membership plan details if available
        try {
            member.setMembershipPlanName(rs.getString("plan_name"));
            member.setMembershipPlanPrice(rs.getDouble("plan_price"));
        } catch (SQLException e) {
            // Plan details not available in this query
        }

        // Set user information
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setPhone(rs.getString("phone"));
        member.setUser(user);

        return member;
    }

    /**
     * Inner class for membership statistics
     */
    public static class MembershipStats {
        private final int total;
        private final int active;
        private final int expired;
        private final int suspended;

        public MembershipStats(int total, int active, int expired, int suspended) {
            this.total = total;
            this.active = active;
            this.expired = expired;
            this.suspended = suspended;
        }

        public int getTotal() { return total; }
        public int getActive() { return active; }
        public int getExpired() { return expired; }
        public int getSuspended() { return suspended; }
    }
    /**
     * Get new members count for specific month and year - FOR REPORTS
     */
    public int getNewMembersForMonth(int month, int year) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM members " +
                "WHERE MONTH(membership_start_date) = ? AND YEAR(membership_start_date) = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, month);
            stmt.setInt(2, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting new members for month: " + month + "/" + year, e);
            return 0;
        }
    }

    /**
     * Get total members count - FOR REPORTS
     */
    public int getTotalMembersCount() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM members";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total members count", e);
            return 0;
        }
    }

    /**
     * Get active members count - FOR REPORTS
     */
    public int getActiveMembersCount() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM members WHERE membership_status = 'ACTIVE'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting active members count", e);
            return 0;
        }
    }
}