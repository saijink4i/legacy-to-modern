package com.example.plms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "part_masters")
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int price = 0;

    @Column(nullable = false, columnDefinition = "integer default 1")
    private int orderUnit = 1;

    @Column(nullable = false, length = 8, columnDefinition = "varchar(8) default '99999999'")
    private String expirationDate = "99999999";

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int leadTimeDays = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public int getOrderUnit() { return orderUnit; }
    public void setOrderUnit(int orderUnit) { this.orderUnit = orderUnit; }
    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }
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
