package view.components.levels;

import javafx.geometry.Point2D;
import model.entity.systems.StartSystem;
import model.entity.systems.DDosSystem;
import model.entity.systems.AntiVirusSystem;
import model.entity.systems.EndSystem;
import model.entity.ports.SquarePort;
import view.components.systems.StartSystemView;
import view.components.systems.DDosSystemView;
import view.components.systems.AntiVirusSystemView;
import view.components.systems.EndSystemView;
import view.components.systems.SystemView;
import view.components.ports.PortView;
import manager.game.LevelManager;

import java.util.ArrayList;
import java.util.List;

public class Level6View extends LevelView {
    private final LevelManager levelManager;
    private List<SystemView> systemViews = new ArrayList<>();

    public Level6View(model.levels.Level level, manager.game.VisualManager visualManager) {
        super(level, visualManager);
        this.levelManager = new LevelManager(visualManager, null, null); // Will be set properly
        setupLevel6Layout();
    }

    /**
     * Setup Level 6 specific layout to test AntiVirus system functionality
     * Layout: start -> ddos -> end
     *                 -> antivirus ->
     * DDos has 2 ports, one within AntiVirus radius and one outside
     */
    private void setupLevel6Layout() {
        // Create systems for Level 6
        createSystems();
        
        // Add system views to game pane
        addSystemViews();
        
        // Setup wire controller for port views
        setupWireController();
    }

    /**
     * Create systems for Level 6 - AntiVirus trojan detection testing
     * Layout: start -> ddos -> end
     *                 -> antivirus ->
     * DDos has 2 input ports and 2 output ports
     * AntiVirus has 1 input port and 1 output port  
     * One pair of DDos ports is within AntiVirus radius (120px), one isn't
     */
    private void createSystems() {
        // Start System (3 output ports) - Left side
        StartSystem startSystem = new StartSystem(new Point2D(120, 250));
        SquarePort startOut1 = new SquarePort("start_out1", startSystem, model.entity.ports.PortType.OUTPUT, new Point2D(160, 220));
        SquarePort startOut2 = new SquarePort("start_out2", startSystem, model.entity.ports.PortType.OUTPUT, new Point2D(160, 250));
        SquarePort startOut3 = new SquarePort("start_out3", startSystem, model.entity.ports.PortType.OUTPUT, new Point2D(160, 280));
        startSystem.addPort(startOut1);
        startSystem.addPort(startOut2);
        startSystem.addPort(startOut3);
        level.addSystem(startSystem);

        // DDos System - Creates trojan packets (20% chance)
        // Positioned with same X as AntiVirus but different Y (increased spacing)
        DDosSystem ddosSystem = new DDosSystem(new Point2D(350, 150));
        SquarePort ddosIn1 = new SquarePort("ddos_in1", ddosSystem, model.entity.ports.PortType.INPUT, new Point2D(310, 130));
        SquarePort ddosIn2 = new SquarePort("ddos_in2", ddosSystem, model.entity.ports.PortType.INPUT, new Point2D(310, 170));
        SquarePort ddosOut1 = new SquarePort("ddos_out1", ddosSystem, model.entity.ports.PortType.OUTPUT, new Point2D(390, 130));
        SquarePort ddosOut2 = new SquarePort("ddos_out2", ddosSystem, model.entity.ports.PortType.OUTPUT, new Point2D(390, 170));
        ddosSystem.addPort(ddosIn1);
        ddosSystem.addPort(ddosIn2);
        ddosSystem.addPort(ddosOut1);
        ddosSystem.addPort(ddosOut2);
        level.addSystem(ddosSystem);

        // AntiVirus System - Same X coordinate as DDos but different Y (increased spacing: 200px apart)
        // With increased spacing, both DDos output ports should be outside detection radius
        AntiVirusSystem antivirusSystem = new AntiVirusSystem(new Point2D(350, 350));
        SquarePort antivirusIn = new SquarePort("antivirus_in", antivirusSystem, model.entity.ports.PortType.INPUT, new Point2D(310, 350));
        SquarePort antivirusOut = new SquarePort("antivirus_out", antivirusSystem, model.entity.ports.PortType.OUTPUT, new Point2D(390, 350));
        antivirusSystem.addPort(antivirusIn);
        antivirusSystem.addPort(antivirusOut);
        level.addSystem(antivirusSystem);

        // End System - Receives packets from all paths (3 input ports)
        EndSystem endSystem = new EndSystem(new Point2D(520, 250));
        SquarePort endIn1 = new SquarePort("end_in1", endSystem, model.entity.ports.PortType.INPUT, new Point2D(480, 220));
        SquarePort endIn2 = new SquarePort("end_in2", endSystem, model.entity.ports.PortType.INPUT, new Point2D(480, 250));
        SquarePort endIn3 = new SquarePort("end_in3", endSystem, model.entity.ports.PortType.INPUT, new Point2D(480, 280));
        endSystem.addPort(endIn1);
        endSystem.addPort(endIn2);
        endSystem.addPort(endIn3);
        level.addSystem(endSystem);

        // Testing scenario:
        // - Packets from start flow through DDos system
        // - DDos and AntiVirus systems share same X coordinate (350) but different Y coordinates
        // - Increased Y spacing: DDos at Y=150, AntiVirus at Y=350 (200px apart)
        // - AntiVirus detection radius increased to 200px for better testing
        // - Some trojans created by DDos will be in AntiVirus detection range
        // - AntiVirus can clean trojans both by entry and radius detection
        // - End system has 3 input ports to receive packets from multiple paths
        // Distance calculations (200px detection radius):
        // - ddosOut1 (390,130) to AntiVirus (350,350): sqrt((390-350)^2 + (130-350)^2) = sqrt(1600+48400) = 224px (OUTSIDE 200px radius)
        // - ddosOut2 (390,170) to AntiVirus (350,350): sqrt((390-350)^2 + (170-350)^2) = sqrt(1600+32400) = 184px (WITHIN 200px radius)
    }

    /**
     * Add system views to the game pane
     */
    private void addSystemViews() {
        for (model.entity.systems.System system : level.getSystems()) {
            SystemView systemView = null;
            
            if (system instanceof StartSystem) {
                systemView = new StartSystemView((StartSystem) system);
            } else if (system instanceof DDosSystem) {
                systemView = new DDosSystemView((DDosSystem) system);
            } else if (system instanceof AntiVirusSystem) {
                systemView = new AntiVirusSystemView((AntiVirusSystem) system);
            } else if (system instanceof EndSystem) {
                systemView = new EndSystemView((EndSystem) system);
            }
            
            if (systemView != null) {
                systemView.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH/2);
                systemView.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT/2);
                systemViews.add(systemView);
                gamePane.getChildren().add(systemView);
                
                // Add ports directly to game pane with absolute coordinates
                addPortsToGamePane(systemView, system.getPosition());
            }
        }
    }

    /**
     * Setup wire controller for connecting ports
     */
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

    /**
     * Add ports to game pane with absolute positioning
     */
    private void addPortsToGamePane(SystemView systemView, Point2D systemPosition) {
        // Add input ports using their model positions
        for (int i = 0; i < systemView.getInputPortViews().size(); i++) {
            PortView portView = systemView.getInputPortViews().get(i);
            model.entity.ports.Port modelPort = systemView.getSystem().getInPorts().get(i);
            
            // Get the actual port size based on port type
            double portSize = getPortSize(portView);
            portView.setLayoutX(modelPort.getPosition().getX() - portSize / 2);
            portView.setLayoutY(modelPort.getPosition().getY() - portSize / 2);
            gamePane.getChildren().add(portView);
        }
        
        // Add output ports using their model positions
        for (int i = 0; i < systemView.getOutputPortViews().size(); i++) {
            PortView portView = systemView.getOutputPortViews().get(i);
            model.entity.ports.Port modelPort = systemView.getSystem().getOutPorts().get(i);
            
            // Get the actual port size based on port type
            double portSize = getPortSize(portView);
            portView.setLayoutX(modelPort.getPosition().getX() - portSize / 2);
            portView.setLayoutY(modelPort.getPosition().getY() - portSize / 2);
            gamePane.getChildren().add(portView);
        }
    }

    /**
     * Get the actual size of a port based on its type
     */
    private double getPortSize(PortView portView) {
        if (portView instanceof view.components.ports.TrianglePortView) {
            return 15.0; // Triangle port size
        } else if (portView instanceof view.components.ports.SquarePortView) {
            return 10.0; // Square port size
        } else if (portView instanceof view.components.ports.HexagonPortView) {
            return 6.0; // Hexagon port size
        } else {
            return 10.0; // Default
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