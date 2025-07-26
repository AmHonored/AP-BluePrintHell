package controller;

import javafx.scene.input.MouseEvent;
import view.components.ports.PortView;
import view.components.wires.WireView;
import javafx.geometry.Point2D;
import view.game.GameScene;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Line;
import javafx.scene.shape.CubicCurve;
import model.entity.ports.Port;
import model.entity.ports.PortType;
import model.wire.Wire;
import java.util.UUID;
import model.levels.Level;
import view.game.HUDScene;
import manager.game.ConnectionManager;

public class WireController {
    // Drag state
    private PortView sourcePort = null;
    private PortView targetPort = null;
    private WireView tempWireView = null;
    private boolean dragging = false;
    private Point2D currentMousePosition = null;
    private GameScene gameScene = null;
    private Pane gamePane = null;
    private Level level;
    private HUDScene hud;
    private ConnectionManager connectionManager;
    private Runnable connectionChangeCallback;

    public void setConnectionChangeCallback(Runnable callback) {
        this.connectionChangeCallback = callback;
    }

    public void setLevel(Level level) { this.level = level; }
    public void setHUD(HUDScene hud) { this.hud = hud; }
    public void setGameScene(GameScene gameScene) { this.gameScene = gameScene; }
    public void setGamePane(Pane gamePane) { this.gamePane = gamePane; }
    public void setConnectionManager(ConnectionManager cm) { this.connectionManager = cm; }

    public void startWireDrag(PortView source, double sceneX, double sceneY) {
        if (dragging) return;
        this.sourcePort = source;
        this.dragging = true;
        this.currentMousePosition = new Point2D(sceneX, sceneY);
        if (gameScene != null || gamePane != null) {
            tempWireView = new WireView(new TempWireModel(source.getModelPort(), sceneX, sceneY));
            tempWireView.setDragging();
            Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
            targetPane.getChildren().add(tempWireView);

        }
        if (level != null && hud != null && connectionManager != null) {
            double rem = connectionManager.getRemainingWireLength();
            hud.getWireBox().setValue(String.format("%.1f", rem));
            if (rem <= 0 && tempWireView != null) {
                tempWireView.setOutOfWire(true);
            }
        }

    }

    public void updateWireDrag(double sceneX, double sceneY, PortView hoveredPort) {
        if (!dragging || tempWireView == null) return;
        this.currentMousePosition = new Point2D(sceneX, sceneY);
        

        
        // Update the wire end position directly on the shape
        Shape wireShape = tempWireView.getWireShape();
        if (wireShape != null) {
            Point2D start = sourcePort.getModelPort().getPosition();
            
            // Convert scene coordinates to local coordinates relative to the wire's parent
            Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
            Point2D localEndPoint = targetPane.sceneToLocal(sceneX, sceneY);
            
            if (wireShape instanceof javafx.scene.shape.Line) {
                javafx.scene.shape.Line line = (javafx.scene.shape.Line) wireShape;
                line.setStartX(start.getX());
                line.setStartY(start.getY());
                line.setEndX(localEndPoint.getX());
                line.setEndY(localEndPoint.getY());
            } else if (wireShape instanceof javafx.scene.shape.CubicCurve) {
                javafx.scene.shape.CubicCurve curve = (javafx.scene.shape.CubicCurve) wireShape;
                curve.setStartX(start.getX());
                curve.setStartY(start.getY());
                curve.setEndX(localEndPoint.getX());
                curve.setEndY(localEndPoint.getY());
                // Update control points for smooth curve
                double controlX1 = start.getX() + (localEndPoint.getX() - start.getX()) / 3;
                double controlY1 = start.getY();
                double controlX2 = localEndPoint.getX() - (localEndPoint.getX() - start.getX()) / 3;
                double controlY2 = localEndPoint.getY();
                curve.setControlX1(controlX1);
                curve.setControlY1(controlY1);
                curve.setControlX2(controlX2);
                curve.setControlY2(controlY2);
            }
        }
        
        // Visual feedback for hovered port
        if (hoveredPort != null && isValidTarget(sourcePort, hoveredPort)) {
            tempWireView.setValidTarget();
        } else {
            tempWireView.setDragging(); // Red color during dragging
        }
    }

    public void finishWireDrag(PortView target, double sceneX, double sceneY) {
        if (!dragging) return;
        
        // Find the actual port at the mouse position instead of using the target parameter
        // because the target parameter might not be accurate due to event bubbling
        PortView actualTarget = null;
        if (gameScene != null || gamePane != null) {
            Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
            Point2D localPoint = targetPane.sceneToLocal(sceneX, sceneY);
            
            for (javafx.scene.Node node : targetPane.getChildren()) {
                if (node instanceof view.components.ports.PortView) {
                    view.components.ports.PortView portView = (view.components.ports.PortView) node;
                    if (portView.getBoundsInParent().contains(localPoint.getX(), localPoint.getY())) {
                        actualTarget = portView;
                        break;
                    }
                }
            }
        }
        
        this.targetPort = actualTarget;
        this.currentMousePosition = new Point2D(sceneX, sceneY);
        
        boolean valid = false;
        if (sourcePort != null && targetPort != null && sourcePort != targetPort) {
            Port src = sourcePort.getModelPort();
            Port dst = targetPort.getModelPort();
            
            // Debug logging for connection attempts
            System.out.println("🔌 CONNECTION ATTEMPT: " + src.getId() + " (" + src.getClass().getSimpleName() + ") → " + 
                              dst.getId() + " (" + dst.getClass().getSimpleName() + ")");
            
            if (isValidTarget(sourcePort, targetPort)) {
                System.out.println("✅ CONNECTION VALID: Creating wire between " + src.getId() + " and " + dst.getId());
                Wire wire = new Wire(UUID.randomUUID().toString(), src, dst);
                if (connectionManager != null && connectionManager.canAddWire(wire)) {
                    src.setWire(wire);
                    dst.setWire(wire);
                    connectionManager.addWire(wire);
                    if (hud != null) hud.getWireBox().setValue(String.format("%.1f", connectionManager.getRemainingWireLength()));
                    
                    // Play connection success sound
                    service.AudioManager.playConnectionSuccess();
                    
                    // Notify that connection status has changed
                    if (connectionChangeCallback != null) {
                        connectionChangeCallback.run();
                    }
                    
                    if (gameScene != null || gamePane != null) {
                        WireView permanentWireView = new WireView(wire);
                        permanentWireView.setOnRemove(() -> {
                            src.setWire(null);
                            dst.setWire(null);
                            connectionManager.removeWire(wire);
                            if (hud != null) hud.getWireBox().setValue(String.format("%.1f", connectionManager.getRemainingWireLength()));
                            
                            // Notify that connection status has changed
                            if (connectionChangeCallback != null) {
                                connectionChangeCallback.run();
                            }
                            
                            Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
                            targetPane.getChildren().remove(permanentWireView);
                        });
                        Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
                        targetPane.getChildren().add(permanentWireView);
                    }
                    valid = true;
                } else {
                    System.out.println("❌ CONNECTION REJECTED: Not enough wire length available");
                    if (tempWireView != null) {
                        tempWireView.setOutOfWire(true);
                    }
                }
            } else {
                System.out.println("❌ CONNECTION REJECTED: Invalid port combination (different types or already connected)");
            }
        }
        if (tempWireView != null) {
            Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
            if (targetPane != null) {
                targetPane.getChildren().remove(tempWireView);
            }
        }
        this.dragging = false;
        this.sourcePort = null;
        this.targetPort = null;
        this.tempWireView = null;
        this.currentMousePosition = null;

    }

    private boolean isValidTarget(PortView source, PortView target) {
        if (source == null || target == null) return false;
        Port src = source.getModelPort();
        Port dst = target.getModelPort();
        
        // Check port types (INPUT/OUTPUT)
        if (src.getType() != PortType.OUTPUT || dst.getType() != PortType.INPUT) return false;
        
        // Check if ports are already connected
        if (src.isConnected() || dst.isConnected()) return false;
        
        // Check if ports are from the same system
        if (src.getSystem() == dst.getSystem()) return false;
        
        // CRITICAL: Check if ports are of the same packet type (SquarePort vs TrianglePort)
        if (src.getClass() != dst.getClass()) return false;
        
        return true;
    }

    public boolean isDragging() { return dragging; }
    public PortView getSourcePort() { return sourcePort; }
    public PortView getTargetPort() { return targetPort; }
    public Point2D getCurrentMousePosition() { return currentMousePosition; }
    public WireView getTempWireView() { return tempWireView; }
    public void setTempWireView(WireView tempWireView) { this.tempWireView = tempWireView; }

    private static class TempWireModel extends model.wire.Wire {
        private Point2D dynamicEnd;
        public TempWireModel(Port source, double endX, double endY) {
            super("temp", source, null);
            this.dynamicEnd = new Point2D(endX, endY);
        }
        public void setEnd(double x, double y) { this.dynamicEnd = new Point2D(x, y); }
        @Override
        public Port getDest() { return new DummyPort(dynamicEnd); }
    }
    private static class DummyPort extends Port {
        private final Point2D pos;
        public DummyPort(Point2D pos) { super("dummy", null, PortType.INPUT, pos); this.pos = pos; }
        @Override public boolean isCompatible(model.entity.packets.Packet packet) { return false; }
        @Override public Point2D getPosition() { return pos; }
    }
}
