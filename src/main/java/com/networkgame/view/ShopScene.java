package com.networkgame.view;

import com.networkgame.controller.GameController;
import com.networkgame.service.audio.AudioManager;
import com.networkgame.service.audio.AudioManager.SoundType;
import com.networkgame.model.state.GameState;
import javafx.animation.SequentialTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class ShopScene {
    private Scene scene;
    private GameController gameController;
    private GameState gameState;
    private AudioManager audioManager;
    
    public ShopScene(GameController gameController, GameState gameState) {
        this.gameController = gameController;
        this.gameState = gameState;
        this.audioManager = AudioManager.getInstance();
        
        initializeUI();
    }
    
    private void initializeUI() {
        // Create main layout
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(20));
        mainLayout.getStyleClass().add("main-menu");
        
        // Create title
        Label titleLabel = new Label("Shop");
        titleLabel.getStyleClass().add("title-label");
        
        // Create coins display
        Label coinsLabel = new Label("Your Coins: " + gameState.getCoins());
        coinsLabel.getStyleClass().addAll("stats-value", "coin-label");
        
        // Create shop items with scroll pane
        ScrollPane shopScrollPane = createShopScrollPane();
        
        // Create back button
        Button backButton = new Button("Back to Game");
        backButton.getStyleClass().add("menu-button");
        backButton.setOnAction(e -> {
            audioManager.playSoundEffect(SoundType.BUTTON_CLICK);
            gameController.hideShop();
        });
        
        // Add all components to the layout
        mainLayout.getChildren().addAll(titleLabel, coinsLabel, shopScrollPane, backButton);
        
        // Create scene and apply CSS
        scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        // Set scene activation handler
        scene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
            if (newWindow != null) {
                newWindow.setOnShown(e -> audioManager.playBackgroundMusic());
            }
        });
    }
    
    private ScrollPane createShopScrollPane() {
        VBox itemsBox = new VBox(15);
        itemsBox.setAlignment(Pos.CENTER);
        itemsBox.setPadding(new Insets(10));
        
        // Existing items
        HBox atarItem = createShopItem(
            "O' Atar", 
            "Disable Impact wave effects for 10 seconds", 
            3,
            () -> {
                if (gameState.getCoins() >= 3) {
                    gameState.disableImpactEffect(10);
                    audioManager.playSoundEffect(SoundType.SHOP_PURCHASE);
                    return true;
                }
                return false;
            }
        );
        
        HBox airyamanItem = createShopItem(
            "O' Airyaman", 
            "Disable packet collisions for 5 seconds", 
            4,
            () -> {
                if (gameState.getCoins() >= 4) {
                    gameState.disableCollision(5);
                    audioManager.playSoundEffect(SoundType.SHOP_PURCHASE);
                    return true;
                }
                return false;
            }
        );
        
        HBox anahitaItem = createShopItem(
            "O' Anahita", 
            "Reset noise for all packets in the network", 
            5,
            () -> {
                if (gameState.getCoins() >= 5) {
                    gameState.resetAllPacketNoise();
                    audioManager.playSoundEffect(SoundType.SHOP_PURCHASE);
                    return true;
                }
                return false;
            }
        );
        
        // New scroll items
        HBox aergiaItem = createShopItem(
            "Scroll of Aergia", 
            "Select a point on network connections to set packet acceleration to zero for 20 seconds", 
            10,
            () -> {
                if (gameState.getCoins() >= 10) {
                    // Placeholder for future implementation
                    gameState.spendCoins(10);
                    audioManager.playSoundEffect(SoundType.SHOP_PURCHASE);
                    return true;
                }
                return false;
            }
        );
        
        HBox sisyphusItem = createShopItem(
            "Scroll of Sisyphus", 
            "Move non-reference systems within a specific radius with wire length constraints", 
            15,
            () -> {
                if (gameState.getCoins() >= 15) {
                    // Placeholder for future implementation
                    gameState.spendCoins(15);
                    audioManager.playSoundEffect(SoundType.SHOP_PURCHASE);
                    return true;
                }
                return false;
            }
        );
        
        HBox eliphasItem = createShopItem(
            "Scroll of Eliphas", 
            "Reset packet center of gravity affected by collisions and impacts for 30 seconds", 
            20,
            () -> {
                if (gameState.getCoins() >= 20) {
                    // Placeholder for future implementation
                    gameState.spendCoins(20);
                    audioManager.playSoundEffect(SoundType.SHOP_PURCHASE);
                    return true;
                }
                return false;
            }
        );
        
        itemsBox.getChildren().addAll(atarItem, airyamanItem, anahitaItem, aergiaItem, sisyphusItem, eliphasItem);
        
        // Create ScrollPane
        ScrollPane scrollPane = new ScrollPane(itemsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPrefHeight(300);
        scrollPane.setMaxHeight(300);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("shop-scroll-pane");
        
        return scrollPane;
    }
    
    private HBox createShopItem(String name, String description, int cost, java.util.function.Supplier<Boolean> action) {
        HBox itemBox = new HBox(20);
        itemBox.setAlignment(Pos.CENTER_LEFT);
        itemBox.setPadding(new Insets(15));
        itemBox.getStyleClass().add("shop-item");
        itemBox.setPrefWidth(650);
        
        // Item information
        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("shop-item-title");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("shop-item-desc");
        infoBox.getChildren().addAll(nameLabel, descLabel);
        
        // Cost and purchase button
        Label costLabel = new Label(cost + " Coins");
        costLabel.getStyleClass().add("coin-label");
        
        Button buyButton = new Button("Buy");
        buyButton.setDisable(gameState.getCoins() < cost);
        
        buyButton.setOnAction(e -> {
            if (action.get()) {
                updateCoinsDisplayWithAnimation();
                updateShopItemButtons();
            }
        });
        
        itemBox.getChildren().addAll(infoBox, costLabel, buyButton);
        
        return itemBox;
    }
    
    private void updateCoinsDisplayWithAnimation() {
        Label coinsLabel = findCoinsLabel();
        if (coinsLabel == null) return;
        
        // Store original text color
        javafx.scene.paint.Paint originalColor = coinsLabel.getTextFill();
        
        // Create scale animation
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), coinsLabel);
        scaleUp.setFromX(1.0);
        scaleUp.setFromY(1.0);
        scaleUp.setToX(1.3);
        scaleUp.setToY(1.3);
        
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), coinsLabel);
        scaleDown.setFromX(1.3);
        scaleDown.setFromY(1.3);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        
        // Create animation sequence
        SequentialTransition sequence = new SequentialTransition(scaleUp, scaleDown);
        
        // Update text and color after scaling up
        scaleUp.setOnFinished(event -> {
            coinsLabel.setText("Your Coins: " + gameState.getCoins());
            coinsLabel.setTextFill(Color.GOLD);
        });
        
        // Reset color after animation completes
        scaleDown.setOnFinished(event -> {
            coinsLabel.setTextFill(originalColor != null ? originalColor : Color.YELLOW);
            coinsLabel.setText("Your Coins: " + gameState.getCoins());
        });
        
        sequence.play();
    }
    
    private Label findCoinsLabel() {
        for (javafx.scene.Node node : ((VBox) scene.getRoot()).getChildren()) {
            if (node instanceof Label && node.getStyleClass().contains("coin-label")) {
                return (Label) node;
            }
        }
        return null;
    }
    
    private void updateShopItemButtons() {
        // Find the scroll pane and get the items container
        ScrollPane shopScrollPane = null;
        for (javafx.scene.Node node : ((VBox) scene.getRoot()).getChildren()) {
            if (node instanceof ScrollPane) {
                shopScrollPane = (ScrollPane) node;
                break;
            }
        }
        
        if (shopScrollPane == null) return;
        
        VBox shopItemsContainer = (VBox) shopScrollPane.getContent();
        
        for (javafx.scene.Node node : shopItemsContainer.getChildren()) {
            if (!(node instanceof HBox)) continue;
            
            HBox itemBox = (HBox) node;
            Label costLabel = null;
            Button buyButton = null;
            
            for (javafx.scene.Node itemNode : itemBox.getChildren()) {
                if (itemNode instanceof Label && itemNode.getStyleClass().contains("coin-label")) {
                    costLabel = (Label) itemNode;
                } else if (itemNode instanceof Button) {
                    buyButton = (Button) itemNode;
                }
            }
            
            if (costLabel != null && buyButton != null) {
                String costText = costLabel.getText();
                int cost = Integer.parseInt(costText.split(" ")[0]);
                
                boolean wasEnabled = !buyButton.isDisable();
                boolean shouldBeEnabled = gameState.getCoins() >= cost;
                
                if (wasEnabled && !shouldBeEnabled) {
                    FadeTransition fade = new FadeTransition(Duration.millis(300), buyButton);
                    fade.setFromValue(1.0);
                    fade.setToValue(0.6);
                    fade.play();
                }
                
                buyButton.setDisable(!shouldBeEnabled);
            }
        }
    }
    
    public Scene getScene() {
        Label coinsLabel = findCoinsLabel();
        if (coinsLabel != null) {
            coinsLabel.setText("Your Coins: " + gameState.getCoins());
        }
        
        updateShopItemButtons();
        audioManager.playBackgroundMusic();
        return scene;
    }
} 
