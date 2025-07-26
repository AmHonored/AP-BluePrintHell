package view.menu;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.StackPane;

/**
 * Settings scene for Blueprint Hell. Contains a sound volume slider and a Back button.
 * No business logic, just UI and event hooks.
 */
public class SettingsScene extends StackPane {
    private final Slider volumeSlider;
    private final Label volumeValueLabel;
    private final Button backButton;

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

        backButton = new Button("Back");
        backButton.setPrefWidth(140);
        backButton.getStyleClass().add("back-button");

        VBox layout = new VBox(30, title, volumeLabel, sliderBox, backButton);
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
