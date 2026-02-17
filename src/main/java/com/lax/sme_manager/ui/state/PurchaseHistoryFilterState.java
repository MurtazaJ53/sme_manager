package com.lax.sme_manager.ui.state;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Manages filter state for Purchase History screen.
 * Centralized, reactive filter state that all components can observe.
 */
public class PurchaseHistoryFilterState {

    // Date filters
    public final ObjectProperty<LocalDate> filterStartDate = new SimpleObjectProperty<>(LocalDate.now().minusMonths(1));
    public final ObjectProperty<LocalDate> filterEndDate = new SimpleObjectProperty<>(LocalDate.now());
    public final StringProperty searchQuery = new SimpleStringProperty("");

    // Vendor filter
    public final ObservableList<Integer> filterVendorIds = FXCollections.observableArrayList();

    // Amount filters
    public final ObjectProperty<BigDecimal> filterMinAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    public final ObjectProperty<BigDecimal> filterMaxAmount = new SimpleObjectProperty<>(null);

    // Cheque filter (null = no filter, true = issued, false = not issued)
    public final ObjectProperty<Boolean> filterChequeIssued = new SimpleObjectProperty<>(null);

    // Pagination
    public final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    public final IntegerProperty totalPages = new SimpleIntegerProperty(0);

    public void resetFilters() {
        filterStartDate.set(LocalDate.now().minusMonths(1));
        filterEndDate.set(LocalDate.now());
        filterVendorIds.clear();
        filterMinAmount.set(BigDecimal.ZERO);
        filterMaxAmount.set(null);
        filterChequeIssued.set(null);
        searchQuery.set("");
        currentPage.set(0);
    }

    public void applyPresetToday() {
        LocalDate today = LocalDate.now();
        filterStartDate.set(today);
        filterEndDate.set(today);
        currentPage.set(0);
    }

    public void applyPresetYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        filterStartDate.set(yesterday);
        filterEndDate.set(yesterday);
        currentPage.set(0);
    }

    public void applyPresetLast7Days() {
        filterStartDate.set(LocalDate.now().minusDays(7));
        filterEndDate.set(LocalDate.now());
        currentPage.set(0);
    }

    public void applyPresetLastMonth() {
        filterStartDate.set(LocalDate.now().minusMonths(1));
        filterEndDate.set(LocalDate.now());
        currentPage.set(0);
    }
}
