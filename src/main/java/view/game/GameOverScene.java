package view.game;

import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

public class GameOverScene extends VBox {
    private final Label messageLabel;
    private final Button retryButton;
    private final Button menuButton;

    public GameOverScene() {
        this.getStyleClass().add("overlay");
        messageLabel = new Label("Game Over");
        messageLabel.getStyleClass().add("overlay-label");
        retryButton = new Button("Retry");
        menuButton = new Button("Menu");
        retryButton.setPrefWidth(180);
        menuButton.setPrefWidth(180);
        this.setSpacing(20);
        this.getChildren().addAll(messageLabel, retryButton, menuButton);
        this.setVisible(false);
    }

    public Label getMessageLabel() { return messageLabel; }
    public Button getRetryButton() { return retryButton; }
    public Button getMenuButton() { return menuButton; }
}
