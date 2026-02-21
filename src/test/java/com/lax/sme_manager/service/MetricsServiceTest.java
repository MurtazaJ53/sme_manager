package com.lax.sme_manager.service;

import com.lax.sme_manager.repository.IPurchaseRepository;
import com.lax.sme_manager.repository.model.PurchaseEntity;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class MetricsServiceTest {

    @Test
    public void testGetDashboardMetrics() {
        // Arrange
        FakePurchaseRepository fakeRepo = new FakePurchaseRepository();
        fakeRepo.setBagsCount(100);
        fakeRepo.setTotalAmount(50000.0);
        fakeRepo.setPendingCheques(5);

        MetricsService service = new MetricsService(fakeRepo);

        // Act
        MetricsService.DashboardMetrics metrics = service.getDashboardMetrics();

        // Assert
        assertEquals(100, metrics.getBagsToday());
        assertEquals(101, metrics.getBagsThisWeek());
        assertEquals(50000.0, metrics.getAmountThisMonth(), 0.01);
        assertEquals(5, metrics.getUnpaidChequesTotal());
    }

    // Fake Repository Implementation
    static class FakePurchaseRepository implements IPurchaseRepository {
        private int bagsCount = 0;
        private double totalAmount = 0.0;
        private int pendingCheques = 0;

        public void setBagsCount(int count) {
            this.bagsCount = count;
        }

        public void setTotalAmount(double amount) {
            this.totalAmount = amount;
        }

        public void setPendingCheques(int count) {
            this.pendingCheques = count;
        }

        @Override
        public Integer getBagsCount(LocalDate startDate, LocalDate endDate) {
            // Return different values for different ranges just for testing diversity
            if (startDate != null && startDate.equals(endDate))
                return bagsCount;
            return bagsCount + 1; // Week/Month
        }

        @Override
        public Double getTotalAmount(LocalDate startDate, LocalDate endDate) {
            return totalAmount;
        }

        @Override
        public Integer countPendingCheques(LocalDate startDate, LocalDate endDate) {
            return pendingCheques;
        }

        @Override
        public Integer countChequesByClearingDate(LocalDate date) {
            return pendingCheques;
        }

        @Override
        public List<PurchaseEntity> findByVendorAndStatus(Integer vendorId, String status) {
            return Collections.emptyList();
        }

        @Override
        public void delete(Integer id) {
        }

        // Unused methods stubbed
        @Override
        public PurchaseEntity save(PurchaseEntity entity) {
            return null;
        }

        @Override
        public Optional<PurchaseEntity> findById(Integer id) {
            return Optional.empty();
        }

        @Override
        public List<PurchaseEntity> findAll() {
            return Collections.emptyList();
        }

        @Override
        public List<PurchaseEntity> findByDate(LocalDate date) {
            return Collections.emptyList();
        }

        @Override
        public List<PurchaseEntity> findByVendorId(Integer vendorId) {
            return Collections.emptyList();
        }

        @Override
        public List<PurchaseEntity> findByDateRange(LocalDate startDate, LocalDate endDate) {
            return Collections.emptyList();
        }

        @Override
        public List<PurchaseEntity> findTodaysPurchases() {
            return Collections.emptyList();
        }

        @Override
        public List<PurchaseEntity> findThisMonthPurchases() {
            return Collections.emptyList();
        }

        @Override
        public int getLastInsertedId() {
            return 0;
        }
    }
}
