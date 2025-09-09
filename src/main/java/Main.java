import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import manager.game.VisualManager;
import service.AudioManager;
import net.NetworkService;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Load CSS stylesheet path
        String cssFile = getClass().getResource("/css/style.css").toExternalForm();

        // Configure the stage (window)
        primaryStage.setTitle("Blueprint Hell");
        primaryStage.initStyle(StageStyle.UNDECORATED); // No OS window borders
        primaryStage.setResizable(false);

        // Delegate all scene management to VisualManager
        VisualManager visualManager = new VisualManager(primaryStage, cssFile);
        
        // Start menu music when application starts
        AudioManager.playMenuMusic();
        
        visualManager.showMenu();

        primaryStage.show();
        
        // Add cleanup when application closes
        primaryStage.setOnCloseRequest(event -> {
            AudioManager.cleanup();
            try { net.NetworkService.getInstance().disconnect("app-exit"); } catch (Throwable ignored) {}
        });

        // Try initial online connect (non-blocking)
        new Thread(() -> {
            try {
                NetworkService.getInstance().connectOnline("127.0.0.1", 5050, System.getProperty("user.name", "player"));
            } catch (Throwable ignored) {}
        }, "initial-connect").start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

