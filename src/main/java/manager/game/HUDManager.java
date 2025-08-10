package manager.game;

import view.game.GameButtons;
import view.game.HUDScene;
import model.levels.Level;
import controller.AbilityController;

public class HUDManager {
	public void updatePauseButtonText(GameButtons buttons, boolean paused) {
		if (buttons == null) return;
		buttons.getPauseButton().setText(paused ? "Resume" : "Pause");
	}

    public void updateAergiaHudButtonEnabled(HUDScene hud, Level level, view.game.GameScene gameScene) {
        if (hud == null || level == null) return;
        boolean enabled = level.getAergiaScrolls() > 0 && !level.isAergiaOnCooldown();
        hud.getAergiaButton().setDisable(!enabled);
        if (gameScene != null) gameScene.updateAergiaButtonText();
    }

    public void wireAergiaButton(HUDScene hud, Level level, view.game.GameScene gameScene,
                                  AbilityController abilityController, ConnectionManager connectionManager) {
        if (hud == null || level == null || abilityController == null) return;
        updateAergiaHudButtonEnabled(hud, level, gameScene);
        hud.getAergiaButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            boolean hasActiveWires = connectionManager != null && connectionManager.getWires().stream().anyMatch(w -> w.isActive());
            if (!hasActiveWires) {
            }
            
            updateAergiaHudButtonEnabled(hud, level, gameScene);
            if (level.getAergiaScrolls() > 0 && !level.isAergiaOnCooldown()) {
                abilityController.startAergiaPlacement();
                if (gameScene != null) {
                    gameScene.updateAergiaButtonText();
                    gameScene.showAergiaPlacementHint();
                }
            }
        });
    }

    public void wireSisyphusButton(HUDScene hud, Level level, AbilityController abilityController) {
        if (hud == null || level == null || abilityController == null) return;
        abilityController.updateSisyphusHudButtonEnabled(hud);
        hud.getSisyphusButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            abilityController.updateSisyphusHudButtonEnabled(hud);
            if (level.getSisyphusScrolls() > 0) {
                abilityController.startSisyphusSelection();
            }
        });
    }

    public void wireEliphasButton(HUDScene hud, Level level, AbilityController abilityController) {
        if (hud == null || level == null || abilityController == null) return;
        abilityController.updateEliphasHudButtonEnabled(hud);
        if (hud.getEliphasButton() == null) return;
        hud.getEliphasButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            abilityController.updateEliphasHudButtonEnabled(hud);
            if (level.getEliphasScrolls() > 0) {
                abilityController.startEliphasPlacement();
            }
        });
    }
}
    
