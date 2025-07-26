package view.components.levels;

import javafx.geometry.Point2D;
import model.entity.systems.StartSystem;
import model.entity.systems.IntermediateSystem;
import model.entity.systems.EndSystem;
import model.entity.ports.SquarePort;
import model.entity.ports.TrianglePort;
import view.components.systems.StartSystemView;
import view.components.systems.IntermediateSystemView;
import view.components.systems.EndSystemView;
import view.components.systems.SystemView;
import view.components.ports.PortView;
import view.components.ports.TrianglePortView;
import controller.WireController;
import manager.game.LevelManager;

import java.util.ArrayList;
import java.util.List;

public class Level2View extends LevelView {
    private final LevelManager levelManager;
    private List<SystemView> systemViews = new ArrayList<>();

    public Level2View(model.levels.Level level, manager.game.VisualManager visualManager) {
        super(level, visualManager);
        this.levelManager = new LevelManager(visualManager, null, null); // Will be set properly
        setupLevel2Layout();
    }

    /**
     * Setup Level 2 specific layout with systems arranged to create packet collisions
     * and demonstrate impact wave deflections
     */
    private void setupLevel2Layout() {
        // Create systems for Level 2 - designed for collisions
        createSystems();
        
        // Add system views to game pane
        addSystemViews();
        
        // Setup wire controller for port views
        setupWireController();
    }

    /**
     * Create systems for Level 2 - designed to create packet collisions
     * Multiple start systems will send packets that intersect in the middle
     */
    private void createSystems() {
        // Start System 1 (2 ports: triangle and square) - Top left
        StartSystem startSystem1 = new StartSystem(new Point2D(200, 150));
        TrianglePort start1Out1 = new TrianglePort("start1_out1", startSystem1, model.entity.ports.PortType.OUTPUT, new Point2D(240, 140));
        SquarePort start1Out2 = new SquarePort("start1_out2", startSystem1, model.entity.ports.PortType.OUTPUT, new Point2D(240, 160));
        startSystem1.addPort(start1Out1);
        startSystem1.addPort(start1Out2);
        level.addSystem(startSystem1);

        // Start System 2 (1 port: triangle) - Bottom left (parallel to Start System 1)
        StartSystem startSystem2 = new StartSystem(new Point2D(200, 270));
        TrianglePort start2Out1 = new TrianglePort("start2_out1", startSystem2, model.entity.ports.PortType.OUTPUT, new Point2D(240, 270));
        startSystem2.addPort(start2Out1);
        level.addSystem(startSystem2);

        // Intermediate System (3 input ports, 3 output ports) - Center
        IntermediateSystem intermediateSystem = new IntermediateSystem(new Point2D(450, 210));
        // Input ports (from start systems)
        TrianglePort interIn1 = new TrianglePort("inter_in1", intermediateSystem, model.entity.ports.PortType.INPUT, new Point2D(410, 200));
        SquarePort interIn2 = new SquarePort("inter_in2", intermediateSystem, model.entity.ports.PortType.INPUT, new Point2D(410, 220));
        TrianglePort interIn3 = new TrianglePort("inter_in3", intermediateSystem, model.entity.ports.PortType.INPUT, new Point2D(410, 240));
        // Output ports (to end system)
        TrianglePort interOut1 = new TrianglePort("inter_out1", intermediateSystem, model.entity.ports.PortType.OUTPUT, new Point2D(490, 200));
        TrianglePort interOut2 = new TrianglePort("inter_out2", intermediateSystem, model.entity.ports.PortType.OUTPUT, new Point2D(490, 220));
        SquarePort interOut3 = new SquarePort("inter_out3", intermediateSystem, model.entity.ports.PortType.OUTPUT, new Point2D(490, 240));
        intermediateSystem.addPort(interIn1);
        intermediateSystem.addPort(interIn2);
        intermediateSystem.addPort(interIn3);
        intermediateSystem.addPort(interOut1);
        intermediateSystem.addPort(interOut2);
        intermediateSystem.addPort(interOut3);
        level.addSystem(intermediateSystem);

        // End System (3 ports: triangle, triangle, square) - Right side
        EndSystem endSystem = new EndSystem(new Point2D(700, 210));
        TrianglePort endIn1 = new TrianglePort("end_in1", endSystem, model.entity.ports.PortType.INPUT, new Point2D(660, 200));
        TrianglePort endIn2 = new TrianglePort("end_in2", endSystem, model.entity.ports.PortType.INPUT, new Point2D(660, 220));
        SquarePort endIn3 = new SquarePort("end_in3", endSystem, model.entity.ports.PortType.INPUT, new Point2D(660, 240));
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
                
                // Add ports directly to game pane with absolute coordinates
                addPortsToGamePane(view, system.getPosition());
                
                // Store the view for later reference
                systemViews.add(view);
            } else if (system instanceof IntermediateSystem) {
                IntermediateSystemView view = new IntermediateSystemView((IntermediateSystem) system);
                view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
                view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
                gamePane.getChildren().add(view);
                
                // Add ports directly to game pane with absolute coordinates
                addPortsToGamePane(view, system.getPosition());
                
                // Store the view for later reference
                systemViews.add(view);
            } else if (system instanceof EndSystem) {
                EndSystemView view = new EndSystemView((EndSystem) system);
                view.setLayoutX(system.getPosition().getX() - SystemView.SYSTEM_WIDTH / 2);
                view.setLayoutY(system.getPosition().getY() - SystemView.SYSTEM_HEIGHT / 2);
                gamePane.getChildren().add(view);
                
                // Add ports directly to game pane with absolute coordinates
                addPortsToGamePane(view, system.getPosition());
                
                // Store the view for later reference
                systemViews.add(view);
            }
        }
    }
    
    /**
     * Setup play button actions for start systems
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
     * Add ports to game pane with absolute coordinates
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
        if (portView instanceof TrianglePortView) {
            return 15.0; // Triangle port size
        } else {
            return 10.0; // Square port size
        }
    }

    /**
     * Setup wire controller for port views
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

    @Override
    protected void restartLevel() {
        levelManager.restartCurrentLevel();
    }

    @Override
    protected void goToNextLevel() {
        levelManager.goToNextLevel();
    }
}
