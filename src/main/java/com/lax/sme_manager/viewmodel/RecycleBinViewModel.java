package com.lax.sme_manager.viewmodel;

import com.lax.sme_manager.repository.IPurchaseRepository;
import com.lax.sme_manager.repository.model.PurchaseEntity;
import com.lax.sme_manager.util.AppLogger;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RecycleBinViewModel {
    private static final Logger LOGGER = AppLogger.getLogger(RecycleBinViewModel.class);
    private final IPurchaseRepository purchaseRepository;

    public final ObservableList<PurchaseEntity> deletedPurchases = FXCollections.observableArrayList();
    public final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    public final StringProperty statusMessage = new SimpleStringProperty("");

    public RecycleBinViewModel(IPurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public void loadDeletedPurchases() {
        isLoading.set(true);
        statusMessage.set("Loading Recycle Bin...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return purchaseRepository.findAllDeleted();
            } catch (Exception e) {
                LOGGER.error("Failed to load deleted purchases", e);
                throw e;
            }
        }).thenAccept(list -> Platform.runLater(() -> {
            deletedPurchases.setAll(list);
            isLoading.set(false);
            statusMessage.set("Found " + list.size() + " deleted entries");
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                isLoading.set(false);
                statusMessage.set("Error: " + ex.getMessage());
            });
            return null;
        });
    }

    public void restorePurchase(PurchaseEntity p) {
        if (p == null) return;
        
        CompletableFuture.runAsync(() -> {
            purchaseRepository.restore(p.getId());
        }).thenRun(() -> Platform.runLater(() -> {
            loadDeletedPurchases();
            statusMessage.set("Entry restored successfully.");
        })).exceptionally(ex -> {
            Platform.runLater(() -> statusMessage.set("Error restoring: " + ex.getMessage()));
            return null;
        });
    }
}
