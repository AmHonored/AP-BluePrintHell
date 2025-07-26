package view.components.systems;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import model.entity.systems.System;
import model.entity.ports.Port;
import model.entity.ports.SquarePort;
import model.entity.ports.TrianglePort;
import model.entity.ports.HexagonPort;
import view.components.ports.PortView;
import view.components.ports.SquarePortView;
import view.components.ports.TrianglePortView;
import view.components.ports.HexagonPortView;
import java.util.ArrayList;
import java.util.List;

public abstract class SystemView extends StackPane {
    // Standard dimensions for all systems
    public static final double SYSTEM_WIDTH = 80;
    public static final double SYSTEM_HEIGHT = 100;
    public static final double INDICATOR_RADIUS = 4;
    public static final double PORT_SIZE = 10;
    
    protected final System system;
    protected final Rectangle systemRectangle;
    protected final Circle indicatorLamp;
    protected final Label systemLabel;
    protected final VBox container;
    protected final List<PortView> inputPortViews;
    protected final List<PortView> outputPortViews;
    
    public SystemView(System system, String labelText) {
        this.system = system;
        this.inputPortViews = new ArrayList<>();
        this.outputPortViews = new ArrayList<>();
        
        // Create main container
        container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(5);
        
        // Create system rectangle
        systemRectangle = new Rectangle(SYSTEM_WIDTH, SYSTEM_HEIGHT);
        systemRectangle.setArcWidth(10);
        systemRectangle.setArcHeight(10);
        
        // Create indicator lamp
        indicatorLamp = new Circle(INDICATOR_RADIUS);
        indicatorLamp.getStyleClass().add("indicator-lamp-off");
        
        // Create system label
        systemLabel = new Label(labelText);
        systemLabel.getStyleClass().add("system-label");
        
        // Position components
        StackPane systemContainer = new StackPane();
        systemContainer.getChildren().addAll(systemRectangle, getSystemContent());
        
        // Add indicator lamp to top-right of system
        StackPane.setAlignment(indicatorLamp, Pos.TOP_RIGHT);
        StackPane.setMargin(indicatorLamp, new javafx.geometry.Insets(5, 5, 0, 0));
        systemContainer.getChildren().add(indicatorLamp);
        
        container.getChildren().addAll(systemContainer, systemLabel);
        this.getChildren().add(container);
        
        // Create and add ports
        createPorts();
        
        // Set position
        updatePosition();
        
        // Apply initial styling
        applySystemStyling();
    }
    
    /**
     * Create and position ports for this system
     */
    private void createPorts() {
        // Create input ports (left side)
        for (int i = 0; i < system.getInPorts().size(); i++) {
            Port port = system.getInPorts().get(i);
            PortView portView = createPortView(port);
            inputPortViews.add(portView);
            
            // Position input ports on the left border (relative to system center)
            double yOffset = (i + 1) * (SYSTEM_HEIGHT / (system.getInPorts().size() + 1));
            portView.setLayoutX(-PORT_SIZE);
            portView.setLayoutY(yOffset - SYSTEM_HEIGHT / 2);
        }
        
        // Create output ports (right side)
        for (int i = 0; i < system.getOutPorts().size(); i++) {
            Port port = system.getOutPorts().get(i);
            PortView portView = createPortView(port);
            outputPortViews.add(portView);
            
            // Position output ports on the right border (relative to system center)
            double yOffset = (i + 1) * (SYSTEM_HEIGHT / (system.getOutPorts().size() + 1));
            portView.setLayoutX(SYSTEM_WIDTH);
            portView.setLayoutY(yOffset - SYSTEM_HEIGHT / 2);
        }
    }
    
    /**
     * Create a port view based on the port type
     */
    private PortView createPortView(Port port) {
        boolean isInput = port.getType() == model.entity.ports.PortType.INPUT;
        if (port instanceof SquarePort) {
            return new SquarePortView(port, isInput);
        } else if (port instanceof TrianglePort) {
            return new TrianglePortView(port, isInput);
        } else if (port instanceof HexagonPort) {
            return new HexagonPortView(port, isInput);
        } else {
            return new PortView(port);
        }
    }
    
    protected abstract void applySystemStyling();
    
    protected abstract StackPane getSystemContent();
    
    public void updatePosition() {
        if (system != null) {
            this.setLayoutX(system.getPosition().getX() - SYSTEM_WIDTH / 2);
            this.setLayoutY(system.getPosition().getY() - SYSTEM_HEIGHT / 2);
        }
    }
    
    public void updateIndicatorLamp(boolean isConnected) {
        indicatorLamp.getStyleClass().clear();
        if (isConnected) {
            indicatorLamp.getStyleClass().add("indicator-lamp-on");
        } else {
            indicatorLamp.getStyleClass().add("indicator-lamp-off");
        }
    }
    
    public void updateReady(boolean ready) {
        system.setReady(ready);
        updateIndicatorLamp(ready);
    }
    
    /**
     * Check if all ports of this system are connected
     */
    public boolean areAllPortsConnected() {
        // Check input ports
        for (model.entity.ports.Port port : system.getInPorts()) {
            if (!port.isConnected()) {
                return false;
            }
        }
        
        // Check output ports
        for (model.entity.ports.Port port : system.getOutPorts()) {
            if (!port.isConnected()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Update the indicator based on connection status
     */
    public void updateConnectionStatus() {
        boolean allConnected = areAllPortsConnected();
        system.setReady(allConnected);
        updateIndicatorLamp(allConnected);
    }
    
    public System getSystem() {
        return system;
    }
    
    public Rectangle getSystemRectangle() {
        return systemRectangle;
    }
    
    public Label getSystemLabel() {
        return systemLabel;
    }
    
    public List<PortView> getInputPortViews() {
        return inputPortViews;
    }
    
    public List<PortView> getOutputPortViews() {
        return outputPortViews;
    }
    
    /**
     * Set wire controller for all ports
     */
    public void setWireController(controller.WireController wireController) {
        for (PortView portView : inputPortViews) {
            portView.setWireController(wireController);
        }
        for (PortView portView : outputPortViews) {
            portView.setWireController(wireController);
        }
    }
}
