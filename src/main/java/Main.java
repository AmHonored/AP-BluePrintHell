import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import manager.game.VisualManager;
import service.AudioManager;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        String cssFile = getClass().getResource("/css/style.css").toExternalForm();

        primaryStage.setTitle("Blueprint Hell");
        primaryStage.initStyle(StageStyle.UNDECORATED); 
        primaryStage.setResizable(false);

        VisualManager visualManager = new VisualManager(primaryStage, cssFile);
        
        AudioManager.playMenuMusic();
        
        visualManager.showMenu();

        primaryStage.show();
        
        primaryStage.setOnCloseRequest(event -> {
            AudioManager.cleanup();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}

