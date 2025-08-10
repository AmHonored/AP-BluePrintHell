package view.components.levels;

public final class LevelResumeDialog {
    private LevelResumeDialog() {}

    public static boolean show(String cssFile, serialization.save.SaveGame saved) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Resume Interrupted Game");
        alert.setHeaderText(null);

        try { alert.getDialogPane().getStylesheets().add(cssFile); } catch (Throwable ignored) {}
        alert.getDialogPane().setStyle("-fx-background-color: #0f1319; -fx-text-fill: #e9edf1; -fx-padding: 16;");

        String when = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(saved.savedAtEpochMillis));
        String ago = humanizeMillis(System.currentTimeMillis() - saved.savedAtEpochMillis);

        javafx.scene.layout.VBox contentBox = new javafx.scene.layout.VBox(8);
        contentBox.setStyle("-fx-alignment: center-left;");

        javafx.scene.control.Label title = new javafx.scene.control.Label("Do you want to continue where you left off?");
        title.setStyle("-fx-text-fill: #e9edf1; -fx-font-size: 16px; -fx-font-weight: bold;");

        javafx.scene.control.Label meta = new javafx.scene.control.Label("Level: " + saved.levelId + "\nSaved at: " + when + " (" + ago + " ago)");
        meta.setStyle("-fx-text-fill: #9fb3c8; -fx-font-size: 12px;");

        javafx.scene.control.Label hint = new javafx.scene.control.Label("Yes: resume from last autosave\nNo: discard autosave and start fresh");
        hint.setStyle("-fx-text-fill: #8dd3ff; -fx-font-size: 12px;");

        contentBox.getChildren().addAll(title, meta, hint);
        alert.getDialogPane().setContent(contentBox);

        javafx.scene.control.ButtonType yesBtn = new javafx.scene.control.ButtonType("Yes", javafx.scene.control.ButtonBar.ButtonData.YES);
        javafx.scene.control.ButtonType noBtn = new javafx.scene.control.ButtonType("No", javafx.scene.control.ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesBtn, noBtn);

        try {
            alert.getDialogPane().lookupButton(yesBtn).setStyle("-fx-background-color: #00d4ff; -fx-text-fill: #0b0f14; -fx-font-weight: bold;");
            alert.getDialogPane().lookupButton(noBtn).setStyle("-fx-background-color: #2a3441; -fx-text-fill: #e9edf1;");
        } catch (Throwable ignored) {}

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesBtn;
    }

    private static String humanizeMillis(long ms) {
        if (ms < 60_000) return (ms / 1000) + "s";
        if (ms < 3_600_000) return (ms / 60_000) + "m";
        if (ms < 86_400_000) return (ms / 3_600_000) + "h";
        return (ms / 86_400_000) + "d";
    }
}



