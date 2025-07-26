package view.components.ports;

import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import model.entity.ports.Port;

public class SquarePortView extends PortView {
    private static final double PORT_SIZE = 10;
    private final Rectangle square;

    public SquarePortView(Port modelPort, boolean isInput) {
        super(modelPort);
        square = new Rectangle(PORT_SIZE, PORT_SIZE);
        square.setFill(isInput ? Color.LIGHTBLUE : Color.BLUE);
        square.setStroke(Color.BLACK);
        square.getStyleClass().add("port");
        this.getChildren().add(square);
    }

    public void highlight() {
        square.getStyleClass().add("highlighted-port");
    }

    public void unhighlight() {
        square.getStyleClass().remove("highlighted-port");
    }
}
