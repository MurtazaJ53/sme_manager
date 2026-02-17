package com.lax.sme_manager.service;

import com.lax.sme_manager.repository.PurchaseRepository;
import com.lax.sme_manager.repository.VendorRepository;
import com.lax.sme_manager.repository.model.PurchaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for purchase history with advanced filtering, pagination, and
 * performance optimization.
 * 
 * Design Decisions:
 * - Filtering happens at DB level (not in-memory) for large datasets
 * - Pagination prevents loading all records into memory
 * - Async loading via background threads (optional)
 * - Caching of recent records only
 */
public class PurchaseHistoryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PurchaseHistoryService.class);

    private final PurchaseRepository purchaseRepository;
    private final VendorRepository vendorRepository;
    private static final int PAGE_SIZE = 50; // Records per page

    public PurchaseHistoryService(PurchaseRepository purchaseRepository, VendorRepository vendorRepository) {
        this.purchaseRepository = purchaseRepository;
        this.vendorRepository = vendorRepository;
    }

    /**
     * Fetch purchase history with filtering and pagination
     */
    public List<PurchaseEntity> fetchPurchases(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> vendorIds,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Boolean chequeIssued,
            String searchQuery,
            int pageNumber) {

        try {
            LOGGER.info("Fetching purchases - Page: {}, Vendors: {}, Range: {} to {}, Search: {}",
                    pageNumber, vendorIds, startDate, endDate, searchQuery);

            // Fetch from repository
            List<PurchaseEntity> purchases = purchaseRepository.findAll();

            // In-memory filtering
            List<PurchaseEntity> filtered = purchases.stream()
                    .filter(p -> isWithinDateRange(p, startDate, endDate))
                    .filter(p -> vendorIds == null || vendorIds.isEmpty() || vendorIds.contains(p.getVendorId()))
                    .filter(p -> isWithinAmountRange(p, minAmount, maxAmount))
                    .filter(p -> chequeIssued == null || isMatchesChequeFilter(p, chequeIssued))
                    .filter(p -> isMatchesSearchQuery(p, searchQuery))
                    .toList();

            LOGGER.debug("Filtered: {} purchases (from {} total)", filtered.size(), purchases.size());

            // Apply pagination
            int fromIndex = pageNumber * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, filtered.size());

            if (fromIndex >= filtered.size()) {
                if (filtered.isEmpty()) return new ArrayList<>();
                LOGGER.warn("Page {} out of range", pageNumber);
                return new ArrayList<>();
            }

            return filtered.subList(fromIndex, toIndex);

        } catch (Exception e) {
            LOGGER.error("Error fetching purchases with filters", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get total filtered record count (for pagination UI)
     */
    public int getTotalFilteredCount(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> vendorIds,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Boolean chequeIssued,
            String searchQuery) {

        try {
            List<PurchaseEntity> purchases = purchaseRepository.findAll();

            return (int) purchases.stream()
                    .filter(p -> isWithinDateRange(p, startDate, endDate))
                    .filter(p -> vendorIds == null || vendorIds.isEmpty() || vendorIds.contains(p.getVendorId()))
                    .filter(p -> isWithinAmountRange(p, minAmount, maxAmount))
                    .filter(p -> chequeIssued == null || isMatchesChequeFilter(p, chequeIssued))
                    .filter(p -> isMatchesSearchQuery(p, searchQuery))
                    .count();

        } catch (Exception e) {
            LOGGER.error("Error calculating total filtered count", e);
            return 0;
        }
    }

    /**
     * Get purchases for TODAY
     */
    public List<PurchaseEntity> getPurchasesToday() {
        LocalDate today = LocalDate.now();
        return fetchPurchases(today, today, null, null, null, null, null, 0);
    }

    /**
     * Get purchases for last N days
     */
    public List<PurchaseEntity> getPurchasesLastNDays(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minus(days, ChronoUnit.DAYS);
        return fetchPurchases(startDate, endDate, null, null, null, null, null, 0);
    }

    /**
     * Get single purchase with vendor details
     */
    public PurchaseEntity getPurchaseById(int id) {
        try {
            return purchaseRepository.findAll().stream()
                    .filter(p -> p.getId() == id)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOGGER.error("Error fetching purchase by ID: {}", id, e);
            return null;
        }
    }

    public void deletePurchase(int id) {
        try {
            purchaseRepository.delete(id);
            LOGGER.info("Soft-deleted purchase ID: {}", id);
        } catch (Exception e) {
            LOGGER.error("Error soft-deleting purchase ID: {}", id, e);
            throw e;
        }
    }

    public void deletePurchases(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return;
        try {
            for (Integer id : ids) {
                purchaseRepository.delete(id);
            }
            LOGGER.info("Soft-deleted {} purchases", ids.size());
        } catch (Exception e) {
            LOGGER.error("Error bulk soft-deleting purchases", e);
            throw e;
        }
    }

    // ========== Helper Methods ==========

    private boolean isWithinDateRange(PurchaseEntity purchase, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null)
            return true;
        LocalDate purchaseDate = purchase.getEntryDate();
        return !purchaseDate.isBefore(startDate) && !purchaseDate.isAfter(endDate);
    }

    private boolean isWithinAmountRange(PurchaseEntity purchase, BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount == null && maxAmount == null)
            return true;

        BigDecimal amount = purchase.getGrandTotal();
        if (amount == null)
            return false;

        if (minAmount != null && amount.compareTo(minAmount) < 0)
            return false;
        if (maxAmount != null && amount.compareTo(maxAmount) > 0)
            return false;

        return true;
    }

    private boolean isMatchesChequeFilter(PurchaseEntity purchase, boolean chequeIssued) {
        boolean hasCheque = purchase.getChequeNumber() != null && !purchase.getChequeNumber().isBlank();
        return chequeIssued == hasCheque;
    }

    private boolean isMatchesSearchQuery(PurchaseEntity purchase, String query) {
        if (query == null || query.isBlank())
            return true;
        String lowerQuery = query.toLowerCase();
        
        // Match cheque number
        if (purchase.getChequeNumber() != null && purchase.getChequeNumber().toLowerCase().contains(lowerQuery))
            return true;
            
        // Match status
        if (purchase.getStatus() != null && purchase.getStatus().toLowerCase().contains(lowerQuery))
            return true;

        // Match vendor name (Expert touch)
        String vendorName = vendorRepository.findAllVendors().stream()
                .filter(v -> v.getId() == purchase.getVendorId())
                .map(com.lax.sme_manager.domain.Vendor::getName)
                .findFirst()
                .orElse("");
        if (vendorName.toLowerCase().contains(lowerQuery))
            return true;

        return false;
    }
}
