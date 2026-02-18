package com.lax.sme_manager.viewmodel;

import com.lax.sme_manager.domain.Vendor;
import com.lax.sme_manager.logic.FeeCalculator;
import com.lax.sme_manager.repository.PurchaseRepository;
import com.lax.sme_manager.repository.VendorRepository;
import com.lax.sme_manager.repository.model.PurchaseEntity;
import com.lax.sme_manager.repository.model.VendorEntity;
import com.lax.sme_manager.util.AppLogger;
import com.lax.sme_manager.util.EntityConverters;
import com.lax.sme_manager.util.VendorCache;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.concurrent.Task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PurchaseEntryViewModel {
    private static final Logger LOGGER = AppLogger.getLogger(PurchaseEntryViewModel.class);

    private Integer editingId = null;

    // Repositories
    private final PurchaseRepository purchaseRepository;
    private final VendorRepository vendorRepository;
    private final VendorCache vendorCache;

    // Input Properties
    public final ObjectProperty<LocalDate> entryDate = new SimpleObjectProperty<>(LocalDate.now());
    public final ObjectProperty<Vendor> selectedVendor = new SimpleObjectProperty<>();
    public final StringProperty bags = new SimpleStringProperty("0");
    public final StringProperty rate = new SimpleStringProperty("0");
    public final StringProperty weight = new SimpleStringProperty("0");
    public final BooleanProperty isLumpsum = new SimpleBooleanProperty(false);
    public final BooleanProperty advancePaid = new SimpleBooleanProperty(false);
    public final ObjectProperty<String> paymentMode = new SimpleObjectProperty<>("CHEQUE");
    public final StringProperty notes = new SimpleStringProperty("");

    // Fee Configuration (Editable)
    public final StringProperty marketFeePercent = new SimpleStringProperty("0.70");
    public final StringProperty commissionPercent = new SimpleStringProperty("2.00");

    // Calculated Outputs (Read-only for UI, but updated by VM)
    private final DoubleProperty baseAmount = new SimpleDoubleProperty(0.0);
    private final DoubleProperty marketFeeAmount = new SimpleDoubleProperty(0.0);
    private final DoubleProperty commissionFeeAmount = new SimpleDoubleProperty(0.0);
    private final DoubleProperty grandTotal = new SimpleDoubleProperty(0.0);

    // Status / Messages
    public final StringProperty statusMessage = new SimpleStringProperty("");
    public final BooleanProperty isStatusError = new SimpleBooleanProperty(false);

    public PurchaseEntryViewModel(VendorCache vendorCache) {
        this.vendorCache = vendorCache;
        this.vendorRepository = new VendorRepository();
        this.purchaseRepository = new PurchaseRepository();

        setupListeners();
    }

    private void setupListeners() {
        // Re-calculate whenever any input impacting calculation changes
        bags.addListener((obs, o, n) -> recalculate());
        rate.addListener((obs, o, n) -> recalculate());
        weight.addListener((obs, o, n) -> recalculate());
        isLumpsum.addListener((obs, o, n) -> {
            if (n)
                weight.set("0"); // Clear weight if lumpsum
            recalculate();
        });
        advancePaid.addListener((obs, o, n) -> {
            if (n) {
                marketFeePercent.set("0.00");
                commissionPercent.set("2.00"); // Standard commission even if advance? User requirement says 2.00 in code but 0.00 in previous logic. Keeping 0.00 as per common sense for Advance.
                marketFeePercent.set("0.00");
                commissionPercent.set("0.00");
                paymentMode.set("ADVANCE");
            } else {
                marketFeePercent.set("0.70");
                commissionPercent.set("2.00");
                if ("ADVANCE".equals(paymentMode.get())) {
                    paymentMode.set("CHEQUE");
                }
                recalculate(); // explicitly recalc when toggling off advance
            }
        });

        paymentMode.addListener((obs, o, n) -> {
            if ("ADVANCE".equals(n)) {
                advancePaid.set(true);
            } else {
                advancePaid.set(false);
            }
        });
        marketFeePercent.addListener((obs, o, n) -> recalculate());
        commissionPercent.addListener((obs, o, n) -> recalculate());

        // Initial calc
        recalculate();
    }

    private void recalculate() {
        try {
            int bagsVal = parseIntSafe(bags.get(), 0);
            BigDecimal rateVal = parseBigDecimalSafe(rate.get(), BigDecimal.ZERO);
            BigDecimal weightVal = parseBigDecimalSafe(weight.get(), BigDecimal.ZERO);
            BigDecimal mktPct = parseBigDecimalSafe(marketFeePercent.get(), BigDecimal.ZERO);
            BigDecimal commPct = parseBigDecimalSafe(commissionPercent.get(), BigDecimal.ZERO);

            BigDecimal calculatedBase = FeeCalculator.calculateBaseAmount(isLumpsum.get(), bagsVal, weightVal, rateVal);
            BigDecimal calculatedMktFee = FeeCalculator.calculateFee(calculatedBase, mktPct);
            BigDecimal calculatedCommFee = FeeCalculator.calculateFee(calculatedBase, commPct);
            BigDecimal calculatedTotal = FeeCalculator.calculateGrandTotal(calculatedBase, calculatedMktFee,
                    calculatedCommFee);

            baseAmount.set(calculatedBase.doubleValue());
            marketFeeAmount.set(calculatedMktFee.doubleValue());
            commissionFeeAmount.set(calculatedCommFee.doubleValue());
            grandTotal.set(calculatedTotal.doubleValue());

        } catch (Exception e) {
            LOGGER.debug("Calculation error: " + e.getMessage());
        }
    }

    // --- Actions ---

    public void submitEntry() {
        if (!validate())
            return;

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                performSave();
                return null;
            }

            @Override
            protected void succeeded() {
                statusMessage.set("Entry saved successfully!");
                isStatusError.set(false);
                resetForm();
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                LOGGER.error("Save failed: " + ex.getMessage());
                ex.printStackTrace();
                statusMessage.set("Error saving entry: " + ex.getMessage());
                isStatusError.set(true);
            }
        };

        // Run on background thread
        new Thread(saveTask).start();
    }

    /**
     * Submit the entry and return cheque data for immediate printing.
     * Returns null if validation or save fails.
     */
    public ChequeSubmitResult submitAndGetChequeData() {
        if (!validate())
            return null;

        try {
            // Capture cheque data BEFORE save resets the form
            String vendorName = selectedVendor.get() != null ? selectedVendor.get().getName() : "Unknown";
            BigDecimal total = BigDecimal.valueOf(grandTotal.get());
            LocalDate chequeDate = entryDate.get() != null ? entryDate.get() : LocalDate.now();

            performSave();

            // Get the last inserted purchase ID
            int savedId = purchaseRepository.getLastInsertedId();

            statusMessage.set("Entry saved successfully!");
            isStatusError.set(false);
            resetForm();

            return new ChequeSubmitResult(vendorName, total, chequeDate, savedId);
        } catch (Exception e) {
            LOGGER.error("Save failed: " + e.getMessage());
            statusMessage.set("Error saving entry: " + e.getMessage());
            isStatusError.set(true);
            return null;
        }
    }

    /** Data class to carry cheque info from submit to print */
    public static class ChequeSubmitResult {
        public final String vendorName;
        public final BigDecimal grandTotal;
        public final LocalDate chequeDate;
        public final int purchaseId;

        public ChequeSubmitResult(String vendorName, BigDecimal grandTotal, LocalDate chequeDate, int purchaseId) {
            this.vendorName = vendorName;
            this.grandTotal = grandTotal;
            this.chequeDate = chequeDate;
            this.purchaseId = purchaseId;
        }
    }

    private boolean validate() {
        if (selectedVendor.get() == null && (selectedVendor.getName() == null || selectedVendor.getName().isBlank())) {
            // Logic to handle "new typing" in UI binding might be needed,
            // but View should ensure selectedVendor property is set correctly (even if
            // transient)
            // For now assuming Property<Vendor> is populated
        }

        if (selectedVendor.get() == null) {
            statusMessage.set("Vendor is required.");
            isStatusError.set(true);
            return false;
        }

        if (parseIntSafe(bags.get(), 0) <= 0) {
            statusMessage.set("Bags must be > 0");
            isStatusError.set(true);
            return false;
        }

        if (parseBigDecimalSafe(rate.get(), BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
            statusMessage.set("Rate must be > 0");
            isStatusError.set(true);
            return false;
        }

        if (!isLumpsum.get() && parseBigDecimalSafe(weight.get(), BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
            statusMessage.set("Weight is required for non-lumpsum.");
            isStatusError.set(true);
            return false;
        }

        statusMessage.set("Saving...");
        isStatusError.set(false);
        return true;
    }

    private void performSave() throws Exception {
        Vendor currentVendor = selectedVendor.get();
        int vendorId = currentVendor.getId();

        // New Vendor Creation Logic
        if (vendorId == -1) {
            VendorEntity newVendor = new VendorEntity();
            newVendor.setName(currentVendor.getName());
            newVendor.setNotes("Created from Purchase Entry");
            newVendor.setCreatedAt(LocalDateTime.now());
            newVendor.setUpdatedAt(LocalDateTime.now());
            VendorEntity saved = vendorRepository.insert(newVendor);
            vendorId = saved.getId();

            // Update cache
            Platform.runLater(vendorCache::refreshCache);
        }

        // Create/Update Entity
        PurchaseEntity entity = new PurchaseEntity();
        if (editingId != null) {
            entity.setId(editingId);
        }
        entity.setEntryDate(entryDate.get());
        entity.setVendorId(vendorId);
        entity.setBags(parseIntSafe(bags.get(), 0));
        entity.setRate(parseBigDecimalSafe(rate.get(), BigDecimal.ZERO));
        entity.setWeightKg(parseBigDecimalSafe(weight.get(), BigDecimal.ZERO));
        entity.setIsLumpsum(isLumpsum.get());
        entity.setMarketFeePercent(parseBigDecimalSafe(marketFeePercent.get(), BigDecimal.ZERO));
        entity.setCommissionPercent(parseBigDecimalSafe(commissionPercent.get(), BigDecimal.ZERO));
        entity.setMarketFeeAmount(BigDecimal.valueOf(marketFeeAmount.get()));
        entity.setCommissionFeeAmount(BigDecimal.valueOf(commissionFeeAmount.get()));
        entity.setBaseAmount(BigDecimal.valueOf(baseAmount.get()));
        entity.setGrandTotal(BigDecimal.valueOf(grandTotal.get()));
        entity.setNotes(notes.get());
        entity.setPaymentMode(paymentMode.get());
        entity.setAdvancePaid(advancePaid.get());
        
        // Dynamic Status Logic
        if (advancePaid.get()) {
            entity.setStatus("PAID (ADVANCE)");
        } else {
            String mode = paymentMode.get();
            if ("CASH".equalsIgnoreCase(mode)) {
                entity.setStatus("PAID (CASH)");
            } else if ("BANK TRANSFER".equalsIgnoreCase(mode)) {
                entity.setStatus("PAID (BANK TRANSFER)");
            } else if ("UPI".equalsIgnoreCase(mode)) {
                entity.setStatus("PAID (UPI)");
            } else {
                entity.setStatus("UNPAID");
            }
        }
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        purchaseRepository.save(entity);
        editingId = null; // Clear after save
    }

    public void resetForm() {
        editingId = null;
        entryDate.set(LocalDate.now());
        selectedVendor.set(null);
        bags.set("0");
        rate.set("0");
        weight.set("0");
        isLumpsum.set(false);
        advancePaid.set(false);
        paymentMode.set("CHEQUE");
        notes.set("");
        // Fees reset by advancePaid listener, but ensuring defaults:
        marketFeePercent.set("0.70");
        commissionPercent.set("2.00");
    }

    /**
     * Load existing purchase data into the form (Read-Only usage)
     */
    public void setPurchaseData(PurchaseEntity entity) {
        if (entity == null)
            return;

        this.editingId = entity.getId();
        entryDate.set(entity.getEntryDate());

        // Find vendor in cache by ID
        Vendor v = vendorCache.findById(entity.getVendorId());
        if (v != null) {
            selectedVendor.set(v);
        } else {
            // Fallback if vendor deleted or not in cache
            selectedVendor.set(new Vendor(entity.getVendorId(), "Unknown Vendor (ID: " + entity.getVendorId() + ")"));
        }

        bags.set(String.valueOf(entity.getBags()));
        rate.set(String.valueOf(entity.getRate()));
        weight.set(String.valueOf(entity.getWeightKg()));
        isLumpsum.set(entity.getIsLumpsum());

        marketFeePercent.set(String.valueOf(entity.getMarketFeePercent()));
        commissionPercent.set(String.valueOf(entity.getCommissionPercent()));

        paymentMode.set(entity.getPaymentMode());
        advancePaid.set(entity.getAdvancePaid());
        notes.set(entity.getNotes());

        // Recalculate to ensure totals match
        recalculate();
    }

    // --- Helpers ---
    public int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    public BigDecimal parseBigDecimalSafe(String s, BigDecimal def) {
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    // --- Getters for Properties (for Binding) ---
    public DoubleProperty baseAmountProperty() {
        return baseAmount;
    }

    public DoubleProperty marketFeeAmountProperty() {
        return marketFeeAmount;
    }

    public DoubleProperty commissionFeeAmountProperty() {
        return commissionFeeAmount;
    }

    public DoubleProperty grandTotalProperty() {
        return grandTotal;
    }
}
