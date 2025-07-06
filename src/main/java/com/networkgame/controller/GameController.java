package com.networkgame.controller;

import com.networkgame.model.*;
import com.networkgame.model.entity.Port;
import com.networkgame.model.manager.LevelManager;
import com.networkgame.model.state.GameState;
import com.networkgame.service.audio.AudioManager;
import com.networkgame.view.GameScene;
import com.networkgame.view.LevelSelectScene;
import com.networkgame.view.MainMenuScene;
import com.networkgame.view.SettingsScene;
import com.networkgame.view.ShopScene;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

/**
 * Main controller that acts as a facade for the game's subsystems.
 * Coordinates between different controllers and manages the game flow.
 */
public class GameController {
    private Stage primaryStage;
    private final GameState gameState;
    private final LevelManager levelManager;
    private final AudioManager audioManager;
    
    // Controllers
    private final GameplayController gameplayController;
    private final LevelController levelController;
    private final UIController uiController;
    private final TemporalController temporalController;
    private final InputController inputController;
    
    // Scenes
    private MainMenuScene mainMenuScene;
    private LevelSelectScene levelSelectScene;
    private SettingsScene settingsScene;
    private GameScene gameScene;
    private ShopScene shopScene;
    
    private boolean isShopOpen = false;
    
    public GameController() {
        this.gameState = new GameState();
        this.levelManager = new LevelManager(gameState);
        this.audioManager = AudioManager.getInstance();
        
        // Initialize all controllers
        gameplayController = new GameplayController(this, gameState);
        levelController = new LevelController(this, gameState, levelManager);
        uiController = new UIController(this);
        temporalController = new TemporalController(this, gameState);
        inputController = new InputController(this);
    }
    
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        setupKeyboardHandling();
    }
    
    private void setupKeyboardHandling() {
        primaryStage.getScene().setOnKeyPressed(event -> {
            KeyCode keyCode = event.getCode();
            
            if (keyCode == KeyCode.ESCAPE) {
                uiController.returnToMainMenu();
                return;
            }
            
            if (keyCode == KeyCode.S && isInGameScene()) {
                uiController.showShop();
                return;
            }
            
            if (isInGameScene()) {
                inputController.handleKeyPress(keyCode);
            }
        });
    }
    
    private boolean isInGameScene() {
        return gameScene != null && primaryStage.getScene() == gameScene.getScene();
    }
    
    public Scene createMainMenuScene() {
        mainMenuScene = new MainMenuScene(this);
        levelSelectScene = new LevelSelectScene(this, levelManager);
        settingsScene = new SettingsScene(this, audioManager);
        
        // Start playing menu music when the application starts
        audioManager.playMenuMusic();
        
        return mainMenuScene.getScene();
    }
    
    public void initialize() {
        gameState.initializeCollisionSystem();
        levelController.initialize();
        gameplayController.initialize();
        
        System.out.println("GameController initialized at level: " + gameState.getCurrentLevel());
    }
    
    public void cleanup() {
        gameplayController.cleanup();
        levelController.cleanup();
        System.out.println("GameController resources cleaned up");
    }
    
    // Game control delegation methods
    public void startGame(int levelNumber) { levelController.startGame(levelNumber); }
    public void stopGame() { gameplayController.stopGame(); }
    public void pauseGame() { gameplayController.pauseGame(); }
    public void resumeGame() { gameplayController.resumeGame(); }
    
    // UI navigation methods
    public void returnToMainMenu() { uiController.returnToMainMenu(); }
    public void showLevelSelect() { uiController.showLevelSelect(); }
    public void showSettings() { uiController.showSettings(); }
    public void showShop() { uiController.showShop(); }
    public void hideShop() { uiController.hideShop(); }
    
    // Input handling
    public void handlePortClick(Port port) { inputController.handlePortClick(port); }
    
    // Game state queries
    public boolean isRunning() { return gameplayController.isRunning(); }
    public boolean isTemporalControlMode() { return temporalController.isTemporalControlMode(); }
    
    // HUD control
    public void toggleHudVisibility() { 
        if (gameState.getHudManager() != null) {
            gameState.getHudManager().toggleHudVisibility();
        }
    }
    
    public boolean isHudVisible() { 
        return gameState.getHudManager() != null && gameState.getHudManager().isHudVisible();
    }
    
    // Audio control
    public void setVolume(double volume) { audioManager.setVolume(volume); }
    
    // Level management
    public void levelCompleted() { levelController.levelCompleted(); }
    public void proceedToNextLevel() { levelController.proceedToNextLevel(); }
    public void restartCurrentLevel() { levelController.restartCurrentLevel(); }
    
    // Temporal mechanics
    public void toggleTemporalControlMode() { temporalController.toggleTemporalControlMode(); }
    public void moveTimeForward() { temporalController.moveTimeForward(); }
    public void moveTimeBackward() { temporalController.moveTimeBackward(); }
    public void travelInTime(int time) { temporalController.travelInTime(time); }
    
    // Getters - only expose what's necessary to other components
    public Stage getPrimaryStage() { return primaryStage; }
    public GameState getGameState() { return gameState; }
    public LevelManager getLevelManager() { return levelManager; }
    public AudioManager getAudioManager() { return audioManager; }
    
    // Scene management
    public GameScene getGameScene() { return gameScene; }
    public void setGameScene(GameScene gameScene) { this.gameScene = gameScene; }
    public MainMenuScene getMainMenuScene() { return mainMenuScene; }
    public LevelSelectScene getLevelSelectScene() { return levelSelectScene; }
    public SettingsScene getSettingsScene() { return settingsScene; }
    public ShopScene getShopScene() { return shopScene; }
    public void setShopScene(ShopScene shopScene) { this.shopScene = shopScene; }
    
    // Shop state
    public boolean isShopOpen() { return isShopOpen; }
    public void setShopOpen(boolean isShopOpen) { this.isShopOpen = isShopOpen; }
    
    // Controller access - consider if all of these need to be public
    public GameplayController getGameplayController() { return gameplayController; }
    public LevelController getLevelController() { return levelController; }
    public UIController getUiController() { return uiController; }
    public TemporalController getTemporalController() { return temporalController; }
    public InputController getInputController() { return inputController; }
} 