package view.components.levels;

import javafx.geometry.Point2D;
import model.entity.systems.StartSystem;
import model.entity.systems.IntermediateSystem;
import model.entity.systems.EndSystem;
import model.entity.systems.DDosSystem;
import model.entity.ports.SquarePort;
import model.entity.ports.TrianglePort;
import model.entity.ports.HexagonPort;
import view.components.systems.StartSystemView;
import view.components.systems.IntermediateSystemView;
import view.components.systems.EndSystemView;
import view.components.systems.SystemView;
import view.components.ports.PortView;
import view.components.systems.DDosSystemView;
import controller.WireController;
import manager.game.LevelManager;

import java.util.ArrayList;
import java.util.List;

public class Level3View extends LevelView {
    private final LevelManager levelManager;
    private List<SystemView> systemViews = new ArrayList<>();

    public Level3View(model.levels.Level level, manager.game.VisualManager visualManager) {
        super(level, visualManager);
        this.levelManager = new LevelManager(visualManager, null, null); // Will be set properly
        setupLevel3Layout();
    }

    /**
     * Setup Level 3 specific layout for hexagon packet testing
     * Layout: 2 Start Systems -> 1 Intermediate System -> 1 End System
     */
    private void setupLevel3Layout() {
        // Create systems for Level 3 - hexagon packet testing
        createSystems();
        
        // Add system views to game pane
        addSystemViews();
        
        // Setup wire controller for port views
        setupWireController();
    }

    /**
     * Create systems for Level 3 - hexagon packet testing
     * Start System 1: 2 ports (triangle and square)
     * Start System 2: 1 port (hexagon)
     * Intermediate System: 3 input ports, 3 output ports (all types)
     * End System: 3 input ports (all types)
     */
    private void createSystems() {
        // Start System 1 (2 ports: triangle and square) - Top left
        StartSystem startSystem1 = new StartSystem(new Point2D(150, 120));
        TrianglePort start1Out1 = new TrianglePort("start1_out1", startSystem1, model.entity.ports.PortType.OUTPUT, new Point2D(190, 110));
        SquarePort start1Out2 = new SquarePort("start1_out2", startSystem1, model.entity.ports.PortType.OUTPUT, new Point2D(190, 130));
        startSystem1.addPort(start1Out1);
        startSystem1.addPort(start1Out2);
        level.addSystem(startSystem1);

        // Start System 2 (1 port: hexagon) - Bottom left
        StartSystem startSystem2 = new StartSystem(new Point2D(150, 280));
        HexagonPort start2Out1 = new HexagonPort("start2_out1", startSystem2, model.entity.ports.PortType.OUTPUT, new Point2D(190, 280));
        startSystem2.addPort(start2Out1);
        level.addSystem(startSystem2);

        // DDoS System (3 input ports, 3 output ports) - Center
        DDosSystem ddosSystem = new DDosSystem(new Point2D(400, 200));
        // Input ports (from start systems) - 20px spacing
        HexagonPort interIn1 = new HexagonPort("inter_in1", ddosSystem, model.entity.ports.PortType.INPUT, new Point2D(360, 180));
        SquarePort interIn2 = new SquarePort("inter_in2", ddosSystem, model.entity.ports.PortType.INPUT, new Point2D(360, 200));
        TrianglePort interIn3 = new TrianglePort("inter_in3", ddosSystem, model.entity.ports.PortType.INPUT, new Point2D(360, 220));
        // Output ports (to end system) - 20px spacing
        TrianglePort interOut1 = new TrianglePort("inter_out1", ddosSystem, model.entity.ports.PortType.OUTPUT, new Point2D(440, 180));
        SquarePort interOut2 = new SquarePort("inter_out2", ddosSystem, model.entity.ports.PortType.OUTPUT, new Point2D(440, 200));
        HexagonPort interOut3 = new HexagonPort("inter_out3", ddosSystem, model.entity.ports.PortType.OUTPUT, new Point2D(440, 220));
        ddosSystem.addPort(interIn1);
        ddosSystem.addPort(interIn2);
        ddosSystem.addPort(interIn3);
        ddosSystem.addPort(interOut1);
        ddosSystem.addPort(interOut2);
        ddosSystem.addPort(interOut3);
        level.addSystem(ddosSystem);

        // End System (3 ports: triangle, square, hexagon) - Right side
        EndSystem endSystem = new EndSystem(new Point2D(650, 200));
        HexagonPort endIn1 = new HexagonPort("end_in1", endSystem, model.entity.ports.PortType.INPUT, new Point2D(610, 180));
        SquarePort endIn2 = new SquarePort("end_in2", endSystem, model.entity.ports.PortType.INPUT, new Point2D(610, 200));
        TrianglePort endIn3 = new TrianglePort("end_in3", endSystem, model.entity.ports.PortType.INPUT, new Point2D(610, 220));
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
            if (system instanceof StartSystem) {
                StartSystemView view = new StartSystemView((StartSystem) system);
                view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
                view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
                gamePane.getChildren().add(view);
                addPortsToGamePane(view, system.getPosition());
                systemViews.add(view);
            } else if (system instanceof DDosSystem) {
                DDosSystemView view = new DDosSystemView((DDosSystem) system);
                view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
                view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
                gamePane.getChildren().add(view);
                addPortsToGamePane(view, system.getPosition());
                systemViews.add(view);
            } else if (system instanceof IntermediateSystem) {
                // IntermediateSystemView view = new IntermediateSystemView((IntermediateSystem) system);
                // view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
                // view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
                // gamePane.getChildren().add(view);
                // addPortsToGamePane(view, system.getPosition());
                // systemViews.add(view);
            } else if (system instanceof EndSystem) {
                EndSystemView view = new EndSystemView((EndSystem) system);
                view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
                view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
                gamePane.getChildren().add(view);
                addPortsToGamePane(view, system.getPosition());
                systemViews.add(view);
            }
        }
    }

    /**
     * Setup wire controller for port views
     */
    private void setupWireController() {
        WireController wireController = new WireController();
        wireController.setLevel(level);
        wireController.setGamePane(gamePane);
        
        // Set wire controller for all system views
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
     * Update all system indicators and play button states
     */
    public void updateSystemIndicators() {
        for (SystemView systemView : systemViews) {
            systemView.updateConnectionStatus();
            
            if (systemView instanceof StartSystemView) {
                StartSystemView startView = (StartSystemView) systemView;
                boolean allReady = true;
                
                // Check if all systems are ready
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
     * Setup start system play button actions and states
     */
    public void setupStartSystemPlayButtons(controller.GameController gameController) {
        for (SystemView systemView : systemViews) {
            if (systemView instanceof StartSystemView) {
                StartSystemView startView = (StartSystemView) systemView;
                
                // Set the play action to the game controller
                startView.setOnPlayAction(() -> gameController.handleStartSystemPlayButton());
                
                // Set the all systems ready checker
                startView.setAllSystemsReadyChecker(() -> gameController.areAllSystemsReady());
                
                // Initial button state update
                updateSystemIndicators();
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
            return 6.0; // Hexagon port size (updated to match the user's change)
        } else {
            return 10.0; // Default
        }
    }

    @Override
    protected void restartLevel() {
        visualManager.showGame(3);
    }

    @Override
    protected void goToNextLevel() {
        // Level 3 is the final test level, go back to menu
        visualManager.showMenu();
    }
} 