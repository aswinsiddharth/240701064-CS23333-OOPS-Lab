package com.gymmanagementsystem;

import com.gymmanagementsystem.dao.UserDAO;
import com.gymmanagementsystem.model.User;
import com.gymmanagementsystem.util.DatabaseConnection;

public class DatabaseTest {
    public static void main(String[] args) {
        System.out.println("=== Database Connection Test ===");
        
        if (DatabaseConnection.isPoolInitialized()) {
            System.out.println("✓ Database connection pool initialized successfully!");
        } else {
            System.out.println("✗ Failed to initialize database connection pool");
            return;
        }
        
        System.out.println("\n=== Testing User Authentication ===");
        UserDAO userDAO = new UserDAO();
        
        User adminUser = userDAO.authenticate("admin", "admin123");
        if (adminUser != null) {
            System.out.println("✓ Successfully authenticated admin user!");
            System.out.println("  Username: " + adminUser.getUsername());
            System.out.println("  Email: " + adminUser.getEmail());
            System.out.println("  Role: " + adminUser.getRole());
            System.out.println("  Name: " + adminUser.getFirstName() + " " + adminUser.getLastName());
        } else {
            System.out.println("✗ Failed to authenticate admin user");
        }
        
        System.out.println("\n=== Testing User Creation ===");
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setPassword("testpass123");
        newUser.setEmail("test@example.com");
        newUser.setRole("MEMBER");
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        newUser.setPhone("555-1234");
        
        boolean created = userDAO.createUser(newUser);
        if (created) {
            System.out.println("✓ Successfully created new user!");
            System.out.println("  User ID: " + newUser.getId());
            System.out.println("  Username: " + newUser.getUsername());
        } else {
            System.out.println("✗ Failed to create new user");
        }
        
        System.out.println("\n=== Testing Multiple Operations (Connection Pool) ===");
        boolean success = true;
        for (int i = 0; i < 5; i++) {
            User user = userDAO.authenticate("admin", "admin123");
            if (user == null) {
                System.out.println("✗ Operation " + (i + 1) + " failed");
                success = false;
                break;
            }
            System.out.println("✓ Operation " + (i + 1) + " successful");
        }
        
        if (success) {
            System.out.println("\n✓ Connection pool is working correctly!");
            System.out.println("  Multiple operations completed without 'connection closed' errors");
        }
        
        System.out.println("\n=== Test Complete ===");
        DatabaseConnection.closeDataSource();
    }
}
