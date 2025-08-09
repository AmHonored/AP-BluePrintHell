package view.components.systems;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.entity.systems.MergeSystem;

public class MergeSystemView extends SystemView {
    private Label mergeLabel;
    private Label circleCountLabel;
    private Label rectCountLabel;
    
    public MergeSystemView(MergeSystem system) {
        super(system, "");
    }

    @Override
    protected void applySystemStyling() {
        // Base like normal system + white glowing border to distinguish
        systemRectangle.getStyleClass().clear();
        systemRectangle.getStyleClass().add("system-normal");
        systemRectangle.setStyle(
            "-fx-fill: #333333;" +
            "-fx-stroke: #ffffff;" +
            "-fx-stroke-width: 3;" +
            "-fx-effect: dropshadow(gaussian, rgba(255, 255, 255, 0.9), 15, 0, 0, 0);"
        );
    }

    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        
        VBox vbox = new VBox(5);
        vbox.setAlignment(Pos.CENTER);
        
        // Create "Merge" label
        mergeLabel = new Label("Merge");
        mergeLabel.getStyleClass().add("system-label");
        mergeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14;");
        
        // Create count labels for bit packets (use better symbols)
        circleCountLabel = new Label("● 0/8");
        circleCountLabel.getStyleClass().add("bit-count-label");
        circleCountLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10;");
        
        rectCountLabel = new Label("■ 0/10");
        rectCountLabel.getStyleClass().add("bit-count-label");
        rectCountLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10;");
        
        vbox.getChildren().addAll(mergeLabel, circleCountLabel, rectCountLabel);
        content.getChildren().add(vbox);
        
        return content;
    }
    
    /**
     * Update the bit packet counts display
     */
    public void updateCounts(int circleCount, int rectCount) {
        if (circleCountLabel != null) {
            circleCountLabel.setText("● " + circleCount + "/8");
            // Highlight when ready to merge
            if (circleCount >= 8) {
                circleCountLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 10; -fx-font-weight: bold;");
            } else {
                circleCountLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10;");
            }
        }
        
        if (rectCountLabel != null) {
            rectCountLabel.setText("■ " + rectCount + "/10");
            // Highlight when ready to merge
            if (rectCount >= 10) {
                rectCountLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 10; -fx-font-weight: bold;");
            } else {
                rectCountLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10;");
            }
        }
    }
    
    /**
     * Get the underlying Merge system
     */
    public MergeSystem getMergeSystem() {
        return (MergeSystem) system;
    }
}
