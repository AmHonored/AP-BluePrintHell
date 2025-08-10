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
        super(system, ""); 
    }
    
    @Override
    protected void applySystemStyling() {
        systemRectangle.getStyleClass().add("system-normal");
        
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
        
        spyLabel = new Label("Spy");
        spyLabel.getStyleClass().add("system-label");
        spyLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold; -fx-font-size: 14;");
        
        capacityLabel = new Label(currentCapacity + "/" + maxCapacity);
        capacityLabel.getStyleClass().addAll("capacity-label", "capacity-normal");
        
        StackPane.setAlignment(spyLabel, Pos.CENTER);
        StackPane.setAlignment(capacityLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(capacityLabel, new javafx.geometry.Insets(0, 0, 10, 0));
        
        content.getChildren().addAll(spyLabel, capacityLabel);
        
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
}
