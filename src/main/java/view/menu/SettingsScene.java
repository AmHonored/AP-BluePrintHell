package view.menu;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;

import net.NetworkService;
import net.client.ProfileManager;
import protocol.messages.Profile;

/**
 * Settings scene for Blueprint Hell. Contains a sound volume slider and a Back button.
 * No business logic, just UI and event hooks.
 */
public class SettingsScene extends StackPane {
    private final Slider volumeSlider;
    private final Label volumeValueLabel;
    private final Button backButton;
    private final Label profileLabel;
    private final Label xpLabel;

    public SettingsScene(double initialVolume) {
        Text title = new Text("Settings");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.getStyleClass().add("settings-title");

        Label volumeLabel = new Label("Sound Volume");
        volumeLabel.getStyleClass().add("settings-label");

        volumeSlider = new Slider(0, 100, initialVolume);
        volumeSlider.setShowTickLabels(false);
        volumeSlider.setShowTickMarks(false);
        volumeSlider.setMajorTickUnit(25);
        volumeSlider.setMinorTickCount(4);
        volumeSlider.setBlockIncrement(1);
        volumeSlider.setPrefWidth(400);
        volumeSlider.setPrefHeight(40);

        // Volume value display
        volumeValueLabel = new Label(String.format("%.0f%%", initialVolume));
        volumeValueLabel.getStyleClass().add("settings-label");
        volumeValueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        volumeValueLabel.setStyle("-fx-text-fill: #00d4ff; -fx-min-width: 60px; -fx-alignment: center;");

        // Update label when slider changes
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            volumeValueLabel.setText(String.format("%.0f%%", newVal.doubleValue()));
        });

        HBox sliderBox = new HBox(15, volumeSlider, volumeValueLabel);
        sliderBox.setAlignment(Pos.CENTER);

        String currentProfile = ProfileManager.getLastOrDefault();
        profileLabel = new Label("Profile: " + currentProfile);
        profileLabel.getStyleClass().add("settings-profile-label");

        xpLabel = new Label("XP: -");
        xpLabel.getStyleClass().add("settings-xp-label");

        // Listen for server profile snapshots to update XP
        try {
            NetworkService.getInstance().onProfileSnapshot((Profile.Snapshot snap) -> {
                Platform.runLater(() -> {
                    xpLabel.setText("XP: " + snap.xp);
                    // If server echoes username, show it alongside profile
                    if (snap.username != null && !snap.username.isEmpty()) {
                        profileLabel.setText("Profile: " + currentProfile + " (" + snap.username + ")");
                    }
                });
            });
        } catch (Throwable ignored) {}

        // Profile switch controls
        TextField profileField = new TextField(currentProfile);
        profileField.setPromptText("profile name");
        profileField.getStyleClass().add("profile-switch-field");
        Button switchButton = new Button("Switch Profile");
        switchButton.getStyleClass().add("profile-switch-button");
        switchButton.setOnAction(e -> {
            String next = profileField.getText();
            if (next == null || next.trim().isEmpty()) return;
            next = next.trim();
            try { ProfileManager.setLast(next); } catch (Exception ignored2) {}
            profileLabel.setText("Profile: " + next);
            // Reconnect to apply new deviceId (derived from profile)
            try {
                NetworkService.getInstance().disconnect("profile-switch");
                NetworkService.getInstance().connectOnline("127.0.0.1", 5050, System.getProperty("user.name", "player"));
            } catch (Throwable ignored3) {}
        });
        HBox profileBox = new HBox(10, profileField, switchButton);
        profileBox.setAlignment(Pos.CENTER);

        backButton = new Button("Back");
        backButton.setPrefWidth(140);
        backButton.getStyleClass().add("back-button");

        VBox layout = new VBox(20, title, profileLabel, xpLabel, profileBox, volumeLabel, sliderBox, backButton);
        layout.setAlignment(Pos.CENTER);
        this.getChildren().add(layout);
        this.setAlignment(Pos.CENTER);
        this.getStyleClass().add("menu-root");
    }

    public Slider getVolumeSlider() {
        return volumeSlider;
    }

    public Button getBackButton() {
        return backButton;
    }

    public Label getVolumeValueLabel() {
        return volumeValueLabel;
    }
}
