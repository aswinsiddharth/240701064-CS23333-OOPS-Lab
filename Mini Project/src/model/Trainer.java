package com.gymmanagementsystem.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enhanced Trainer model with validation and utility methods
 */
public class Trainer {
    private int id;
    private int userId;
    private String specialization;
    private String certifications;
    private BigDecimal hourlyRate;
    private String availability;
    private User user; // For join queries

    // Additional computed fields
    private int totalClasses; // Total classes taught
    private double averageRating; // Average rating from members
    private boolean isActive; // Active status

    // Constructors
    public Trainer() {}

    public Trainer(int userId, String specialization, String certifications,
                   BigDecimal hourlyRate, String availability) {
        this.userId = userId;
        this.specialization = specialization != null ? specialization.trim() : null;
        this.certifications = certifications != null ? certifications.trim() : null;
        this.hourlyRate = hourlyRate;
        this.availability = availability != null ? availability.trim() : null;
        this.isActive = true;
    }

    // Business Logic Methods

    /**
     * Get list of specializations (split by comma)
     */
    public List<String> getSpecializationList() {
        if (specialization == null || specialization.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(specialization.split(",\\s*"));
    }

    /**
     * Get list of certifications (split by comma or newline)
     */
    public List<String> getCertificationsList() {
        if (certifications == null || certifications.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(certifications.split("[,\\n]\\s*"));
    }

    /**
     * Check if trainer has a specific specialization
     */
    public boolean hasSpecialization(String spec) {
        if (specialization == null || spec == null) {
            return false;
        }
        return getSpecializationList().stream()
                .anyMatch(s -> s.equalsIgnoreCase(spec.trim()));
    }

    /**
     * Check if trainer has a specific certification
     */
    public boolean hasCertification(String cert) {
        if (certifications == null || cert == null) {
            return false;
        }
        return getCertificationsList().stream()
                .anyMatch(c -> c.toLowerCase().contains(cert.toLowerCase()));
    }

    /**
     * Calculate monthly earning potential (assuming 40 hours/week)
     */
    public BigDecimal getMonthlyEarningPotential() {
        if (hourlyRate == null) {
            return BigDecimal.ZERO;
        }
        // 40 hours/week * 4 weeks = 160 hours/month
        return hourlyRate.multiply(new BigDecimal("160"));
    }

    /**
     * Calculate annual earning potential
     */
    public BigDecimal getAnnualEarningPotential() {
        return getMonthlyEarningPotential().multiply(new BigDecimal("12"));
    }

    /**
     * Get experience level based on certifications count
     */
    public String getExperienceLevel() {
        int certCount = getCertificationsList().size();

        if (certCount == 0) {
            return "Entry Level";
        } else if (certCount <= 2) {
            return "Intermediate";
        } else if (certCount <= 4) {
            return "Advanced";
        } else {
            return "Expert";
        }
    }

    /**
     * Check if hourly rate is competitive (above minimum)
     */
    public boolean isCompetitiveRate() {
        if (hourlyRate == null) {
            return false;
        }
        // Minimum competitive rate: $25/hour
        return hourlyRate.compareTo(new BigDecimal("25.00")) >= 0;
    }

    /**
     * Get rating display (stars)
     */
    public String getRatingDisplay() {
        if (averageRating == 0) {
            return "No ratings yet";
        }
        int fullStars = (int) averageRating;
        boolean hasHalfStar = (averageRating - fullStars) >= 0.5;

        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < fullStars; i++) {
            stars.append("⭐");
        }
        if (hasHalfStar) {
            stars.append("✨");
        }
        return stars.toString() + " (" + String.format("%.1f", averageRating) + ")";
    }

    // Validation Methods

    /**
     * Validate hourly rate
     */
    public boolean isValidHourlyRate() {
        if (hourlyRate == null) {
            return false;
        }
        // Rate must be between $10 and $200 per hour
        return hourlyRate.compareTo(new BigDecimal("10.00")) >= 0 &&
                hourlyRate.compareTo(new BigDecimal("200.00")) <= 0;
    }

    /**
     * Validate specialization
     */
    public boolean hasValidSpecialization() {
        return specialization != null && !specialization.trim().isEmpty();
    }

    /**
     * Check if trainer profile is complete
     */
    public boolean isProfileComplete() {
        return hasValidSpecialization() &&
                isValidHourlyRate() &&
                certifications != null && !certifications.trim().isEmpty() &&
                availability != null && !availability.trim().isEmpty();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) {
        this.specialization = specialization != null ? specialization.trim() : null;
    }

    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) {
        this.certifications = certifications != null ? certifications.trim() : null;
    }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) {
        this.availability = availability != null ? availability.trim() : null;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "Trainer{" +
                "id=" + id +
                ", userId=" + userId +
                ", specialization='" + specialization + '\'' +
                ", hourlyRate=" + hourlyRate +
                ", experienceLevel='" + getExperienceLevel() + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}