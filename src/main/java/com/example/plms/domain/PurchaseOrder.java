package com.example.plms.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int receivedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private LocalDate expectedArrivalDate;
    private LocalDateTime lastReceiptDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime orderDate;
    
    private String remarks;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Part getPart() { return part; }
    public void setPart(Part part) { this.part = part; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(int receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public LocalDate getExpectedArrivalDate() { return expectedArrivalDate; }
    public void setExpectedArrivalDate(LocalDate expectedArrivalDate) { this.expectedArrivalDate = expectedArrivalDate; }
    public LocalDateTime getLastReceiptDate() { return lastReceiptDate; }
    public void setLastReceiptDate(LocalDateTime lastReceiptDate) { this.lastReceiptDate = lastReceiptDate; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @PrePersist
    public void prePersist() {
        this.orderDate = LocalDateTime.now();
    }
}
