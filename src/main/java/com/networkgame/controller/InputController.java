package com.networkgame.controller;

import com.networkgame.model.entity.Port;
import com.networkgame.view.GameScene;
import javafx.animation.PauseTransition;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

public class InputController {
    private GameController mainController;
    private Port selectedPort;
    
    public InputController(GameController mainController) {
        this.mainController = mainController;
    }
    
    public void handleKeyPress(KeyCode keyCode) {
        switch (keyCode) {
            case ESCAPE:
            case SPACE:
            case PLAY:
                toggleGameRunningState();
                break;
            case T:
                mainController.toggleTemporalControlMode();
                break;
            case RIGHT:
                mainController.moveTimeForward();
                break;
            case LEFT:
                mainController.moveTimeBackward();
                break;
            case S:
                toggleShop();
                break;
        }
    }
    
    private void toggleGameRunningState() {
        if (mainController.isRunning()) {
            mainController.pauseGame();
        } else {
            mainController.resumeGame();
        }
    }
    
    private void toggleShop() {
        if (!mainController.isShopOpen()) {
            mainController.showShop();
        } else {
            mainController.hideShop();
        }
    }
    
    public void handlePortClick(Port port) {
        selectedPort = port;
        highlightPortTemporarily(port);
    }
    
    private void highlightPortTemporarily(Port port) {
        ((GameScene)mainController.getGameScene()).highlightPort(port);
        
        PauseTransition delay = new PauseTransition(Duration.millis(300));
        delay.setOnFinished(e -> ((GameScene)mainController.getGameScene()).clearHighlights());
        delay.play();
    }
    
    public Port getSelectedPort() {
        return selectedPort;
    }
} 
