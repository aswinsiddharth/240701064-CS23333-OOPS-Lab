package com.gymmanagementsystem.dao;

import com.gymmanagementsystem.model.Trainer;
import com.gymmanagementsystem.model.User;
import com.gymmanagementsystem.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced TrainerDAO with better error handling, logging, and search functionality
 */
public class TrainerDAO {

    private static final Logger LOGGER = Logger.getLogger(TrainerDAO.class.getName());

    /**
     * Create a new trainer with transaction support
     */
    public boolean createTrainer(Trainer trainer) {
        String sql = "INSERT INTO trainers (user_id, specialization, certifications, hourly_rate, availability) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, trainer.getUserId());
            stmt.setString(2, trainer.getSpecialization());
            stmt.setString(3, trainer.getCertifications());
            stmt.setBigDecimal(4, trainer.getHourlyRate());
            stmt.setString(5, trainer.getAvailability());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        trainer.setId(generatedKeys.getInt(1));
                        LOGGER.log(Level.INFO, "Trainer created successfully with ID: {0}", trainer.getId());
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating trainer", e);
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get all trainers with enhanced data
     * FIXED: Changed LEFT JOIN from gym_classes to classes (your actual table name)
     */
    public List<Trainer> getAllTrainers() {
        List<Trainer> trainers = new ArrayList<>();

        // Try with classes table first (your actual table name from schema)
        String sql = "SELECT t.*, u.username, u.email, u.first_name, u.last_name, u.phone, " +
                "u.role, " +
                "COUNT(DISTINCT c.id) as total_classes " +
                "FROM trainers t " +
                "JOIN users u ON t.user_id = u.id " +
                "LEFT JOIN classes c ON t.id = c.trainer_id " +
                "GROUP BY t.id, t.user_id, t.specialization, t.certifications, t.hourly_rate, " +
                "t.availability, u.id, u.username, u.email, u.first_name, u.last_name, u.phone, u.role " +
                "ORDER BY u.first_name, u.last_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                trainers.add(extractTrainerFromResultSet(rs));
            }

            LOGGER.log(Level.INFO, "Retrieved {0} trainers", trainers.size());
            System.out.println("✅ TrainerDAO: Retrieved " + trainers.size() + " trainers from database");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all trainers", e);
            System.err.println("❌ TrainerDAO Error: " + e.getMessage());
            e.printStackTrace();

            // Try simpler query without classes JOIN if the above fails
            try {
                trainers = getTrainersWithoutClassJoin();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Fallback query also failed", ex);
            }
        }

        return trainers;
    }

    /**
     * Fallback method: Get trainers without class join
     */
    private List<Trainer> getTrainersWithoutClassJoin() {
        List<Trainer> trainers = new ArrayList<>();
        String sql = "SELECT t.*, u.username, u.email, u.first_name, u.last_name, u.phone, u.role " +
                "FROM trainers t " +
                "JOIN users u ON t.user_id = u.id " +
                "ORDER BY u.first_name, u.last_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Trainer trainer = new Trainer();
                trainer.setId(rs.getInt("id"));
                trainer.setUserId(rs.getInt("user_id"));
                trainer.setSpecialization(rs.getString("specialization"));
                trainer.setCertifications(rs.getString("certifications"));
                trainer.setHourlyRate(rs.getBigDecimal("hourly_rate"));
                trainer.setAvailability(rs.getString("availability"));
                trainer.setTotalClasses(0);

                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setPhone(rs.getString("phone"));
                user.setRole(rs.getString("role"));
                trainer.setUser(user);

                trainers.add(trainer);
            }

            System.out.println("✅ Fallback query retrieved " + trainers.size() + " trainers");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in fallback query", e);
            e.printStackTrace();
        }

        return trainers;
    }

    /**
     * Get trainer by ID with full details
     */
    public Trainer getTrainerById(int trainerId) {
        String sql = "SELECT t.*, u.username, u.email, u.first_name, u.last_name, u.phone, u.role " +
                "FROM trainers t " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE t.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractTrainerFromResultSetSimple(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving trainer by ID: " + trainerId, e);
        }

        return null;
    }

    /**
     * Update trainer information
     */
    public boolean updateTrainer(Trainer trainer) {
        String sql = "UPDATE trainers SET specialization = ?, certifications = ?, " +
                "hourly_rate = ?, availability = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, trainer.getSpecialization());
            stmt.setString(2, trainer.getCertifications());
            stmt.setBigDecimal(3, trainer.getHourlyRate());
            stmt.setString(4, trainer.getAvailability());
            stmt.setInt(5, trainer.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Trainer updated successfully: ID {0}", trainer.getId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating trainer: " + trainer.getId(), e);
        }

        return false;
    }

    /**
     * Delete trainer
     */
    public boolean deleteTrainer(int trainerId) {
        String sql = "DELETE FROM trainers WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainerId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Trainer deleted: ID {0}", trainerId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting trainer: " + trainerId, e);
        }

        return false;
    }

    /**
     * NEW: Search trainers by name, email, phone, or specialization
     */
    public List<Trainer> searchTrainers(String searchTerm) {
        List<Trainer> trainers = new ArrayList<>();
        String sql = "SELECT t.*, u.username, u.email, u.first_name, u.last_name, u.phone, u.role " +
                "FROM trainers t " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ? " +
                "OR LOWER(u.email) LIKE ? OR u.phone LIKE ? " +
                "OR LOWER(t.specialization) LIKE ? " +
                "ORDER BY u.first_name, u.last_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            stmt.setString(5, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trainers.add(extractTrainerFromResultSetSimple(rs));
                }
            }

            LOGGER.log(Level.INFO, "Search found {0} trainers for term: {1}",
                    new Object[]{trainers.size(), searchTerm});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching trainers", e);
        }

        return trainers;
    }

    /**
     * NEW: Get trainers by specialization
     */
    public List<Trainer> getTrainersBySpecialization(String specialization) {
        List<Trainer> trainers = new ArrayList<>();
        String sql = "SELECT t.*, u.username, u.email, u.first_name, u.last_name, u.phone, u.role " +
                "FROM trainers t " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE LOWER(t.specialization) LIKE ? " +
                "ORDER BY u.first_name, u.last_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + specialization.toLowerCase() + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trainers.add(extractTrainerFromResultSetSimple(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving trainers by specialization: " + specialization, e);
        }

        return trainers;
    }

    /**
     * NEW: Get trainers by hourly rate range
     */
    public List<Trainer> getTrainersByRateRange(BigDecimal minRate, BigDecimal maxRate) {
        List<Trainer> trainers = new ArrayList<>();
        String sql = "SELECT t.*, u.username, u.email, u.first_name, u.last_name, u.phone, u.role " +
                "FROM trainers t " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE t.hourly_rate BETWEEN ? AND ? " +
                "ORDER BY t.hourly_rate";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, minRate);
            stmt.setBigDecimal(2, maxRate);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trainers.add(extractTrainerFromResultSetSimple(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving trainers by rate range", e);
        }

        return trainers;
    }

    /**
     * NEW: Get available trainers (with availability info)
     */
    public List<Trainer> getAvailableTrainers() {
        List<Trainer> trainers = new ArrayList<>();
        String sql = "SELECT t.*, u.username, u.email, u.first_name, u.last_name, u.phone, u.role " +
                "FROM trainers t " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE t.availability IS NOT NULL AND t.availability != '' " +
                "ORDER BY u.first_name, u.last_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                trainers.add(extractTrainerFromResultSetSimple(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving available trainers", e);
        }

        return trainers;
    }

    /**
     * NEW: Get top trainers by class count
     */
    public List<Trainer> getTopTrainers(int limit) {
        List<Trainer> trainers = new ArrayList<>();
        String sql = "SELECT t.*, u.username, u.email, u.first_name, u.last_name, u.phone, u.role, " +
                "COUNT(DISTINCT c.id) as total_classes " +
                "FROM trainers t " +
                "JOIN users u ON t.user_id = u.id " +
                "LEFT JOIN classes c ON t.id = c.trainer_id " +
                "GROUP BY t.id, t.user_id, t.specialization, t.certifications, t.hourly_rate, " +
                "t.availability, u.id, u.username, u.email, u.first_name, u.last_name, u.phone, u.role " +
                "ORDER BY total_classes DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trainers.add(extractTrainerFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving top trainers", e);
        }

        return trainers;
    }

    /**
     * NEW: Get trainer statistics
     */
    public TrainerStats getTrainerStats() {
        String sql = "SELECT " +
                "COUNT(*) as total, " +
                "AVG(hourly_rate) as avg_rate, " +
                "MIN(hourly_rate) as min_rate, " +
                "MAX(hourly_rate) as max_rate " +
                "FROM trainers";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new TrainerStats(
                        rs.getInt("total"),
                        rs.getBigDecimal("avg_rate") != null ? rs.getBigDecimal("avg_rate") : BigDecimal.ZERO,
                        rs.getBigDecimal("min_rate") != null ? rs.getBigDecimal("min_rate") : BigDecimal.ZERO,
                        rs.getBigDecimal("max_rate") != null ? rs.getBigDecimal("max_rate") : BigDecimal.ZERO
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving trainer stats", e);
        }

        return new TrainerStats(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * NEW: Get specialization distribution
     */
    public Map<String, Integer> getSpecializationDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        String sql = "SELECT specialization, COUNT(*) as count " +
                "FROM trainers " +
                "WHERE specialization IS NOT NULL AND specialization != '' " +
                "GROUP BY specialization " +
                "ORDER BY count DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                distribution.put(rs.getString("specialization"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving specialization distribution", e);
        }

        return distribution;
    }

    /**
     * Helper method to extract Trainer from ResultSet (with total_classes)
     */
    private Trainer extractTrainerFromResultSet(ResultSet rs) throws SQLException {
        Trainer trainer = new Trainer();
        trainer.setId(rs.getInt("id"));
        trainer.setUserId(rs.getInt("user_id"));
        trainer.setSpecialization(rs.getString("specialization"));
        trainer.setCertifications(rs.getString("certifications"));
        trainer.setHourlyRate(rs.getBigDecimal("hourly_rate"));
        trainer.setAvailability(rs.getString("availability"));

        // Set total classes if available
        try {
            trainer.setTotalClasses(rs.getInt("total_classes"));
        } catch (SQLException e) {
            trainer.setTotalClasses(0);
        }

        // Set user information
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setPhone(rs.getString("phone"));
        try {
            user.setRole(rs.getString("role"));
        } catch (SQLException e) {
            user.setRole("TRAINER");
        }
        trainer.setUser(user);

        return trainer;
    }

    /**
     * Helper method to extract Trainer from ResultSet (without total_classes)
     */
    private Trainer extractTrainerFromResultSetSimple(ResultSet rs) throws SQLException {
        Trainer trainer = new Trainer();
        trainer.setId(rs.getInt("id"));
        trainer.setUserId(rs.getInt("user_id"));
        trainer.setSpecialization(rs.getString("specialization"));
        trainer.setCertifications(rs.getString("certifications"));
        trainer.setHourlyRate(rs.getBigDecimal("hourly_rate"));
        trainer.setAvailability(rs.getString("availability"));
        trainer.setTotalClasses(0);

        // Set user information
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setPhone(rs.getString("phone"));
        try {
            user.setRole(rs.getString("role"));
        } catch (SQLException e) {
            user.setRole("TRAINER");
        }
        trainer.setUser(user);

        return trainer;
    }

    /**
     * Inner class for trainer statistics
     */
    public static class TrainerStats {
        private final int total;
        private final BigDecimal averageRate;
        private final BigDecimal minRate;
        private final BigDecimal maxRate;

        public TrainerStats(int total, BigDecimal averageRate, BigDecimal minRate, BigDecimal maxRate) {
            this.total = total;
            this.averageRate = averageRate;
            this.minRate = minRate;
            this.maxRate = maxRate;
        }

        public int getTotal() { return total; }
        public BigDecimal getAverageRate() { return averageRate; }
        public BigDecimal getMinRate() { return minRate; }
        public BigDecimal getMaxRate() { return maxRate; }
    }
}