package com.gymmanagementsystem.dao;

import com.gymmanagementsystem.model.User;
import com.gymmanagementsystem.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setPhone(rs.getString("phone"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                return user;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error authenticating user", e);
        }

        return null;
    }

    public boolean createUser(User user) {
        String sql = "INSERT INTO users (username, password, email, role, first_name, last_name, phone) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getRole());
            stmt.setString(5, user.getFirstName());
            stmt.setString(6, user.getLastName());
            stmt.setString(7, user.getPhone());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
                LOGGER.log(Level.INFO, "User created: {0}", user.getUsername());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating user", e);
        }

        return false;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setPhone(rs.getString("phone"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                users.add(user);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all users", e);
        }

        return users;
    }

    /**
     * FIXED: Get user by ID
     */
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password")); // Include password
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setPhone(rs.getString("phone"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                return user;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting user by ID: " + userId, e);
        }

        return null;
    }

    /**
     * FIXED: Update user with optional password update
     * If user.getPassword() is null or empty, password won't be updated
     */
    public boolean updateUser(User user) {
        if (user == null || user.getId() == 0) {
            LOGGER.warning("Invalid user data for update");
            return false;
        }

        LOGGER.log(Level.INFO, "=== UPDATE USER START ===");
        LOGGER.log(Level.INFO, "User ID: {0}, Username: {1}",
                new Object[]{user.getId(), user.getUsername()});

        // Check if password should be updated
        boolean updatePassword = user.getPassword() != null && !user.getPassword().trim().isEmpty();

        String sql;
        if (updatePassword) {
            LOGGER.info("Updating user WITH password");
            sql = "UPDATE users SET username = ?, password = ?, email = ?, role = ?, " +
                    "first_name = ?, last_name = ?, phone = ? WHERE id = ?";
        } else {
            LOGGER.info("Updating user WITHOUT password");
            sql = "UPDATE users SET username = ?, email = ?, role = ?, first_name = ?, " +
                    "last_name = ?, phone = ? WHERE id = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            stmt.setString(paramIndex++, user.getUsername());

            if (updatePassword) {
                stmt.setString(paramIndex++, user.getPassword());
            }

            stmt.setString(paramIndex++, user.getEmail());
            stmt.setString(paramIndex++, user.getRole());
            stmt.setString(paramIndex++, user.getFirstName());
            stmt.setString(paramIndex++, user.getLastName());
            stmt.setString(paramIndex++, user.getPhone());
            stmt.setInt(paramIndex++, user.getId());

            int rowsAffected = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Rows affected: {0}", rowsAffected);

            if (rowsAffected > 0) {
                LOGGER.info("=== UPDATE USER SUCCESS ===");
                return true;
            } else {
                LOGGER.warning("No rows affected - user may not exist");
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "=== SQL EXCEPTION ===");
            LOGGER.log(Level.SEVERE, "Error Code: " + e.getErrorCode());
            LOGGER.log(Level.SEVERE, "SQL State: " + e.getSQLState());
            LOGGER.log(Level.SEVERE, "Error updating user", e);
        }

        return false;
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "User deleted: ID {0}", userId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting user: " + userId, e);
        }

        return false;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking username existence", e);
        }

        return false;
    }

    /**
     * NEW: Check if email exists (excluding specific user ID)
     */
    public boolean emailExists(String email, Integer excludeUserId) {
        String sql = "SELECT id FROM users WHERE email = ?";
        if (excludeUserId != null) {
            sql += " AND id != ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            if (excludeUserId != null) {
                stmt.setInt(2, excludeUserId);
            }

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking email existence", e);
        }

        return false;
    }
}