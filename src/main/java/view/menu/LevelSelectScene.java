package view.menu;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;

/**
 * Level selection scene with scrollable level list. Shows 3 levels per frame.
 */
public class LevelSelectScene extends StackPane {
    private final Button[] levelButtons;
    private final Button backButton;
    private static final int VISIBLE_LEVELS = 3;
    private static final String[] LEVEL_NAMES = {
        "Level 1",
        "Level 2", 
        "Level 3 - Hexagon Test",
        "Level 4 - Spy System",
        "Level 5 - VPN Test Lab",
        "Level 6 - AntiVirus System",
        "Level 7 - Confidential Packets"
    };

    public LevelSelectScene(boolean level2Unlocked) {
        // Title
        Text title = new Text("Select Level");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.getStyleClass().add("level-select-title");

        // Create level buttons
        levelButtons = new Button[LEVEL_NAMES.length];
        VBox levelContainer = new VBox(15);
        levelContainer.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < LEVEL_NAMES.length; i++) {
            levelButtons[i] = createLevelButton(i, level2Unlocked);
            levelContainer.getChildren().add(levelButtons[i]);
        }

        // Scrollable area for levels
        ScrollPane scrollPane = new ScrollPane(levelContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefHeight(VISIBLE_LEVELS * 60 + 30); // 3 buttons + spacing
        scrollPane.getStyleClass().add("level-scroll-pane");

        // Back button
        backButton = new Button("Back");
        backButton.setPrefWidth(140);
        backButton.getStyleClass().add("back-button");

        // Layout
        VBox layout = new VBox(30, title, scrollPane, backButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        
        this.getChildren().add(layout);
        this.setAlignment(Pos.CENTER);
        this.getStyleClass().add("menu-root");
    }

    private Button createLevelButton(int levelIndex, boolean level2Unlocked) {
        Button button = new Button(LEVEL_NAMES[levelIndex]);
        button.setPrefWidth(260);
        button.getStyleClass().add("level-button");
        
        // Lock logic
        if (levelIndex == 1 && !level2Unlocked) {
            button.setDisable(true);
            button.setText(LEVEL_NAMES[levelIndex] + " (Locked)");
        }
        
        return button;
    }

    // Getters for level buttons
    public Button getLevel1Button() { return levelButtons[0]; }
    public Button getLevel2Button() { return levelButtons[1]; }
    public Button getLevel3Button() { return levelButtons[2]; }
    public Button getLevel4Button() { return levelButtons[3]; }
    public Button getLevel5Button() { return levelButtons[4]; }
    public Button getLevel6Button() { return levelButtons[5]; }
    public Button getLevel7Button() { return levelButtons[6]; }
    public Button getBackButton() { return backButton; }
}
