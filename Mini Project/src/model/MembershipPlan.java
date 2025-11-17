package com.gymmanagementsystem.model;

import java.math.BigDecimal;

public class MembershipPlan {
    private int id;
    private String planName;
    private String description;
    private BigDecimal price;
    private int durationInMonths;
    private boolean isActive;

    // Constructors
    public MembershipPlan() {}

    public MembershipPlan(String planName, String description, BigDecimal price, int durationInMonths) {
        this.planName = planName;
        this.description = description;
        this.price = price;
        this.durationInMonths = durationInMonths;
        this.isActive = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getDurationInMonths() { return durationInMonths; }
    public void setDurationInMonths(int durationInMonths) { this.durationInMonths = durationInMonths; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
