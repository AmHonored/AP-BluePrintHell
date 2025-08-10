package view.game;

import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class LevelCompleteScene extends VBox {
    private final VBox titleContainer;
    private final Label statusLabel;
    private final Label titleLabel;
    private final Text subtitleText;
    private final Rectangle accentBar;
    private final HBox buttonContainer;
    private final Button nextLevelButton;
    private final Button retryButton;
    private final Button menuButton;

    public LevelCompleteScene() {
        this.getStyleClass().add("level-complete-overlay");
        this.setAlignment(Pos.CENTER);
        this.setSpacing(35);
        this.setPadding(new Insets(50, 60, 50, 60));

        titleContainer = new VBox();
        titleContainer.setAlignment(Pos.CENTER);
        titleContainer.setSpacing(12);
        
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("level-complete-status");
        
        titleLabel = new Label("LEVEL COMPLETE");
        titleLabel.getStyleClass().add("level-complete-title");
        
        subtitleText = new Text("MISSION ACCOMPLISHED");
        subtitleText.getStyleClass().add("level-complete-subtitle");
        
        accentBar = new Rectangle(300, 4);
        accentBar.getStyleClass().add("level-complete-accent-bar");
        
        titleContainer.getChildren().addAll(statusLabel, titleLabel, subtitleText, accentBar);

        nextLevelButton = new Button("NEXT LEVEL");
        retryButton = new Button("REPLAY LEVEL");
        menuButton = new Button("MAIN MENU");
        
        nextLevelButton.getStyleClass().addAll("cyberpunk-button", "cyberpunk-button-success");
        retryButton.getStyleClass().addAll("cyberpunk-button", "cyberpunk-button-secondary");
        menuButton.getStyleClass().addAll("cyberpunk-button", "cyberpunk-button-danger");

        buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setSpacing(30);
        buttonContainer.setPadding(new Insets(25, 0, 0, 0));
        buttonContainer.getChildren().addAll(nextLevelButton, retryButton, menuButton);

        this.getChildren().addAll(titleContainer, buttonContainer);
        this.setVisible(false);
    }

    public Label getMessageLabel() { return titleLabel; }
    public Button getNextLevelButton() { return nextLevelButton; }
    public Button getRetryButton() { return retryButton; }
    public Button getMenuButton() { return menuButton; }
}
