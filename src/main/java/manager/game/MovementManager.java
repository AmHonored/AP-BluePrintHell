package manager.game;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import manager.packets.HexagonPacketManager;
import manager.packets.PacketManager;
import model.entity.packets.MassivePacket;
import model.entity.packets.Packet;
import model.levels.Level;
import model.wire.Wire;


public class MovementManager extends AnimationTimer {
    private long lastUpdateTime = 0;
    
    @Override
    public void handle(long currentTimeNanos) {
        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTimeNanos;
            return;
        }
        
        double deltaTimeSeconds = (currentTimeNanos - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = currentTimeNanos;
        
        PacketManager.updateMovingPackets(deltaTimeSeconds);
    }
    
    public void startMovementUpdates() {
        start();
    }
    
    public void stopMovementUpdates() {
        stop();
    }
    
    public boolean isRunning() {
        return true;
    }


    public static void moveStandard(Packet packet, double deltaTimeSeconds, double wireLength, Level level) {
        if (packet == null || packet.getCurrentWire() == null) return;
        Wire currentWire = packet.getCurrentWire();
        double frozen = packet.getAergiaFrozenSpeedOrNegative();
        double speed = (frozen >= 0.0) ? frozen : packet.getSpeed();
        if (currentWire != null && frozen < 0.0 && (packet instanceof model.entity.packets.SquarePacket || packet instanceof model.entity.packets.TrianglePacket || packet instanceof model.entity.packets.ProtectedPacket)) {
            boolean inputCompatible = (currentWire.getDest() != null) && currentWire.getDest().isCompatible(packet);
            if (!inputCompatible) {
                speed *= 2.0;
            }
        }
        double distanceToMove = speed * deltaTimeSeconds;
        double progressIncrement = distanceToMove / wireLength;
        double newProgress = packet.getMovementProgress() + progressIncrement;
        if (newProgress > 1.0) newProgress = 1.0;
        packet.setMovementProgress(newProgress);
        Point2D newPosition = currentWire.getPositionAtProgress(newProgress);
        packet.setPosition(new Point2D(newPosition.getX(), newPosition.getY()));

        applyAergiaFreezeIfCrossed(packet, currentWire, newProgress, speed, level);
        recenterDeflectionAfterEliphas(packet, currentWire, newProgress, deltaTimeSeconds, level);
    }

    public static double updateHexagonDistance(model.entity.packets.HexagonPacket packet, Wire wire, double dt, boolean inputCompatible) {
        return HexagonPacketManager.updateDistance(packet, wire, dt, inputCompatible);
    }

    public static void deliver(Packet packet, model.entity.systems.System destinationSystem, Level level, controller.PacketController packetController) {
        if (destinationSystem == null) return;
        switch (destinationSystem.getType()) {
            case IntermediateSystem: {
                model.entity.systems.IntermediateSystem sys = (model.entity.systems.IntermediateSystem) destinationSystem;
                manager.systems.IntermediateSystemManager mgr = new manager.systems.IntermediateSystemManager(sys);
                receivePlaceHide(packet, packetController, sys, mgr::receivePacket);
                break;
            }
            case DDosSystem: {
                model.entity.systems.DDosSystem sys = (model.entity.systems.DDosSystem) destinationSystem;
                manager.systems.DDosSystemManager mgr = new manager.systems.DDosSystemManager(sys);
                receivePlaceHide(packet, packetController, sys, mgr::receivePacket);
                break;
            }
            case SpySystem: {
                model.entity.systems.SpySystem sys = (model.entity.systems.SpySystem) destinationSystem;
                manager.systems.SpySystemManager mgr = new manager.systems.SpySystemManager(sys);
                receivePlaceHide(packet, packetController, sys, mgr::receivePacket);
                break;
            }
            case VPNSystem: {
                model.entity.systems.VPNSystem sys = (model.entity.systems.VPNSystem) destinationSystem;
                manager.systems.VPNSystemManager mgr = new manager.systems.VPNSystemManager(sys);
                mgr.setLevel(level);
                receivePlaceHide(packet, packetController, sys, mgr::receivePacket);
                break;
            }
            case AntiVirusSystem: {
                model.entity.systems.AntiVirusSystem sys = (model.entity.systems.AntiVirusSystem) destinationSystem;
                manager.systems.AntiVirusSystemManager mgr = new manager.systems.AntiVirusSystemManager(sys);
                receivePlaceHide(packet, packetController, sys, mgr::receivePacket);
                break;
            }
            case DistributorSystem: {
                model.entity.systems.DistributorSystem sys = (model.entity.systems.DistributorSystem) destinationSystem;
                manager.systems.DistributorSystemManager mgr = new manager.systems.DistributorSystemManager(sys);
                placeHideThenReceive(packet, packetController, sys, mgr::receivePacket);
                if (packet instanceof MassivePacket && level != null) level.removePacket(packet);
                break;
            }
            case MergeSystem: {
                model.entity.systems.MergeSystem sys = (model.entity.systems.MergeSystem) destinationSystem;
                manager.systems.MergeSystemManager mgr = new manager.systems.MergeSystemManager(sys);
                if (packetController != null) packetController.hidePacket(packet);
                if (level != null) level.removePacket(packet);
                mgr.receivePacket(packet);
                if (packetController != null) {
                    try {
                        javafx.scene.layout.Pane pane = packetController.getPacketLayer();
                        if (pane != null) {
                            for (javafx.scene.Node node : pane.getChildren()) {
                                if (node instanceof view.components.systems.MergeSystemView) {
                                    view.components.systems.MergeSystemView msv = (view.components.systems.MergeSystemView) node;
                                    if (msv.getMergeSystem() == sys) {
                                        mgr.updateView(msv);
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                break;
            }
            case EndSystem: {
                model.entity.systems.EndSystem sys = (model.entity.systems.EndSystem) destinationSystem;
                sys.claimPacket(packet, level);
                if (packetController != null) packetController.deliverPacket(packet);
                break;
            }
            case StartSystem:
            default:
                break;
        }
    }

    private static void receivePlaceHide(Packet packet, controller.PacketController packetController, model.entity.systems.System sys, java.util.function.Consumer<Packet> receive) {
        receive.accept(packet);
        packet.setPosition(sys.getPosition());
        if (packetController != null) packetController.hidePacket(packet);
    }

    private static void placeHideThenReceive(Packet packet, controller.PacketController packetController, model.entity.systems.System sys, java.util.function.Consumer<Packet> receive) {
        packet.setPosition(sys.getPosition());
        if (packetController != null) packetController.hidePacket(packet);
        receive.accept(packet);
    }

    // ===== Effects =====
    public static void applyAergiaFreezeIfCrossed(Packet packet, Wire wire, double progress, double speed, Level level) {
        if (packet == null || wire == null || level == null) return;
        if (level.getAergiaMarks().isEmpty()) return;
        long now = java.lang.System.nanoTime();
        for (model.logic.Shop.Aergia.AergiaMark mark : level.getAergiaMarks()) {
            if (mark.wire == wire && mark.effectEndNanos > now && progress >= mark.progress) {
                packet.setAergiaFreeze(speed, mark.effectEndNanos);
                break;
            }
        }
    }

    public static void recenterDeflectionAfterEliphas(Packet packet, Wire wire, double progress, double deltaTimeSeconds, Level level) {
        if (packet == null || wire == null || level == null) return;
        if (level.getEliphasMarks().isEmpty()) return;
        long now = java.lang.System.nanoTime();
        for (model.logic.Shop.Eliphas.EliphasMark mark : level.getEliphasMarks()) {
            if (mark.wire == wire && mark.effectEndNanos > now && progress >= mark.progress) {
                double dx = packet.getDeflectedX();
                double dy = packet.getDeflectedY();
                double k = 6.0;
                double step = Math.min(1.0, k * deltaTimeSeconds);
                packet.applyDeflection(-dx * step, -dy * step);
                break;
            }
        }
    }
}