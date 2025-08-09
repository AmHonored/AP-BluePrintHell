package view.components.levels;

import javafx.geometry.Point2D;
import model.entity.systems.StartSystem;
import model.entity.systems.DistributorSystem;
import model.entity.systems.IntermediateSystem;
import model.entity.systems.EndSystem;
import model.entity.ports.SquarePort;
import model.entity.ports.TrianglePort;
import view.components.systems.StartSystemView;
import view.components.systems.DistributorSystemView;
import view.components.systems.IntermediateSystemView;
import view.components.systems.EndSystemView;
import view.components.systems.SystemView;
import view.components.ports.PortView;
import view.components.ports.TrianglePortView;
import controller.WireController;
import manager.game.LevelManager;

import java.util.ArrayList;
import java.util.List;

public class Level8View extends LevelView {
    private final LevelManager levelManager;
    private List<SystemView> systemViews = new ArrayList<>();

    public Level8View(model.levels.Level level, manager.game.VisualManager visualManager) {
        super(level, visualManager);
        this.levelManager = new LevelManager(visualManager, null, null);
        setupLevel8Layout();
    }

    private void setupLevel8Layout() {
        createSystems();
        addSystemViews();
        setupWireController();
    }

    private void createSystems() {
        // Start -> Distributor -> Intermediate (normal) -> End
        StartSystem start = new StartSystem(new Point2D(120, 220));
        // Two OUTPUT ports for start
        SquarePort startOut1 = new SquarePort("start_out1", start, model.entity.ports.PortType.OUTPUT, new Point2D(160, 210));
        TrianglePort startOut2 = new TrianglePort("start_out2", start, model.entity.ports.PortType.OUTPUT, new Point2D(160, 230));
        start.addPort(startOut1);
        start.addPort(startOut2);
        level.addSystem(start);

        DistributorSystem distributor = new DistributorSystem(new Point2D(320, 220));
        // Two INPUT ports and two OUTPUT ports for distributor (as normal ports)
        // Inputs on left side
        SquarePort distIn1 = new SquarePort("dist_in1", distributor, model.entity.ports.PortType.INPUT, new Point2D(280, 210));
        TrianglePort distIn2 = new TrianglePort("dist_in2", distributor, model.entity.ports.PortType.INPUT, new Point2D(280, 230));
        // Outputs on right side
        SquarePort distOut1 = new SquarePort("dist_out1", distributor, model.entity.ports.PortType.OUTPUT, new Point2D(360, 210));
        TrianglePort distOut2 = new TrianglePort("dist_out2", distributor, model.entity.ports.PortType.OUTPUT, new Point2D(360, 230));
        distributor.addPort(distIn1);
        distributor.addPort(distIn2);
        distributor.addPort(distOut1);
        distributor.addPort(distOut2);
        level.addSystem(distributor);

        IntermediateSystem normal = new IntermediateSystem(new Point2D(520, 220));
        // Two INPUT and two OUTPUT ports
        SquarePort normIn1 = new SquarePort("norm_in1", normal, model.entity.ports.PortType.INPUT, new Point2D(480, 210));
        TrianglePort normIn2 = new TrianglePort("norm_in2", normal, model.entity.ports.PortType.INPUT, new Point2D(480, 230));
        SquarePort normOut1 = new SquarePort("norm_out1", normal, model.entity.ports.PortType.OUTPUT, new Point2D(560, 210));
        TrianglePort normOut2 = new TrianglePort("norm_out2", normal, model.entity.ports.PortType.OUTPUT, new Point2D(560, 230));
        normal.addPort(normIn1);
        normal.addPort(normIn2);
        normal.addPort(normOut1);
        normal.addPort(normOut2);
        level.addSystem(normal);

        EndSystem end = new EndSystem(new Point2D(700, 220));
        // Two INPUT ports
        SquarePort endIn1 = new SquarePort("end_in1", end, model.entity.ports.PortType.INPUT, new Point2D(660, 210));
        TrianglePort endIn2 = new TrianglePort("end_in2", end, model.entity.ports.PortType.INPUT, new Point2D(660, 230));
        end.addPort(endIn1);
        end.addPort(endIn2);
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
            } else if (system instanceof IntermediateSystem) {
                IntermediateSystemView view = new IntermediateSystemView((IntermediateSystem) system);
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
    public void setupWireControllerForPorts(controller.WireController wireController) {
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
    protected void restartLevel() {
        levelManager.restartCurrentLevel();
    }

    @Override
    protected void goToNextLevel() {
        levelManager.goToNextLevel();
    }
}


