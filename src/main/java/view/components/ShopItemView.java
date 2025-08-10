package view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import model.logic.Shop.ShopItem;

public class ShopItemView extends VBox {
    private final ShopItem item;
    private final Label nameLabel;
    private final Label priceLabel;
    private final Label durationLabel;
    private final Button buyButton;
    

    public ShopItemView(ShopItem item) {
        this.item = item;
        this.setSpacing(16);
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(18));
        this.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #16213ecc, #0f3460cc);" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: #00d4ff;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, #00d4ff44, 12, 0.3, 0, 2);"
        );
        this.setOnMouseEntered(e -> setStyle(
            "-fx-background-color: linear-gradient(to bottom, #1e2a4a, #1a4a73);" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: #00d4ff;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, #00d4ff88, 18, 0.5, 0, 4);"
        ));
        this.setOnMouseExited(e -> setStyle(
            "-fx-background-color: linear-gradient(to bottom, #16213ecc, #0f3460cc);" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: #00d4ff;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, #00d4ff44, 12, 0.3, 0, 2);"
        ));

        nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #00d4ff; -fx-padding: 0 0 8 0;");
        
        priceLabel = new Label("Price: " + item.getPrice() + " coins");
        priceLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #00d4ff;");
        
        durationLabel = new Label(item.getDurationSeconds() > 0 ? ("Duration: " + item.getDurationSeconds() + "s") : "Instant");
        durationLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #00d4ff;");
        
        // Create buttons container
        HBox buttonsBox = new HBox(12);
        buttonsBox.setAlignment(Pos.CENTER);
        
        // Buy button
        buyButton = new Button("Buy");
        buyButton.setPrefWidth(80);
        buyButton.setStyle(getBuyButtonStyle());
        buyButton.setOnMouseEntered(e -> buyButton.setStyle(getBuyButtonHoverStyle()));
        buyButton.setOnMouseExited(e -> buyButton.setStyle(getBuyButtonStyle()));
        
        buttonsBox.getChildren().addAll(buyButton);
        
        this.getChildren().addAll(nameLabel, priceLabel, durationLabel, buttonsBox);
    }

    private String getBuyButtonStyle() {
        return "-fx-font-size: 16px; -fx-background-radius: 8; -fx-background-color: linear-gradient(to bottom, #00d4ff, #2d5fa4); -fx-text-fill: #fff; -fx-font-weight: bold; -fx-border-color: #00d4ff; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #00d4ff, 4, 0.3, 0, 1);";
    }
    
    private String getBuyButtonHoverStyle() {
        return "-fx-font-size: 16px; -fx-background-radius: 8; -fx-background-color: linear-gradient(to bottom, #2d5fa4, #00d4ff); -fx-text-fill: #fff; -fx-font-weight: bold; -fx-border-color: #00d4ff; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #00d4ff, 8, 0.5, 0, 2);";
    }
    

    public Button getBuyButton() { return buyButton; }
    public ShopItem getItem() { return item; }
}
