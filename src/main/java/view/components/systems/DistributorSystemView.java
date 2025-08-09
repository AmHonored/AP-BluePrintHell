package view.components.systems;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.entity.systems.DistributorSystem;

public class DistributorSystemView extends SystemView {
    public DistributorSystemView(DistributorSystem system) {
        super(system, "");
    }

    @Override
    protected void applySystemStyling() {
        // Base like normal system + pink glowing border to distinguish
        systemRectangle.getStyleClass().clear();
        systemRectangle.getStyleClass().add("system-normal");
        systemRectangle.setStyle(
            "-fx-fill: #333333;" +
            "-fx-stroke:rgb(232, 61, 195);" +
            "-fx-stroke-width: 3;" +
            "-fx-effect: dropshadow(gaussian, rgba(232, 61, 195), 15, 0, 0, 0);"
        );
    }

    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        Label lbl = new Label("Distributor");
        lbl.getStyleClass().add("system-label");
        StackPane.setAlignment(lbl, Pos.CENTER);
        content.getChildren().add(lbl);
        return content;
    }
}


