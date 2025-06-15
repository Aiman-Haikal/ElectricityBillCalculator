package com.example.electricitybillcalculator;

public class Bill {
    // Firestore document ID will be stored here as a String
    private String id;
    private String month;
    private double unitsUsed;
    private double totalCharges;
    private double rebatePercentage;
    private double finalCost;
    private long timestamp; // Added for ordering in Firestore

    // Default constructor required for Firestore's .toObject(Bill.class)
    public Bill() {
        // Public no-arg constructor needed
    }

    // Constructor for creating a new Bill (without ID, for insertion)
    public Bill(String month, double unitsUsed, double totalCharges, double rebatePercentage, double finalCost) {
        this.month = month;
        this.unitsUsed = unitsUsed;
        this.totalCharges = totalCharges;
        this.rebatePercentage = rebatePercentage;
        this.finalCost = finalCost;
        this.timestamp = System.currentTimeMillis(); // Set current timestamp on creation
    }

    // Constructor for retrieving Bill from database (with ID and timestamp)
    public Bill(String id, String month, double unitsUsed, double totalCharges, double rebatePercentage, double finalCost, long timestamp) {
        this.id = id;
        this.month = month;
        this.unitsUsed = unitsUsed;
        this.totalCharges = totalCharges;
        this.rebatePercentage = rebatePercentage;
        this.finalCost = finalCost;
        this.timestamp = timestamp;
    }

    // Getters (all required for Firestore's .toObject(Bill.class))
    public String getId() {
        return id;
    }

    public String getMonth() {
        return month;
    }

    public double getUnitsUsed() {
        return unitsUsed;
    }

    public double getTotalCharges() {
        return totalCharges;
    }

    public double getRebatePercentage() {
        return rebatePercentage;
    }

    public double getFinalCost() {
        return finalCost;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters (required for Firestore's .toObject(Bill.class) to populate fields)
    public void setId(String id) {
        this.id = id;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public void setUnitsUsed(double unitsUsed) {
        this.unitsUsed = unitsUsed;
    }

    public void setTotalCharges(double totalCharges) {
        this.totalCharges = totalCharges;
    }

    public void setRebatePercentage(double rebatePercentage) {
        this.rebatePercentage = rebatePercentage;
    }

    public void setFinalCost(double finalCost) {
        this.finalCost = finalCost;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
