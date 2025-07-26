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
        // Mouse events for drag handling
        this.setOnMousePressed(this::handleMousePressed);
        this.setOnMouseDragged(this::handleMouseDragged);
        this.setOnMouseReleased(this::handleMouseReleased);
        // Hover highlight logic
        this.setOnMouseEntered(e -> highlight());
        this.setOnMouseExited(e -> unhighlight());
    }

    public Port getModelPort() {
        return modelPort;
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {
            // Just store the initial press position, don't start dragging yet
            pressedX = event.getSceneX();
            pressedY = event.getSceneY();
            isDragStarted = false;
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (event.isPrimaryButtonDown() && !isDragStarted) {
            // Calculate distance moved to determine if this is a real drag
            double deltaX = event.getSceneX() - pressedX;
            double deltaY = event.getSceneY() - pressedY;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            
            // Start dragging only if moved more than 5 pixels (drag threshold) and this is an output port
            if (distance > 5.0 && wireController != null && modelPort.getType() == model.entity.ports.PortType.OUTPUT) {
                wireController.startWireDrag(this, pressedX, pressedY);
                isDragStarted = true;
            }
        }
        
        // Continue updating drag if already started
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

    // Default highlight/unhighlight for base PortView (no-op, overridden in subclasses)
    public void highlight() {}
    public void unhighlight() {}

    /**
     * Set the wire controller for this port view
     */
    public void setWireController(WireController wireController) {
        this.wireController = wireController;
    }

    /**
     * Get the wire controller
     */
    public WireController getWireController() {
        return wireController;
    }
}
