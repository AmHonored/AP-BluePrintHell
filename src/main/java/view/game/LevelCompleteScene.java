package view.game;

import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

public class LevelCompleteScene extends VBox {
    private final Label messageLabel;
    private final Button nextLevelButton;
    private final Button retryButton;
    private final Button menuButton;

    public LevelCompleteScene() {
        this.getStyleClass().add("overlay");
        messageLabel = new Label("Level Complete!");
        messageLabel.getStyleClass().add("overlay-label");
        nextLevelButton = new Button("Next Level");
        retryButton = new Button("Retry");
        menuButton = new Button("Menu");
        nextLevelButton.setPrefWidth(180);
        retryButton.setPrefWidth(180);
        menuButton.setPrefWidth(180);
        this.setSpacing(20);
        this.getChildren().addAll(messageLabel, nextLevelButton, retryButton, menuButton);
        this.setVisible(false);
    }

    public Label getMessageLabel() { return messageLabel; }
    public Button getNextLevelButton() { return nextLevelButton; }
    public Button getRetryButton() { return retryButton; }
    public Button getMenuButton() { return menuButton; }
}
