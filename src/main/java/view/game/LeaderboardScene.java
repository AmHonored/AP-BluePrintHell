package view.game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import net.NetworkService;
import protocol.messages.Leaderboard;

import java.util.List;

public class LeaderboardScene extends VBox {
    private final Label title;
    private final GridPane table;
    private final Button closeButton;
    private final ToggleButton tabTime;
    private final ToggleButton tabXp;
    private final ToggleButton tabCampaign;
    private String currentLevelCode;

    public LeaderboardScene() {
        setSpacing(20);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);
        // Enhanced overlay styling to match menu theme
        setStyle("-fx-background-color: linear-gradient(to bottom, rgba(10, 14, 39, 0.95), rgba(22, 33, 62, 0.95));" +
                " -fx-border-color: #e94560; -fx-border-width: 3; -fx-border-radius: 15; -fx-background-radius: 15;" +
                " -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 20, 0, 0, 0);");

        title = new Label("Leaderboard");
        // Enhanced title styling to match menu title
        title.setStyle("-fx-font-family: 'Arial Black', 'Segoe UI', Arial, sans-serif;" +
                " -fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #fff;" +
                " -fx-effect: dropshadow(gaussian, #e94560, 8, 0.7, 0, 2), dropshadow(gaussian, #222, 2, 0.8, 0, 0);");

        // Tabs
        ToggleGroup group = new ToggleGroup();
        tabTime = new ToggleButton("Level Records");
        tabXp = new ToggleButton("Top Players (XP)");
        tabCampaign = new ToggleButton("Speedrun Campaign");
        tabTime.setToggleGroup(group);
        tabXp.setToggleGroup(group);
        tabCampaign.setToggleGroup(group);
        styleTab(tabTime);
        styleTab(tabXp);
        styleTab(tabCampaign);
        tabXp.setSelected(true);
        javafx.scene.layout.HBox tabs = new javafx.scene.layout.HBox(8, tabTime, tabXp, tabCampaign);
        tabs.setAlignment(Pos.CENTER);

        table = new GridPane();
        table.setHgap(20);
        table.setVgap(12);
        table.setPadding(new Insets(20));
        // Enhanced table styling
        table.setStyle("-fx-background-color: rgba(10, 14, 39, 0.4);" +
                " -fx-background-radius: 8;");

        addHeaderRow();

        ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(350);
        // Enhanced scroll pane styling to match theme
        scroll.getStyleClass().add("level-scroll-pane");
        scroll.setStyle("-fx-background-color: rgba(22, 33, 62, 0.3);" +
                " -fx-border-color: #00d4ff; -fx-border-width: 1; -fx-border-radius: 10;" +
                " -fx-background-radius: 10;");

        closeButton = new Button("Close");
        closeButton.setOnAction(e -> this.setVisible(false));
        // Style close button to match menu buttons
        closeButton.getStyleClass().add("menu-root");
        closeButton.setPrefWidth(180);
        closeButton.setStyle("-fx-background-color: linear-gradient(to bottom, #16213e, #0f3460);" +
                " -fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: bold;" +
                " -fx-background-radius: 12; -fx-border-radius: 12;" +
                " -fx-border-color: linear-gradient(to bottom, #00d4ff, #e94560); -fx-border-width: 2;" +
                " -fx-padding: 12 25; -fx-cursor: hand;" +
                " -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 8, 0.3, 0, 2);");
        
        closeButton.setOnMouseEntered(e -> closeButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1e2a4a, #1a4a73);" +
                " -fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: bold;" +
                " -fx-background-radius: 12; -fx-border-radius: 12;" +
                " -fx-border-color: linear-gradient(to bottom, #ff6b85, #00f0ff); -fx-border-width: 2;" +
                " -fx-padding: 12 25; -fx-cursor: hand;" +
                " -fx-effect: dropshadow(gaussian, rgba(0, 212, 255, 0.6), 15, 0.5, 0, 3);" +
                " -fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
        
        closeButton.setOnMouseExited(e -> closeButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #16213e, #0f3460);" +
                " -fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: bold;" +
                " -fx-background-radius: 12; -fx-border-radius: 12;" +
                " -fx-border-color: linear-gradient(to bottom, #00d4ff, #e94560); -fx-border-width: 2;" +
                " -fx-padding: 12 25; -fx-cursor: hand;" +
                " -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 8, 0.3, 0, 2);" +
                " -fx-scale-x: 1.0; -fx-scale-y: 1.0;"));

        BorderPane container = new BorderPane();
        VBox header = new VBox(8, title, tabs);
        header.setAlignment(Pos.CENTER);
        container.setTop(header);
        BorderPane.setAlignment(header, Pos.CENTER);
        container.setCenter(scroll);
        container.setBottom(closeButton);
        BorderPane.setAlignment(closeButton, Pos.CENTER);
        BorderPane.setMargin(closeButton, new Insets(10));

        getChildren().add(container);

        NetworkService.getInstance().onLeaderboard(resp -> populate(resp.entries));

        tabTime.setOnAction(e -> {
            if (currentLevelCode != null) requestTopTimes(currentLevelCode);
        });
        tabXp.setOnAction(e -> requestTopXp());
        tabCampaign.setOnAction(e -> requestCampaign());
    }

    private void addHeaderRow() {
        addRow(0, "#", "Username", "Time", "XP", "Date");
    }

    private void addRow(int row, String rank, String user, String time, String xp, String when) {
        // Enhanced styling for different row types
        String headerStyle = row == 0 ? "-fx-text-fill: #00d4ff; -fx-font-size: 18px;" : "";
        String rankStyle = row == 0 ? headerStyle : "-fx-text-fill: #e94560;";
        String userStyle = row == 0 ? headerStyle : "-fx-text-fill: #fff;";
        String timeStyle = row == 0 ? headerStyle : "-fx-text-fill: #00d4ff;";
        String xpStyle = row == 0 ? headerStyle : "-fx-text-fill: #32cd32;";
        String whenStyle = row == 0 ? headerStyle : "-fx-text-fill: #aaa;";
        
        table.add(styled(rank, rankStyle), 0, row);
        table.add(styled(user, userStyle), 1, row);
        table.add(styled(time, timeStyle), 2, row);
        table.add(styled(xp, xpStyle), 3, row);
        table.add(styled(when, whenStyle), 4, row);
    }

    private Label styled(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style + " -fx-font-size: 16px; -fx-font-weight: bold;" +
                " -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 2, 0.3, 0, 1);");
        return l;
    }

    private String fmtTime(long durationMs) {
        long s = durationMs / 1000L;
        long m = s / 60L; s %= 60L;
        return String.format("%02d:%02d", m, s);
    }

    private String fmtWhen(long epochMs) {
        java.time.Instant i = java.time.Instant.ofEpochMilli(epochMs);
        java.time.ZoneId z = java.time.ZoneId.systemDefault();
        java.time.LocalDateTime dt = java.time.LocalDateTime.ofInstant(i, z);
        return dt.toLocalDate().toString();
    }

    public void populate(List<Leaderboard.Entry> entries) {
        table.getChildren().clear();
        addHeaderRow();
        int row = 1;
        if (entries != null) {
            for (Leaderboard.Entry e : entries) {
                addRow(row++, String.valueOf(e.rank), e.username, fmtTime(e.durationMs), String.valueOf(e.xp), fmtWhen(e.when));
            }
        }
    }

    public void requestTopTimes(String levelCode) {
        this.setVisible(true);
        this.currentLevelCode = levelCode;
        NetworkService.getInstance().requestLeaderboard(levelCode, "time", 20);
    }

    public void requestTopXp() {
        this.setVisible(true);
        NetworkService.getInstance().requestLeaderboard(null, "xp", 20);
    }

    public void requestCampaign() {
        this.setVisible(true);
        NetworkService.getInstance().requestLeaderboard(null, "campaign", 20);
    }

    private void styleTab(ToggleButton b) {
        // Enhanced tab styling to match menu button design
        String normalStyle = "-fx-background-color: linear-gradient(to bottom, #16213e, #0f3460);" +
                " -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;" +
                " -fx-background-radius: 10; -fx-border-radius: 10;" +
                " -fx-border-color: #00d4ff; -fx-border-width: 2;" +
                " -fx-padding: 10 20; -fx-cursor: hand;" +
                " -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 6, 0.3, 0, 2);";
        
        String hoverStyle = "-fx-background-color: linear-gradient(to bottom, #1e2a4a, #1a4a73);" +
                " -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;" +
                " -fx-background-radius: 10; -fx-border-radius: 10;" +
                " -fx-border-color: #00ffff; -fx-border-width: 2;" +
                " -fx-padding: 10 20; -fx-cursor: hand;" +
                " -fx-effect: dropshadow(gaussian, rgba(0, 212, 255, 0.6), 12, 0.5, 0, 3);" +
                " -fx-scale-x: 1.03; -fx-scale-y: 1.03;";
        
        String selectedStyle = "-fx-background-color: linear-gradient(to bottom, #2d5fa4, #00d4ff);" +
                " -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;" +
                " -fx-background-radius: 10; -fx-border-radius: 10;" +
                " -fx-border-color: #e94560; -fx-border-width: 2;" +
                " -fx-padding: 10 20; -fx-cursor: hand;" +
                " -fx-effect: dropshadow(gaussian, rgba(233, 69, 96, 0.6), 10, 0.5, 0, 3);";
        
        b.setStyle(normalStyle);
        b.setOnMouseEntered(e -> {
            if (!b.isSelected()) b.setStyle(hoverStyle);
        });
        b.setOnMouseExited(e -> {
            if (!b.isSelected()) b.setStyle(normalStyle);
            else b.setStyle(selectedStyle);
        });
        
        // Update style when selection changes
        b.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                b.setStyle(selectedStyle);
            } else {
                b.setStyle(normalStyle);
            }
        });
    }
}


