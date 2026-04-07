package com.example.plms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parts")
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int incomingQuantity = 0;

    @Column(nullable = false, columnDefinition = "integer default 100")
    private int price = 100;

    @Column(nullable = false, columnDefinition = "integer default 1")
    private int orderUnit = 1;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int expirationDays = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.stockQuantity < 0) {
            this.stockQuantity = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public int getIncomingQuantity() { return incomingQuantity; }
    public void setIncomingQuantity(int incomingQuantity) { this.incomingQuantity = incomingQuantity; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public int getOrderUnit() { return orderUnit; }
    public void setOrderUnit(int orderUnit) { this.orderUnit = orderUnit; }
    public int getExpirationDays() { return expirationDays; }
    public void setExpirationDays(int expirationDays) { this.expirationDays = expirationDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
