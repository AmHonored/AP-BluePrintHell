package view.components.ports;

import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import model.entity.ports.Port;

public class SquarePortView extends PortView {
    private static final double PORT_SIZE = 10;
    private final Rectangle square;

    public SquarePortView(Port modelPort) {
        super(modelPort);
        square = new Rectangle(PORT_SIZE, PORT_SIZE);
        boolean isInput = getModelPort().getType() == model.entity.ports.PortType.INPUT;
        square.setFill(isInput ? Color.LIGHTBLUE : Color.BLUE);
        square.setStroke(Color.BLACK);
        square.getStyleClass().add("port");
        this.getChildren().add(square);
    }

    @Override
    public void highlight() {
        square.getStyleClass().add("highlighted-port");
    }

    @Override
    public void unhighlight() {
        square.getStyleClass().remove("highlighted-port");
    }
}
