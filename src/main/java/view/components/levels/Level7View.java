package view.components.levels;

import javafx.geometry.Point2D;
import model.entity.systems.StartSystem;
import model.entity.systems.VPNSystem;
import model.entity.systems.IntermediateSystem;
import model.entity.systems.DistributorSystem;
import model.entity.systems.EndSystem;
import model.entity.ports.SquarePort;
import model.entity.ports.TrianglePort;
import view.components.systems.StartSystemView;
import view.components.systems.VPNSystemView;
import view.components.systems.IntermediateSystemView;
import view.components.systems.EndSystemView;
import view.components.systems.DistributorSystemView;
import view.components.systems.SystemView;
import view.components.ports.PortView;
import view.components.ports.TrianglePortView;
import manager.game.LevelManager;

import java.util.ArrayList;
import java.util.List;

public class Level7View extends LevelView {
    private final LevelManager levelManager;
    private List<SystemView> systemViews = new ArrayList<>();

    public Level7View(model.levels.Level level, manager.game.VisualManager visualManager) {
        super(level, visualManager);
        this.levelManager = new LevelManager(visualManager, null, null);
        setupLevel7Layout();
    }

    /**
     * Setup Level 7 layout to test confidential packet behaviors:
     * 
     * Type 1 Behavior Test: Speed reduction when destination system has packets
     * Type 2 Behavior Test: Distance maintenance through VPN transformation
     * 
     * Layout:
     * StartSystem1(Confidential) --------> VPN --------> IntermediateSystem1 -----> EndSystem1
     * StartSystem2(Square)      --------> IntermediateSystem2 --------> EndSystem2
     * StartSystem3(Triangle)    --------> IntermediateSystem2 --------> EndSystem2
     * 
     * This layout allows:
     * 1. Confidential Type1 packets to go through VPN and become Type2
     * 2. Type1 packets to test speed reduction when IntermediateSystem1 has stored packets
     * 3. Type2 packets to test distance maintenance with other packets on network
     * 4. Multiple packet flows for interaction testing
     */
    private void setupLevel7Layout() {
        createSystems();
        addSystemViews();
        setupWireController();
    }

    private void createSystems() {
        // Start System 1 - Confidential packets will be generated randomly (20% chance per port)
        StartSystem startSystem1 = new StartSystem(new Point2D(120, 150));
        SquarePort confOut1 = new SquarePort("conf_out1", startSystem1, model.entity.ports.PortType.OUTPUT, new Point2D(160, 150));
        startSystem1.addPort(confOut1);
        level.addSystem(startSystem1);

        // Start System 2 - Square packets (middle)  
        StartSystem startSystem2 = new StartSystem(new Point2D(120, 250));
        SquarePort sqOut1 = new SquarePort("sq_out1", startSystem2, model.entity.ports.PortType.OUTPUT, new Point2D(160, 250));
        startSystem2.addPort(sqOut1);
        level.addSystem(startSystem2);

        // Start System 3 - Triangle packets (bottom)
        StartSystem startSystem3 = new StartSystem(new Point2D(120, 350));
        TrianglePort triOut1 = new TrianglePort("tri_out1", startSystem3, model.entity.ports.PortType.OUTPUT, new Point2D(160, 350));
        startSystem3.addPort(triOut1);
        level.addSystem(startSystem3);

        // VPN System - Transforms Type1 to Type2 confidential packets
        VPNSystem vpnSystem = new VPNSystem(new Point2D(320, 150));
        SquarePort vpnIn = new SquarePort("vpn_in", vpnSystem, model.entity.ports.PortType.INPUT, new Point2D(280, 150));
        SquarePort vpnOut = new SquarePort("vpn_out", vpnSystem, model.entity.ports.PortType.OUTPUT, new Point2D(360, 150));
        vpnSystem.addPort(vpnIn);
        vpnSystem.addPort(vpnOut);
        level.addSystem(vpnSystem);

        // Intermediate System 1 - For confidential packets to test speed reduction
        IntermediateSystem intermediateSystem1 = new IntermediateSystem(new Point2D(520, 150));
        SquarePort inter1In = new SquarePort("inter1_in", intermediateSystem1, model.entity.ports.PortType.INPUT, new Point2D(480, 150));
        SquarePort inter1Out = new SquarePort("inter1_out", intermediateSystem1, model.entity.ports.PortType.OUTPUT, new Point2D(560, 150));
        intermediateSystem1.addPort(inter1In);
        intermediateSystem1.addPort(inter1Out);
        level.addSystem(intermediateSystem1);

        // Intermediate System 2 - For other packets to create traffic and test interactions
        IntermediateSystem intermediateSystem2 = new IntermediateSystem(new Point2D(320, 300));
        SquarePort inter2In1 = new SquarePort("inter2_in1", intermediateSystem2, model.entity.ports.PortType.INPUT, new Point2D(280, 280));
        TrianglePort inter2In2 = new TrianglePort("inter2_in2", intermediateSystem2, model.entity.ports.PortType.INPUT, new Point2D(280, 320));
        SquarePort inter2Out1 = new SquarePort("inter2_out1", intermediateSystem2, model.entity.ports.PortType.OUTPUT, new Point2D(360, 280));
        TrianglePort inter2Out2 = new TrianglePort("inter2_out2", intermediateSystem2, model.entity.ports.PortType.OUTPUT, new Point2D(360, 320));
        intermediateSystem2.addPort(inter2In1);
        intermediateSystem2.addPort(inter2In2);
        intermediateSystem2.addPort(inter2Out1);
        intermediateSystem2.addPort(inter2Out2);
        level.addSystem(intermediateSystem2);

        // End System 1 - For confidential packets
        EndSystem endSystem1 = new EndSystem(new Point2D(720, 150));
        SquarePort end1In = new SquarePort("end1_in", endSystem1, model.entity.ports.PortType.INPUT, new Point2D(680, 150));
        endSystem1.addPort(end1In);
        level.addSystem(endSystem1);

        // End System 2 - For other packets  
        EndSystem endSystem2 = new EndSystem(new Point2D(520, 300));
        SquarePort end2In1 = new SquarePort("end2_in1", endSystem2, model.entity.ports.PortType.INPUT, new Point2D(480, 280));
        TrianglePort end2In2 = new TrianglePort("end2_in2", endSystem2, model.entity.ports.PortType.INPUT, new Point2D(480, 320));
        endSystem2.addPort(end2In1);
        endSystem2.addPort(end2In2);
        level.addSystem(endSystem2);
    }

    private void addSystemViews() {
        for (model.entity.systems.System system : level.getSystems()) {
            SystemView view = createSystemView(system);
            if (view != null) {
                view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
                view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
                gamePane.getChildren().add(view);
                addPortsToGamePane(view, system.getPosition());
                systemViews.add(view);
            }
        }
    }

    private SystemView createSystemView(model.entity.systems.System system) {
        if (system instanceof StartSystem) {
            return new StartSystemView((StartSystem) system);
        } else if (system instanceof VPNSystem) {
            return new VPNSystemView((VPNSystem) system);
        } else if (system instanceof IntermediateSystem) {
            return new IntermediateSystemView((IntermediateSystem) system);
        } else if (system instanceof DistributorSystem) {
            return new DistributorSystemView((DistributorSystem) system);
        } else if (system instanceof EndSystem) {
            return new EndSystemView((EndSystem) system);
        }
        return null;
    }

    /**
     * Setup play button actions for start systems
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
        // Add input ports
        for (int i = 0; i < systemView.getInputPortViews().size(); i++) {
            PortView portView = systemView.getInputPortViews().get(i);
            model.entity.ports.Port modelPort = systemView.getSystem().getInPorts().get(i);
            
            double portSize = getPortSize(portView);
            portView.setLayoutX(modelPort.getPosition().getX() - portSize / 2);
            portView.setLayoutY(modelPort.getPosition().getY() - portSize / 2);
            gamePane.getChildren().add(portView);
        }
        
        // Add output ports
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
            return 10.0; // Square port size
        }
    }

    private void setupWireController() {
        // This will be handled by the GameController when it's initialized
    }

    public void setupWireControllerForPorts(controller.WireController wireController) {
        for (javafx.scene.Node node : gamePane.getChildren()) {
            if (node instanceof view.components.systems.SystemView) {
                view.components.systems.SystemView systemView = (view.components.systems.SystemView) node;
                systemView.setWireController(wireController);
            }
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