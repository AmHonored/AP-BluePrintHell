package view.components.systems;

import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import model.entity.systems.VPNSystem;

public class VPNSystemView extends SystemView {
    private static final String NORMAL_STYLE = 
        "-fx-fill: #333333;" +
        "-fx-stroke: #00ffff;" +
        "-fx-stroke-width: 3;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,255,255,0.8), 15, 0, 0, 0);";
    
    private static final String DISABLED_STYLE = 
        "-fx-fill: #333333;" +
        "-fx-stroke: #ff0000;" +
        "-fx-stroke-width: 3;" +
        "-fx-effect: dropshadow(gaussian, rgba(255,0,0,0.8), 15, 0, 0, 0);";
    
    private VPNSystem vpnSystem;
    private javafx.scene.shape.Circle failureIndicator;
    private Text vpnLabel;
    private javafx.scene.control.Label capacityLabel;
    private int currentCapacity = 0; 
    private final int maxCapacity = 5; 

    public VPNSystemView(VPNSystem vpnSystem) {
        super(vpnSystem, "");
        this.vpnSystem = vpnSystem;
    }

    @Override
    protected void applySystemStyling() {
        systemRectangle.getStyleClass().add("system-normal");
        
        updateVPNVisuals();
    }

    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        content.setAlignment(Pos.CENTER);

        vpnLabel = new Text("VPN");
        vpnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        vpnLabel.setFill(Color.CYAN); 
        
        capacityLabel = new javafx.scene.control.Label(currentCapacity + "/" + maxCapacity);
        capacityLabel.getStyleClass().addAll("capacity-label", "capacity-normal");
        StackPane.setAlignment(capacityLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(capacityLabel, new javafx.geometry.Insets(0, 0, 10, 0));

        failureIndicator = new javafx.scene.shape.Circle(6);
        failureIndicator.setFill(Color.RED);
        failureIndicator.setStroke(Color.DARKRED);
        failureIndicator.setStrokeWidth(2.0);
        failureIndicator.setVisible(false);
        
        StackPane.setAlignment(failureIndicator, Pos.TOP_LEFT);
        StackPane.setMargin(failureIndicator, new javafx.geometry.Insets(5, 0, 0, 5));
        
        content.getChildren().addAll(vpnLabel, capacityLabel, failureIndicator);
        
        return content;
    }

    public void updateVPNVisuals() {
        if (vpnSystem == null) {
            return;
        }
        
        if (vpnSystem.isDisabled()) {
            systemRectangle.setStyle(DISABLED_STYLE);
            if (failureIndicator != null) {
                failureIndicator.setVisible(true);
            }
        } else {
            systemRectangle.setStyle(NORMAL_STYLE);
            if (failureIndicator != null) {
                failureIndicator.setVisible(false);
            }
        }
    }

    public VPNSystem getVPNSystem() {
        return vpnSystem;
    }

    public void updateCapacity(int newCapacity) {
        this.currentCapacity = newCapacity;
        if (capacityLabel != null) {
            String newText = newCapacity + "/" + maxCapacity;
            capacityLabel.setText(newText);
            boolean shouldBeVisible = newCapacity > 0;
            capacityLabel.setVisible(shouldBeVisible);
        }
    }
} 