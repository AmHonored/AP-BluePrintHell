package view.menu;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.StackPane;

/**
 * Level selection scene for Blueprint Hell. Three levels, Level 2 and 3 are unlocked for testing.
 * No business logic, just UI and event hooks.
 */
public class LevelSelectScene extends StackPane {
    private final Button level1Button;
    private final Button level2Button;
    private final Button level3Button;
    private final Button backButton;

    public LevelSelectScene(boolean level2Unlocked) {
        Text title = new Text("Select Level");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.getStyleClass().add("level-select-title");

        level1Button = new Button("Level 1");
        level1Button.setPrefWidth(260);
        level1Button.getStyleClass().add("level-button");

        level2Button = new Button("Level 2");
        level2Button.setPrefWidth(260);
        level2Button.getStyleClass().add("level-button");
        level2Button.setDisable(!level2Unlocked);
        if (!level2Unlocked) {
            level2Button.setText("Level 2 (Locked)");
        }

        level3Button = new Button("Level 3 - Hexagon Test");
        level3Button.setPrefWidth(260);
        level3Button.getStyleClass().add("level-button");
        // Level 3 is unlocked by default for testing hexagon packets

        backButton = new Button("Back");
        backButton.setPrefWidth(140);
        backButton.getStyleClass().add("back-button");

        VBox buttonBox = new VBox(20, level1Button, level2Button, level3Button, backButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox layout = new VBox(40, title, buttonBox);
        layout.setAlignment(Pos.CENTER);
        this.getChildren().add(layout);
        this.setAlignment(Pos.CENTER);
        this.getStyleClass().add("menu-root");
    }

    public Button getLevel1Button() {
        return level1Button;
    }

    public Button getLevel2Button() {
        return level2Button;
    }

    public Button getLevel3Button() {
        return level3Button;
    }

    public Button getBackButton() {
        return backButton;
    }
}
