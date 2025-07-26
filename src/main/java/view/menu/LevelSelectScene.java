package view.menu;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.StackPane;

/**
 * Level selection scene for Blueprint Hell. Two levels, Level 2 is locked until Level 1 is completed.
 * No business logic, just UI and event hooks.
 */
public class LevelSelectScene extends StackPane {
    private final Button level1Button;
    private final Button level2Button;
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

        backButton = new Button("Back");
        backButton.setPrefWidth(140);
        backButton.getStyleClass().add("back-button");

        VBox buttonBox = new VBox(20, level1Button, level2Button, backButton);
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

    public Button getBackButton() {
        return backButton;
    }
}
