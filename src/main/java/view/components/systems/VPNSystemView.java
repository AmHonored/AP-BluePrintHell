package view.components.systems;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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

    public VPNSystemView(VPNSystem vpnSystem) {
        super(vpnSystem, "");
        this.vpnSystem = vpnSystem;
    }

    @Override
    protected void applySystemStyling() {
        // Apply normal system styling with CSS classes
        systemRectangle.getStyleClass().add("system-normal");
        
        // Update based on current status
        updateVPNVisuals();
    }

    @Override
    protected StackPane getSystemContent() {
        StackPane content = new StackPane();
        content.setAlignment(Pos.CENTER);

        // Add "VPN" label in the center (styled like other systems)
        vpnLabel = new Text("VPN");
        vpnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        vpnLabel.setFill(Color.CYAN); // Cyan text to match the border
        
        // Create failure indicator (larger, more visible red circle)
        failureIndicator = new javafx.scene.shape.Circle(6);
        failureIndicator.setFill(Color.RED);
        failureIndicator.setStroke(Color.DARKRED);
        failureIndicator.setStrokeWidth(2.0);
        failureIndicator.setVisible(false);
        
        // Position failure indicator in top-left corner
        StackPane.setAlignment(failureIndicator, Pos.TOP_LEFT);
        StackPane.setMargin(failureIndicator, new javafx.geometry.Insets(5, 0, 0, 5));
        
        content.getChildren().addAll(vpnLabel, failureIndicator);
        
        return content;
    }

    /**
     * Update VPN-specific visual state
     */
    public void updateVPNVisuals() {
        // Add null check to prevent NPE during initialization
        if (vpnSystem == null) {
            return;
        }
        
        if (vpnSystem.isDisabled()) {
            // Show disabled state with red glow
            systemRectangle.setStyle(DISABLED_STYLE);
            if (failureIndicator != null) {
                failureIndicator.setVisible(true);
            }
        } else {
            // Show normal state with cyan glow
            systemRectangle.setStyle(NORMAL_STYLE);
            if (failureIndicator != null) {
                failureIndicator.setVisible(false);
            }
        }
    }

    /**
     * Get the VPN system associated with this view
     */
    public VPNSystem getVPNSystem() {
        return vpnSystem;
    }
} 