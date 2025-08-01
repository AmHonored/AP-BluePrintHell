package view.components.systems;

import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import model.entity.systems.SpySystem;

public class SpySystemView extends SystemView {
    private Label spyLabel;
    private Label capacityLabel;
    private int currentCapacity = 0;
    private final int maxCapacity = 5;
    
    public SpySystemView(SpySystem system) {
        super(system, "");  // No bottom label for spy systems
    }
    
    @Override
    protected void applySystemStyling() {
        // Apply normal system styling with red glowing border (like DDoS but red)
        systemRectangle.getStyleClass().add("system-normal");
        
        // Add red glow effect similar to DDoS system
        systemRectangle.setStyle(
            "-fx-fill: #333333;" +
            "-fx-stroke: #ff0000;" +
            "-fx-stroke-width: 3;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,0,0,0.8), 15, 0, 0, 0);"
        );
    }
    
    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        
        // Create "Spy" label inside the system
        spyLabel = new Label("Spy");
        spyLabel.getStyleClass().add("system-label");
        spyLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold; -fx-font-size: 14;");
        
        // Create capacity label
        capacityLabel = new Label(currentCapacity + "/" + maxCapacity);
        capacityLabel.getStyleClass().addAll("capacity-label", "capacity-normal");
        
        // Position labels
        StackPane.setAlignment(spyLabel, Pos.CENTER);
        StackPane.setAlignment(capacityLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(capacityLabel, new javafx.geometry.Insets(0, 0, 10, 0));
        
        content.getChildren().addAll(spyLabel, capacityLabel);
        
        return content;
    }
    
    public void updateCapacity(int newCapacity) {
        if (capacityLabel != null) {
            String oldText = capacityLabel.getText();
            String newText = newCapacity + "/" + maxCapacity;
            capacityLabel.setText(newText);
            
            // Update visibility based on capacity
            boolean shouldBeVisible = newCapacity > 0;
            capacityLabel.setVisible(shouldBeVisible);
        }
    }
    
    public int getCurrentCapacity() {
        return currentCapacity;
    }
    
    public int getMaxCapacity() {
        return maxCapacity;
    }
    
    public Label getCapacityLabel() {
        return capacityLabel;
    }
    
    public Label getSpyLabel() {
        return spyLabel;
    }
}
