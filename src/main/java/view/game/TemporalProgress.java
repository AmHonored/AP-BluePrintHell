package view.game;

import javafx.scene.layout.HBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Circle;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import model.levels.Level;

public class TemporalProgress extends HBox {
    private final ProgressBar progressBar;
    private final Circle thumb;
    private final Label timeLabel;

    public TemporalProgress(Level level) {
        this.setSpacing(10);
        this.setAlignment(Pos.CENTER);
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(180);
        progressBar.getStyleClass().add("progress-bar");
        
        thumb = new Circle(6);
        thumb.getStyleClass().add("temporal-thumb");
        
        timeLabel = new Label("01:00");
        timeLabel.getStyleClass().add("time-label");
        
        this.getChildren().addAll(progressBar, thumb, timeLabel);
    }

    public ProgressBar getProgressBar() { return progressBar; }
    public Circle getThumb() { return thumb; }
    public Label getTimeLabel() { return timeLabel; }
}
