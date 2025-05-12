package com.networkgame.view;

import com.networkgame.model.*;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles rendering of network systems in the game
 */
public class SystemRenderer {
    private final GameState gameState;
    private final Pane gamePane;
    private final UIComponentFactory uiComponentFactory;
    private final GameEventHandler gameEventHandler;
    
    // Map to store capacity labels for systems
    private final Map<NetworkSystem, Label> systemCapacityLabels = new HashMap<>();
    
    // Constants for styling and positioning
    private static final int MAX_CAPACITY = 5;
    private static final double NORMAL_CAPACITY_THRESHOLD = 0.4;
    private static final double MEDIUM_CAPACITY_THRESHOLD = 0.7;
    private static final double HIGH_CAPACITY_THRESHOLD = 0.9;
    private static final double LABEL_VERTICAL_OFFSET = 5.0;
    private static final String PULSE_ANIMATION_KEY = "pulse_animation";
    
    private static final String SYSTEM_LABEL_STYLE = "system-label";
    private static final String CAPACITY_LABEL_BASE_STYLE = "capacity-label";
    private static final String DEFAULT_CAPACITY_STYLE = "-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-padding: 2 5; -fx-background-radius: 3;";
    
    // Capacity level enum for cleaner styling logic
    private enum CapacityLevel {
        NORMAL(Color.rgb(0, 180, 0), null, "-fx-font-weight: normal; -fx-background-color: transparent;", false),
        MEDIUM(Color.rgb(220, 180, 0), null, "-fx-font-weight: normal; -fx-background-color: transparent;", false),
        HIGH(Color.rgb(255, 140, 0), new Glow(0.4), "-fx-font-weight: bold; -fx-background-color: rgba(255,200,0,0.3); -fx-padding: 2px 4px; -fx-background-radius: 3px;", false),
        CRITICAL(Color.rgb(220, 0, 0), new Glow(0.8), "-fx-font-weight: bold; -fx-background-color: rgba(255,0,0,0.3); -fx-padding: 2px 4px; -fx-background-radius: 3px;", true);
        
        final Color textColor;
        final Glow glowEffect;
        final String styleString;
        final boolean needsPulse;
        
        CapacityLevel(Color textColor, Glow glowEffect, String styleString, boolean needsPulse) {
            this.textColor = textColor;
            this.glowEffect = glowEffect;
            this.styleString = styleString;
            this.needsPulse = needsPulse;
        }
        
        // Get level based on capacity percentage
        static CapacityLevel fromPercentage(double percentage) {
            if (percentage < NORMAL_CAPACITY_THRESHOLD) return NORMAL;
            if (percentage < MEDIUM_CAPACITY_THRESHOLD) return MEDIUM;
            if (percentage < HIGH_CAPACITY_THRESHOLD) return HIGH;
            return CRITICAL;
        }
    }

    public SystemRenderer(GameState gameState, Pane gamePane,
                         UIComponentFactory uiComponentFactory, GameEventHandler gameEventHandler) {
        this.gameState = gameState;
        this.gamePane = gamePane;
        this.uiComponentFactory = uiComponentFactory;
        this.gameEventHandler = gameEventHandler;
    }

    /**
     * Initialize and render all network systems and their components
     */
    public void initializeSystemElements() {
        resetGamePane();
        renderSystems();
        renderPorts();
        gameEventHandler.setSystemCapacityLabels(systemCapacityLabels);
    }

    /**
     * Reset the game pane and capacity labels
     */
    private void resetGamePane() {
        gamePane.getChildren().clear();
        systemCapacityLabels.clear();
    }

    /**
     * Render all network systems and their components
     */
    private void renderSystems() {
        for (NetworkSystem system : gameState.getSystems()) {
            renderSystemComponents(system);
        }
    }
    
    /**
     * Render all components for a single system
     */
    private void renderSystemComponents(NetworkSystem system) {
        renderSystemShape(system);
        renderInnerBox(system);
        addIndicatorLamp(system);
        
        if (!system.isStartSystem() && !system.isEndSystem()) {
            addCapacityLabel(system);
        }
        
        if (system.isStartSystem()) {
            createAndAddPlayButton(system);
        }
        
        makeSystemDraggable(system);
    }

    /**
     * Render a system's shape with appropriate styling
     */
    private void renderSystemShape(NetworkSystem system) {
        Shape systemShape = system.getShape();
        systemShape.getStyleClass().add("network-system");
        
        addSystemTypeStyle(system, systemShape);
        gamePane.getChildren().add(systemShape);
    }
    
    /**
     * Add system type specific styling
     */
    private void addSystemTypeStyle(NetworkSystem system, Shape systemShape) {
        if (system.isStartSystem()) {
            systemShape.getStyleClass().add("system-start");
            addSystemLabel(system, "START", 25);
        } else if (system.isEndSystem()) {
            systemShape.getStyleClass().add("system-end");
            addSystemLabel(system, "END", 15);
        } else if (system.isReference()) {
            systemShape.getStyleClass().add("system-reference");
        } else {
            systemShape.getStyleClass().add("system-normal");
        }
    }

    /**
     * Add a label to a system
     */
    private void addSystemLabel(NetworkSystem system, String text, double xOffset) {
        Label label = createPositionedLabel(
            text,
            system.getPosition().getX() + system.getWidth()/2 - xOffset,
            system.getPosition().getY() + system.getHeight() + LABEL_VERTICAL_OFFSET
        );
        label.getStyleClass().add(SYSTEM_LABEL_STYLE);
        gamePane.getChildren().add(label);
    }
    
    /**
     * Create a label with position
     */
    private Label createPositionedLabel(String text, double x, double y) {
        Label label = new Label(text);
        label.setLayoutX(x);
        label.setLayoutY(y);
        return label;
    }

    /**
     * Render the inner box for non-start/end systems
     */
    private void renderInnerBox(NetworkSystem system) {
        if (!system.isStartSystem() && !system.isEndSystem()) {
            Rectangle innerBox = system.getInnerBox();
            if (innerBox != null) {
                gamePane.getChildren().add(innerBox);
                innerBox.toFront();
            }
        }
    }

    /**
     * Add indicator lamp to the system
     */
    private void addIndicatorLamp(NetworkSystem system) {
        gamePane.getChildren().add(system.getIndicatorLamp());
        system.updateIndicatorLamp();
    }

    /**
     * Add capacity label for non-start/end systems
     */
    private void addCapacityLabel(NetworkSystem system) {
        Label capacityLabel = createPositionedLabel(
            "0/" + MAX_CAPACITY,
            system.getPosition().getX() + system.getWidth()/2 - 10,
            system.getPosition().getY() + system.getHeight() + LABEL_VERTICAL_OFFSET
        );
        capacityLabel.getStyleClass().add(CAPACITY_LABEL_BASE_STYLE);
        capacityLabel.setStyle(DEFAULT_CAPACITY_STYLE);
        
        gamePane.getChildren().add(capacityLabel);
        systemCapacityLabels.put(system, capacityLabel);
    }

    /**
     * Make the system draggable
     */
    private void makeSystemDraggable(NetworkSystem system) {
        gameEventHandler.makeSystemDraggable(system, system.getShape());
    }

    /**
     * Create and add a play button to a start system
     */
    private void createAndAddPlayButton(NetworkSystem system) {
        double x = system.getPosition().getX() + system.getWidth()/2 - 15;
        double y = system.getPosition().getY() + system.getHeight()/2 - 15;
        
        Group playButton = uiComponentFactory.createPlayButton(x, y);
        gameEventHandler.setupStartButtonHandler(playButton);
        gamePane.getChildren().add(playButton);
        system.setPlayButton(playButton);
    }

    /**
     * Render all input and output ports
     */
    private void renderPorts() {
        for (NetworkSystem system : gameState.getSystems()) {
            renderPortsForSystem(system.getInputPorts());
            renderPortsForSystem(system.getOutputPorts());
        }
    }

    /**
     * Render a collection of ports
     */
    private void renderPortsForSystem(Iterable<Port> ports) {
        for (Port port : ports) {
            Shape portShape = port.getShape();
            portShape.getStyleClass().add("port");
            portShape.setUserData(port);
            gamePane.getChildren().add(portShape);
            gameEventHandler.setupPortEventHandlers(port);
        }
    }

    /**
     * Update the capacity labels for all systems
     */
    public void updateCapacityLabels() {
        if (gameState == null) return;
        
        gameState.getSystems().stream()
            .filter(system -> !system.isStartSystem() && !system.isEndSystem())
            .forEach(this::updateSystemCapacity);
    }
    
    /**
     * Update capacity for an individual system
     */
    private void updateSystemCapacity(NetworkSystem system) {
        Label capacityLabel = systemCapacityLabels.get(system);
        if (capacityLabel != null) {
            updateCapacityLabel(system, capacityLabel);
        }
    }
    
    /**
     * Update a single capacity label based on system capacity
     */
    private void updateCapacityLabel(NetworkSystem system, Label capacityLabel) {
        int currentCapacity = system.getCurrentCapacityUsed();
        double capacityPercentage = (double)currentCapacity / MAX_CAPACITY;
        
        CapacityLevel level = CapacityLevel.fromPercentage(capacityPercentage);
        applyCapacityStyle(capacityLabel, level);
        
        // Update the label text
        capacityLabel.setText(currentCapacity + "/" + MAX_CAPACITY);
    }
    
    /**
     * Apply style based on capacity level
     */
    private void applyCapacityStyle(Label label, CapacityLevel level) {
        label.setTextFill(level.textColor);
        label.setEffect(level.glowEffect);
        label.setStyle(level.styleString);
        
        if (level.needsPulse) {
            addPulseAnimationIfNeeded(label);
        } else if (label.getProperties().containsKey(PULSE_ANIMATION_KEY)) {
            stopPulsation(label);
        }
    }
    
    /**
     * Add pulse animation if needed and not already present
     */
    private void addPulseAnimationIfNeeded(Label label) {
        if (!label.getProperties().containsKey(PULSE_ANIMATION_KEY)) {
            FadeTransition pulse = createPulseAnimation(label);
            label.getProperties().put(PULSE_ANIMATION_KEY, pulse);
        }
    }
    
    /**
     * Create a pulse animation for critical capacity
     */
    private FadeTransition createPulseAnimation(Label label) {
        FadeTransition pulse = new FadeTransition(Duration.millis(600), label);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.7);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
        return pulse;
    }
    
    /**
     * Stop pulsation effect on a label
     */
    private void stopPulsation(Label label) {
        FadeTransition pulse = (FadeTransition)label.getProperties().get(PULSE_ANIMATION_KEY);
        pulse.stop();
        label.setOpacity(1.0);
        label.getProperties().remove(PULSE_ANIMATION_KEY);
    }
    
    /**
     * Get the system capacity labels map
     * @return Map of systems to their capacity labels
     */
    public Map<NetworkSystem, Label> getSystemCapacityLabels() {
        return systemCapacityLabels;
    }
} 