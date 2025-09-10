package view.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Interpolator;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Simple toast notification utility for showing brief messages.
 */
public class Toast {
    
    /**
     * Show a toast message on the given parent container.
     * @param parent The parent container to show the toast on
     * @param message The message to display
     * @param isSuccess Whether this is a success (true) or error (false) message
     */
    public static void show(javafx.scene.Parent parent, String message, boolean isSuccess) {
        try {
            Label toast = new Label(message);
            String baseStyle = "-fx-background-color: rgba(0,0,0,0.8); -fx-padding: 8 12; " +
                             "-fx-background-radius: 8; -fx-font-weight: bold; " +
                             "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 4, 0.5, 0, 2);";
            
            String colorStyle = isSuccess ? 
                "-fx-text-fill: #00ff88; -fx-border-color: #00ff88;" :
                "-fx-text-fill: #ff4d4d; -fx-border-color: #ff4d4d;";
                
            toast.setStyle(baseStyle + colorStyle + "-fx-border-width: 1; -fx-border-radius: 8;");
            toast.setMouseTransparent(true);
            toast.setOpacity(0.0);
            
            if (parent instanceof StackPane) {
                StackPane stackPane = (StackPane) parent;
                stackPane.getChildren().add(toast);
                StackPane.setAlignment(toast, Pos.TOP_CENTER);
                
                // Smooth fade-in, stay, fade-out
                FadeTransition fadeIn = new FadeTransition(Duration.millis(280), toast);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.setInterpolator(Interpolator.EASE_BOTH);

                PauseTransition stay = new PauseTransition(Duration.seconds(2.4));

                FadeTransition fadeOut = new FadeTransition(Duration.millis(350), toast);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setInterpolator(Interpolator.EASE_BOTH);

                SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
                seq.setOnFinished(e -> stackPane.getChildren().remove(toast));
                seq.play();
            }
        } catch (Throwable ignored) {
            // Silent fail - toast is not critical functionality
        }
    }
}
