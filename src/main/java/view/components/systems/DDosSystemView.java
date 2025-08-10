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
        super(system, "");  
    }
    
    @Override
    protected void applySystemStyling() {
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
        
        ddosLabel = new Label("DDOS");
        ddosLabel.getStyleClass().add("system-label");
        ddosLabel.setStyle("-fx-text-fill: #ff6600; -fx-font-weight: bold; -fx-font-size: 14;");
        
        capacityLabel = new Label(currentCapacity + "/" + maxCapacity);
        capacityLabel.getStyleClass().addAll("capacity-label", "capacity-normal");
        
        StackPane.setAlignment(ddosLabel, Pos.CENTER);
        StackPane.setAlignment(capacityLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(capacityLabel, new javafx.geometry.Insets(0, 0, 10, 0));
        
        content.getChildren().addAll(ddosLabel, capacityLabel);
        
        return content;
    }
    
    public void updateCapacity(int newCapacity) {
        if (capacityLabel != null) {
            String newText = newCapacity + "/" + maxCapacity;
            capacityLabel.setText(newText);
            
            boolean shouldBeVisible = newCapacity > 0;
            capacityLabel.setVisible(shouldBeVisible);
        }
    }
    
    public DDosSystem getDDosSystem() {
        return (DDosSystem) system;
    }
}
