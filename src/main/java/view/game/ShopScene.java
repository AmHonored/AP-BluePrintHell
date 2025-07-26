package view.game;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import manager.game.ShopManager;
import model.logic.Shop.ShopItem;
import view.components.ShopItemView;
import model.levels.Level;
import java.util.List;

public class ShopScene extends VBox {
    private final ShopManager shopManager;
    private final Level level;
    private final Label coinsLabel;
    private final HBox itemsBox;
    private final Button closeButton;

    public ShopScene(ShopManager shopManager, Level level) {
        this.shopManager = shopManager;
        this.level = level;
        this.setSpacing(30);
        this.setAlignment(Pos.CENTER);
        this.setPadding(new javafx.geometry.Insets(40, 40, 40, 40));
        this.getStyleClass().add("overlay");

        Label title = new Label("Shop");
        title.getStyleClass().add("overlay-label");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #e94560; -fx-padding: 0 0 20 0;");

        coinsLabel = new Label("Coins: " + level.getCoins());
        coinsLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #00d4ff; -fx-padding: 0 0 20 0;");

        itemsBox = new HBox(40);
        itemsBox.setAlignment(Pos.CENTER);

        closeButton = new Button("Close");
        closeButton.setPrefWidth(140);
        closeButton.setStyle("-fx-font-size: 20px; -fx-background-radius: 12; -fx-background-color: linear-gradient(to bottom, #e94560, #a34242); -fx-text-fill: #fff; -fx-font-weight: bold; -fx-border-color: #e94560; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #e94560, 8, 0.5, 0, 2); -fx-padding: 10 0 10 0; -fx-margin-top: 30px;");
        closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-font-size: 20px; -fx-background-radius: 12; -fx-background-color: linear-gradient(to bottom, #a34242, #e94560); -fx-text-fill: #fff; -fx-font-weight: bold; -fx-border-color: #e94560; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #e94560, 12, 0.7, 0, 3); -fx-padding: 10 0 10 0; -fx-margin-top: 30px;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-font-size: 20px; -fx-background-radius: 12; -fx-background-color: linear-gradient(to bottom, #e94560, #a34242); -fx-text-fill: #fff; -fx-font-weight: bold; -fx-border-color: #e94560; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #e94560, 8, 0.5, 0, 2); -fx-padding: 10 0 10 0; -fx-margin-top: 30px;"));
        VBox.setMargin(closeButton, new javafx.geometry.Insets(30, 0, 0, 0));

        this.getChildren().addAll(title, coinsLabel, itemsBox, closeButton);
        refreshItems();
    }

    private void refreshItems() {
        itemsBox.getChildren().clear();
        List<ShopItem> items = shopManager.getItems();
        for (ShopItem item : items) {
            ShopItemView itemView = new ShopItemView(item);
            itemView.getBuyButton().setOnAction(e -> {
                if (shopManager.purchase(item)) {
                    coinsLabel.setText("Coins: " + level.getCoins());
                    refreshItems();
                }
            });
            itemsBox.getChildren().add(itemView);
        }
    }

    public Button getCloseButton() { return closeButton; }
}
