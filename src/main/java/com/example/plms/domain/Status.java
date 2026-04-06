package com.example.plms.domain;

public enum Status {
    ORDERED("발주"),
    RECEIVED("입고"),
    DISPOSED("폐기");

    private final String description;

    Status(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
