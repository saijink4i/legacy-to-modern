package com.example.plms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipt_logs")
public class ReceiptLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @Column(nullable = false)
    private int receivedQuantity;

    private String remarks;

    @Column(nullable = false, updatable = false)
    private LocalDateTime receiveDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PurchaseOrder getOrder() { return order; }
    public void setOrder(PurchaseOrder order) { this.order = order; }
    public int getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(int receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getReceiveDate() { return receiveDate; }

    @PrePersist
    public void prePersist() {
        this.receiveDate = LocalDateTime.now();
    }
}
