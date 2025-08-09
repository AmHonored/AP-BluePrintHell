package view.game;

import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.geometry.Pos;

public class GameButtons extends HBox {
    private final Button shopButton;
    private final Button pauseButton;
    private final Button menuButton;
    // Aergia moved to HUD

    public GameButtons() {
        this.getStyleClass().add("controls-pane");
        this.setSpacing(40);
        this.setAlignment(Pos.CENTER);

        shopButton = new Button("Shop");
        pauseButton = new Button("Pause");
        menuButton = new Button("Menu");

        // Remove inline styles - now using CSS classes
        shopButton.getStyleClass().add("button");
        pauseButton.getStyleClass().add("button");
        menuButton.getStyleClass().add("button");

        this.getChildren().addAll(shopButton, pauseButton, menuButton);
    }

    public Button getShopButton() { return shopButton; }
    public Button getPauseButton() { return pauseButton; }
    public Button getMenuButton() { return menuButton; }
}
