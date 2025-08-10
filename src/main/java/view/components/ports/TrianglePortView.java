package view.components.ports;

import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import model.entity.ports.Port;

public class TrianglePortView extends PortView {
    private static final double PORT_SIZE = 15;
    private final Polygon triangle;

    public TrianglePortView(Port modelPort) {
        super(modelPort);
        triangle = new Polygon();
        double h = Math.sqrt(3) / 2 * PORT_SIZE;
        triangle.getPoints().addAll(
            0.0, -h / 2, // Top
            -PORT_SIZE / 2, h / 2, // Bottom left
            PORT_SIZE / 2, h / 2 // Bottom right
        );
        boolean isInput = getModelPort().getType() == model.entity.ports.PortType.INPUT;
        triangle.setFill(isInput ? Color.LIGHTGREEN : Color.GREEN);
        triangle.setStroke(Color.BLACK);
        triangle.getStyleClass().add("port");
        this.getChildren().add(triangle);
    }

    @Override
    public void highlight() {
        triangle.getStyleClass().add("highlighted-port");
    }

    @Override
    public void unhighlight() {
        triangle.getStyleClass().remove("highlighted-port");
    }
}
