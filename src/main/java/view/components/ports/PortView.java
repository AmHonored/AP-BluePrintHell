package view.components.ports;

import javafx.scene.layout.StackPane;
import javafx.scene.input.MouseEvent;
import controller.WireController;
import model.entity.ports.Port;

public class PortView extends StackPane {
    private final Port modelPort;
    private WireController wireController;
    private boolean isDragStarted = false;
    private double pressedX, pressedY;

    public PortView(Port modelPort) {
        this.modelPort = modelPort;
        // Enable focus to receive keyboard events
        setFocusTraversable(true);

        this.setOnMousePressed(this::handleMousePressed);
        this.setOnMouseDragged(this::handleMouseDragged);
        this.setOnMouseReleased(this::handleMouseReleased);

        this.setOnMouseEntered(e -> highlight());
        this.setOnMouseExited(e -> unhighlight());

        this.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.R && wireController != null) {
                if (modelPort.getType() == model.entity.ports.PortType.OUTPUT && modelPort.isConnected()) {
                    wireController.removeWireFromOutputPort(this);
                    event.consume();
                }
            }
        });
    }

    public Port getModelPort() {
        return modelPort;
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {

            pressedX = event.getSceneX();
            pressedY = event.getSceneY();
            isDragStarted = false;
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (event.isPrimaryButtonDown() && !isDragStarted) {

            double deltaX = event.getSceneX() - pressedX;
            double deltaY = event.getSceneY() - pressedY;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            
            if (distance > 5.0 && wireController != null && modelPort.getType() == model.entity.ports.PortType.OUTPUT) {
                wireController.startWireDrag(this, pressedX, pressedY);
                isDragStarted = true;
            }
        }
        
        if (isDragStarted && wireController != null && wireController.isDragging()) {
            wireController.updateWireDrag(event.getSceneX(), event.getSceneY(), null);
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (isDragStarted && wireController != null && wireController.isDragging()) {
            wireController.finishWireDrag(this, event.getSceneX(), event.getSceneY());
        }
        isDragStarted = false;
    }

    public void highlight() {}
    public void unhighlight() {}

    public void setWireController(WireController wireController) {
        this.wireController = wireController;
    }

    
}
