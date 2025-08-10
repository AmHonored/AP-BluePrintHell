package controller;

import model.logic.system.NetworkSystem;
import model.levels.Level;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class SystemController {
    private final Level level;
    @SuppressWarnings("unused")
    private final PacketController packetController;
    private final NetworkSystem networkSystem;
    private Timeline systemUpdateTimer;
    private Timeline continuousTransferTimer;

    public SystemController(Level level, PacketController packetController) {
        this.level = level;
        this.packetController = packetController;
        this.networkSystem = new NetworkSystem(level);
    }

    public boolean areAllSystemsReady() {
        return networkSystem.areAllSystemsReady();
    }
    
    public void updateAllSystemsReadyState() {
        networkSystem.updateAllSystemsReadyState();
    }

    public void processSystems() {
        networkSystem.processSystems();
    }

    public void updateSystemStates() {
        networkSystem.updateSystemStates();
    }

    public void processContinuousTransfers() {
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof model.entity.systems.IntermediateSystem) {
                model.entity.systems.IntermediateSystem intermediateSystem =
                    (model.entity.systems.IntermediateSystem) system;
                manager.systems.IntermediateSystemManager manager =
                    new manager.systems.IntermediateSystemManager(intermediateSystem);
                manager.forwardPackets();
            } else if (system instanceof model.entity.systems.DDosSystem) {
                model.entity.systems.DDosSystem ddosSystem =
                    (model.entity.systems.DDosSystem) system;
                manager.systems.DDosSystemManager manager =
                    new manager.systems.DDosSystemManager(ddosSystem);
                manager.forwardPackets();
            } else if (system instanceof model.entity.systems.AntiVirusSystem) {
                model.entity.systems.AntiVirusSystem antivirusSystem =
                    (model.entity.systems.AntiVirusSystem) system;
                manager.systems.AntiVirusSystemManager manager =
                    new manager.systems.AntiVirusSystemManager(antivirusSystem);
                manager.forwardPackets();
                manager.processActiveTrojanPackets(level.getPackets());
            }
        }
        manager.systems.SpySystemManager.forwardPacketsFromAnySpySystem(level);
    }

    public void initTimers() {

        systemUpdateTimer = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            if (!level.isPaused()) {
                processSystems();
                updateSystemStates();
            }
        }));
        systemUpdateTimer.setCycleCount(Timeline.INDEFINITE);

        continuousTransferTimer = new Timeline(new KeyFrame(Duration.millis(10), e -> {
            if (level != null && !level.isPaused()) {
                processContinuousTransfers();
            }
        }));
        continuousTransferTimer.setCycleCount(Timeline.INDEFINITE);
    }

    public void startTimers() {
        if (systemUpdateTimer != null) systemUpdateTimer.play();
        if (continuousTransferTimer != null) continuousTransferTimer.play();
    }

    public void stopTimers() {
        if (systemUpdateTimer != null) systemUpdateTimer.stop();
        if (continuousTransferTimer != null) continuousTransferTimer.stop();
    }

    public boolean commitSisyphusMove(Level level, model.entity.systems.System system, javafx.geometry.Point2D target, javafx.scene.layout.Pane pane) {
        boolean moved = model.logic.Shop.Sisyphus.moveSystem(level, system, target);
        if (moved && pane != null) {
            updatePortViewsForSystem(system, pane);
        }
        return moved;
    }

    public void updatePortViewsForSystem(model.entity.systems.System system, javafx.scene.layout.Pane pane) {
        if (pane == null) return;
        java.util.Set<model.entity.ports.Port> systemPorts = new java.util.HashSet<>();
        systemPorts.addAll(system.getInPorts());
        systemPorts.addAll(system.getOutPorts());
        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof view.components.ports.PortView) {
                view.components.ports.PortView pv = (view.components.ports.PortView) node;
                model.entity.ports.Port modelPort = pv.getModelPort();
                if (systemPorts.contains(modelPort)) {
                    double size = (pv instanceof view.components.ports.TrianglePortView) ? 15.0 : 10.0;
                    javafx.geometry.Point2D p = modelPort.getPosition();
                    pv.setLayoutX(p.getX() - size / 2.0);
                    pv.setLayoutY(p.getY() - size / 2.0);
                }
            }
        }
    }
}
