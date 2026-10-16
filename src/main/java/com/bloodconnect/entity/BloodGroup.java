package com.bloodconnect.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String displayValue;

    BloodGroup(String displayValue) {
        this.displayValue = displayValue;
    }

    @JsonValue
    public String getDisplayValue() {
        return displayValue;
    }

    @JsonCreator
    public static BloodGroup fromString(String text) {
        if (text == null) return null;
        for (BloodGroup bg : BloodGroup.values()) {
            if (bg.name().equalsIgnoreCase(text) || bg.displayValue.equalsIgnoreCase(text.trim())) {
                return bg;
            }
        }
        throw new IllegalArgumentException("Invalid blood group: " + text + ". Expected one of: A+, A-, B+, B-, AB+, AB-, O+, O-");
    }
}
