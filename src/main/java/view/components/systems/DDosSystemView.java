package view.components.systems;

import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import model.entity.systems.DDosSystem;

public class DDosSystemView extends SystemView {
    private Label ddosLabel;
    private Label capacityLabel;
    private int currentCapacity = 0;
    private final int maxCapacity = 5;
    
    public DDosSystemView(DDosSystem system) {
        super(system, "");  // No bottom label for DDoS systems
    }
    
    @Override
    protected void applySystemStyling() {
        // Apply normal system styling with orange glowing border
        systemRectangle.getStyleClass().add("system-normal");
        
        // Add orange glow effect
        systemRectangle.setStyle(
            "-fx-fill: #333333;" +
            "-fx-stroke: #ff6600;" +
            "-fx-stroke-width: 3;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,102,0,0.8), 15, 0, 0, 0);"
        );
    }
    
    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        
        // Create "DDOS" label inside the system
        ddosLabel = new Label("DDOS");
        ddosLabel.getStyleClass().add("system-label");
        ddosLabel.setStyle("-fx-text-fill: #ff6600; -fx-font-weight: bold; -fx-font-size: 14;");
        
        // Create capacity label
        capacityLabel = new Label(currentCapacity + "/" + maxCapacity);
        capacityLabel.getStyleClass().addAll("capacity-label", "capacity-normal");
        
        // Position labels
        StackPane.setAlignment(ddosLabel, Pos.CENTER);
        StackPane.setAlignment(capacityLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(capacityLabel, new javafx.geometry.Insets(0, 0, 10, 0));
        
        content.getChildren().addAll(ddosLabel, capacityLabel);
        
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
    
    public Label getDdosLabel() {
        return ddosLabel;
    }
    
    /**
     * Get the DDoS system model
     */
    public DDosSystem getDDosSystem() {
        return (DDosSystem) system;
    }
}
