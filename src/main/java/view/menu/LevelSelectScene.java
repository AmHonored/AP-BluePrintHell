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
import config.levels.LevelConfigLoader;
import config.levels.LevelIndex;


public class LevelSelectScene extends StackPane {
    private final Button[] levelButtons;
    private final Button backButton;
    private static final int VISIBLE_LEVELS = 3;
    private final String[] levelNames;

    public LevelSelectScene() {
        LevelConfigLoader loader = new LevelConfigLoader();
        LevelIndex index = loader.loadIndex("levels/levels-index.json");
        this.levelNames = index.getLevels().stream().map(LevelIndex.Entry::getName).toArray(String[]::new);

        Text title = new Text("Select Level");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.getStyleClass().add("level-select-title");

        levelButtons = new Button[levelNames.length];
        VBox levelContainer = new VBox(15);
        levelContainer.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < levelNames.length; i++) {
            levelButtons[i] = createLevelButton(i);
            levelContainer.getChildren().add(levelButtons[i]);
        }

        ScrollPane scrollPane = new ScrollPane(levelContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefHeight(VISIBLE_LEVELS * 60 + 30); // 3 buttons + spacing
        scrollPane.getStyleClass().add("level-scroll-pane");

        backButton = new Button("Back");
        backButton.setPrefWidth(140);
        backButton.getStyleClass().add("back-button");

        VBox layout = new VBox(30, title, scrollPane, backButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        
        this.getChildren().add(layout);
        this.setAlignment(Pos.CENTER);
        this.getStyleClass().add("menu-root");
    }

    private Button createLevelButton(int levelIndex) {
        Button button = new Button(levelNames[levelIndex]);
        button.setPrefWidth(260);
        button.getStyleClass().add("level-button");
                
        return button;
    }

    public Button getLevel1Button() { return levelButtons[0]; }
    public Button getLevel2Button() { return levelButtons[1]; }
    public Button getLevel3Button() { return levelButtons[2]; }
    public Button getLevel4Button() { return levelButtons[3]; }
    public Button getLevel5Button() { return levelButtons[4]; }
    public Button getLevel6Button() { return levelButtons[5]; }
    public Button getLevel7Button() { return levelButtons[6]; }

    public Button getCapacityButton() { return levelButtons.length > 0 ? levelButtons[0] : null; }
    public Button getCollisionButton() { return levelButtons.length > 1 ? levelButtons[1] : null; }
    public Button getHexagonAndDdosButton() { return levelButtons.length > 2 ? levelButtons[2] : null; }
    public Button getSpyButton() { return levelButtons.length > 3 ? levelButtons[3] : null; }
    public Button getVpnButton() { return levelButtons.length > 4 ? levelButtons[4] : null; }
    public Button getAntivirusButton() { return levelButtons.length > 5 ? levelButtons[5] : null; }
    public Button getDistributeAndMergeButton() { return levelButtons.length > 6 ? levelButtons[6] : null; }
    public Button getBackButton() { return backButton; }
}
