package view.game;

import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

public class StatsBox extends VBox {
    private final Label titleLabel;
    private final Label valueLabel;

    public StatsBox(String title, String value) {
        this.getStyleClass().add("stats-box");
        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stats-title");
        valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stats-value");
        this.getChildren().addAll(titleLabel, valueLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public Label getValueLabel() {
        return valueLabel;
    }

    public Label getTitleLabel() {
        return titleLabel;
    }
} 