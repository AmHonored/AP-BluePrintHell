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

        String currentUsername = net.client.UserIdentity.getEffectiveUsername();
        profileLabel = new Label("Current Username: " + currentUsername);
        profileLabel.getStyleClass().add("settings-profile-label");

        xpLabel = new Label("XP: -");
        xpLabel.getStyleClass().add("settings-xp-label");

        // Listen for server profile snapshots to update XP
        try {
            NetworkService.getInstance().onProfileSnapshot((Profile.Snapshot snap) -> {
                Platform.runLater(() -> {
                    xpLabel.setText("XP: " + snap.xp);
                    // Update username display if server echoes it back
                    if (snap.username != null && !snap.username.isEmpty()) {
                        profileLabel.setText("Current Username: " + snap.username);
                    }
                });
            });
        } catch (Throwable ignored) {}

        // Username field for multiplayer
        TextField usernameField = new TextField(net.client.UserIdentity.getEffectiveUsername());
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("profile-switch-field");
        usernameField.setTooltip(new javafx.scene.control.Tooltip("Your display name in multiplayer"));
        usernameField.setPrefWidth(320);
        usernameField.setMaxWidth(420);
        
        Button switchButton = new Button("Apply Username");
        switchButton.getStyleClass().add("profile-switch-button");
        switchButton.setPrefWidth(220);
        
        // Checkbox for temporary session mode (for multiplayer testing)
        javafx.scene.control.CheckBox tempSessionBox = new javafx.scene.control.CheckBox("Temporary Session");
        tempSessionBox.setTooltip(new javafx.scene.control.Tooltip("Enable for testing multiplayer with multiple clients"));
        tempSessionBox.getStyleClass().add("session-checkbox");
        
        switchButton.setOnAction(e -> {
            String username = usernameField.getText();
            if (username == null || username.trim().isEmpty()) {
                try { view.components.Toast.show(this, "Please enter a username", false); } catch (Throwable ignored) {}
                return;
            }
            username = username.trim();
            
            // Use username as the profile name
            String profileName = username;
            
            // If temporary session is checked, create a unique session profile
            if (tempSessionBox.isSelected()) {
                profileName = "temp_" + username + "_" + System.currentTimeMillis();
                System.setProperty("networkgame.sessionbased", "true");
            } else {
                // Clear session-based mode if unchecked
                System.clearProperty("networkgame.sessionbased");
            }
            
            // Set both profile and username
            System.setProperty("networkgame.profile", profileName);
            System.setProperty("networkgame.username", username);
            
            // Only persist last-profile if not a temporary session
            if (!tempSessionBox.isSelected()) {
                try {
                    ProfileManager.setLast(profileName);
                } catch (Exception ignored2) {}
            }
            
            profileLabel.setText("Username: " + username + (tempSessionBox.isSelected() ? " (temp)" : ""));
            
            // Reconnect to apply new profile
            try {
                NetworkService.getInstance().disconnect("profile-switch");
                NetworkService.getInstance().connectOnline("127.0.0.1", 5050, username);
            } catch (Throwable ignored3) {}
            
            try { view.components.Toast.show(this, "Switched to: " + username, true); } catch (Throwable ignored) {}
        });
        
        // Create styled label
        Label usernameFieldLabel = new Label("Username:");
        usernameFieldLabel.getStyleClass().add("settings-label");
        
        VBox profileControls = new VBox(15);
        profileControls.setFillWidth(false);
        profileControls.setMaxWidth(500);
        VBox usernameFieldBox = new VBox(8, usernameFieldLabel, usernameField);
        usernameFieldBox.setAlignment(Pos.CENTER);
        
        profileControls.getChildren().addAll(usernameFieldBox, tempSessionBox, switchButton);
        profileControls.setAlignment(Pos.CENTER);

        backButton = new Button("Back");
        backButton.setPrefWidth(140);
        backButton.getStyleClass().add("back-button");

        VBox layout = new VBox(20, title, profileLabel, xpLabel, profileControls, volumeLabel, sliderBox, backButton);
        layout.setFillWidth(false);
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
