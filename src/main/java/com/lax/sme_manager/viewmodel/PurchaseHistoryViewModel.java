package com.lax.sme_manager.viewmodel;

import com.lax.sme_manager.repository.model.PurchaseEntity;
import com.lax.sme_manager.service.PurchaseHistoryService;
import com.lax.sme_manager.ui.state.PurchaseHistoryFilterState;
import com.lax.sme_manager.util.AppLogger;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PurchaseHistoryViewModel {
    private static final Logger LOGGER = AppLogger.getLogger(PurchaseHistoryViewModel.class);
    private final PurchaseHistoryService historyService;

    // State is encapsulated here
    public final PurchaseHistoryFilterState filterState;

    // Data List
    public final ObservableList<PurchaseEntity> purchaseList = FXCollections.observableArrayList();
    public final ObservableList<PurchaseEntity> selectedPurchases = FXCollections.observableArrayList();

    // Status
    public final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    public final StringProperty statusMessage = new SimpleStringProperty("");
    public final StringProperty paginationLabel = new SimpleStringProperty("Page 1 / 1");
    // Stats
    public final IntegerProperty totalRecords = new SimpleIntegerProperty(0);
    public final IntegerProperty filteredRecords = new SimpleIntegerProperty(0);

    public PurchaseHistoryViewModel(PurchaseHistoryService historyService) {
        this.historyService = historyService;
        this.filterState = new PurchaseHistoryFilterState();

        // Reload when page changes or search query/filters change
        filterState.currentPage.addListener((obs, old, newVal) -> loadPurchases());
        filterState.searchQuery.addListener((obs, old, newVal) -> applyFilters());
        filterState.filterStartDate.addListener((obs, old, newVal) -> applyFilters());
        filterState.filterEndDate.addListener((obs, old, newVal) -> applyFilters());
        filterState.filterMinAmount.addListener((obs, old, newVal) -> applyFilters());
        filterState.filterMaxAmount.addListener((obs, old, newVal) -> applyFilters());
        filterState.filterVendorIds.addListener((javafx.collections.ListChangeListener<Integer>) c -> applyFilters());
    }

    public PurchaseHistoryFilterState getFilterState() {
        return filterState;
    }

    public void applyFilters() {
        // Reset to page 0 when filtering
        filterState.currentPage.set(0);
        loadPurchases();
    }

    public void resetFilters() {
        filterState.resetFilters();
        loadPurchases();
    }

    public void loadPurchases() {
        isLoading.set(true);
        statusMessage.set("Loading history...");

        CompletableFuture.supplyAsync(() -> {
            try {
                // Get Data
                List<PurchaseEntity> data = historyService.fetchPurchases(
                        filterState.filterStartDate.get(),
                        filterState.filterEndDate.get(),
                        filterState.filterVendorIds,
                        filterState.filterMinAmount.get(),
                        filterState.filterMaxAmount.get(),
                        filterState.filterChequeIssued.get(),
                        filterState.searchQuery.get(),
                        filterState.currentPage.get());

                // Get Counts
                int filteredCount = historyService.getTotalFilteredCount(
                        filterState.filterStartDate.get(),
                        filterState.filterEndDate.get(),
                        filterState.filterVendorIds,
                        filterState.filterMinAmount.get(),
                        filterState.filterMaxAmount.get(),
                        filterState.filterChequeIssued.get(),
                        filterState.searchQuery.get());

                // Total count (unfiltered) could be fetched from repo if needed, but for now 0
                // is fine or fetch separate
                // Let's just use filtered count for now to save a query if "Total" isn't
                // strictly required

                return new HistoryResult(data, filteredCount);
            } catch (Exception e) {
                LOGGER.error("Load failed", e);
                throw e;
            }
        }).thenAccept(result -> Platform.runLater(() -> {
            purchaseList.setAll(result.data);

            // Calculate pages
            int total = result.filteredCount;
            filteredRecords.set(total);

            int pageSize = 50; // Must match Service PAGE_SIZE
            int pages = (int) Math.ceil((double) total / pageSize);
            if (pages == 0)
                pages = 1;
            filterState.totalPages.set(pages);

            paginationLabel.set("Page " + (filterState.currentPage.get() + 1) + " / " + pages);

            isLoading.set(false);
            statusMessage.set("Loaded " + result.data.size() + " entries");

        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                isLoading.set(false);
                statusMessage.set("Error loading data: " + ex.getMessage());
            });
            return null;
        });
    }

    public void nextPage() {
        if (filterState.currentPage.get() < filterState.totalPages.get() - 1) {
            filterState.currentPage.set(filterState.currentPage.get() + 1);
        }
    }

    public void prevPage() {
        if (filterState.currentPage.get() > 0) {
            filterState.currentPage.set(filterState.currentPage.get() - 1);
        }
    }

    public void deletePurchase(PurchaseEntity p) {
        CompletableFuture.runAsync(() -> {
            historyService.deletePurchase(p.getId());
        }).thenRun(() -> Platform.runLater(() -> {
            loadPurchases();
            statusMessage.set("Entry deleted successfully.");
        })).exceptionally(ex -> {
            Platform.runLater(() -> statusMessage.set("Error deleting: " + ex.getMessage()));
            return null;
        });
    }

    public void deleteSelectedPurchases() {
        if (selectedPurchases.isEmpty())
            return;

        List<Integer> ids = selectedPurchases.stream().map(PurchaseEntity::getId).toList();
        CompletableFuture.runAsync(() -> {
            historyService.deletePurchases(ids);
        }).thenRun(() -> Platform.runLater(() -> {
            selectedPurchases.clear();
            loadPurchases();
            statusMessage.set("Selected entries deleted successfully.");
        })).exceptionally(ex -> {
            Platform.runLater(() -> statusMessage.set("Error bulk deleting: " + ex.getMessage()));
            return null;
        });
    }

    private record HistoryResult(List<PurchaseEntity> data, int filteredCount) {
    }
}
