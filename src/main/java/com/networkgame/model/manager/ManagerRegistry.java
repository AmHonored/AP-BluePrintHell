package com.networkgame.model.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Central registry for all manager instances.
 * This class breaks cyclic dependencies by providing a single point of access
 * to all managers without requiring direct imports between managers and GameState.
 */
public class ManagerRegistry {
    private static final Logger LOGGER = Logger.getLogger(ManagerRegistry.class.getName());
    
    private final Map<Class<?>, Object> managers = new HashMap<>();
    private final Map<String, Object> namedManagers = new HashMap<>();
    
    /**
     * Register a manager instance by its class type
     * @param type The class type of the manager
     * @param instance The manager instance
     */
    public <T> void register(Class<T> type, T instance) {
        if (type == null || instance == null) {
            throw new IllegalArgumentException("Type and instance cannot be null");
        }
        
        managers.put(type, instance);
        LOGGER.log(Level.FINE, "Registered manager: {0}", type.getSimpleName());
    }
    
    /**
     * Register a manager instance by name
     * @param name The name identifier for the manager
     * @param instance The manager instance
     */
    public void register(String name, Object instance) {
        if (name == null || instance == null) {
            throw new IllegalArgumentException("Name and instance cannot be null");
        }
        
        namedManagers.put(name, instance);
        LOGGER.log(Level.FINE, "Registered named manager: {0}", name);
    }
    
    /**
     * Get a manager instance by its class type
     * @param type The class type of the manager
     * @return The manager instance, or null if not found
     */
    public <T> T get(Class<T> type) {
        if (type == null) {
            return null;
        }
        
        Object manager = managers.get(type);
        if (manager != null && type.isAssignableFrom(manager.getClass())) {
            return type.cast(manager);
        }
        return null;
    }
    
    /**
     * Get a manager instance by name
     * @param name The name identifier for the manager
     * @param type The expected class type
     * @return The manager instance, or null if not found
     */
    public <T> T get(String name, Class<T> type) {
        if (name == null || type == null) {
            return null;
        }
        
        Object manager = namedManagers.get(name);
        if (manager != null && type.isAssignableFrom(manager.getClass())) {
            return type.cast(manager);
        }
        return null;
    }
    
    /**
     * Get a manager instance by its class type, wrapped in Optional
     * @param type The class type of the manager
     * @return Optional containing the manager instance
     */
    public <T> Optional<T> getOptional(Class<T> type) {
        return Optional.ofNullable(get(type));
    }
    
    /**
     * Get a manager instance by name, wrapped in Optional
     * @param name The name identifier for the manager
     * @param type The expected class type
     * @return Optional containing the manager instance
     */
    public <T> Optional<T> getOptional(String name, Class<T> type) {
        return Optional.ofNullable(get(name, type));
    }
    
    /**
     * Check if a manager is registered by type
     * @param type The class type to check
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(Class<?> type) {
        return type != null && managers.containsKey(type);
    }
    
    /**
     * Check if a manager is registered by name
     * @param name The name to check
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(String name) {
        return name != null && namedManagers.containsKey(name);
    }
    
    /**
     * Unregister a manager by type
     * @param type The class type to unregister
     */
    public void unregister(Class<?> type) {
        if (type != null) {
            managers.remove(type);
            LOGGER.log(Level.FINE, "Unregistered manager: {0}", type.getSimpleName());
        }
    }
    
    /**
     * Unregister a manager by name
     * @param name The name to unregister
     */
    public void unregister(String name) {
        if (name != null) {
            namedManagers.remove(name);
            LOGGER.log(Level.FINE, "Unregistered named manager: {0}", name);
        }
    }
    
    /**
     * Clear all registered managers
     */
    public void clear() {
        managers.clear();
        namedManagers.clear();
        LOGGER.log(Level.FINE, "Cleared all registered managers");
    }
    
    /**
     * Get the number of registered managers
     * @return The total number of registered managers
     */
    public int getRegisteredCount() {
        return managers.size() + namedManagers.size();
    }
} 