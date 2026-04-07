package com.example.plms.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "part_transactions")
public class PartTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int quantity;

    private LocalDate expectedArrivalDate;

    private LocalDateTime transactionDate;
    private String remarks;

    @PrePersist
    public void prePersist() {
        this.transactionDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Part getPart() { return part; }
    public void setPart(Part part) { this.part = part; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public LocalDate getExpectedArrivalDate() { return expectedArrivalDate; }
    public void setExpectedArrivalDate(LocalDate expectedArrivalDate) { this.expectedArrivalDate = expectedArrivalDate; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
