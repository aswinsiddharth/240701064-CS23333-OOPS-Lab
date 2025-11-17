package com.gymmanagementsystem.dao;

import com.gymmanagementsystem.model.GymClass;
import com.gymmanagementsystem.model.Trainer;
import com.gymmanagementsystem.model.User;
import com.gymmanagementsystem.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced ClassDAO with better error handling, validation, and advanced queries
 */
public class ClassDAO {

    private static final Logger LOGGER = Logger.getLogger(ClassDAO.class.getName());

    /**
     * Create a new class with enhanced validation
     */
    public boolean createClass(GymClass gymClass) {
        // Validate input
        if (!gymClass.isValid()) {
            LOGGER.warning("Invalid class data provided");
            return false;
        }

        // Check for trainer conflicts
        if (hasTrainerConflict(gymClass.getTrainerId(),
                gymClass.getStartTime(),
                gymClass.getEndTime(), null)) {
            LOGGER.warning("Trainer has conflicting schedule");
            return false;
        }

        String sql = "INSERT INTO classes (class_name, description, trainer_id, start_time, " +
                "end_time, max_capacity, current_bookings, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, gymClass.getClassName());
            stmt.setString(2, gymClass.getDescription());
            stmt.setInt(3, gymClass.getTrainerId());
            stmt.setTimestamp(4, gymClass.getStartTime());
            stmt.setTimestamp(5, gymClass.getEndTime());
            stmt.setInt(6, gymClass.getMaxCapacity());
            stmt.setInt(7, gymClass.getCurrentBookings());
            stmt.setString(8, gymClass.getStatus());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        gymClass.setId(generatedKeys.getInt(1));
                        LOGGER.log(Level.INFO, "Class created successfully: {0}", gymClass.getClassName());
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating class", e);
        }

        return false;
    }

    /**
     * Get all classes with enhanced data
     */
    public List<GymClass> getAllClasses() {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "ORDER BY c.start_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                classes.add(extractGymClassFromResultSet(rs));
            }

            LOGGER.log(Level.INFO, "Retrieved {0} classes", classes.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all classes", e);
        }

        return classes;
    }

    /**
     * Get class by ID
     */
    public GymClass getClassById(int classId) {
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE c.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractGymClassFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving class by ID: " + classId, e);
        }

        return null;
    }

    /**
     * Get classes by trainer
     */
    public List<GymClass> getClassesByTrainer(int trainerId) {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE c.trainer_id = ? " +
                "ORDER BY c.start_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    classes.add(extractGymClassFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving classes for trainer: " + trainerId, e);
        }

        return classes;
    }

    /**
     * Get upcoming classes
     */
    public List<GymClass> getUpcomingClasses() {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE c.start_time > NOW() AND c.status = 'SCHEDULED' " +
                "ORDER BY c.start_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                classes.add(extractGymClassFromResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving upcoming classes", e);
        }

        return classes;
    }

    /**
     * Get classes by date
     */
    public List<GymClass> getClassesByDate(LocalDate date) {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE DATE(c.start_time) = ? " +
                "ORDER BY c.start_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    classes.add(extractGymClassFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving classes for date: " + date, e);
        }

        return classes;
    }

    /**
     * Get classes by date range
     */
    public List<GymClass> getClassesByDateRange(LocalDate startDate, LocalDate endDate) {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE DATE(c.start_time) BETWEEN ? AND ? " +
                "ORDER BY c.start_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    classes.add(extractGymClassFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving classes for date range", e);
        }

        return classes;
    }

    /**
     * Search classes by name or description
     */
    public List<GymClass> searchClasses(String searchTerm) {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE LOWER(c.class_name) LIKE ? " +
                "OR LOWER(c.description) LIKE ? " +
                "OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE ? " +
                "ORDER BY c.start_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    classes.add(extractGymClassFromResultSet(rs));
                }
            }

            LOGGER.log(Level.INFO, "Search found {0} classes for term: {1}",
                    new Object[]{classes.size(), searchTerm});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching classes", e);
        }

        return classes;
    }

    /**
     * Get all classes booked by a specific member
     * FIXED: Simplified query without status filter
     */
    public List<GymClass> getBookedClassesByMember(int memberId) {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "JOIN class_bookings cb ON c.id = cb.class_id " +
                "WHERE cb.member_id = ? " +
                "ORDER BY c.start_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, memberId);

            LOGGER.log(Level.INFO, "Executing query to get booked classes for member ID: {0}", memberId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    classes.add(extractGymClassFromResultSet(rs));
                }
            }

            LOGGER.log(Level.INFO, "Retrieved {0} booked classes for member {1}",
                    new Object[]{classes.size(), memberId});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving booked classes for member: " + memberId, e);
        }

        return classes;
    }

    /**
     * Get all available classes (not booked by a specific member)
     * FIXED: Simplified to exclude any booking status
     */
    public List<GymClass> getAvailableClassesForMember(int memberId) {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE c.id NOT IN ( " +
                "    SELECT class_id FROM class_bookings " +
                "    WHERE member_id = ? " +
                ") " +
                "AND c.status = 'SCHEDULED' " +
                "AND c.start_time > NOW() " +
                "ORDER BY c.start_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, memberId);

            LOGGER.log(Level.INFO, "Executing query to get available classes for member ID: {0}", memberId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    classes.add(extractGymClassFromResultSet(rs));
                }
            }

            LOGGER.log(Level.INFO, "Retrieved {0} available classes for member {1}",
                    new Object[]{classes.size(), memberId});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving available classes for member: " + memberId, e);
        }

        return classes;
    }

    /**
     * Update class with validation
     */
    public boolean updateClass(GymClass gymClass) {
        // Validate input
        if (!gymClass.isValid()) {
            LOGGER.warning("Invalid class data provided for update");
            return false;
        }

        // Check for trainer conflicts (excluding this class)
        if (hasTrainerConflict(gymClass.getTrainerId(),
                gymClass.getStartTime(),
                gymClass.getEndTime(),
                gymClass.getId())) {
            LOGGER.warning("Trainer has conflicting schedule");
            return false;
        }

        String sql = "UPDATE classes SET class_name = ?, description = ?, trainer_id = ?, " +
                "start_time = ?, end_time = ?, max_capacity = ?, current_bookings = ?, " +
                "status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gymClass.getClassName());
            stmt.setString(2, gymClass.getDescription());
            stmt.setInt(3, gymClass.getTrainerId());
            stmt.setTimestamp(4, gymClass.getStartTime());
            stmt.setTimestamp(5, gymClass.getEndTime());
            stmt.setInt(6, gymClass.getMaxCapacity());
            stmt.setInt(7, gymClass.getCurrentBookings());
            stmt.setString(8, gymClass.getStatus());
            stmt.setInt(9, gymClass.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Class updated successfully: {0}", gymClass.getClassName());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating class", e);
        }

        return false;
    }

    /**
     * Delete class with cascade handling
     */
    public boolean deleteClass(int classId) {
        String sql = "DELETE FROM classes WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Class deleted: ID {0}", classId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting class: " + classId, e);
        }

        return false;
    }

    /**
     * Check if trainer has conflicting schedule
     */
    public boolean hasTrainerConflict(int trainerId, Timestamp startTime,
                                      Timestamp endTime, Integer excludeClassId) {
        String sql = "SELECT COUNT(*) FROM classes " +
                "WHERE trainer_id = ? " +
                "AND status != 'CANCELLED' " +
                "AND ((start_time < ? AND end_time > ?) " +
                "OR (start_time < ? AND end_time > ?) " +
                "OR (start_time >= ? AND end_time <= ?))";

        if (excludeClassId != null) {
            sql += " AND id != ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainerId);
            stmt.setTimestamp(2, endTime);
            stmt.setTimestamp(3, startTime);
            stmt.setTimestamp(4, endTime);
            stmt.setTimestamp(5, startTime);
            stmt.setTimestamp(6, startTime);
            stmt.setTimestamp(7, endTime);

            if (excludeClassId != null) {
                stmt.setInt(8, excludeClassId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking trainer conflict", e);
        }

        return false;
    }

    /**
     * Check if member has already booked a class
     */
    public boolean isClassBookedByMember(int classId, int memberId) {
        String sql = "SELECT COUNT(*) FROM class_bookings " +
                "WHERE class_id = ? AND member_id = ? AND status = 'CONFIRMED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);
            stmt.setInt(2, memberId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking class booking", e);
        }

        return false;
    }

    /**
     * Book a class for a member with proper validation
     * DIAGNOSTIC VERSION - Shows detailed error information
     */
    public boolean bookClass(int classId, int memberId) {
        String checkClassSql = "SELECT id, class_name, max_capacity, current_bookings, status FROM classes WHERE id = ?";
        String checkBookingSql = "SELECT id FROM class_bookings WHERE class_id = ? AND member_id = ?";
        String insertSql = "INSERT INTO class_bookings (class_id, member_id, booking_date, status) " +
                "VALUES (?, ?, NOW(), 'CONFIRMED')";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();

            if (conn == null) {
                LOGGER.severe("Database connection is NULL!");
                return false;
            }

            LOGGER.log(Level.INFO, "=== BOOKING ATTEMPT START ===");
            LOGGER.log(Level.INFO, "Class ID: {0}, Member ID: {1}", new Object[]{classId, memberId});

            conn.setAutoCommit(false);

            // Check class existence and availability
            int maxCapacity = 0;
            int currentBookings = 0;
            String status = "";
            String className = "";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkClassSql)) {
                checkStmt.setInt(1, classId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        className = rs.getString("class_name");
                        maxCapacity = rs.getInt("max_capacity");
                        currentBookings = rs.getInt("current_bookings");
                        status = rs.getString("status");

                        LOGGER.log(Level.INFO, "Class found: {0}", className);
                        LOGGER.log(Level.INFO, "Max Capacity: {0}, Current Bookings: {1}, Status: {2}",
                                new Object[]{maxCapacity, currentBookings, status});

                        if (currentBookings >= maxCapacity) {
                            LOGGER.warning("Class is FULL - cannot book");
                            conn.rollback();
                            return false;
                        }

                        if ("CANCELLED".equals(status) || "COMPLETED".equals(status)) {
                            LOGGER.warning("Class status is " + status + " - cannot book");
                            conn.rollback();
                            return false;
                        }
                    } else {
                        LOGGER.severe("Class ID " + classId + " NOT FOUND in database!");
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Check for existing booking
            try (PreparedStatement checkBookingStmt = conn.prepareStatement(checkBookingSql)) {
                checkBookingStmt.setInt(1, classId);
                checkBookingStmt.setInt(2, memberId);
                try (ResultSet rs = checkBookingStmt.executeQuery()) {
                    if (rs.next()) {
                        LOGGER.warning("Member already has a booking for this class (ID: " + rs.getInt("id") + ")");
                        conn.rollback();
                        return false;
                    } else {
                        LOGGER.info("No existing booking found - proceeding with insert");
                    }
                }
            }

            // Insert booking
            LOGGER.info("Inserting booking into database...");
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, classId);
                insertStmt.setInt(2, memberId);

                int rowsInserted = insertStmt.executeUpdate();
                LOGGER.log(Level.INFO, "INSERT executed - rows affected: {0}", rowsInserted);

                if (rowsInserted > 0) {
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int bookingId = generatedKeys.getInt(1);
                            LOGGER.log(Level.INFO, "Booking created with ID: {0}", bookingId);
                        }
                    }
                } else {
                    LOGGER.severe("INSERT failed - no rows affected!");
                    conn.rollback();
                    return false;
                }
            }

            // Verify the trigger worked
            try (PreparedStatement verifyStmt = conn.prepareStatement(
                    "SELECT current_bookings FROM classes WHERE id = ?")) {
                verifyStmt.setInt(1, classId);
                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (rs.next()) {
                        int newBookings = rs.getInt("current_bookings");
                        LOGGER.log(Level.INFO, "After insert - current_bookings: {0} (was {1})",
                                new Object[]{newBookings, currentBookings});
                    }
                }
            }

            conn.commit();
            LOGGER.log(Level.INFO, "=== BOOKING SUCCESS - Transaction committed ===");
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "=== SQL EXCEPTION ===");
            LOGGER.log(Level.SEVERE, "Error Code: " + e.getErrorCode());
            LOGGER.log(Level.SEVERE, "SQL State: " + e.getSQLState());
            LOGGER.log(Level.SEVERE, "Message: " + e.getMessage(), e);

            if (conn != null) {
                try {
                    conn.rollback();
                    LOGGER.log(Level.INFO, "Transaction rolled back");
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "=== UNEXPECTED EXCEPTION ===");
            LOGGER.log(Level.SEVERE, "Error: " + e.getMessage(), e);

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                    LOGGER.info("Connection closed");
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }
    }

    /**
     * Cancel a class booking
     * FIXED VERSION - Relies on database trigger for current_bookings decrement
     */
    /**
     * Cancel a class booking
     * FIXED VERSION - Checks for any booking record, not just CONFIRMED status
     */
    public boolean cancelBooking(int classId, int memberId) {
        // First check if booking exists
        String checkSql = "SELECT id, status FROM class_bookings WHERE class_id = ? AND member_id = ?";
        String deleteSql = "DELETE FROM class_bookings WHERE class_id = ? AND member_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();

            if (conn == null) {
                LOGGER.severe("Database connection is NULL!");
                return false;
            }

            conn.setAutoCommit(false);

            LOGGER.log(Level.INFO, "=== CANCEL BOOKING ATTEMPT START ===");
            LOGGER.log(Level.INFO, "Class ID: {0}, Member ID: {1}", new Object[]{classId, memberId});

            // Check if booking exists
            int bookingId = -1;
            String bookingStatus = null;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, classId);
                checkStmt.setInt(2, memberId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        bookingId = rs.getInt("id");
                        try {
                            bookingStatus = rs.getString("status");
                            LOGGER.log(Level.INFO, "Booking found - ID: {0}, Status: {1}",
                                    new Object[]{bookingId, bookingStatus});
                        } catch (SQLException e) {
                            // Status column might not exist
                            LOGGER.info("Status column not found, proceeding without status check");
                        }
                    } else {
                        LOGGER.warning("No booking found for Class ID: " + classId + ", Member ID: " + memberId);
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Delete the booking (trigger will automatically decrement current_bookings)
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, classId);
                deleteStmt.setInt(2, memberId);
                int rowsAffected = deleteStmt.executeUpdate();

                LOGGER.log(Level.INFO, "DELETE executed - rows affected: {0}", rowsAffected);

                if (rowsAffected == 0) {
                    LOGGER.warning("DELETE failed - no rows affected");
                    conn.rollback();
                    return false;
                }
            }

            // Verify deletion
            try (PreparedStatement verifyStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM class_bookings WHERE class_id = ? AND member_id = ?")) {
                verifyStmt.setInt(1, classId);
                verifyStmt.setInt(2, memberId);
                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        LOGGER.log(Level.INFO, "After delete - remaining bookings: {0}", count);
                    }
                }
            }

            // Verify current_bookings was decremented
            try (PreparedStatement verifyStmt = conn.prepareStatement(
                    "SELECT current_bookings FROM classes WHERE id = ?")) {
                verifyStmt.setInt(1, classId);
                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (rs.next()) {
                        int currentBookings = rs.getInt("current_bookings");
                        LOGGER.log(Level.INFO, "After cancel - current_bookings: {0}", currentBookings);
                    }
                }
            }

            conn.commit();
            LOGGER.log(Level.INFO, "=== CANCEL BOOKING SUCCESS - Transaction committed ===");
            LOGGER.log(Level.INFO, "Booking cancelled: Class ID {0}, Member ID {1}",
                    new Object[]{classId, memberId});
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "=== SQL EXCEPTION DURING CANCEL ===");
            LOGGER.log(Level.SEVERE, "Error Code: " + e.getErrorCode());
            LOGGER.log(Level.SEVERE, "SQL State: " + e.getSQLState());
            LOGGER.log(Level.SEVERE, "Message: " + e.getMessage(), e);

            if (conn != null) {
                try {
                    conn.rollback();
                    LOGGER.info("Transaction rolled back");
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "=== UNEXPECTED EXCEPTION DURING CANCEL ===");
            LOGGER.log(Level.SEVERE, "Error: " + e.getMessage(), e);

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                    LOGGER.info("Connection closed");
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }
    }

    /**
     * Get class statistics
     */
    public ClassStats getClassStats() {
        String sql = "SELECT " +
                "COUNT(*) as total_classes, " +
                "COUNT(CASE WHEN status = 'SCHEDULED' THEN 1 END) as scheduled, " +
                "COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed, " +
                "COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled, " +
                "AVG(current_bookings * 100.0 / max_capacity) as avg_occupancy, " +
                "SUM(current_bookings) as total_bookings " +
                "FROM classes";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new ClassStats(
                        rs.getInt("total_classes"),
                        rs.getInt("scheduled"),
                        rs.getInt("completed"),
                        rs.getInt("cancelled"),
                        rs.getDouble("avg_occupancy"),
                        rs.getInt("total_bookings")
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving class stats", e);
        }

        return new ClassStats(0, 0, 0, 0, 0.0, 0);
    }

    /**
     * Get most popular classes
     */
    public List<GymClass> getMostPopularClasses(int limit) {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, u.first_name, u.last_name, u.email, u.phone, " +
                "t.specialization " +
                "FROM classes c " +
                "JOIN trainers t ON c.trainer_id = t.id " +
                "JOIN users u ON t.user_id = u.id " +
                "ORDER BY c.current_bookings DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    classes.add(extractGymClassFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving popular classes", e);
        }

        return classes;
    }

    /**
     * Update class statuses based on current time
     */
    public int updateClassStatuses() {
        String sql = "UPDATE classes SET status = CASE " +
                "WHEN end_time < NOW() AND status != 'CANCELLED' THEN 'COMPLETED' " +
                "WHEN start_time <= NOW() AND end_time > NOW() AND status = 'SCHEDULED' THEN 'IN_PROGRESS' " +
                "WHEN current_bookings >= max_capacity AND status = 'SCHEDULED' THEN 'FULL' " +
                "ELSE status END " +
                "WHERE status IN ('SCHEDULED', 'IN_PROGRESS', 'FULL')";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            int updated = stmt.executeUpdate(sql);
            if (updated > 0) {
                LOGGER.log(Level.INFO, "Updated {0} class statuses", updated);
            }
            return updated;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating class statuses", e);
        }

        return 0;
    }

    /**
     * Helper method to extract GymClass from ResultSet
     */
    private GymClass extractGymClassFromResultSet(ResultSet rs) throws SQLException {
        GymClass gymClass = new GymClass();
        gymClass.setId(rs.getInt("id"));
        gymClass.setClassName(rs.getString("class_name"));
        gymClass.setDescription(rs.getString("description"));
        gymClass.setTrainerId(rs.getInt("trainer_id"));
        gymClass.setStartTime(rs.getTimestamp("start_time"));
        gymClass.setEndTime(rs.getTimestamp("end_time"));
        gymClass.setMaxCapacity(rs.getInt("max_capacity"));
        gymClass.setCurrentBookings(rs.getInt("current_bookings"));
        gymClass.setStatus(rs.getString("status"));

        // Set trainer information
        Trainer trainer = new Trainer();
        trainer.setId(rs.getInt("trainer_id"));

        User user = new User();
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        trainer.setUser(user);

        try {
            trainer.setSpecialization(rs.getString("specialization"));
        } catch (SQLException e) {
            // Specialization might not be in all queries
        }

        gymClass.setTrainer(trainer);
        return gymClass;
    }

    /**
     * Inner class for class statistics
     */
    public static class ClassStats {
        private final int totalClasses;
        private final int scheduledClasses;
        private final int completedClasses;
        private final int cancelledClasses;
        private final double averageOccupancy;
        private final int totalBookings;

        public ClassStats(int totalClasses, int scheduledClasses, int completedClasses,
                          int cancelledClasses, double averageOccupancy, int totalBookings) {
            this.totalClasses = totalClasses;
            this.scheduledClasses = scheduledClasses;
            this.completedClasses = completedClasses;
            this.cancelledClasses = cancelledClasses;
            this.averageOccupancy = averageOccupancy;
            this.totalBookings = totalBookings;
        }

        public int getTotalClasses() { return totalClasses; }
        public int getScheduledClasses() { return scheduledClasses; }
        public int getCompletedClasses() { return completedClasses; }
        public int getCancelledClasses() { return cancelledClasses; }
        public double getAverageOccupancy() { return averageOccupancy; }
        public int getTotalBookings() { return totalBookings; }
    }
}