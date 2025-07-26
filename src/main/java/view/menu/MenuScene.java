package view.menu;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;

/**
 * Main menu scene for Blueprint Hell. Contains buttons for navigation.
 * Follows SOLID and clean code principles. No business logic here.
 */
public class MenuScene extends StackPane {
    private final Button startGameButton;
    private final Button levelSelectButton;
    private final Button settingsButton;
    private final Button exitButton;

    public MenuScene() {
        Text title = new Text("Blueprint Hell");
        title.getStyleClass().add("menu-title-red");

        startGameButton = new Button("Start Game");
        levelSelectButton = new Button("Level Select");
        settingsButton = new Button("Settings");
        exitButton = new Button("Exit");

        startGameButton.setPrefWidth(220);
        levelSelectButton.setPrefWidth(220);
        settingsButton.setPrefWidth(220);
        exitButton.setPrefWidth(220);

        VBox buttonBox = new VBox(20, startGameButton, levelSelectButton, settingsButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox layout = new VBox(50, title, buttonBox);
        layout.setAlignment(Pos.CENTER);
        this.getChildren().add(layout);
        this.setAlignment(Pos.CENTER);
        this.getStyleClass().add("menu-root");
    }

    public Button getStartGameButton() {
        return startGameButton;
    }

    public Button getLevelSelectButton() {
        return levelSelectButton;
    }

    public Button getSettingsButton() {
        return settingsButton;
    }

    public Button getExitButton() {
        return exitButton;
    }
}
