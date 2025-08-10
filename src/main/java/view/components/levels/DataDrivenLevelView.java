package view.components.levels;

import config.levels.LevelDefinition;
import config.levels.LevelDisplayDefinition;
import manager.game.VisualManager;
import model.levels.Level;
import view.components.ports.PortView;
import view.components.ports.TrianglePortView;
import view.components.ports.HexagonPortView;
import view.components.systems.*;

/**
 * A generic LevelView that renders systems and ports based on a LevelDefinition.
 * Not referenced by current game flow; safe to compile in isolation.
 */
public class DataDrivenLevelView extends LevelView {
    private final LevelDefinition definition;
    private final java.util.List<SystemView> systemViews = new java.util.ArrayList<>();

    public DataDrivenLevelView(Level level, VisualManager visualManager, LevelDefinition definition) {
        super(level, visualManager);
        this.definition = definition;
        applyDisplaySettings();
        addSystemViews();
    }

    private void applyDisplaySettings() {
        LevelDisplayDefinition.GamePaneSize size = definition.getDisplay().getGamePane();
        getGamePane().setPrefSize(size.getWidth(), size.getHeight());
    }

    private void addSystemViews() {
        for (model.entity.systems.System system : level.getSystems()) {
            SystemView view = createSystemView(system);
            if (view == null) continue;
            view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
            view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
            gamePane.getChildren().add(view);
            systemViews.add(view);
            addPortsToGamePane(view);
        }
    }

    private SystemView createSystemView(model.entity.systems.System system) {
        if (system instanceof model.entity.systems.StartSystem) return new StartSystemView((model.entity.systems.StartSystem) system);
        if (system instanceof model.entity.systems.IntermediateSystem) return new IntermediateSystemView((model.entity.systems.IntermediateSystem) system);
        if (system instanceof model.entity.systems.EndSystem) return new EndSystemView((model.entity.systems.EndSystem) system);
        if (system instanceof model.entity.systems.DDosSystem) return new DDosSystemView((model.entity.systems.DDosSystem) system);
        if (system instanceof model.entity.systems.SpySystem) return new SpySystemView((model.entity.systems.SpySystem) system);
        if (system instanceof model.entity.systems.VPNSystem) return new VPNSystemView((model.entity.systems.VPNSystem) system);
        if (system instanceof model.entity.systems.DistributorSystem) return new DistributorSystemView((model.entity.systems.DistributorSystem) system);
        if (system instanceof model.entity.systems.MergeSystem) return new MergeSystemView((model.entity.systems.MergeSystem) system);
        if (system instanceof model.entity.systems.AntiVirusSystem) return new AntiVirusSystemView((model.entity.systems.AntiVirusSystem) system);
        return null;
    }

    /**
     * Called by GameController to set up wire controller for all ports.
     */
    public void setupWireControllerForPorts(controller.WireController wireController) {
        for (javafx.scene.Node node : gamePane.getChildren()) {
            if (node instanceof SystemView) {
                ((SystemView) node).setWireController(wireController);
            }
        }
    }

    /**
     * Optional: update connection indicators after wiring changes.
     */
    public void updateSystemIndicators() {
        boolean allReady = true;
        for (SystemView sv : systemViews) {
            sv.updateConnectionStatus();
            if (!sv.areAllPortsConnected()) {
                allReady = false;
            }
        }
        // Update play button state(s) based on aggregate readiness
        for (SystemView sv : systemViews) {
            if (sv instanceof view.components.systems.StartSystemView) {
                view.components.systems.StartSystemView startView = (view.components.systems.StartSystemView) sv;
                startView.updateButtonState(allReady);
            }
        }
    }

    /**
     * Hook up the Start system play buttons to the controller and enable/disable based on connectivity.
     */
    public void setupStartSystemPlayButtons(controller.GameController gameController) {
        boolean allReady = true;
        for (SystemView sv : systemViews) {
            if (!sv.areAllPortsConnected()) {
                allReady = false;
                break;
            }
        }
        for (SystemView sv : systemViews) {
            if (sv instanceof view.components.systems.StartSystemView) {
                view.components.systems.StartSystemView startView = (view.components.systems.StartSystemView) sv;
                startView.setOnPlayAction(gameController::handleStartSystemPlayButton);
                startView.setAllSystemsReadyChecker(gameController::areAllSystemsReady);
                startView.updateButtonState(allReady);
            }
        }
    }

    private void addPortsToGamePane(SystemView systemView) {
        // Inputs
        for (int i = 0; i < systemView.getInputPortViews().size(); i++) {
            PortView portView = systemView.getInputPortViews().get(i);
            model.entity.ports.Port modelPort = systemView.getSystem().getInPorts().get(i);
            positionPortView(portView, modelPort);
            gamePane.getChildren().add(portView);
        }
        // Outputs
        for (int i = 0; i < systemView.getOutputPortViews().size(); i++) {
            PortView portView = systemView.getOutputPortViews().get(i);
            model.entity.ports.Port modelPort = systemView.getSystem().getOutPorts().get(i);
            positionPortView(portView, modelPort);
            gamePane.getChildren().add(portView);
        }
    }

    private void positionPortView(PortView portView, model.entity.ports.Port modelPort) {
        double portSize = getPortSize(portView);
        portView.setLayoutX(modelPort.getPosition().getX() - portSize / 2);
        portView.setLayoutY(modelPort.getPosition().getY() - portSize / 2);
    }

    private double getPortSize(PortView portView) {
        Double square = definition.getDisplay().getPortSizes().getOrDefault("SQUARE", 10.0);
        Double triangle = definition.getDisplay().getPortSizes().getOrDefault("TRIANGLE", 15.0);
        Double hexagon = definition.getDisplay().getPortSizes().getOrDefault("HEXAGON", 6.0);
        if (portView instanceof TrianglePortView) return triangle;
        if (portView instanceof HexagonPortView) return hexagon;
        return square;
    }

    @Override
    protected void restartLevel() {
        // Defer to visual manager; not wired here to avoid changing game flow
        visualManager.showMenu();
    }

    @Override
    protected void goToNextLevel() {
        visualManager.showMenu();
    }
}


