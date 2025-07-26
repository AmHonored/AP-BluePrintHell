package view.components.ports;

import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import model.entity.ports.Port;

public class TrianglePortView extends PortView {
    private static final double PORT_SIZE = 15;
    private final Polygon triangle;

    public TrianglePortView(Port modelPort, boolean isInput) {
        super(modelPort);
        triangle = new Polygon();
        // Equilateral triangle centered at (0,0)
        double h = Math.sqrt(3) / 2 * PORT_SIZE;
        triangle.getPoints().addAll(
            0.0, -h / 2, // Top
            -PORT_SIZE / 2, h / 2, // Bottom left
            PORT_SIZE / 2, h / 2 // Bottom right
        );
        triangle.setFill(isInput ? Color.LIGHTGREEN : Color.GREEN);
        triangle.setStroke(Color.BLACK);
        triangle.getStyleClass().add("port");
        this.getChildren().add(triangle);
    }

    public void highlight() {
        triangle.getStyleClass().add("highlighted-port");
    }

    public void unhighlight() {
        triangle.getStyleClass().remove("highlighted-port");
    }
}
