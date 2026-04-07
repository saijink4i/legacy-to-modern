package com.example.plms.domain;

public enum OrderStatus {
    PENDING("대기중"),
    COMPLETED("완료됨"),
    CANCELLED("취소됨");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
