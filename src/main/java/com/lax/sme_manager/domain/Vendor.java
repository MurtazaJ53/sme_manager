package com.lax.sme_manager.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
public class Vendor {
    private int id;
    private String name;
    private String bankName;
    private String contactInfo;
    private String address;
    private String gstinCode;
    private String ifscCode;
    private String notes;
    private String status;
    private boolean isDeleted; // Added isDeleted field
    private String panNumber;
    private java.math.BigDecimal defaultAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor: ID + Name (for temporary/new vendors)
    public Vendor(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Full constructor
    public Vendor(int id, String name, String contactInfo, String address,
                  String gstinCode, String ifscCode, String notes,
                  String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.contactInfo = contactInfo;
        this.address = address;
        this.gstinCode = gstinCode;
        this.ifscCode = ifscCode;
        this.notes = notes;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ✅ CRITICAL: Proper equals() and hashCode() based on NAME (not ID)
    // This allows ComboBox selection to work correctly
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vendor vendor = (Vendor) obj;
        return Objects.equals(this.name, vendor.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    @Override
    public String toString() {
        return this.name != null ? this.name : "";
    }

}
