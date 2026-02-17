package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.repository.model.PurchaseEntity;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import java.util.function.Consumer;

/**
 * Modern Action Buttons for TableView with soft-tinted backgrounds.
 */
public class ActionButtonsTableCell extends TableCell<PurchaseEntity, Void> {
    
    private final HBox container = new HBox(10);
    private final Button viewBtn;
    private final Button editBtn;
    private final Button printBtn;
    private final Button deleteBtn;

    public ActionButtonsTableCell(
            Consumer<PurchaseEntity> onView,
            Consumer<PurchaseEntity> onEdit,
            Consumer<PurchaseEntity> onPrint,
            Consumer<PurchaseEntity> onDelete) {
        
        // 1. View Button (Blue)
        viewBtn = createModernButton(
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z",
            "#E3F2FD", "#1976D2", "#BBDEFB", "View Invoice"
        );

        // 2. Edit Button (Orange)
        editBtn = createModernButton(
            "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
            "#FFF3E0", "#F57C00", "#FFE0B2", "Edit Entry"
        );

        // 3. Print Button (Teal)
        printBtn = createModernButton(
            "M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z",
            "#E0F2F1", "#00897B", "#B2DFDB", "Print Cheque"
        );

        // 4. Delete Button (Red)
        deleteBtn = createModernButton(
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
            "#FFEBEE", "#D32F2F", "#FFCDD2", "Delete Permanent"
        );

        viewBtn.setOnAction(e -> applyAction(onView));
        editBtn.setOnAction(e -> applyAction(onEdit));
        printBtn.setOnAction(e -> applyAction(onPrint));
        deleteBtn.setOnAction(e -> applyAction(onDelete));

        container.getChildren().addAll(viewBtn, editBtn, printBtn, deleteBtn);
        container.setAlignment(Pos.CENTER);
    }

    private void applyAction(Consumer<PurchaseEntity> action) {
        PurchaseEntity p = getTableView().getItems().get(getIndex());
        if (p != null && action != null) {
            action.accept(p);
        }
    }

    private Button createModernButton(String svgPath, String bgColor, String iconColor, String hoverColor, String tooltip) {
        SVGPath path = new SVGPath();
        path.setContent(svgPath);
        path.setFill(Color.web(iconColor));
        path.setScaleX(1.1);
        path.setScaleY(1.1);

        Button btn = new Button();
        btn.setGraphic(path);
        btn.setTooltip(new Tooltip(tooltip));
        
        // Base Style
        String baseStyle = String.format("-fx-background-color: %s; -fx-padding: 5 8; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;", bgColor);
        btn.setStyle(baseStyle);

        // Hover Effect
        btn.setOnMouseEntered(e -> btn.setStyle(baseStyle.replace(bgColor, hoverColor)));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));

        return btn;
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            PurchaseEntity p = getTableView().getItems().get(getIndex());
            if (p != null) {
                boolean isCheque = "CHEQUE".equalsIgnoreCase(p.getPaymentMode()) && !Boolean.TRUE.equals(p.getAdvancePaid());
                printBtn.setDisable(!isCheque);
                
                // Adjust appearance for disabled state
                if (!isCheque) {
                    printBtn.setOpacity(0.4);
                    String reason = Boolean.TRUE.equals(p.getAdvancePaid()) ? "Advance Payment" : p.getPaymentMode();
                    printBtn.setTooltip(new Tooltip("Printing restricted for " + reason));
                } else {
                    printBtn.setOpacity(1.0);
                    printBtn.setTooltip(new Tooltip("Print Cheque"));
                }
            }
            setGraphic(container);
        }
    }
}
