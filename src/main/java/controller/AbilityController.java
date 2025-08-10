package controller;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import model.levels.Level;
import view.game.GameScene;
import view.game.HUDScene;
import view.components.levels.LevelView;

public class AbilityController {
    private final Level level;
    private final GameScene gameScene;
    private final LevelView levelView;
    private final SystemController systemController;
    private final manager.game.ConnectionManager connectionManager;

    // Sisyphus state
    private boolean awaitingSisyphusSystemSelection = false;
    private boolean isDraggingSystemForSisyphus = false;
    private model.entity.systems.System selectedSystem = null;
    private javafx.geometry.Point2D originalSystemPosition = null;
    private view.components.systems.SystemView selectedSystemView = null;

    // Aergia/Eliphas state
    private boolean awaitingAergiaPlacement = false;
    private final java.util.List<MarkVisual> activeAergiaVisuals = new java.util.ArrayList<>();
    private boolean awaitingEliphasPlacement = false;
    private final java.util.List<MarkVisual> activeEliphasVisuals = new java.util.ArrayList<>();

    private static class MarkVisual {
        final javafx.scene.text.Text crossText;
        final model.wire.Wire wire;
        final double progress;
        final long removeTime;
        MarkVisual(javafx.scene.text.Text crossText, model.wire.Wire wire, double progress, long removeTime) {
            this.crossText = crossText;
            this.wire = wire;
            this.progress = progress;
            this.removeTime = removeTime;
        }
    }

    public AbilityController(Level level, Object view, SystemController systemController, manager.game.ConnectionManager connectionManager) {
        this.level = level;
        this.systemController = systemController;
        this.connectionManager = connectionManager;
        if (view instanceof GameScene) {
            this.gameScene = (GameScene) view;
            this.levelView = null;
        } else if (view instanceof LevelView) {
            this.gameScene = null;
            this.levelView = (LevelView) view;
        } else {
            this.gameScene = null;
            this.levelView = null;
        }
    }

    public void setupSisyphusMovementHandler() {
        Pane pane = getGamePane();
        if (pane == null) return;

        pane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!awaitingSisyphusSystemSelection) return;
            javafx.geometry.Point2D local = pane.sceneToLocal(e.getSceneX(), e.getSceneY());
            if (selectedSystem == null) {
                model.entity.systems.System clickedSystem = findSystemAtPosition(local);
                if (clickedSystem != null && clickedSystem.isDraggableWithSisyphus() && model.logic.Shop.Sisyphus.canMoveSystem(clickedSystem)) {
                    selectedSystem = clickedSystem;
                    originalSystemPosition = clickedSystem.getPosition();
                    selectedSystemView = findSystemViewFor(clickedSystem);
                    isDraggingSystemForSisyphus = true;
                }
            } else {
                isDraggingSystemForSisyphus = true;
            }
        });

        pane.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!awaitingSisyphusSystemSelection || !isDraggingSystemForSisyphus || selectedSystem == null) return;
            javafx.geometry.Point2D local = pane.sceneToLocal(e.getSceneX(), e.getSceneY());
            if (selectedSystemView == null) {
                selectedSystemView = findSystemViewFor(selectedSystem);
            }
            if (selectedSystemView != null) {
                double w = view.components.systems.SystemView.SYSTEM_WIDTH;
                double h = view.components.systems.SystemView.SYSTEM_HEIGHT;
                selectedSystemView.setLayoutX(local.getX() - w / 2.0);
                selectedSystemView.setLayoutY(local.getY() - h / 2.0);
                javafx.geometry.Point2D delta = local.subtract(originalSystemPosition);
                previewPortViewsForSystem(selectedSystem, delta);
            }
            e.consume();
        });

        pane.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (!awaitingSisyphusSystemSelection || selectedSystem == null) return;
            javafx.geometry.Point2D local = pane.sceneToLocal(e.getSceneX(), e.getSceneY());
            // Ensure model at original before commit attempt
            selectedSystem.setPosition(originalSystemPosition);
            if (selectedSystemView != null) selectedSystemView.updatePosition();
            boolean moved = systemController.commitSisyphusMove(level, selectedSystem, local, pane);
            if (moved) {
                level.addSisyphusScrolls(-1);
                if (selectedSystemView != null) selectedSystemView.updatePosition();
                // After committing, refresh all wire views
                safeRefreshAllWireViews();
                HUDScene currentHud = getHUD();
                if (currentHud != null) updateSisyphusHudButtonEnabled(currentHud);
            } else {
                selectedSystem.setPosition(originalSystemPosition);
                if (selectedSystemView != null) selectedSystemView.updatePosition();
                // Also ensure ports visually remain at original positions
                systemController.updatePortViewsForSystem(selectedSystem, pane);
                safeRefreshAllWireViews();
            }

            // Reset state
            selectedSystem = null;
            originalSystemPosition = null;
            selectedSystemView = null;
            isDraggingSystemForSisyphus = false;
            awaitingSisyphusSystemSelection = false;

            // Update HUD labels
            HUDScene currentHud = getHUD();
            if (currentHud != null) updateSisyphusHudButtonEnabled(currentHud);
            if (gameScene != null) gameScene.updateSisyphusButtonText();
            e.consume();
        });
    }

    public void startSisyphusSelection() {
        awaitingSisyphusSystemSelection = true;
        selectedSystem = null;
        originalSystemPosition = null;
        selectedSystemView = null;
    }

    public void updateSisyphusHudButtonEnabled(HUDScene hud) {
        boolean enabled = level.getSisyphusScrolls() > 0;
        String sisyphusText = "Sisyphus (" + level.getSisyphusScrolls() + ")";
        hud.getSisyphusButton().setText(sisyphusText);
        hud.getSisyphusButton().setDisable(!enabled);
    }

    // ----- Aergia -----
    public void setupAergiaPlacementHandler() {
        Pane pane = getGamePane();
        if (pane == null) return;
        pane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (!awaitingAergiaPlacement) return;
            javafx.geometry.Point2D local = pane.sceneToLocal(e.getSceneX(), e.getSceneY());
            model.wire.Wire chosenWire = null;
            double chosenT = 0.0;
            double bestDistance = Double.MAX_VALUE;
            double tolerancePx = 16.0;
            for (model.wire.Wire w : connectionManager.getWires()) {
                if (w == null || !w.isActive()) continue;
                double tCandidate = model.logic.Shop.Aergia.findClosestProgress(w, local);
                javafx.geometry.Point2D posOnWire = w.getPositionAtProgress(tCandidate);
                double d = posOnWire.distance(local);
                if (d < bestDistance) { bestDistance = d; chosenWire = w; chosenT = tCandidate; }
            }
            if (chosenWire == null || bestDistance > tolerancePx) return;
            model.logic.Shop.Aergia.addMark(level, chosenWire, chosenT);
            level.addAergiaScrolls(-1);
            addMarkVisual(pane, chosenWire, chosenT,
                "-fx-font-size: 20px; -fx-fill: #ff6b85; -fx-effect: dropshadow(gaussian, rgba(233,69,96,0.7), 8, 0.6, 0, 0);",
                20, activeAergiaVisuals);
            awaitingAergiaPlacement = false;
            if (gameScene != null) gameScene.updateAergiaButtonText();
            HUDScene currentHud = getHUD();
            if (currentHud != null) updateAergiaHudButtonEnabled(currentHud);
            e.consume();
        });
    }

    public void startAergiaPlacement() {
        awaitingAergiaPlacement = true;
    }

    public void updateAergiaHudButtonEnabled(HUDScene hud) {
        boolean enabled = level.getAergiaScrolls() > 0 && !level.isAergiaOnCooldown();
        hud.getAergiaButton().setDisable(!enabled);
        if (gameScene != null) gameScene.updateAergiaButtonText();
    }

    // ----- Eliphas -----
    public void setupEliphasPlacementHandler() {
        Pane pane = getGamePane();
        if (pane == null) return;
        pane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (!awaitingEliphasPlacement) return;
            javafx.geometry.Point2D local = pane.sceneToLocal(e.getSceneX(), e.getSceneY());
            model.wire.Wire chosenWire = null;
            double chosenT = 0.0;
            double bestDistance = Double.MAX_VALUE;
            double tolerancePx = 16.0;
            for (model.wire.Wire w : connectionManager.getWires()) {
                if (w == null || !w.isActive()) continue;
                double tCandidate = model.logic.Shop.Aergia.findClosestProgress(w, local);
                javafx.geometry.Point2D posOnWire = w.getPositionAtProgress(tCandidate);
                double d = posOnWire.distance(local);
                if (d < bestDistance) { bestDistance = d; chosenWire = w; chosenT = tCandidate; }
            }
            if (chosenWire == null || bestDistance > tolerancePx) return;
            model.logic.Shop.Eliphas.addMark(level, chosenWire, chosenT);
            level.addEliphasScrolls(-1);
            addMarkVisual(pane, chosenWire, chosenT,
                "-fx-font-size: 20px; -fx-fill: #00d4ff; -fx-effect: dropshadow(gaussian, rgba(0,212,255,0.7), 8, 0.6, 0, 0);",
                30, activeEliphasVisuals);
            awaitingEliphasPlacement = false;
            HUDScene currentHud = getHUD();
            if (currentHud != null) updateEliphasHudButtonEnabled(currentHud);
            e.consume();
        });
    }

    public void startEliphasPlacement() {
        awaitingEliphasPlacement = true;
    }

    public void updateEliphasHudButtonEnabled(HUDScene hud) {
        boolean enabled = level.getEliphasScrolls() > 0;
        if (hud.getEliphasButton() != null) {
            String text = "Eliphas (" + level.getEliphasScrolls() + ")";
            hud.getEliphasButton().setText(text);
            hud.getEliphasButton().setDisable(!enabled);
        }
    }

    // ----- Visual mark helpers -----
    public void updateAergiaMarkPositions() {
        updateMarkPositions(activeAergiaVisuals);
    }

    public void updateEliphasMarkPositions() {
        updateMarkPositions(activeEliphasVisuals);
    }

    private void updateMarkPositions(java.util.List<MarkVisual> visuals) {
        long now = java.lang.System.nanoTime();
        visuals.removeIf(visual -> {
            if (now >= visual.removeTime) {
                Pane pane = getGamePane();
                if (pane != null) {
                    pane.getChildren().remove(visual.crossText);
                }
                return true;
            }
            return false;
        });
        for (MarkVisual visual : visuals) {
            if (visual.wire != null) {
                javafx.geometry.Point2D newPos = visual.wire.getPositionAtProgress(visual.progress);
                visual.crossText.setX(newPos.getX() - 6);
                visual.crossText.setY(newPos.getY() + 6);
            }
        }
    }

    private void addMarkVisual(Pane pane, model.wire.Wire wire, double progress, String style,
                               int lifetimeSeconds, java.util.List<MarkVisual> tracker) {
        javafx.geometry.Point2D p = wire.getPositionAtProgress(progress);
        javafx.scene.text.Text cross = new javafx.scene.text.Text("❌");
        cross.setStyle(style);
        cross.setX(p.getX() - 6);
        cross.setY(p.getY() + 6);
        pane.getChildren().add(cross);
        long removeTime = java.lang.System.nanoTime() + (long) lifetimeSeconds * 1_000_000_000L;
        MarkVisual visual = new MarkVisual(cross, wire, progress, removeTime);
        tracker.add(visual);
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(lifetimeSeconds));
        delay.setOnFinished(ev -> {
            pane.getChildren().remove(cross);
            tracker.remove(visual);
        });
        delay.play();
    }

    private Pane getGamePane() {
        if (gameScene != null) return gameScene.getGamePane();
        if (levelView != null) return levelView.getGamePane();
        return null;
    }

    private HUDScene getHUD() {
        if (gameScene != null) return gameScene.getHUDScene();
        if (levelView != null) return levelView.getHUDScene();
        return null;
    }

    private model.entity.systems.System findSystemAtPosition(javafx.geometry.Point2D position) {
        for (model.entity.systems.System system : level.getSystems()) {
            javafx.geometry.Point2D systemPos = system.getPosition();
            double width = model.entity.systems.System.WIDTH;
            double height = model.entity.systems.System.HEIGHT;
            if (position.getX() >= systemPos.getX() - width/2 && 
                position.getX() <= systemPos.getX() + width/2 &&
                position.getY() >= systemPos.getY() - height/2 && 
                position.getY() <= systemPos.getY() + height/2) {
                return system;
            }
        }
        return null;
    }

    private view.components.systems.SystemView findSystemViewFor(model.entity.systems.System system) {
        Pane pane = getGamePane();
        if (pane == null) return null;
        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof view.components.systems.SystemView) {
                view.components.systems.SystemView sv = (view.components.systems.SystemView) node;
                if (sv.getSystem() == system) return sv;
            }
        }
        return null;
    }

    private void previewPortViewsForSystem(model.entity.systems.System system, javafx.geometry.Point2D delta) {
        Pane pane = getGamePane();
        if (pane == null) return;
        java.util.Set<model.entity.ports.Port> systemPorts = new java.util.HashSet<>();
        systemPorts.addAll(system.getInPorts());
        systemPorts.addAll(system.getOutPorts());
        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof view.components.ports.PortView) {
                view.components.ports.PortView pv = (view.components.ports.PortView) node;
                model.entity.ports.Port modelPort = pv.getModelPort();
                if (systemPorts.contains(modelPort)) {
                    double size = getPortVisualSize(pv);
                    javafx.geometry.Point2D p = modelPort.getPosition().add(delta);
                    pv.setLayoutX(p.getX() - size / 2.0);
                    pv.setLayoutY(p.getY() - size / 2.0);
                }
            }
        }
    }

    private double getPortVisualSize(view.components.ports.PortView pv) {
        if (pv instanceof view.components.ports.TrianglePortView) return 15.0;
        return 10.0;
    }

    private void safeRefreshAllWireViews() {
        java.util.List<model.wire.Wire> wires = connectionManager.getWires();
        if (wires == null || wires.isEmpty()) return;
        for (model.wire.Wire wire : wires) {
            view.components.wires.WireView.refresh(wire);
        }
    }
}


