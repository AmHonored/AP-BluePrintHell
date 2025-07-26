package view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import model.logic.Shop.ShopItem;

public class ShopItemView extends VBox {
    private final ShopItem item;
    private final Label nameLabel;
    private final Label priceLabel;
    private final Label durationLabel;
    private final Button buyButton;
    private final Button infoButton;

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
        
        // Info button
        infoButton = new Button("i");
        infoButton.setPrefWidth(30);
        infoButton.setPrefHeight(30);
        infoButton.setStyle(getInfoButtonStyle());
        infoButton.setOnMouseEntered(e -> infoButton.setStyle(getInfoButtonHoverStyle()));
        infoButton.setOnMouseExited(e -> infoButton.setStyle(getInfoButtonStyle()));
        infoButton.setOnAction(e -> showItemInfo());
        
        buttonsBox.getChildren().addAll(buyButton, infoButton);
        
        this.getChildren().addAll(nameLabel, priceLabel, durationLabel, buttonsBox);
    }

    private String getBuyButtonStyle() {
        return "-fx-font-size: 16px; -fx-background-radius: 8; -fx-background-color: linear-gradient(to bottom, #00d4ff, #2d5fa4); -fx-text-fill: #fff; -fx-font-weight: bold; -fx-border-color: #00d4ff; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #00d4ff, 4, 0.3, 0, 1);";
    }
    
    private String getBuyButtonHoverStyle() {
        return "-fx-font-size: 16px; -fx-background-radius: 8; -fx-background-color: linear-gradient(to bottom, #2d5fa4, #00d4ff); -fx-text-fill: #fff; -fx-font-weight: bold; -fx-border-color: #00d4ff; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #00d4ff, 8, 0.5, 0, 2);";
    }
    
    private String getInfoButtonStyle() {
        return "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-background-color: linear-gradient(to bottom, #e94560, #a34242); -fx-text-fill: #fff; -fx-border-color: #e94560; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #e94560, 4, 0.3, 0, 1);";
    }
    
    private String getInfoButtonHoverStyle() {
        return "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-background-color: linear-gradient(to bottom, #a34242, #e94560); -fx-text-fill: #fff; -fx-border-color: #e94560; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #e94560, 8, 0.5, 0, 2);";
    }
    
    private void showItemInfo() {
        service.AudioManager.playButtonClick();
        
        String description = getItemDescription();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Item Information");
        alert.setHeaderText(item.getName());
        alert.setContentText(description);
        
        // Style the alert to match the game theme
        alert.getDialogPane().setStyle(
            "-fx-background-color: linear-gradient(to bottom, #0a0e27, #16213e);" +
            "-fx-border-color: #00d4ff;" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 15;" +
            "-fx-background-radius: 15;"
        );
        
        // Style the content text
        alert.getDialogPane().lookup(".content.label").setStyle(
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 20px;"
        );
        
        // Style the header text
        alert.getDialogPane().lookup(".header-panel .label").setStyle(
            "-fx-text-fill: #00d4ff;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;"
        );
        
        alert.showAndWait();
    }
    
    private String getItemDescription() {
        switch (item.getName()) {
            case "O' Atar":
                return "Cost: 3 coins\n" +
                       "Duration: 10 seconds\n\n" +
                       "Disables Impact Wave effects for 10 seconds. " +
                       "During this time, impact waves that normally damage packets " +
                       "will not affect any packets in the network.";
                       
            case "O' Airyaman":
                return "Cost: 4 coins\n" +
                       "Duration: 5 seconds\n\n" +
                       "Disables packet collisions for 5 seconds. " +
                       "During this time, packets can pass through each other " +
                       "without causing damage or interference.";
                       
            case "O' Anahita":
                return "Cost: 5 coins\n" +
                       "Effect: Instant\n\n" +
                       "Instantly removes all noise from every packet " +
                       "currently present in the network. This improves " +
                       "the quality and reliability of all active packets.";
                       
            default:
                return "No description available for this item.";
        }
    }

    public Button getBuyButton() { return buyButton; }
    public Button getInfoButton() { return infoButton; }
    public ShopItem getItem() { return item; }
}
