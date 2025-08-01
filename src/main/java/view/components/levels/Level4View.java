package view.components.levels;

import javafx.geometry.Point2D;
import model.entity.systems.StartSystem;
import model.entity.systems.SpySystem;
import model.entity.systems.EndSystem;
import model.entity.ports.SquarePort;
import model.entity.ports.TrianglePort;
import view.components.systems.StartSystemView;
import view.components.systems.SpySystemView;
import view.components.systems.EndSystemView;
import view.components.systems.SystemView;
import view.components.ports.PortView;
import controller.WireController;
import manager.game.LevelManager;

import java.util.ArrayList;
import java.util.List;

public class Level4View extends LevelView {
    private final LevelManager levelManager;
    private List<SystemView> systemViews = new ArrayList<>();

    public Level4View(model.levels.Level level, manager.game.VisualManager visualManager) {
        super(level, visualManager);
        this.levelManager = new LevelManager(visualManager, null, null); // Will be set properly
        setupLevel4Layout();
    }

    /**
     * Setup Level 4 specific layout with spy system scenario
     * Layout: Start -> Spy1 -> Spy2/Spy3 -> End
     */
    private void setupLevel4Layout() {
        // Create systems for Level 4 - spy system testing
        createSystems();
        
        // Add system views to game pane
        addSystemViews();
        
        // Setup wire controller for port views
        setupWireController();
    }

    /**
     * Create systems for Level 4 - spy system scenario
     * Start System: 1 output port (square)
     * Spy System 1: 1 input port (square)
     * Spy System 2: 2 output ports (square, triangle)
     * Spy System 3: 2 output ports (square, triangle)
     * End System: 2 input ports (square, triangle)
     */
    private void createSystems() {
        // Start System (1 output port: square) - Left side
        StartSystem startSystem = new StartSystem(new Point2D(120, 200));
        SquarePort startOut1 = new SquarePort("start_out1", startSystem, model.entity.ports.PortType.OUTPUT, new Point2D(160, 200));
        startSystem.addPort(startOut1);
        level.addSystem(startSystem);

        // Spy System 1 (1 input port: square) - Center left
        SpySystem spySystem1 = new SpySystem(new Point2D(280, 200));
        SquarePort spy1In1 = new SquarePort("spy1_in1", spySystem1, model.entity.ports.PortType.INPUT, new Point2D(240, 200));
        spySystem1.addPort(spy1In1);
        level.addSystem(spySystem1);

        // Spy System 2 (1 output port: square) - Top right
        SpySystem spySystem2 = new SpySystem(new Point2D(440, 140));
        SquarePort spy2Out1 = new SquarePort("spy2_out1", spySystem2, model.entity.ports.PortType.OUTPUT, new Point2D(480, 140));
        spySystem2.addPort(spy2Out1);
        level.addSystem(spySystem2);

        // Spy System 3 (1 output port: triangle) - Bottom right
        SpySystem spySystem3 = new SpySystem(new Point2D(440, 260));
        TrianglePort spy3Out2 = new TrianglePort("spy3_out2", spySystem3, model.entity.ports.PortType.OUTPUT, new Point2D(480, 260));
        spySystem3.addPort(spy3Out2);
        level.addSystem(spySystem3);

        // End System (2 input ports: square, triangle) - Right side
        EndSystem endSystem = new EndSystem(new Point2D(620, 200));
        SquarePort endIn1 = new SquarePort("end_in1", endSystem, model.entity.ports.PortType.INPUT, new Point2D(580, 190));
        TrianglePort endIn2 = new TrianglePort("end_in2", endSystem, model.entity.ports.PortType.INPUT, new Point2D(580, 210));
        endSystem.addPort(endIn1);
        endSystem.addPort(endIn2);
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
            } else if (system instanceof SpySystem) {
                SpySystemView view = new SpySystemView((SpySystem) system);
                view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
                view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
                gamePane.getChildren().add(view);
                addPortsToGamePane(view, system.getPosition());
                systemViews.add(view);
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
            return 6.0; // Hexagon port size
        } else {
            return 10.0; // Default
        }
    }

    @Override
    protected void restartLevel() {
        // Implementation for restarting Level 4
    }

    @Override
    protected void goToNextLevel() {
        // Implementation for going to next level after Level 4
    }
} 