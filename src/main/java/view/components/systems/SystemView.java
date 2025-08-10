package view.components.systems;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
        
        container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(5);
        
        systemRectangle = new Rectangle(SYSTEM_WIDTH, SYSTEM_HEIGHT);
        systemRectangle.setArcWidth(10);
        systemRectangle.setArcHeight(10);
        
        indicatorLamp = new Circle(INDICATOR_RADIUS);
        indicatorLamp.getStyleClass().add("indicator-lamp-off");
        
        systemLabel = new Label(labelText);
        systemLabel.getStyleClass().add("system-label");
        
        StackPane systemContainer = new StackPane();
        systemContainer.getChildren().addAll(systemRectangle, getSystemContent());
        
        StackPane.setAlignment(indicatorLamp, Pos.TOP_RIGHT);
        StackPane.setMargin(indicatorLamp, new javafx.geometry.Insets(5, 5, 0, 0));
        systemContainer.getChildren().add(indicatorLamp);
        
        container.getChildren().addAll(systemContainer, systemLabel);
        this.getChildren().add(container);
        
        createPorts();
        
        updatePosition();
        
        applySystemStyling();

        if (system instanceof model.entity.systems.DistributorSystem) {
            systemRectangle.getStyleClass().add("system-distributor");
        }
    }
    
    private void createPorts() {

        for (int i = 0; i < system.getInPorts().size(); i++) {
            Port port = system.getInPorts().get(i);
            PortView portView = createPortView(port);
            inputPortViews.add(portView);
            
            double yOffset = (i + 1) * (SYSTEM_HEIGHT / (system.getInPorts().size() + 1));
            portView.setLayoutX(-PORT_SIZE);
            portView.setLayoutY(yOffset - SYSTEM_HEIGHT / 2);
        }
        
        for (int i = 0; i < system.getOutPorts().size(); i++) {
            Port port = system.getOutPorts().get(i);
            PortView portView = createPortView(port);
            outputPortViews.add(portView);
            
            double yOffset = (i + 1) * (SYSTEM_HEIGHT / (system.getOutPorts().size() + 1));
            portView.setLayoutX(SYSTEM_WIDTH);
            portView.setLayoutY(yOffset - SYSTEM_HEIGHT / 2);
        }
    }

    private PortView createPortView(Port port) {
        if (port instanceof SquarePort) {
            return new SquarePortView(port);
        } else if (port instanceof TrianglePort) {
            return new TrianglePortView(port);
        } else if (port instanceof HexagonPort) {
            return new HexagonPortView(port);
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

    public void setIndicatorWarning() {
        indicatorLamp.getStyleClass().clear();
        indicatorLamp.getStyleClass().add("indicator-lamp-warning");
    }
    
    public void updateReady(boolean ready) {
        system.setReady(ready);
        updateIndicatorLamp(ready);
    }
    

    public boolean areAllPortsConnected() {

        for (model.entity.ports.Port port : system.getInPorts()) {
            if (!port.isConnected()) {
                return false;
            }
        }
        
        for (model.entity.ports.Port port : system.getOutPorts()) {
            if (!port.isConnected()) {
                return false;
            }
        }
        
        return true;
    }

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
    
    public void setWireController(controller.WireController wireController) {
        for (PortView portView : inputPortViews) {
            portView.setWireController(wireController);
        }
        for (PortView portView : outputPortViews) {
            portView.setWireController(wireController);
        }
    }
}
