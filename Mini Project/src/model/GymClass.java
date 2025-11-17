package com.gymmanagementsystem.model;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced GymClass model with validation and utility methods
 */
public class GymClass {
    private int id;
    private String className;
    private String description;
    private int trainerId;
    private Timestamp startTime;
    private Timestamp endTime;
    private int maxCapacity;
    private int currentBookings;
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, FULL
    private Trainer trainer; // For join queries

    // Additional computed fields
    private int availableSpots;
    private double occupancyRate;
    private boolean isFull;

    // Constants
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_FULL = "FULL";

    // Constructors
    public GymClass() {
        this.currentBookings = 0;
        this.status = STATUS_SCHEDULED;
    }

    public GymClass(String className, String description, int trainerId,
                    Timestamp startTime, Timestamp endTime, int maxCapacity) {
        this.className = className != null ? className.trim() : null;
        this.description = description != null ? description.trim() : null;
        this.trainerId = trainerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCapacity = maxCapacity;
        this.currentBookings = 0;
        this.status = STATUS_SCHEDULED;
        updateComputedFields();
    }

    // Business Logic Methods

    /**
     * Check if class has available spots
     */
    public boolean hasAvailableSpots() {
        return currentBookings < maxCapacity && !STATUS_CANCELLED.equals(status);
    }

    /**
     * Get number of available spots
     */
    public int getAvailableSpots() {
        return Math.max(0, maxCapacity - currentBookings);
    }

    /**
     * Calculate occupancy rate as percentage
     */
    public double getOccupancyRate() {
        if (maxCapacity == 0) return 0.0;
        return (currentBookings * 100.0) / maxCapacity;
    }

    /**
     * Check if class is full
     */
    public boolean isFull() {
        return currentBookings >= maxCapacity;
    }

    /**
     * Get class duration in minutes
     */
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        LocalDateTime start = startTime.toLocalDateTime();
        LocalDateTime end = endTime.toLocalDateTime();
        return Duration.between(start, end).toMinutes();
    }

    /**
     * Get formatted duration (e.g., "1h 30m")
     */
    public String getFormattedDuration() {
        long minutes = getDurationMinutes();
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
     * Get formatted date and time
     */
    public String getFormattedStartTime() {
        if (startTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return startTime.toLocalDateTime().format(formatter);
    }

    public String getFormattedEndTime() {
        if (endTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return endTime.toLocalDateTime().format(formatter);
    }

    /**
     * Get just the time portion
     */
    public String getStartTimeOnly() {
        if (startTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return startTime.toLocalDateTime().format(formatter);
    }

    public String getEndTimeOnly() {
        if (endTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return endTime.toLocalDateTime().format(formatter);
    }

    /**
     * Get date portion
     */
    public String getDateOnly() {
        if (startTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return startTime.toLocalDateTime().format(formatter);
    }

    /**
     * Check if class is upcoming
     */
    public boolean isUpcoming() {
        if (startTime == null) return false;
        return startTime.toLocalDateTime().isAfter(LocalDateTime.now())
                && STATUS_SCHEDULED.equals(status);
    }

    /**
     * Check if class is currently in progress
     */
    public boolean isInProgress() {
        if (startTime == null || endTime == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime.toLocalDateTime())
                && now.isBefore(endTime.toLocalDateTime())
                && STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Check if class is completed
     */
    public boolean isCompleted() {
        if (endTime == null) return false;
        return endTime.toLocalDateTime().isBefore(LocalDateTime.now())
                || STATUS_COMPLETED.equals(status);
    }

    /**
     * Check if class is cancelled
     */
    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(status);
    }

    /**
     * Auto-update status based on current time
     */
    public void updateStatus() {
        if (STATUS_CANCELLED.equals(status)) return; // Don't change cancelled status

        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && endTime != null) {
            LocalDateTime start = startTime.toLocalDateTime();
            LocalDateTime end = endTime.toLocalDateTime();

            if (now.isAfter(end)) {
                this.status = STATUS_COMPLETED;
            } else if (now.isAfter(start) && now.isBefore(end)) {
                this.status = STATUS_IN_PROGRESS;
            } else if (isFull()) {
                this.status = STATUS_FULL;
            } else {
                this.status = STATUS_SCHEDULED;
            }
        }
    }

    /**
     * Get status display with color coding
     */
    public String getStatusDisplay() {
        return status != null ? status : STATUS_SCHEDULED;
    }

    /**
     * Get booking summary (e.g., "15/20" or "Full")
     */
    public String getBookingSummary() {
        if (isFull()) {
            return "Full (" + maxCapacity + "/" + maxCapacity + ")";
        }
        return currentBookings + "/" + maxCapacity;
    }

    /**
     * Get capacity percentage for progress bars
     */
    public int getCapacityPercentage() {
        if (maxCapacity == 0) return 0;
        return (int) ((currentBookings * 100.0) / maxCapacity);
    }

    /**
     * Check if bookings can be modified
     */
    public boolean canModifyBookings() {
        return !STATUS_COMPLETED.equals(status)
                && !STATUS_CANCELLED.equals(status)
                && isUpcoming();
    }

    /**
     * Increment bookings (with validation)
     */
    public boolean incrementBookings() {
        if (currentBookings < maxCapacity) {
            currentBookings++;
            updateComputedFields();
            return true;
        }
        return false;
    }

    /**
     * Decrement bookings (with validation)
     */
    public boolean decrementBookings() {
        if (currentBookings > 0) {
            currentBookings--;
            updateComputedFields();
            return true;
        }
        return false;
    }

    /**
     * Update computed fields
     */
    private void updateComputedFields() {
        this.availableSpots = getAvailableSpots();
        this.occupancyRate = getOccupancyRate();
        this.isFull = isFull();
    }

    // Validation Methods

    /**
     * Validate class name
     */
    public boolean hasValidClassName() {
        return className != null && !className.trim().isEmpty()
                && className.length() >= 3 && className.length() <= 100;
    }

    /**
     * Validate time range
     */
    public boolean hasValidTimeRange() {
        if (startTime == null || endTime == null) return false;
        return endTime.after(startTime);
    }

    /**
     * Validate capacity
     */
    public boolean hasValidCapacity() {
        return maxCapacity > 0 && maxCapacity <= 100;
    }

    /**
     * Validate trainer assignment
     */
    public boolean hasValidTrainer() {
        return trainerId > 0;
    }

    /**
     * Check if class data is complete and valid
     */
    public boolean isValid() {
        return hasValidClassName()
                && hasValidTimeRange()
                && hasValidCapacity()
                && hasValidTrainer();
    }

    /**
     * Check if class overlaps with another class
     */
    public boolean overlapsWith(GymClass other) {
        if (this.startTime == null || this.endTime == null
                || other.startTime == null || other.endTime == null) {
            return false;
        }

        return this.startTime.before(other.endTime)
                && this.endTime.after(other.startTime);
    }

    /**
     * Check if same trainer
     */
    public boolean hasSameTrainer(GymClass other) {
        return this.trainerId == other.trainerId;
    }

    /**
     * Get trainer name (if trainer object is loaded)
     */
    public String getTrainerName() {
        if (trainer != null && trainer.getUser() != null) {
            return trainer.getUser().getFullName();
        }
        return "Unknown";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getClassName() { return className; }
    public void setClassName(String className) {
        this.className = className != null ? className.trim() : null;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public int getTrainerId() { return trainerId; }
    public void setTrainerId(int trainerId) { this.trainerId = trainerId; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
        updateComputedFields();
    }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
        updateComputedFields();
    }

    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        updateComputedFields();
    }

    public int getCurrentBookings() { return currentBookings; }
    public void setCurrentBookings(int currentBookings) {
        this.currentBookings = currentBookings;
        updateComputedFields();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Trainer getTrainer() { return trainer; }
    public void setTrainer(Trainer trainer) { this.trainer = trainer; }

    @Override
    public String toString() {
        return "GymClass{" +
                "id=" + id +
                ", className='" + className + '\'' +
                ", trainer=" + getTrainerName() +
                ", startTime=" + getFormattedStartTime() +
                ", duration=" + getFormattedDuration() +
                ", bookings=" + getBookingSummary() +
                ", status='" + status + '\'' +
                '}';
    }
}