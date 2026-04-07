package com.example.plms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_masters")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false, unique = true)
    private Part part;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int currentStock = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int pendingIncoming = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Part getPart() { return part; }
    public void setPart(Part part) { this.part = part; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getPendingIncoming() { return pendingIncoming; }
    public void setPendingIncoming(int pendingIncoming) { this.pendingIncoming = pendingIncoming; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
