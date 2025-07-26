package view.components.ports;

import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import model.entity.ports.Port;

public class HexagonPortView extends PortView {
    private static final double PORT_SIZE = 6; // Reduced from 12 to 8 to make it smaller
    private final Polygon hexagon;

    public HexagonPortView(Port modelPort, boolean isInput) {
        super(modelPort);
        hexagon = new Polygon();
        
        // Create a regular hexagon (6 sides, 60° angles)
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3; // 60 degrees each
            double x = PORT_SIZE * Math.cos(angle);
            double y = PORT_SIZE * Math.sin(angle);
            hexagon.getPoints().addAll(x, y);
        }
        
        // Set colors: light gray for input, dark gray for output
        hexagon.setFill(isInput ? Color.LIGHTGRAY : Color.DARKGRAY);
        hexagon.setStroke(Color.BLACK);
        hexagon.getStyleClass().add("port");
        this.getChildren().add(hexagon);
    }

    @Override
    public void highlight() {
        hexagon.getStyleClass().add("highlighted-port");
    }

    @Override
    public void unhighlight() {
        hexagon.getStyleClass().remove("highlighted-port");
    }
}
