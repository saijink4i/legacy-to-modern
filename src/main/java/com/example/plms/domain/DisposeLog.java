package com.example.plms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispose_logs")
public class DisposeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(nullable = false)
    private int disposedQuantity;

    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime disposeDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Part getPart() { return part; }
    public void setPart(Part part) { this.part = part; }
    public int getDisposedQuantity() { return disposedQuantity; }
    public void setDisposedQuantity(int disposedQuantity) { this.disposedQuantity = disposedQuantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getDisposeDate() { return disposeDate; }

    @PrePersist
    public void prePersist() {
        this.disposeDate = LocalDateTime.now();
    }
}
