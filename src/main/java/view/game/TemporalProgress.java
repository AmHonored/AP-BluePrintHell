package view.game;

import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Circle;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import model.levels.Level;

public class TemporalProgress extends HBox {
    private final ProgressBar progressBar;
    private final Circle thumb;
    private final Label timeLabel;
    private final StackPane barStack;

    public TemporalProgress(Level level) {
        this.setSpacing(8);
        this.setAlignment(Pos.CENTER);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(160);
        progressBar.setPrefHeight(12);
        progressBar.getStyleClass().add("progress-bar");

        thumb = new Circle(6);
        thumb.getStyleClass().add("temporal-thumb");

        barStack = new StackPane(progressBar, thumb);
        barStack.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(thumb, Pos.CENTER_LEFT);

        timeLabel = new Label("01:00");
        timeLabel.getStyleClass().add("time-label");
        timeLabel.setManaged(false);
        timeLabel.setVisible(false);

        this.getChildren().addAll(barStack);
    }

    public ProgressBar getProgressBar() { return progressBar; }
    public Circle getThumb() { return thumb; }
    public Label getTimeLabel() { return timeLabel; }
}
