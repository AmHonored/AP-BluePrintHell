package view.components.levels;

import javafx.geometry.Point2D;
import model.entity.systems.*;
import model.entity.ports.*;
import view.components.systems.*;
import view.components.ports.PortView;
import controller.WireController;
import manager.game.LevelManager;

import java.util.ArrayList;
import java.util.List;

public class Level5View extends LevelView {
    private final LevelManager levelManager;
    private List<SystemView> systemViews = new ArrayList<>();

    public Level5View(model.levels.Level level, manager.game.VisualManager visualManager) {
        super(level, visualManager);
        this.levelManager = new LevelManager(visualManager, null, null);
        setupLevel5Layout();
        setupVPNVisualUpdater();
    }

    /**
     * Setup Level 5 layout - VPN System Test Lab
     * Test VPN system functionality:
     * - Square packets → VPN1 → Protected packets
     * - Triangle/Hexagon packets → VPN2 → May cause VPN failure
     * - Protected packets → DDoS → Convert back to original
     * - 5 coins for protected packets reaching end
     */
    private void setupLevel5Layout() {
        createSystems();
        addSystemViews();
        setupWireController();
    }

    /**
     * Create VPN system testing scenario
     * Simple layout: Start → VPN → DDoS → End (testing all VPN features)
     */
    private void createSystems() {
        // Start System (multiple packet types for comprehensive testing)
        StartSystem startSystem = new StartSystem(new Point2D(100, 200));
        SquarePort startOut1 = new SquarePort("start_out1", startSystem, PortType.OUTPUT, new Point2D(140, 180));
        TrianglePort startOut2 = new TrianglePort("start_out2", startSystem, PortType.OUTPUT, new Point2D(140, 200));
        HexagonPort startOut3 = new HexagonPort("start_out3", startSystem, PortType.OUTPUT, new Point2D(140, 220));
        startSystem.addPort(startOut1);
        startSystem.addPort(startOut2);
        startSystem.addPort(startOut3);
        level.addSystem(startSystem);

        // VPN System 1: Convert packets to protected packets
        VPNSystem vpnSystem1 = new VPNSystem(new Point2D(260, 150));
        SquarePort vpn1In1 = new SquarePort("vpn1_in1", vpnSystem1, PortType.INPUT, new Point2D(220, 150));
        SquarePort vpn1Out1 = new SquarePort("vpn1_out1", vpnSystem1, PortType.OUTPUT, new Point2D(300, 150));
        vpnSystem1.addPort(vpn1In1);
        vpnSystem1.addPort(vpn1Out1);
        level.addSystem(vpnSystem1);

        // VPN System 2: Test failure scenario with fast packets
        VPNSystem vpnSystem2 = new VPNSystem(new Point2D(260, 270));
        TrianglePort vpn2In1 = new TrianglePort("vpn2_in1", vpnSystem2, PortType.INPUT, new Point2D(220, 250));
        HexagonPort vpn2In2 = new HexagonPort("vpn2_in2", vpnSystem2, PortType.INPUT, new Point2D(220, 290));
        TrianglePort vpn2Out1 = new TrianglePort("vpn2_out1", vpnSystem2, PortType.OUTPUT, new Point2D(300, 250));
        HexagonPort vpn2Out2 = new HexagonPort("vpn2_out2", vpnSystem2, PortType.OUTPUT, new Point2D(300, 290));
        vpnSystem2.addPort(vpn2In1);
        vpnSystem2.addPort(vpn2In2);
        vpnSystem2.addPort(vpn2Out1);
        vpnSystem2.addPort(vpn2Out2);
        level.addSystem(vpnSystem2);

        // DDoS System: Test protected packet conversion back to original
        DDosSystem ddosSystem = new DDosSystem(new Point2D(460, 200));
        SquarePort ddosIn1 = new SquarePort("ddos_in1", ddosSystem, PortType.INPUT, new Point2D(420, 180));
        TrianglePort ddosIn2 = new TrianglePort("ddos_in2", ddosSystem, PortType.INPUT, new Point2D(420, 200));
        HexagonPort ddosIn3 = new HexagonPort("ddos_in3", ddosSystem, PortType.INPUT, new Point2D(420, 220));
        SquarePort ddosOut1 = new SquarePort("ddos_out1", ddosSystem, PortType.OUTPUT, new Point2D(500, 180));
        TrianglePort ddosOut2 = new TrianglePort("ddos_out2", ddosSystem, PortType.OUTPUT, new Point2D(500, 200));
        HexagonPort ddosOut3 = new HexagonPort("ddos_out3", ddosSystem, PortType.OUTPUT, new Point2D(500, 220));
        ddosSystem.addPort(ddosIn1);
        ddosSystem.addPort(ddosIn2);
        ddosSystem.addPort(ddosIn3);
        ddosSystem.addPort(ddosOut1);
        ddosSystem.addPort(ddosOut2);
        ddosSystem.addPort(ddosOut3);
        level.addSystem(ddosSystem);

        // End System: Collect all packet types
        EndSystem endSystem = new EndSystem(new Point2D(660, 200));
        SquarePort endIn1 = new SquarePort("end_in1", endSystem, PortType.INPUT, new Point2D(620, 180));
        TrianglePort endIn2 = new TrianglePort("end_in2", endSystem, PortType.INPUT, new Point2D(620, 200));
        HexagonPort endIn3 = new HexagonPort("end_in3", endSystem, PortType.INPUT, new Point2D(620, 220));
        endSystem.addPort(endIn1);
        endSystem.addPort(endIn2);
        endSystem.addPort(endIn3);
        level.addSystem(endSystem);
    }

    /**
     * Add system views to the game pane
     */
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

    /**
     * Create appropriate system view based on system type
     */
    private SystemView createSystemView(model.entity.systems.System system) {
        if (system instanceof StartSystem) {
            return new StartSystemView((StartSystem) system);
        } else if (system instanceof VPNSystem) {
            return new VPNSystemView((VPNSystem) system);
        } else if (system instanceof IntermediateSystem) {
            return new IntermediateSystemView((IntermediateSystem) system);
        } else if (system instanceof DDosSystem) {
            return new DDosSystemView((DDosSystem) system);
        } else if (system instanceof SpySystem) {
            return new SpySystemView((SpySystem) system);
        } else if (system instanceof DistributorSystem) {
            return new DistributorSystemView((DistributorSystem) system);
        } else if (system instanceof model.entity.systems.MergeSystem) {
            return new MergeSystemView((model.entity.systems.MergeSystem) system);
        } else if (system instanceof EndSystem) {
            return new EndSystemView((EndSystem) system);
        }
        return null;
    }

    /**
     * Setup wire controller for port views
     */
    private void setupWireController() {
        WireController wireController = new WireController();
        wireController.setLevel(level);
        wireController.setGamePane(gamePane);
        
        for (SystemView systemView : systemViews) {
            systemView.setWireController(wireController);
        }
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
     * Update all system indicators and VPN system states
     */
    public void updateSystemIndicators() {
        for (SystemView systemView : systemViews) {
            systemView.updateConnectionStatus();
            
            // Update VPN system visual state
            if (systemView instanceof VPNSystemView) {
                VPNSystemView vpnView = (VPNSystemView) systemView;
                vpnView.updateVPNVisuals();
            }
            
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
    
    /**
     * Setup VPN visual updater to handle VPN system failure visual updates
     */
    private void setupVPNVisualUpdater() {
        manager.systems.VPNSystemManager.setVisualUpdater(new manager.systems.VPNSystemManager.VPNVisualUpdater() {
            @Override
            public void updateVPNSystemVisuals() {
                // Update all VPN system visuals when any VPN fails
                for (SystemView systemView : systemViews) {
                    if (systemView instanceof VPNSystemView) {
                        VPNSystemView vpnView = (VPNSystemView) systemView;
                        vpnView.updateVPNVisuals();
                                                 java.lang.System.out.println("🔄 UPDATED VPN VISUAL: " + vpnView.getVPNSystem().getPosition() + 
                            " - disabled: " + vpnView.getVPNSystem().isDisabled());
                    }
                }
            }
        });
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
     * Add ports to game pane with absolute positioning
     */
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

    /**
     * Get port size based on type
     */
    private double getPortSize(PortView portView) {
        if (portView instanceof view.components.ports.TrianglePortView) {
            return 15.0;
        } else if (portView instanceof view.components.ports.SquarePortView) {
            return 10.0;
        } else if (portView instanceof view.components.ports.HexagonPortView) {
            return 6.0;
        } else {
            return 10.0;
        }
    }

    @Override
    protected void restartLevel() {
        // Implementation for restarting Level 5
    }

    @Override
    protected void goToNextLevel() {
        // Implementation for going to next level after Level 5
    }
} 