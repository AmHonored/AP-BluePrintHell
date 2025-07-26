package view.components.systems;

import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import model.entity.systems.EndSystem;

public class EndSystemView extends SystemView {
    private Label endLabel;
    
    public EndSystemView(EndSystem system) {
        super(system, "END");
    }
    
    @Override
    protected void applySystemStyling() {
        systemRectangle.getStyleClass().add("system-end");
    }
    
    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        
        // Add END label inside the system
        endLabel = new Label("END");
        endLabel.getStyleClass().add("system-label");
        endLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        content.getChildren().add(endLabel);
        StackPane.setAlignment(endLabel, Pos.CENTER);
        
        return content;
    }
    
    public Label getEndLabel() {
        return endLabel;
    }
}
