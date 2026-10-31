package com.example.dinepulse.models;

public class CartItem {
    private MenuItem menuItem;
    private int quantity;
    private double modifiedPrice; // Added to store the specific price for this cart item
    private String notes;

    // Updated constructor to accept all 4 arguments
    public CartItem(MenuItem menuItem, int quantity, double modifiedPrice, String notes) {
        this.menuItem = menuItem;
        this.quantity = quantity;
        this.modifiedPrice = modifiedPrice;
        this.notes = notes;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void incrementQuantity() {
        this.quantity++;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getModifiedPrice() {
        return modifiedPrice;
    }

    public void setModifiedPrice(double modifiedPrice) {
        this.modifiedPrice = modifiedPrice;
    }

    // Added to match the method called in your CartRepository
    public double getSubtotal() {
        return modifiedPrice * quantity;
    }

    public double getTotalPrice() {
        return getSubtotal();
    }
}