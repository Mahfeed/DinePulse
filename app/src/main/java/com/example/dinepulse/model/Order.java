package com.example.dinepulse.models;

import java.util.List;
import java.util.Map;

public class Order {
    private String orderId;
    private String customerId;
    private String table;
    private String status; // "Pending", "Preparing", "Ready"
    private double totalAmount;
    private Object timestamp; // Changed from Timestamp to Object to safely handle Long or Timestamp
    private List<Map<String, Object>> items;
    private String customerNotes;

    // Required empty constructor for Firestore object mapping
    public Order() {}

    public Order(String orderId, String customerId, String table, String status, double totalAmount, Object timestamp, List<Map<String, Object>> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.table = table;
        this.status = status;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public String getTable() { return table != null ? table : "Table 4"; }
    public String getStatus() { return status != null ? status : "Pending"; }
    public double getTotalAmount() { return totalAmount; }
    public Object getTimestamp() { return timestamp; }
    public List<Map<String, Object>> getItems() { return items; }
    public String getCustomerNotes() { return customerNotes; }
    public void setCustomerNotes(String customerNotes) { this.customerNotes = customerNotes; }
}