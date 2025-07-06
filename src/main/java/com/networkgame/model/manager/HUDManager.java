package com.networkgame.model.manager; 

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages HUD components in the game
 */
public class HUDManager {
    private static final HUDManager instance = new HUDManager();
    private final Map<String, Node> components = new HashMap<>();
    private final Map<String, Node> hudComponents = new HashMap<>();
    private Pane hudContainer;
    private HBox mainHudPane;
    private boolean hudVisible = true;
    
    private HUDManager() {}
    
    public static HUDManager getInstance() {
        return instance;
    }
    
    /**
     * Set the container where HUD elements will be displayed
     */
    public void setHUDContainer(Pane container) {
        this.hudContainer = container;
    }
    
    public Pane getHUDContainer() {
        return hudContainer;
    }
    
    /**
     * Set the main HUD pane that contains all HUD elements
     */
    public void setMainHudPane(HBox hudPane) {
        this.mainHudPane = hudPane;
    }
    
    public HBox getMainHudPane() {
        return mainHudPane;
    }
    
    /**
     * Register a time-related component
     * @param id Unique identifier for the component
     * @param component The UI node to register
     */
    public void registerTimeComponent(String id, Node component) {
        components.put(id, component);
    }
    
    /**
     * Register a HUD component that can be toggled on/off
     * @param id Unique identifier for the component
     * @param component The UI node to register
     */
    public void registerHudComponent(String id, Node component) {
        hudComponents.put(id, component);
    }
    
    /**
     * Get a registered time component
     * @param id The component identifier
     * @return The component or null if not found
     */
    public Node getTimeComponent(String id) {
        return components.get(id);
    }
    
    /**
     * Get a registered HUD component
     * @param id The component identifier
     * @return The component or null if not found
     */
    public Node getHudComponent(String id) {
        return hudComponents.get(id);
    }
    
    /**
     * Toggle the visibility of HUD components
     * Time progress components remain visible
     */
    public void toggleHudVisibility() {
        hudVisible = !hudVisible;
        
        // Toggle visibility of all registered HUD components
        for (Node component : hudComponents.values()) {
            component.setVisible(hudVisible);
        }
        
        // Ensure time components are always visible
        for (Node component : components.values()) {
            component.setVisible(true);
        }
    }
    
    /**
     * Set HUD visibility state
     * @param visible True to show HUD, false to hide
     */
    public void setHudVisible(boolean visible) {
        this.hudVisible = visible;
        
        // Set visibility of all registered HUD components
        for (Node component : hudComponents.values()) {
            component.setVisible(visible);
        }
        
        // Ensure time components are always visible
        for (Node component : components.values()) {
            component.setVisible(true);
        }
    }
    
    /**
     * Check if HUD is currently visible
     * @return True if HUD is visible, false otherwise
     */
    public boolean isHudVisible() {
        return hudVisible;
    }
} 
