package view.components.systems;

import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.scene.shape.Polygon;
import javafx.geometry.Pos;
import model.entity.systems.StartSystem;

public class StartSystemView extends SystemView {
    private Button playButton;
    private Polygon playTriangle;
    private Runnable playAction;
    private java.util.function.Supplier<Boolean> allSystemsReadyChecker;
    
    public StartSystemView(StartSystem system) {
        super(system, "START");
    }
    
    @Override
    protected void applySystemStyling() {
        systemRectangle.getStyleClass().add("system-start");
    }
    
    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        
        playButton = new Button();
        playButton.getStyleClass().add("play-button");
        playButton.setPrefSize(30, 30);
        
        playTriangle = new Polygon();
        playTriangle.getStyleClass().add("play-triangle");
        
        playTriangle.getPoints().addAll(new Double[]{
            0.0, 0.0,    // Top vertex
            12.0, 6.0,   // Right vertex  
            0.0, 12.0    // Bottom vertex
        });
        
        playButton.setGraphic(playTriangle);
        
        updateButtonState(false);
        
        playButton.setOnMouseEntered(e -> {
            if (!playButton.isDisable()) {
                playButton.setScaleX(1.1);
                playButton.setScaleY(1.1);
            }
        });
        
        playButton.setOnMouseExited(e -> {
            playButton.setScaleX(1.0);
            playButton.setScaleY(1.0);
        });
        
        playButton.setOnAction(e -> handlePlayButtonClick());
        
        content.getChildren().add(playButton);
        StackPane.setAlignment(playButton, Pos.CENTER);
        
        return content;
    }
    
    public Button getPlayButton() {
        return playButton;
    }
    
    public void setOnPlayAction(Runnable action) {
        this.playAction = action;
    }
    
    public void setAllSystemsReadyChecker(java.util.function.Supplier<Boolean> checker) {
        this.allSystemsReadyChecker = checker;
    }
    
    public void updateButtonState(boolean allSystemsReady) {
        if (playButton != null) {
            playButton.setDisable(!allSystemsReady);
            if (allSystemsReady) {
                playButton.setOpacity(1.0);
            } else {
                playButton.setOpacity(0.5);
            }
        }
    }
    
    private void handlePlayButtonClick() {
        if (allSystemsReadyChecker != null && allSystemsReadyChecker.get() && playAction != null) {
            service.AudioManager.playButtonClick();
            playAction.run();
        }
    }
}
