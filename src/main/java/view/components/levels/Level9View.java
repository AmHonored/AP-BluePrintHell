package view.components.levels;

import javafx.geometry.Point2D;
import model.entity.systems.StartSystem;
import model.entity.systems.DistributorSystem;
import model.entity.systems.MergeSystem;
import model.entity.systems.EndSystem;
import model.entity.ports.SquarePort;
import model.entity.ports.TrianglePort;
import model.entity.ports.HexagonPort;
import view.components.systems.StartSystemView;
import view.components.systems.DistributorSystemView;
import view.components.systems.MergeSystemView;
import view.components.systems.EndSystemView;
import view.components.systems.SystemView;
import view.components.ports.PortView;
import view.components.ports.TrianglePortView;
import controller.WireController;
import manager.game.LevelManager;

import java.util.ArrayList;
import java.util.List;

public class Level9View extends LevelView {
    private final LevelManager levelManager;
    private List<SystemView> systemViews = new ArrayList<>();

    public Level9View(model.levels.Level level, manager.game.VisualManager visualManager) {
        super(level, visualManager);
        this.levelManager = new LevelManager(visualManager, null, null);
        setupLevel9Layout();
    }

    private void setupLevel9Layout() {
        createSystems();
        addSystemViews();
        setupWireController();
    }

    private void createSystems() {
        // Start System with 3 output ports
        StartSystem start = new StartSystem(new Point2D(120, 220));
        SquarePort startOut1 = new SquarePort("start_out1", start, model.entity.ports.PortType.OUTPUT, new Point2D(160, 200));
        TrianglePort startOut2 = new TrianglePort("start_out2", start, model.entity.ports.PortType.OUTPUT, new Point2D(160, 220));
        HexagonPort startOut3 = new HexagonPort("start_out3", start, model.entity.ports.PortType.OUTPUT, new Point2D(160, 240));
        start.addPort(startOut1);
        start.addPort(startOut2);
        start.addPort(startOut3);
        level.addSystem(start);

        // Distributor System with 3 input and 3 output ports
        DistributorSystem distributor = new DistributorSystem(new Point2D(320, 220));
        // Input ports (left side)
        SquarePort distIn1 = new SquarePort("dist_in1", distributor, model.entity.ports.PortType.INPUT, new Point2D(280, 200));
        TrianglePort distIn2 = new TrianglePort("dist_in2", distributor, model.entity.ports.PortType.INPUT, new Point2D(280, 220));
        HexagonPort distIn3 = new HexagonPort("dist_in3", distributor, model.entity.ports.PortType.INPUT, new Point2D(280, 240));
        // Output ports (right side)
        SquarePort distOut1 = new SquarePort("dist_out1", distributor, model.entity.ports.PortType.OUTPUT, new Point2D(360, 200));
        TrianglePort distOut2 = new TrianglePort("dist_out2", distributor, model.entity.ports.PortType.OUTPUT, new Point2D(360, 220));
        HexagonPort distOut3 = new HexagonPort("dist_out3", distributor, model.entity.ports.PortType.OUTPUT, new Point2D(360, 240));
        distributor.addPort(distIn1);
        distributor.addPort(distIn2);
        distributor.addPort(distIn3);
        distributor.addPort(distOut1);
        distributor.addPort(distOut2);
        distributor.addPort(distOut3);
        level.addSystem(distributor);

        // Merge System with 3 input and 3 output ports
        MergeSystem merge = new MergeSystem(new Point2D(520, 220));
        // Input ports (left side)
        SquarePort mergeIn1 = new SquarePort("merge_in1", merge, model.entity.ports.PortType.INPUT, new Point2D(480, 200));
        TrianglePort mergeIn2 = new TrianglePort("merge_in2", merge, model.entity.ports.PortType.INPUT, new Point2D(480, 220));
        HexagonPort mergeIn3 = new HexagonPort("merge_in3", merge, model.entity.ports.PortType.INPUT, new Point2D(480, 240));
        // Output ports (right side)
        SquarePort mergeOut1 = new SquarePort("merge_out1", merge, model.entity.ports.PortType.OUTPUT, new Point2D(560, 200));
        TrianglePort mergeOut2 = new TrianglePort("merge_out2", merge, model.entity.ports.PortType.OUTPUT, new Point2D(560, 220));
        HexagonPort mergeOut3 = new HexagonPort("merge_out3", merge, model.entity.ports.PortType.OUTPUT, new Point2D(560, 240));
        merge.addPort(mergeIn1);
        merge.addPort(mergeIn2);
        merge.addPort(mergeIn3);
        merge.addPort(mergeOut1);
        merge.addPort(mergeOut2);
        merge.addPort(mergeOut3);
        level.addSystem(merge);

        // End System with 3 input ports
        EndSystem end = new EndSystem(new Point2D(720, 220));
        SquarePort endIn1 = new SquarePort("end_in1", end, model.entity.ports.PortType.INPUT, new Point2D(680, 200));
        TrianglePort endIn2 = new TrianglePort("end_in2", end, model.entity.ports.PortType.INPUT, new Point2D(680, 220));
        HexagonPort endIn3 = new HexagonPort("end_in3", end, model.entity.ports.PortType.INPUT, new Point2D(680, 240));
        end.addPort(endIn1);
        end.addPort(endIn2);
        end.addPort(endIn3);
        level.addSystem(end);
    }

    private void addSystemViews() {
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof StartSystem) {
                StartSystemView view = new StartSystemView((StartSystem) system);
                placeSystemView(view, system);
            } else if (system instanceof DistributorSystem) {
                DistributorSystemView view = new DistributorSystemView((DistributorSystem) system);
                placeSystemView(view, system);
            } else if (system instanceof MergeSystem) {
                MergeSystemView view = new MergeSystemView((MergeSystem) system);
                placeSystemView(view, system);
            } else if (system instanceof EndSystem) {
                EndSystemView view = new EndSystemView((EndSystem) system);
                placeSystemView(view, system);
            }
        }
    }

    private void placeSystemView(SystemView view, model.entity.systems.System system) {
        view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
        view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
        gamePane.getChildren().add(view);
        addPortsToGamePane(view, system.getPosition());
        systemViews.add(view);
    }

    private void setupWireController() {
        // This will be handled by the GameController when it's initialized
        // The WireController will be set up to handle wire creation between ports
        // The setupPortViews method in GameController will be called after initialization
    }
    
    /**
     * Called by GameController to set up wire controller for all ports
     */
    public void setupWireControllerForPorts(WireController wireController) {
        for (javafx.scene.Node node : gamePane.getChildren()) {
            if (node instanceof view.components.systems.SystemView) {
                view.components.systems.SystemView systemView = (view.components.systems.SystemView) node;
                systemView.setWireController(wireController);
            }
        }
    }

    /**
     * Get system views for external access
     */
    public List<SystemView> getSystemViews() {
        return systemViews;
    }

    /**
     * Setup start system play button actions
     */
    public void setupStartSystemPlayButtons(controller.GameController gameController) {
        for (SystemView systemView : systemViews) {
            if (systemView instanceof StartSystemView) {
                StartSystemView startView = (StartSystemView) systemView;
                startView.setOnPlayAction(() -> gameController.handleStartSystemPlayButton());
                startView.setAllSystemsReadyChecker(() -> gameController.areAllSystemsReady());
                updateSystemIndicators();
            }
        }
    }

    /**
     * Update all system connection indicators
     */
    public void updateSystemIndicators() {
        for (SystemView systemView : systemViews) {
            systemView.updateConnectionStatus();
            
            if (systemView instanceof StartSystemView) {
                StartSystemView startView = (StartSystemView) systemView;
                boolean allReady = true;
                
                for (SystemView sv : systemViews) {
                    if (!sv.areAllPortsConnected()) {
                        allReady = false;
                        break;
                    }
                }
                
                startView.updateButtonState(allReady);
            }
        }
    }

    private void addPortsToGamePane(SystemView systemView, Point2D systemPosition) {
        // Input ports
        for (int i = 0; i < systemView.getInputPortViews().size(); i++) {
            PortView portView = systemView.getInputPortViews().get(i);
            model.entity.ports.Port modelPort = systemView.getSystem().getInPorts().get(i);
            double portSize = getPortSize(portView);
            portView.setLayoutX(modelPort.getPosition().getX() - portSize / 2);
            portView.setLayoutY(modelPort.getPosition().getY() - portSize / 2);
            gamePane.getChildren().add(portView);
        }
        // Output ports
        for (int i = 0; i < systemView.getOutputPortViews().size(); i++) {
            PortView portView = systemView.getOutputPortViews().get(i);
            model.entity.ports.Port modelPort = systemView.getSystem().getOutPorts().get(i);
            double portSize = getPortSize(portView);
            portView.setLayoutX(modelPort.getPosition().getX() - portSize / 2);
            portView.setLayoutY(modelPort.getPosition().getY() - portSize / 2);
            gamePane.getChildren().add(portView);
        }
    }

    private double getPortSize(PortView portView) {
        if (portView instanceof TrianglePortView) {
            return 15.0;
        } else {
            return 10.0;
        }
    }

    @Override
    public void goToNextLevel() {
        // Level 9 is the test level for merge system, no next level defined
        // Could return to menu or restart level
        visualManager.showMenu();
    }

    @Override
    public void restartLevel() {
        // Restart Level 9
        manager.game.LevelManager levelManager = new manager.game.LevelManager(
            visualManager, 
            visualManager.getPrimaryStage(), 
            visualManager.getCssFile()
        );
        levelManager.showLevel(9);
    }
}
