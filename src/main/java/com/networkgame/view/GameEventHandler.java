package com.networkgame.view;

import com.networkgame.controller.GameController;
import com.networkgame.model.*;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.service.audio.AudioManager;

/**
 * Handles mouse events and interactions in the game
 */
public class GameEventHandler {
    private GameState gameState;
    private GameController gameController;
    private Pane gamePane;
    
    // Fields for wire dragging
    private Port dragSourcePort;
    private Line tempConnectionLine;
    private boolean isDraggingWire = false;
    
    private ConnectionManager connectionManager;
    private DialogManager dialogManager;
    
    // Highlighted port
    private Port highlightedPort;
    
    // Map to store capacity labels for systems
    private Map<NetworkSystem, Label> systemCapacityLabels = new HashMap<>();
    
    // Last time wire lengths were updated (for throttling)
    private long lastWireLengthUpdate = 0;
    
    public GameEventHandler(GameState gameState, GameController gameController, Pane gamePane, 
                           ConnectionManager connectionManager, DialogManager dialogManager) {
        this.gameState = gameState;
        this.gameController = gameController;
        this.gamePane = gamePane;
        this.connectionManager = connectionManager;
        this.dialogManager = dialogManager;
    }
    
    /**
     * Set the system capacity labels map
     */
    public void setSystemCapacityLabels(Map<NetworkSystem, Label> capacityLabels) {
        this.systemCapacityLabels = capacityLabels;
    }
    
    /**
     * Set up event handlers for mouse interactions on game pane
     */
    public void setupEventHandlers() {
        gamePane.setOnMouseDragged(e -> {
            if (isDraggingWire && tempConnectionLine != null) {
                updateTempLineEndPosition(e.getX(), e.getY());
                connectionManager.updateTempLineColor(tempConnectionLine, dragSourcePort, e.getSceneX(), e.getSceneY());
            }
        });
        
        gamePane.setOnMouseReleased(e -> {
            if (isDraggingWire) {
                handleWireDragRelease(e);
            }
        });
    }
    
    /**
     * Handle wire drag release - attempt to create connection if valid
     */
    private void handleWireDragRelease(MouseEvent e) {
        Port targetPort = connectionManager.findPortAtPosition(e.getSceneX(), e.getSceneY());
        
        if (isValidConnection(targetPort)) {
            double wireLength = calculateWireLength(targetPort);
            
            if (wireLength <= gameState.getRemainingWireLength()) {
                connectionManager.connectPorts(dragSourcePort, targetPort);
                AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.CONNECTION_SUCCESS);
            } else {
                AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.PACKET_DAMAGE);
            }
        }
        
        cleanupDragOperation();
    }
    
    private boolean isValidConnection(Port targetPort) {
        return targetPort != null && 
               targetPort.isInput() && 
               !targetPort.isConnected() &&
               targetPort.getSystem() != dragSourcePort.getSystem() &&
               targetPort.getType() == dragSourcePort.getType();
    }
    
    private double calculateWireLength(Port targetPort) {
        double[] sourceCenter = connectionManager.getPortCenter(dragSourcePort);
        double[] targetCenter = connectionManager.getPortCenter(targetPort);
        return connectionManager.distance(sourceCenter[0], sourceCenter[1], 
                                       targetCenter[0], targetCenter[1]);
    }
    
    private void cleanupDragOperation() {
        gamePane.getChildren().remove(tempConnectionLine);
        tempConnectionLine = null;
        isDraggingWire = false;
        dragSourcePort = null;
    }
    
    /**
     * Setup event handlers for a port
     */
    public void setupPortEventHandlers(Port port) {
        Shape portShape = port.getShape();
        
        portShape.setOnMousePressed(e -> {
            if (!port.isInput() && !port.isConnected()) {
                startWireDrag(e, port);
            } else if (e.isSecondaryButtonDown()) {
                connectionManager.removePortConnections(port);
                e.consume();
            }
        });
        
        portShape.setOnMouseReleased(e -> {
            if (isDraggingWire) {
                handleWireDragRelease(e);
            }
        });
        
        portShape.setOnMouseDragged(e -> {
            if (isDraggingWire && tempConnectionLine != null) {
                Point2D localPoint = gamePane.sceneToLocal(e.getSceneX(), e.getSceneY());
                updateTempLineEndPosition(localPoint.getX(), localPoint.getY());
                connectionManager.updateTempLineColor(tempConnectionLine, dragSourcePort, e.getSceneX(), e.getSceneY());
                e.consume();
            }
        });
        
        setupPortHoverEffects(port, portShape);
    }
    
    private void setupPortHoverEffects(Port port, Shape portShape) {
        portShape.setOnMouseEntered(e -> {
            portShape.getStyleClass().add("highlighted-port");
            setPortStrokeStyle(portShape, 2.0, Color.WHITE);
            
            String tooltipText = getPortTooltipText(port);
            Tooltip tooltip = new Tooltip(tooltipText);
            Tooltip.install(portShape, tooltip);
            
            highlightPort(port);
        });
        
        portShape.setOnMouseExited(e -> {
            portShape.getStyleClass().remove("highlighted-port");
            setPortStrokeStyle(portShape, 1.0, Color.BLACK);
            clearHighlights();
        });
    }
    
    private void setPortStrokeStyle(Shape portShape, double strokeWidth, Color strokeColor) {
        if (portShape instanceof Rectangle) {
            ((Rectangle)portShape).setStrokeWidth(strokeWidth);
            ((Rectangle)portShape).setStroke(strokeColor);
        } else if (portShape instanceof Polygon) {
            ((Polygon)portShape).setStrokeWidth(strokeWidth);
            ((Polygon)portShape).setStroke(strokeColor);
        }
    }
    
    private String getPortTooltipText(Port port) {
        if (port.isInput()) {
            return port.isConnected() ? "Input Port (Already connected)" : "Input Port (Drag wire to here)";
        } else {
            return port.isConnected() ? 
                   "Output Port (Already connected)\nRight-click to remove connection" : 
                   "Output Port (Drag from here to create wire)\nRight-click to remove connections";
        }
    }
    
    /**
     * Start wire dragging from a specific port
     */
    private void startWireDrag(MouseEvent e, Port port) {
        dragSourcePort = port;
        isDraggingWire = true;
        
        createTempLine(e, port);
        ensureProperZOrdering();
        
        e.consume();
    }
    
    private void createTempLine(MouseEvent e, Port port) {
        tempConnectionLine = new Line();
        tempConnectionLine.setStrokeWidth(3);
        tempConnectionLine.setStroke(Color.BLUE);
        
        double[] portCenter = connectionManager.getPortCenter(port);
        Point2D localMousePoint = gamePane.sceneToLocal(e.getSceneX(), e.getSceneY());
        
        tempConnectionLine.setStartX(portCenter[0]);
        tempConnectionLine.setStartY(portCenter[1]);
        tempConnectionLine.setEndX(localMousePoint.getX());
        tempConnectionLine.setEndY(localMousePoint.getY());
        
        gamePane.getChildren().add(tempConnectionLine);
        tempConnectionLine.toBack();
    }
    
    private void updateTempLineEndPosition(double x, double y) {
        tempConnectionLine.setEndX(x);
        tempConnectionLine.setEndY(y);
    }
    
    private void ensureProperZOrdering() {
        // Bring all play buttons in front of their systems
        gameState.getSystems().forEach(this::bringSystemElementsToFront);
        
        // Bring all ports to the front
        gameState.getSystems().forEach(system -> {
            system.getInputPorts().forEach(p -> p.getShape().toFront());
            system.getOutputPorts().forEach(p -> p.getShape().toFront());
        });
    }
    
    private void bringSystemElementsToFront(NetworkSystem system) {
        if (system.isStartSystem() && system.getPlayButton() != null) {
            system.getPlayButton().toFront();
        }
        system.getIndicatorLamp().toFront();
    }
    
    /**
     * Make a system draggable
     */
    public void makeSystemDraggable(NetworkSystem system, Shape systemShape) {
        if (system.isFixedPosition()) {
            return;
        }
        
        class Delta { double x, y; }
        final Delta dragDelta = new Delta();
        
        systemShape.setOnMousePressed(e -> {
            dragDelta.x = systemShape.getTranslateX() - e.getSceneX();
            dragDelta.y = systemShape.getTranslateY() - e.getSceneY();
            systemShape.setCursor(Cursor.MOVE);
            
            systemShape.toFront();
            system.getAllPorts().forEach(port -> port.getShape().toFront());
        });
        
        systemShape.setOnMouseDragged(e -> {
            double newTranslateX = e.getSceneX() + dragDelta.x;
            double newTranslateY = e.getSceneY() + dragDelta.y;
            
            double deltaX = newTranslateX - systemShape.getTranslateX();
            double deltaY = newTranslateY - systemShape.getTranslateY();
            
            systemShape.setTranslateX(newTranslateX);
            systemShape.setTranslateY(newTranslateY);
            
            updatePortPositions(system, deltaX, deltaY);
            updateCapacityLabelPosition(system, deltaX, deltaY);
            
            // Throttle wire length updates during dragging for smoother UI
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWireLengthUpdate > 100) { // 100ms throttle
                connectionManager.updateWireLengths();
                lastWireLengthUpdate = currentTime;
            }
        });
        
        systemShape.setOnMouseReleased(e -> {
            systemShape.setCursor(Cursor.HAND);
            system.getAllPorts().forEach(connectionManager::updateConnectionsForPort);
            connectionManager.updateWireLengths();
        });
        
        systemShape.setOnMouseEntered(e -> systemShape.setCursor(Cursor.HAND));
    }
    
    private void updatePortPositions(NetworkSystem system, double deltaX, double deltaY) {
        system.getAllPorts().forEach(port -> {
            Shape portShape = port.getShape();
            portShape.setTranslateX(portShape.getTranslateX() + deltaX);
            portShape.setTranslateY(portShape.getTranslateY() + deltaY);
            connectionManager.updateConnectionsForPort(port);
        });
    }
    
    private void updateCapacityLabelPosition(NetworkSystem system, double deltaX, double deltaY) {
        Label capacityLabel = systemCapacityLabels.get(system);
        if (capacityLabel != null) {
            capacityLabel.setTranslateX(capacityLabel.getTranslateX() + deltaX);
            capacityLabel.setTranslateY(capacityLabel.getTranslateY() + deltaY);
        }
    }
    
    /**
     * Add click handler to start button
     */
    public void setupStartButtonHandler(Group playButton) {
        playButton.setOnMouseClicked(e -> checkConnections());
    }
    
    /**
     * Check if all systems are connected properly and start the game flow
     */
    public void checkConnections() {
        NetworkSystem[] endSystems = {null};
        NetworkSystem[] startSystems = {null};
        
        boolean allSystemsReady = gameState.getSystems().stream()
            .allMatch(system -> {
                if (system.isEndSystem()) endSystems[0] = system;
                if (system.isStartSystem()) startSystems[0] = system;
                return system.isIndicatorOn();
            });
        
        NetworkSystem endSystem = endSystems[0];
        NetworkSystem startSystem = startSystems[0];
        
        if (endSystem == null) {
            dialogManager.showErrorMessage("All indicators must be turned on!");
            return;
        }
        
        if (startSystem == null) {
            dialogManager.showErrorMessage("Start system not found!");
            return;
        }
        
        if (!allSystemsReady) {
            dialogManager.showErrorMessage("All systems must be properly connected first!");
            return;
        }
        
        // Check for wire collisions with systems
        if (hasWireCollisions()) {
            dialogManager.showErrorMessage("Network cannot be started! Some wires pass through systems. Use bend points to route around them.");
            return;
        }
        
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.CONNECTION_SUCCESS);
        gameController.resumeGame();
        startSystem.startSendingPackets(2.0);
    }
    
    /**
     * Check if any connections have collisions with systems
     */
    private boolean hasWireCollisions() {
        for (Connection connection : gameState.getConnections()) {
            if (gameState.getWireCollisionManager().hasCollisions(connection)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Highlight a specific port
     */
    public void highlightPort(Port port) {
        clearHighlights();
        port.getShape().getStyleClass().add("highlighted-port");
        this.highlightedPort = port;
    }
    
    /**
     * Clear all port highlights
     */
    public void clearHighlights() {
        if (highlightedPort != null) {
            highlightedPort.getShape().getStyleClass().remove("highlighted-port");
            highlightedPort = null;
        }
    }
    
    /**
     * Check if user is currently dragging a wire
     */
    public boolean isDraggingWire() {
        return isDraggingWire;
    }
    
    /**
     * Set the dragging wire state
     */
    public void setDraggingWire(boolean dragging) {
        this.isDraggingWire = dragging;
    }
    
    /**
     * Get the source port for current wire drag operation
     */
    public Port getDragSourcePort() {
        return dragSourcePort;
    }
    
    /**
     * Get the temporary connection line for current wire drag operation
     */
    public Line getTempConnectionLine() {
        return tempConnectionLine;
    }
} 