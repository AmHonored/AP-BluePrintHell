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
import manager.game.WireBendManager;
import manager.game.ShopManager;

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
    private WireBendManager wireBendManager;
    private Runnable connectionChangeCallback;

    public void setConnectionChangeCallback(Runnable callback) {
        this.connectionChangeCallback = callback;
    }

    public void setLevel(Level level) { 
        this.level = level; 
        if (level != null && connectionManager != null) {
            this.wireBendManager = new WireBendManager(level, new ShopManager(level));
        }
    }
    
    public void setHUD(HUDScene hud) { this.hud = hud; }
    public void setGameScene(GameScene gameScene) { this.gameScene = gameScene; }
    public void setGamePane(Pane gamePane) { this.gamePane = gamePane; }
    public void setConnectionManager(ConnectionManager cm) { 
        this.connectionManager = cm; 
        if (level != null && cm != null) {
            this.wireBendManager = new WireBendManager(level, new ShopManager(level));
        }
    }

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
        
        // Update the wire end position directly on the first curve
        if (!tempWireView.getCurves().isEmpty()) {
            Shape wireShape = tempWireView.getCurves().get(0);
            Point2D start = sourcePort.getModelPort().getPosition();
            
            // Convert scene coordinates to local coordinates relative to the wire's parent
            Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
            Point2D localEndPoint = targetPane.sceneToLocal(sceneX, sceneY);
            
            if (wireShape instanceof javafx.scene.shape.QuadCurve) {
                javafx.scene.shape.QuadCurve curve = (javafx.scene.shape.QuadCurve) wireShape;
                curve.setStartX(start.getX());
                curve.setStartY(start.getY());
                curve.setEndX(localEndPoint.getX());
                curve.setEndY(localEndPoint.getY());
                // Set control point to midpoint for straight line during dragging
                double midX = (start.getX() + localEndPoint.getX()) / 2;
                double midY = (start.getY() + localEndPoint.getY()) / 2;
                curve.setControlX(midX);
                curve.setControlY(midY);
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
        
        // Find the actual port at the mouse position
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
            
            System.out.println("🔌 CONNECTION ATTEMPT: " + src.getId() + " → " + dst.getId());
            
            if (isValidTarget(sourcePort, targetPort)) {
                System.out.println("✅ CONNECTION VALID: Creating wire");
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
                        
                        // Set up bend point purchase callback
                        if (wireBendManager != null) {
                            permanentWireView.setOnBendPointPurchase(w -> wireBendManager.purchaseBendPoint(w));
                        }
                        
                        // Set up wire length change callback for real-time HUD updates
                        permanentWireView.setOnWireLengthChanged(() -> {
                            if (connectionManager != null) {
                                connectionManager.recalculateWireLengths();
                                if (hud != null) {
                                    hud.getWireBox().setValue(String.format("%.1f", connectionManager.getRemainingWireLength()));
                                }
                            }
                            
                            // Update total path length for any hexagon packets on this wire
                            updateHexagonPacketPathLengths(wire);
                        });
                        
                        permanentWireView.setOnRemove(() -> {
                            src.setWire(null);
                            dst.setWire(null);
                            connectionManager.removeWire(wire);
                            if (hud != null) hud.getWireBox().setValue(String.format("%.1f", connectionManager.getRemainingWireLength()));
                            
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
                    System.out.println("❌ CONNECTION REJECTED: Not enough wire length");
                    if (tempWireView != null) {
                        tempWireView.setOutOfWire(true);
                    }
                }
            } else {
                System.out.println("❌ CONNECTION REJECTED: Invalid port combination");
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
        
        // Allow any port types to connect regardless of packet shape/type
        
        return true;
    }

    public boolean isDragging() { return dragging; }
    public PortView getSourcePort() { return sourcePort; }
    public PortView getTargetPort() { return targetPort; }
    public Point2D getCurrentMousePosition() { return currentMousePosition; }
    public WireView getTempWireView() { return tempWireView; }
    public void setTempWireView(WireView tempWireView) { this.tempWireView = tempWireView; }
    
    /**
     * Set up bend point callbacks on existing wire views in the scene
     */
    public void setupExistingWireBendCallbacks() {
        if (wireBendManager == null) {
            return;
        }
        
        Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
        if (targetPane == null) {
            return;
        }
        
        for (javafx.scene.Node node : targetPane.getChildren()) {
            if (node instanceof WireView) {
                WireView wireView = (WireView) node;
                wireView.setOnBendPointPurchase(w -> wireBendManager.purchaseBendPoint(w));
                
                // Set up wire length change callback for existing wires
                wireView.setOnWireLengthChanged(() -> {
                    if (connectionManager != null) {
                        connectionManager.recalculateWireLengths();
                        if (hud != null) {
                            hud.getWireBox().setValue(String.format("%.1f", connectionManager.getRemainingWireLength()));
                        }
                    }
                    
                    // Update total path length for any hexagon packets on this wire
                    updateHexagonPacketPathLengths(wireView.getWireModel());
                });
            }
        }
    }

    /**
     * Update the total path length for any hexagon packets currently on the specified wire
     */
    private void updateHexagonPacketPathLengths(Wire wire) {
        if (wire == null) return;
        
        // Find all hexagon packets on this wire and update their path length
        for (model.entity.packets.Packet packet : manager.packets.PacketManager.getMovingPackets()) {
            if (packet instanceof model.entity.packets.HexagonPacket && 
                packet.getCurrentWire() == wire) {
                
                model.entity.packets.HexagonPacket hexPacket = 
                    (model.entity.packets.HexagonPacket) packet;
                
                // Update the total path length to the current wire length
                hexPacket.setTotalPathLength(wire.getLength());
                
                java.lang.System.out.println("📐 HEXAGON PATH UPDATE: " + packet.getId() + 
                    " - New path length: " + String.format("%.1f", wire.getLength()));
            }
        }
    }

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