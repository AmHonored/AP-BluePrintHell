package controller;
import view.components.ports.PortView;
import view.components.wires.WireView;
import javafx.geometry.Point2D;
import view.game.GameScene;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Shape;
import model.entity.ports.Port;
import model.entity.ports.PortType;
import model.wire.Wire;
import java.util.UUID;
import model.levels.Level;
import view.game.HUDScene;
import manager.game.ConnectionManager;
import manager.game.WireBendManager;

public class WireController {
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
            this.wireBendManager = new WireBendManager(level);
        }
    }
    
    public void setHUD(HUDScene hud) { this.hud = hud; }
    public void setGameScene(GameScene gameScene) { this.gameScene = gameScene; }
    public void setGamePane(Pane gamePane) { this.gamePane = gamePane; }
    public void setConnectionManager(ConnectionManager cm) { 
        this.connectionManager = cm; 
        if (level != null && cm != null) {
            this.wireBendManager = new WireBendManager(level);
        }
    }

    public void setupDragHandlers(Pane targetPane) {
        if (targetPane == null) return;
        targetPane.setOnMouseDragged(event -> {
            if (isDragging()) {
                PortView hoveredPort = findPortAtPosition(targetPane, event.getSceneX(), event.getSceneY());
                updateWireDrag(event.getSceneX(), event.getSceneY(), hoveredPort);
                event.consume();
            }
        });
        targetPane.setOnMouseReleased(event -> {
            if (isDragging()) {
                finishWireDrag(null, event.getSceneX(), event.getSceneY());
                event.consume();
            }
        });
    }

    private PortView findPortAtPosition(Pane targetPane, double sceneX, double sceneY) {
        if (targetPane == null) return null;
        javafx.geometry.Point2D localPoint = targetPane.sceneToLocal(sceneX, sceneY);
        for (javafx.scene.Node node : targetPane.getChildren()) {
            if (node instanceof view.components.ports.PortView) {
                view.components.ports.PortView portView = (view.components.ports.PortView) node;
                if (portView.getBoundsInParent().contains(localPoint.getX(), localPoint.getY())) {
                    return portView;
                }
            }
        }
        return null;
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
        
        if (!tempWireView.getCurves().isEmpty()) {
            Shape wireShape = tempWireView.getCurves().get(0);
            Point2D start = sourcePort.getModelPort().getPosition();
            
            Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
            Point2D localEndPoint = targetPane.sceneToLocal(sceneX, sceneY);
            
            if (wireShape instanceof javafx.scene.shape.QuadCurve) {
                javafx.scene.shape.QuadCurve curve = (javafx.scene.shape.QuadCurve) wireShape;
                curve.setStartX(start.getX());
                curve.setStartY(start.getY());
                curve.setEndX(localEndPoint.getX());
                curve.setEndY(localEndPoint.getY());
                double midX = (start.getX() + localEndPoint.getX()) / 2;
                double midY = (start.getY() + localEndPoint.getY()) / 2;
                curve.setControlX(midX);
                curve.setControlY(midY);
            }
        }
        
        if (hoveredPort != null && isValidTarget(sourcePort, hoveredPort)) {
            tempWireView.setValidTarget();
        } else {
            tempWireView.setDragging(); 
        }
    }

    public void finishWireDrag(PortView target, double sceneX, double sceneY) {
        if (!dragging) return;
        
            PortView actualTarget = null;
            if (gameScene != null || gamePane != null) {
                Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
                actualTarget = findPortAtPosition(targetPane, sceneX, sceneY);
            }
        
        this.targetPort = actualTarget;
        this.currentMousePosition = new Point2D(sceneX, sceneY);
        
        if (sourcePort != null && targetPort != null && sourcePort != targetPort) {
            Port src = sourcePort.getModelPort();
            Port dst = targetPort.getModelPort();
            if (isValidTarget(sourcePort, targetPort)) {
                Wire wire = new Wire(UUID.randomUUID().toString(), src, dst);
                
                if (connectionManager != null && connectionManager.canAddWire(wire)) {
                    src.setWire(wire);
                    dst.setWire(wire);
                    connectionManager.addWire(wire);
                    if (hud != null) hud.getWireBox().setValue(String.format("%.1f", connectionManager.getRemainingWireLength()));
                    service.AudioManager.playConnectionSuccess();
                    
                    if (connectionChangeCallback != null) {
                        connectionChangeCallback.run();
                    }
                    
                    if (gameScene != null || gamePane != null) {
                        WireView permanentWireView = new WireView(wire);
                        
                        if (wireBendManager != null) {
                            permanentWireView.setOnBendPointPurchase(w -> {
                                boolean ok = wireBendManager.purchaseBendPoint(w);
                                if (ok && hud != null && level != null) {
                                    hud.getCoinsBox().setValue(String.valueOf(level.getCoins()));
                                }
                                return ok;
                            });
                            permanentWireView.setOnBendPointRefund(w -> {
                                boolean ok = wireBendManager.refundBendPoint(w);
                                if (ok && hud != null && level != null) {
                                    hud.getCoinsBox().setValue(String.valueOf(level.getCoins()));
                                }
                                return ok;
                            });
                        }
                        
                        permanentWireView.setOnWireLengthChanged(() -> {
                            if (connectionManager != null) {
                                connectionManager.recalculateWireLengths();
                                if (hud != null) {
                                    hud.getWireBox().setValue(String.format("%.1f", connectionManager.getRemainingWireLength()));
                                }
                            }
                            
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
                } else if (tempWireView != null) {
                    tempWireView.setOutOfWire(true);
                }
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
        
        if (src.getType() != PortType.OUTPUT || dst.getType() != PortType.INPUT) return false;
        
        if (src.isConnected() || dst.isConnected()) return false;
        
        if (src.getSystem() == dst.getSystem()) return false;
        
        return true;
    }

    public boolean isDragging() { return dragging; }
    public PortView getSourcePort() { return sourcePort; }
    public PortView getTargetPort() { return targetPort; }
    public Point2D getCurrentMousePosition() { return currentMousePosition; }
    public WireView getTempWireView() { return tempWireView; }
    public void setTempWireView(WireView tempWireView) { this.tempWireView = tempWireView; }
    
    public void removeWireFromOutputPort(PortView outputPortView) {
        if (outputPortView == null || connectionManager == null) return;
        Port src = outputPortView.getModelPort();
        if (src == null || src.getType() != PortType.OUTPUT || !src.isConnected()) return;
        model.wire.Wire wire = src.getWire();
        if (wire == null) return;
        Port dst = wire.getDest();
        src.setWire(null);
        if (dst != null) dst.setWire(null);
        connectionManager.removeWire(wire);
        if (hud != null) hud.getWireBox().setValue(String.format("%.1f", connectionManager.getRemainingWireLength()));
        if (connectionChangeCallback != null) connectionChangeCallback.run();
        Pane targetPane = gameScene != null ? gameScene.getGamePane() : gamePane;
        if (targetPane != null) {
            for (javafx.scene.Node node : new java.util.ArrayList<>(targetPane.getChildren())) {
                if (node instanceof WireView) {
                    WireView wv = (WireView) node;
                    if (wv.getWireModel() == wire) {
                        targetPane.getChildren().remove(wv);
                        break;
                    }
                }
            }
        }
    }
    
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
                wireView.setOnBendPointPurchase(w -> {
                    boolean ok = wireBendManager.purchaseBendPoint(w);
                    if (ok && hud != null && level != null) {
                        hud.getCoinsBox().setValue(String.valueOf(level.getCoins()));
                    }
                    return ok;
                });
                wireView.setOnBendPointRefund(w -> {
                    boolean ok = wireBendManager.refundBendPoint(w);
                    if (ok && hud != null && level != null) {
                        hud.getCoinsBox().setValue(String.valueOf(level.getCoins()));
                    }
                    return ok;
                });
                
                wireView.setOnWireLengthChanged(() -> {
                    if (connectionManager != null) {
                        connectionManager.recalculateWireLengths();
                        if (hud != null) {
                            hud.getWireBox().setValue(String.format("%.1f", connectionManager.getRemainingWireLength()));
                        }
                    }
                    
                    updateHexagonPacketPathLengths(wireView.getWireModel());
                });
            }
        }
    }

    private void updateHexagonPacketPathLengths(Wire wire) {
        if (wire == null) return;
        
        for (model.entity.packets.Packet packet : manager.packets.PacketManager.getMovingPackets()) {
            if (packet instanceof model.entity.packets.HexagonPacket && 
                packet.getCurrentWire() == wire) {
                
                model.entity.packets.HexagonPacket hexPacket = 
                    (model.entity.packets.HexagonPacket) packet;
                
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
        @SuppressWarnings("unused")
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