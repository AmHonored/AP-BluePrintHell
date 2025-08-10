package view.components.systems;

import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import model.entity.systems.AntiVirusSystem;

public class AntiVirusSystemView extends SystemView {
    private Label antivirusLabel;
    private Label capacityLabel;
    private Label statusLabel;
    private int currentCapacity = 0;
    private final int maxCapacity = 5;
    
    public AntiVirusSystemView(AntiVirusSystem system) {
        super(system, ""); 
    }
    
    @Override
    protected void applySystemStyling() {
        systemRectangle.getStyleClass().add("system-normal");
        
        systemRectangle.setStyle(
            "-fx-fill: #333333;" +
            "-fx-stroke: #ffff00;" +
            "-fx-stroke-width: 3;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,255,0,0.8), 15, 0, 0, 0);"
        );
    }
    
    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        
        antivirusLabel = new Label("AntiVirus");
        antivirusLabel.getStyleClass().add("system-label");
        antivirusLabel.setStyle("-fx-text-fill: #ffff00; -fx-font-weight: bold; -fx-font-size: 12;");
        
        capacityLabel = new Label(currentCapacity + "/" + maxCapacity);
        capacityLabel.getStyleClass().addAll("capacity-label", "capacity-normal");
        
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setStyle("-fx-text-fill: #ff6600; -fx-font-weight: bold; -fx-font-size: 10;");
        
        StackPane.setAlignment(antivirusLabel, Pos.CENTER);
        StackPane.setAlignment(capacityLabel, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(statusLabel, Pos.TOP_CENTER);
        StackPane.setMargin(capacityLabel, new javafx.geometry.Insets(0, 0, 10, 0));
        StackPane.setMargin(statusLabel, new javafx.geometry.Insets(5, 0, 0, 0));
        
        content.getChildren().addAll(antivirusLabel, capacityLabel, statusLabel);
        
        return content;
    }

    public void updateCapacity(int currentCapacity) {
        this.currentCapacity = currentCapacity;
        if (capacityLabel != null) {
            String newText = currentCapacity + "/" + maxCapacity;
            capacityLabel.setText(newText);
            
            // Update visibility based on capacity
            boolean shouldBeVisible = currentCapacity > 0;
            capacityLabel.setVisible(shouldBeVisible);
        }
    }
    
    public void updateDisabledStatus(boolean disabled, long remainingTimeMs) {
        if (statusLabel != null) {
            if (disabled && remainingTimeMs > 0) {
                long remainingSeconds = (remainingTimeMs + 999) / 1000; 
                statusLabel.setText("DISABLED " + remainingSeconds + "s");
                statusLabel.setVisible(true);
                
                systemRectangle.setStyle(
                    "-fx-fill: #222222;" +
                    "-fx-stroke: #888800;" +
                    "-fx-stroke-width: 3;" +
                    "-fx-effect: dropshadow(gaussian, rgba(136,136,0,0.4), 15, 0, 0, 0);"
                );
                antivirusLabel.setStyle("-fx-text-fill: #888800; -fx-font-weight: bold; -fx-font-size: 12;");
            } else {
                statusLabel.setText("");
                statusLabel.setVisible(false);
                
                applySystemStyling();
                antivirusLabel.setStyle("-fx-text-fill: #ffff00; -fx-font-weight: bold; -fx-font-size: 12;");
            }
        }
    }
    
    public AntiVirusSystem getAntiVirusSystem() {
        return (AntiVirusSystem) system;
    }
} 