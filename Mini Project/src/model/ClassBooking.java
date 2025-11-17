package com.gymmanagementsystem.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model class for class bookings
 */
public class ClassBooking {
    private int id;
    private int classId;
    private int memberId;
    private Timestamp bookingDate;
    private String status; // CONFIRMED, CANCELLED, ATTENDED, NO_SHOW

    // Related objects for joins
    private GymClass gymClass;
    private Member member;

    // Constants
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_ATTENDED = "ATTENDED";
    public static final String STATUS_NO_SHOW = "NO_SHOW";

    // Constructors
    public ClassBooking() {
        this.bookingDate = new Timestamp(System.currentTimeMillis());
        this.status = STATUS_CONFIRMED;
    }

    public ClassBooking(int classId, int memberId) {
        this();
        this.classId = classId;
        this.memberId = memberId;
    }

    public ClassBooking(int classId, int memberId, String status) {
        this(classId, memberId);
        this.status = status;
    }

    // Business Logic Methods

    /**
     * Check if booking is confirmed
     */
    public boolean isConfirmed() {
        return STATUS_CONFIRMED.equals(status);
    }

    /**
     * Check if booking is cancelled
     */
    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(status);
    }

    /**
     * Check if member attended
     */
    public boolean isAttended() {
        return STATUS_ATTENDED.equals(status);
    }

    /**
     * Check if member was a no-show
     */
    public boolean isNoShow() {
        return STATUS_NO_SHOW.equals(status);
    }

    /**
     * Check if booking can be cancelled
     */
    public boolean canBeCancelled() {
        if (isCancelled() || isAttended() || isNoShow()) {
            return false;
        }

        // Can't cancel if class has already started
        if (gymClass != null && gymClass.getStartTime() != null) {
            return gymClass.getStartTime().toLocalDateTime().isAfter(LocalDateTime.now());
        }

        return true;
    }

    /**
     * Get formatted booking date
     */
    public String getFormattedBookingDate() {
        if (bookingDate == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return bookingDate.toLocalDateTime().format(formatter);
    }

    /**
     * Get booking date only
     */
    public String getBookingDateOnly() {
        if (bookingDate == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return bookingDate.toLocalDateTime().format(formatter);
    }

    /**
     * Get member name (if member object is loaded)
     */
    public String getMemberName() {
        if (member != null && member.getUser() != null) {
            return member.getUser().getFullName();
        }
        return "Unknown Member";
    }

    /**
     * Get class name (if gymClass object is loaded)
     */
    public String getClassName() {
        if (gymClass != null) {
            return gymClass.getClassName();
        }
        return "Unknown Class";
    }

    /**
     * Get status display with icon
     */
    public String getStatusDisplay() {
        switch (status) {
            case STATUS_CONFIRMED:
                return "✓ Confirmed";
            case STATUS_CANCELLED:
                return "✗ Cancelled";
            case STATUS_ATTENDED:
                return "★ Attended";
            case STATUS_NO_SHOW:
                return "⚠ No Show";
            default:
                return status;
        }
    }

    /**
     * Calculate hours until class starts
     */
    public long getHoursUntilClass() {
        if (gymClass == null || gymClass.getStartTime() == null) {
            return -1;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime classStart = gymClass.getStartTime().toLocalDateTime();

        if (classStart.isBefore(now)) {
            return -1; // Class has started or passed
        }

        return java.time.Duration.between(now, classStart).toHours();
    }

    /**
     * Check if within cancellation window (e.g., 24 hours before class)
     */
    public boolean isWithinCancellationWindow(int hoursBeforeClass) {
        long hoursUntil = getHoursUntilClass();
        return hoursUntil >= 0 && hoursUntil >= hoursBeforeClass;
    }

    /**
     * Confirm the booking
     */
    public void confirm() {
        this.status = STATUS_CONFIRMED;
    }

    /**
     * Cancel the booking
     */
    public void cancel() {
        if (canBeCancelled()) {
            this.status = STATUS_CANCELLED;
        }
    }

    /**
     * Mark as attended
     */
    public void markAttended() {
        if (isConfirmed()) {
            this.status = STATUS_ATTENDED;
        }
    }

    /**
     * Mark as no-show
     */
    public void markNoShow() {
        if (isConfirmed()) {
            this.status = STATUS_NO_SHOW;
        }
    }

    // Validation Methods

    /**
     * Validate booking data
     */
    public boolean isValid() {
        return classId > 0 && memberId > 0 && bookingDate != null && status != null;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public Timestamp getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(Timestamp bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public GymClass getGymClass() {
        return gymClass;
    }

    public void setGymClass(GymClass gymClass) {
        this.gymClass = gymClass;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    @Override
    public String toString() {
        return "ClassBooking{" +
                "id=" + id +
                ", class='" + getClassName() + '\'' +
                ", member='" + getMemberName() + '\'' +
                ", bookingDate=" + getFormattedBookingDate() +
                ", status='" + status + '\'' +
                '}';
    }
}