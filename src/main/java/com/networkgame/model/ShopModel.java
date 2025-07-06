package com.networkgame.model;

import com.networkgame.controller.GameController;
import com.networkgame.service.audio.AudioManager.SoundType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.networkgame.model.state.GameState;
import com.networkgame.service.audio.AudioManager;


/**
 * Model class for the in-game shop
 */
public class ShopModel {
    private Scene scene;
    private final GameController gameController;
    private final GameState gameState;
    private final AudioManager audioManager;
    
    // Constants
    private static final int O_ATAR_COST = 3;
    private static final int O_AIRYAMAN_COST = 4;
    private static final int O_ANAHITA_COST = 5;
    
    private static final int O_ATAR_DURATION = 10;
    private static final int O_AIRYAMAN_DURATION = 5;
    
    public ShopModel(GameController gameController, GameState gameState) {
        this.gameController = gameController;
        this.gameState = gameState;
        this.audioManager = AudioManager.getInstance();
        
        initializeScene();
    }
    
    private void initializeScene() {
        // Create main layout
        VBox mainLayout = createMainLayout();
        
        // Create scene
        scene = new Scene(mainLayout, 800, 600);
        
        // Apply CSS
        String cssPath = getClass().getResource("/css/style.css").toExternalForm();
        scene.getStylesheets().add(cssPath);
    }
    
    private VBox createMainLayout() {
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(20));
        mainLayout.getStyleClass().add("main-menu");
        
        // Create title
        Label titleLabel = createTitleLabel();
        
        // Create coins display
        Label coinsLabel = createCoinsLabel();
        
        // Create shop items
        VBox shopItems = createShopItems();
        
        // Create back button
        Button backButton = createBackButton();
        
        // Add all components to the layout
        mainLayout.getChildren().addAll(
            titleLabel,
            coinsLabel,
            shopItems,
            backButton
        );
        
        return mainLayout;
    }
    
    private Label createTitleLabel() {
        Label titleLabel = new Label("Shop");
        titleLabel.getStyleClass().add("title-label");
        return titleLabel;
    }
    
    private Label createCoinsLabel() {
        Label coinsLabel = new Label("Your Coins: " + gameState.getCoins());
        coinsLabel.getStyleClass().addAll("stats-value", "coin-label");
        return coinsLabel;
    }
    
    private Button createBackButton() {
        Button backButton = new Button("Back to Game");
        backButton.getStyleClass().add("menu-button");
        
        backButton.setOnAction(e -> {
            audioManager.playSoundEffect(SoundType.BUTTON_CLICK);
            gameController.hideShop();
        });
        
        return backButton;
    }
    
    private VBox createShopItems() {
        VBox itemsBox = new VBox(15);
        itemsBox.setAlignment(Pos.CENTER);
        
        // O' Atar item - disables impact waves for 10 seconds
        HBox atarItem = createShopItem(
            "O' Atar", 
            "Disable Impact wave effects for " + O_ATAR_DURATION + " seconds", 
            O_ATAR_COST,
            this::purchaseOAtar
        );
        
        // O' Airyaman item - disables packet collisions
        HBox airyamanItem = createShopItem(
            "O' Airyaman", 
            "Disable packet collisions for " + O_AIRYAMAN_DURATION + " seconds", 
            O_AIRYAMAN_COST,
            this::purchaseOAiryaman
        );
        
        // O' Anahita item - resets noise for all packets
        HBox anahitaItem = createShopItem(
            "O' Anahita", 
            "Reset noise for all packets in the network", 
            O_ANAHITA_COST,
            this::purchaseOAnahita
        );
        
        itemsBox.getChildren().addAll(atarItem, airyamanItem, anahitaItem);
        
        return itemsBox;
    }
    
    private boolean purchaseOAtar() {
        if (hasEnoughCoins(O_ATAR_COST)) {
            gameState.disableImpactEffect(O_ATAR_DURATION);
            playPurchaseSound();
            return true;
        }
        return false;
    }
    
    private boolean purchaseOAiryaman() {
        if (hasEnoughCoins(O_AIRYAMAN_COST)) {
            gameState.disableCollision(O_AIRYAMAN_DURATION);
            playPurchaseSound();
            return true;
        }
        return false;
    }
    
    private boolean purchaseOAnahita() {
        if (hasEnoughCoins(O_ANAHITA_COST)) {
            gameState.resetAllPacketNoise();
            playPurchaseSound();
            return true;
        }
        return false;
    }
    
    private boolean hasEnoughCoins(int cost) {
        return gameState.getCoins() >= cost;
    }
    
    private void playPurchaseSound() {
        audioManager.playSoundEffect(SoundType.SHOP_PURCHASE);
    }
    
    private HBox createShopItem(String name, String description, int cost, java.util.function.Supplier<Boolean> action) {
        HBox itemBox = new HBox(20);
        itemBox.setAlignment(Pos.CENTER_LEFT);
        itemBox.setPadding(new Insets(15));
        itemBox.getStyleClass().add("shop-item");
        itemBox.setPrefWidth(650);
        
        VBox infoBox = createItemInfoBox(name, description);
        Label costLabel = createCostLabel(cost);
        Button buyButton = createBuyButton(cost, action);
        
        itemBox.getChildren().addAll(infoBox, costLabel, buyButton);
        
        return itemBox;
    }
    
    private VBox createItemInfoBox(String name, String description) {
        VBox infoBox = new VBox(5);
        
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("shop-item-title");
        
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("shop-item-desc");
        
        infoBox.getChildren().addAll(nameLabel, descLabel);
        return infoBox;
    }
    
    private Label createCostLabel(int cost) {
        Label costLabel = new Label(cost + " Coins");
        costLabel.getStyleClass().add("coin-label");
        return costLabel;
    }
    
    private Button createBuyButton(int cost, java.util.function.Supplier<Boolean> action) {
        Button buyButton = new Button("Buy");
        buyButton.setDisable(!hasEnoughCoins(cost));
        
        buyButton.setOnAction(e -> {
            if (action.get()) {
                updateUI();
            }
        });
        
        return buyButton;
    }
    
    private void updateUI() {
        updateCoinsDisplay();
        updateShopItemButtons();
    }
    
    private void updateCoinsDisplay() {
        // Find and update coins label
        for (javafx.scene.Node node : ((VBox) scene.getRoot()).getChildren()) {
            if (node instanceof Label && node.getStyleClass().contains("coin-label")) {
                ((Label) node).setText("Your Coins: " + gameState.getCoins());
                break;
            }
        }
    }
    
    private void updateShopItemButtons() {
        VBox shopItemsContainer = (VBox) ((VBox) scene.getRoot()).getChildren().get(2);
        
        for (javafx.scene.Node itemNode : shopItemsContainer.getChildren()) {
            if (itemNode instanceof HBox) {
                HBox itemBox = (HBox) itemNode;
                updateItemButton(itemBox);
            }
        }
    }
    
    private void updateItemButton(HBox itemBox) {
        // Find cost label and buy button
        int cost = 0;
        Button buyButton = null;
        
        for (javafx.scene.Node childNode : itemBox.getChildren()) {
            if (childNode instanceof Label && ((Label) childNode).getText().contains("Coins")) {
                cost = extractCostFromLabel((Label) childNode);
            } else if (childNode instanceof Button) {
                buyButton = (Button) childNode;
            }
        }
        
        // Disable button if not enough coins
        if (buyButton != null && cost > 0) {
            buyButton.setDisable(!hasEnoughCoins(cost));
        }
    }
    
    private int extractCostFromLabel(Label label) {
        String costText = label.getText();
        try {
            return Integer.parseInt(costText.split(" ")[0]);
        } catch (NumberFormatException e) {
            return 0; // Return 0 if parsing fails
        }
    }
    
    public Scene getScene() {
        // Update coin display and button states whenever scene is shown
        updateUI();
        return scene;
    }
} 
