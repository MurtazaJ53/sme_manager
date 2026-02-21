package com.lax.sme_manager.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain model for Vendor (Vepari)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorEntity {
    private int id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private java.math.BigDecimal defaultAmount;
    private Boolean isDeleted; // Soft delete flag
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return name;
    }
}
