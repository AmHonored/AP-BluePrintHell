package view.components.systems;

import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import model.entity.systems.IntermediateSystem;

public class IntermediateSystemView extends SystemView {
    private Label normalLabel;
    private Label capacityLabel;
    private int currentCapacity = 0;
    private final int maxCapacity = 5;
    
    public IntermediateSystemView(IntermediateSystem system) {
        super(system, "");  // No bottom label for intermediate systems
    }
    
    @Override
    protected void applySystemStyling() {
        systemRectangle.getStyleClass().add("system-normal");
    }
    
    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        
        // Create "Normal" label inside the system
        normalLabel = new Label("Normal");
        normalLabel.getStyleClass().add("system-label");
        normalLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        
        // Create capacity label
        capacityLabel = new Label(currentCapacity + "/" + maxCapacity);
        capacityLabel.getStyleClass().addAll("capacity-label", "capacity-normal");
        
        // Position labels
        StackPane.setAlignment(normalLabel, Pos.CENTER);
        StackPane.setAlignment(capacityLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(capacityLabel, new javafx.geometry.Insets(0, 0, 10, 0));
        
        content.getChildren().addAll(normalLabel, capacityLabel);
        
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
    
    public Label getNormalLabel() {
        return normalLabel;
    }
} 