package com.gymmanagementsystem.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public class ValidationUtil {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{10,15}$"
    );

    // Private constructor to prevent instantiation
    private ValidationUtil() {}

    /**
     * Validate time string (HH:MM format)
     */
    public static boolean isValidTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return false;
        }

        try {
            LocalTime.parse(time.trim(), TIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Parse time string to LocalTime
     */
    public static LocalTime parseTime(String time) throws DateTimeParseException {
        if (time == null || time.trim().isEmpty()) {
            throw new DateTimeParseException("Time string is empty", time, 0);
        }
        return LocalTime.parse(time.trim(), TIME_FORMATTER);
    }

    /**
     * Validate that end time is after start time
     */
    public static boolean isTimeRangeValid(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return false;
        }
        return end.isAfter(start);
    }

    /**
     * Validate that end time is after start time (same day)
     */
    public static boolean isTimeRangeValid(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return false;
        }
        return end.isAfter(start);
    }

    /**
     * Validate capacity (1-100)
     */
    public static boolean isValidCapacity(int capacity) {
        return capacity > 0 && capacity <= 100;
    }

    /**
     * Validate capacity with custom range
     */
    public static boolean isValidCapacity(int capacity, int min, int max) {
        return capacity >= min && capacity <= max;
    }

    /**
     * Validate string is not null or empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Validate string length
     */
    public static boolean isValidLength(String value, int minLength, int maxLength) {
        if (value == null) return false;
        int length = value.trim().length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate phone number
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        String cleanPhone = phone.trim().replaceAll("[\\s()-]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * Validate positive number
     */
    public static boolean isPositiveNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            double num = Double.parseDouble(value.trim());
            return num > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate integer
     */
    public static boolean isValidInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate double
     */
    public static boolean isValidDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate date is not in the past
     */
    public static boolean isNotPastDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(LocalDate.now());
    }

    /**
     * Validate datetime is not in the past
     */
    public static boolean isNotPastDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return !dateTime.isBefore(LocalDateTime.now());
    }

    /**
     * Validate date is within range
     */
    public static boolean isDateInRange(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null || start == null || end == null) {
            return false;
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * Validate password strength
     * Must be at least 8 characters with uppercase, lowercase, and number
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasUpper && hasLower && hasDigit;
    }

    /**
     * Get password strength message
     */
    public static String getPasswordStrengthMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }

        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        if (!hasUpper) return "Password must contain at least one uppercase letter";
        if (!hasLower) return "Password must contain at least one lowercase letter";
        if (!hasDigit) return "Password must contain at least one number";

        if (password.length() >= 12 && hasSpecial) {
            return "Strong password";
        } else if (password.length() >= 10) {
            return "Good password";
        } else {
            return "Weak password - consider adding special characters";
        }
    }

    /**
     * Sanitize string input (remove extra spaces, trim)
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Validate username (alphanumeric with underscore, 3-20 chars)
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String cleaned = username.trim();
        return cleaned.length() >= 3 &&
                cleaned.length() <= 20 &&
                cleaned.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Validate name (letters, spaces, hyphens only)
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String cleaned = name.trim();
        return cleaned.length() >= 2 &&
                cleaned.length() <= 50 &&
                cleaned.matches("^[a-zA-Z\\s-]+$");
    }

    /**
     * Format time for display
     */
    public static String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        return time.format(TIME_FORMATTER);
    }

    /**
     * Format date for display
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    /**
     * Format datetime for display
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }

    /**
     * Calculate duration between two times in minutes
     */
    public static long getDurationMinutes(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return java.time.Duration.between(start, end).toMinutes();
    }

    /**
     * Calculate duration between two datetimes in minutes
     */
    public static long getDurationMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return java.time.Duration.between(start, end).toMinutes();
    }

    /**
     * Format duration in human-readable format
     */
    public static String formatDuration(long minutes) {
        if (minutes < 0) {
            return "Invalid duration";
        }

        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours > 0 && mins > 0) {
            return hours + "h " + mins + "m";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return mins + "m";
        }
    }

    /**
     * Check if two time ranges overlap
     */
    public static boolean doTimeRangesOverlap(LocalDateTime start1, LocalDateTime end1,
                                              LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * Validate number is within range
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Validate number is within range
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Create validation error message
     */
    public static String createErrorMessage(String field, String requirement) {
        return "• " + field + " " + requirement;
    }

    /**
     * Validate all fields are filled
     */
    public static boolean areFieldsFilled(String... fields) {
        for (String field : fields) {
            if (field == null || field.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get current date
     */
    public static LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    /**
     * Get current datetime
     */
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    /**
     * Check if date is today
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(LocalDate.now());
    }

    /**
     * Check if datetime is today
     */
    public static boolean isToday(LocalDateTime dateTime) {
        return dateTime != null && dateTime.toLocalDate().equals(LocalDate.now());
    }

    /**
     * Get days between two dates
     */
    public static long getDaysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(start, end);
    }
}