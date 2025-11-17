package com.gymmanagementsystem.model;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Enhanced Member model with validation and utility methods
 */
public class Member {
    private int id;
    private int userId;
    private String emergencyContact;
    private String medicalConditions;
    private int membershipPlanId;
    private Date membershipStartDate;
    private Date membershipEndDate;
    private String membershipStatus; // ACTIVE, EXPIRED, SUSPENDED
    private User user; // For join queries

    // Additional computed fields
    private String membershipPlanName;
    private double membershipPlanPrice;

    // Constructors
    public Member() {}

    public Member(int userId, String emergencyContact, String medicalConditions,
                  int membershipPlanId, Date membershipStartDate, Date membershipEndDate) {
        this.userId = userId;
        this.emergencyContact = emergencyContact;
        this.medicalConditions = medicalConditions;
        this.membershipPlanId = membershipPlanId;
        this.membershipStartDate = membershipStartDate;
        this.membershipEndDate = membershipEndDate;
        this.membershipStatus = determineStatus();
    }

    // Business Logic Methods

    /**
     * Automatically determine membership status based on end date
     */
    public String determineStatus() {
        if (membershipEndDate == null) {
            return "ACTIVE";
        }

        LocalDate endDate = membershipEndDate.toLocalDate();
        LocalDate today = LocalDate.now();

        if (endDate.isBefore(today)) {
            return "EXPIRED";
        } else if (endDate.isEqual(today) || endDate.isBefore(today.plusDays(7))) {
            return "EXPIRING_SOON";
        }

        return "ACTIVE";
    }

    /**
     * Get days remaining until membership expires
     */
    public long getDaysRemaining() {
        if (membershipEndDate == null) {
            return -1;
        }

        LocalDate endDate = membershipEndDate.toLocalDate();
        LocalDate today = LocalDate.now();

        return ChronoUnit.DAYS.between(today, endDate);
    }

    /**
     * Check if membership is active and valid
     */
    public boolean isActiveMembership() {
        return "ACTIVE".equals(membershipStatus) && getDaysRemaining() > 0;
    }

    /**
     * Check if membership is expiring soon (within 7 days)
     */
    public boolean isExpiringSoon() {
        long daysRemaining = getDaysRemaining();
        return daysRemaining >= 0 && daysRemaining <= 7;
    }

    /**
     * Get membership duration in months
     */
    public long getMembershipDurationMonths() {
        if (membershipStartDate == null || membershipEndDate == null) {
            return 0;
        }

        LocalDate startDate = membershipStartDate.toLocalDate();
        LocalDate endDate = membershipEndDate.toLocalDate();

        return ChronoUnit.MONTHS.between(startDate, endDate);
    }

    // Validation Methods

    /**
     * Validate emergency contact format (basic phone number validation)
     */
    public boolean isValidEmergencyContact() {
        if (emergencyContact == null || emergencyContact.trim().isEmpty()) {
            return false;
        }
        // Basic validation: contains digits and optional dashes/spaces
        return emergencyContact.matches("[0-9\\s\\-+()]{7,20}");
    }

    /**
     * Validate membership dates
     */
    public boolean hasValidMembershipDates() {
        if (membershipStartDate == null || membershipEndDate == null) {
            return false;
        }
        return membershipEndDate.after(membershipStartDate);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact != null ? emergencyContact.trim() : null;
    }

    public String getMedicalConditions() { return medicalConditions; }
    public void setMedicalConditions(String medicalConditions) {
        this.medicalConditions = medicalConditions != null ? medicalConditions.trim() : null;
    }

    public int getMembershipPlanId() { return membershipPlanId; }
    public void setMembershipPlanId(int membershipPlanId) { this.membershipPlanId = membershipPlanId; }

    public Date getMembershipStartDate() { return membershipStartDate; }
    public void setMembershipStartDate(Date membershipStartDate) {
        this.membershipStartDate = membershipStartDate;
    }

    public Date getMembershipEndDate() { return membershipEndDate; }
    public void setMembershipEndDate(Date membershipEndDate) {
        this.membershipEndDate = membershipEndDate;
        // Auto-update status when end date changes
        if (membershipStatus != null && !membershipStatus.equals("SUSPENDED")) {
            this.membershipStatus = determineStatus();
        }
    }

    public String getMembershipStatus() { return membershipStatus; }
    public void setMembershipStatus(String membershipStatus) { this.membershipStatus = membershipStatus; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMembershipPlanName() { return membershipPlanName; }
    public void setMembershipPlanName(String membershipPlanName) { this.membershipPlanName = membershipPlanName; }

    public double getMembershipPlanPrice() { return membershipPlanPrice; }
    public void setMembershipPlanPrice(double membershipPlanPrice) { this.membershipPlanPrice = membershipPlanPrice; }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", userId=" + userId +
                ", membershipStatus='" + membershipStatus + '\'' +
                ", daysRemaining=" + getDaysRemaining() +
                '}';
    }
}