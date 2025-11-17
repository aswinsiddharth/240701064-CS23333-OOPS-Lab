package com.gymmanagementsystem.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Payment {
    private int id;
    private int memberId;
    private String transactionId; // Unique transaction reference
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal finalAmount;
    private String paymentMethod; // CASH, CARD, ONLINE, UPI, WALLET
    private String paymentType; // MEMBERSHIP, CLASS, RENEWAL, OTHER
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED
    private Timestamp paymentDate;
    private String description;
    private String invoiceNumber;
    private String couponCode;
    private BigDecimal refundAmount;
    private Timestamp refundDate;
    private String refundReason;
    private int processedBy; // Admin/Staff who processed
    private Member member; // For join queries
    private MembershipPlan membershipPlan; // For membership payments

    // Constructors
    public Payment() {
        this.discount = BigDecimal.ZERO;
        this.refundAmount = BigDecimal.ZERO;
    }

    public Payment(int memberId, BigDecimal amount, String paymentMethod,
                   String paymentType, String description) {
        this.memberId = memberId;
        this.amount = amount;
        this.discount = BigDecimal.ZERO;
        this.finalAmount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentType = paymentType;
        this.description = description;
        this.status = "COMPLETED";
        this.paymentDate = new Timestamp(System.currentTimeMillis());
        this.transactionId = generateTransactionId();
        this.invoiceNumber = generateInvoiceNumber();
        this.refundAmount = BigDecimal.ZERO;
    }

    // Generate unique transaction ID
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    // Generate unique invoice number
    private String generateInvoiceNumber() {
        return "INV" + System.currentTimeMillis();
    }

    // Calculate final amount after discount
    public void calculateFinalAmount() {
        if (discount != null && amount != null) {
            this.finalAmount = amount.subtract(discount);
        } else {
            this.finalAmount = amount;
        }
    }

    // Apply discount percentage
    public void applyDiscountPercentage(double percentage) {
        if (amount != null && percentage > 0 && percentage <= 100) {
            this.discount = amount.multiply(BigDecimal.valueOf(percentage / 100));
            calculateFinalAmount();
        }
    }

    // Apply fixed discount
    public void applyFixedDiscount(BigDecimal discountAmount) {
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.discount = discountAmount;
            calculateFinalAmount();
        }
    }

    // Check if payment is refundable
    public boolean isRefundable() {
        return "COMPLETED".equals(status) &&
                (refundAmount == null || refundAmount.compareTo(finalAmount) < 0);
    }

    // Process refund
    public void processRefund(BigDecimal refundAmt, String reason) {
        if (isRefundable()) {
            this.refundAmount = refundAmt;
            this.refundReason = reason;
            this.refundDate = new Timestamp(System.currentTimeMillis());
            if (refundAmount.compareTo(finalAmount) >= 0) {
                this.status = "REFUNDED";
            }
        }
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        calculateFinalAmount();
    }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
        calculateFinalAmount();
    }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Timestamp paymentDate) { this.paymentDate = paymentDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public Timestamp getRefundDate() { return refundDate; }
    public void setRefundDate(Timestamp refundDate) { this.refundDate = refundDate; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public int getProcessedBy() { return processedBy; }
    public void setProcessedBy(int processedBy) { this.processedBy = processedBy; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public MembershipPlan getMembershipPlan() { return membershipPlan; }
    public void setMembershipPlan(MembershipPlan membershipPlan) { this.membershipPlan = membershipPlan; }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", finalAmount=" + finalAmount +
                ", status='" + status + '\'' +
                '}';
    }
}