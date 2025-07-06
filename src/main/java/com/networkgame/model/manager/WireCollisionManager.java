package com.networkgame.model.manager;

import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.Port;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.system.NetworkSystem;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.CubicCurve;

import java.util.*;

/**
 * Manages wire-system collision detection and bend point system
 */
public class WireCollisionManager {
    private final GameState gameState;
    private final Map<Connection, List<CollisionSegment>> collisionSegments = new HashMap<>();
    private final Map<Connection, List<BendPoint>> bendPoints = new HashMap<>();
    private final Map<Connection, List<Button>> bendButtons = new HashMap<>();
    private final Map<Connection, List<Line>> collisionVisualSegments = new HashMap<>();
    private final Map<Connection, Polyline> wirePolylines = new HashMap<>();
    private final Map<Connection, List<CubicCurve>> wireCurves = new HashMap<>();
    
    // Constants
    private static final int MAX_BEND_POINTS = 3;
    private static final int BEND_POINT_COST = 1;
    private static final double BEND_POINT_RADIUS = 30.0;
    private static final double COLLISION_TOLERANCE = 5.0;
    private static final double COLLISION_SEGMENT_RADIUS = 15.0; // Radius around collision point to show as blue
    private static final double CURVE_CONTROL_OFFSET = 50.0; // Distance for Bezier curve control points
    
    public WireCollisionManager(GameState gameState) {
        this.gameState = gameState;
    }
    
    /**
     * Check all connections for collisions with systems
     */
    public void checkAllWireCollisions() {
        for (Connection connection : gameState.getConnections()) {
            checkWireCollision(connection);
        }
    }
    
    /**
     * Check a specific connection for collisions with systems
     */
    public void checkWireCollision(Connection connection) {
        System.out.println("Checking wire collision for connection: " + connection);
        List<CollisionSegment> segments = new ArrayList<>();
        
        // Get wire line
        Line wireLine = connection.getLine();
        if (wireLine == null) {
            System.out.println("Wire line is null, skipping collision check");
            return;
        }
        
        Point2D wireStart = new Point2D(wireLine.getStartX(), wireLine.getStartY());
        Point2D wireEnd = new Point2D(wireLine.getEndX(), wireLine.getEndY());
        System.out.println("Wire from " + wireStart + " to " + wireEnd);
        
        // Check collision with each system
        for (NetworkSystem system : gameState.getSystems()) {
            // Skip the systems that this wire connects to
            if (system == connection.getSourcePort().getSystem() || 
                system == connection.getTargetPort().getSystem()) {
                continue;
            }
            
            CollisionSegment segment = checkLineSystemIntersection(wireStart, wireEnd, system);
            if (segment != null) {
                segments.add(segment);
                System.out.println("Found collision with system: " + system.getLabel());
            }
        }
        
        System.out.println("Total collision segments found: " + segments.size());
        
        // Update collision segments for this connection
        collisionSegments.put(connection, segments);
        
        // Update visual representation
        updateConnectionVisuals(connection, segments);
    }
    
    /**
     * Check if a line intersects with a system
     */
    private CollisionSegment checkLineSystemIntersection(Point2D lineStart, Point2D lineEnd, NetworkSystem system) {
        Rectangle systemRect = (Rectangle) system.getShape();
        
        // Get system bounds
        double sysX = systemRect.getX();
        double sysY = systemRect.getY();
        double sysWidth = systemRect.getWidth();
        double sysHeight = systemRect.getHeight();
        
        // Check if line intersects with system rectangle
        List<Point2D> intersectionPoints = getLineRectangleIntersection(
            lineStart, lineEnd, sysX, sysY, sysWidth, sysHeight);
        
        if (intersectionPoints.size() >= 2) {
            return new CollisionSegment(intersectionPoints.get(0), intersectionPoints.get(1), system);
        }
        
        return null;
    }
    
    /**
     * Get intersection points between a line and a rectangle
     */
    private List<Point2D> getLineRectangleIntersection(Point2D lineStart, Point2D lineEnd, 
                                                      double rectX, double rectY, double rectWidth, double rectHeight) {
        List<Point2D> intersections = new ArrayList<>();
        
        // Check intersection with each edge of the rectangle
        Point2D topLeft = new Point2D(rectX, rectY);
        Point2D topRight = new Point2D(rectX + rectWidth, rectY);
        Point2D bottomLeft = new Point2D(rectX, rectY + rectHeight);
        Point2D bottomRight = new Point2D(rectX + rectWidth, rectY + rectHeight);
        
        // Check top edge
        Point2D intersection = getLineIntersection(lineStart, lineEnd, topLeft, topRight);
        if (intersection != null) intersections.add(intersection);
        
        // Check right edge
        intersection = getLineIntersection(lineStart, lineEnd, topRight, bottomRight);
        if (intersection != null) intersections.add(intersection);
        
        // Check bottom edge
        intersection = getLineIntersection(lineStart, lineEnd, bottomRight, bottomLeft);
        if (intersection != null) intersections.add(intersection);
        
        // Check left edge
        intersection = getLineIntersection(lineStart, lineEnd, bottomLeft, topLeft);
        if (intersection != null) intersections.add(intersection);
        
        return intersections;
    }
    
    /**
     * Get intersection point between two line segments
     */
    private Point2D getLineIntersection(Point2D p1, Point2D p2, Point2D p3, Point2D p4) {
        double x1 = p1.getX(), y1 = p1.getY();
        double x2 = p2.getX(), y2 = p2.getY();
        double x3 = p3.getX(), y3 = p3.getY();
        double x4 = p4.getX(), y4 = p4.getY();
        
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-10) return null; // Lines are parallel
        
        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;
        
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            double x = x1 + t * (x2 - x1);
            double y = y1 + t * (y2 - y1);
            return new Point2D(x, y);
        }
        
        return null;
    }
    
    /**
     * Update connection visuals based on collision segments
     */
    private void updateConnectionVisuals(Connection connection, List<CollisionSegment> segments) {
        Line wireLine = connection.getLine();
        if (wireLine == null) return;
        
        // Remove existing collision visual segments
        removeCollisionVisualSegments(connection);
        
        if (segments.isEmpty()) {
            // No collisions - normal green color, remove buttons
            wireLine.getStyleClass().removeAll("wire-collision", "wire-modified");
            wireLine.setStroke(Color.rgb(50, 205, 50));
            removeBendButtons(connection);
        } else {
            // Has collisions - keep wire green but add blue segments
            wireLine.setStroke(Color.rgb(50, 205, 50));
            createCollisionVisualSegments(connection, segments);
            createBendButtons(connection, segments);
        }
    }
    
    /**
     * Create blue visual segments for collision areas
     */
    private void createCollisionVisualSegments(Connection connection, List<CollisionSegment> segments) {
        Line wireLine = connection.getLine();
        if (wireLine == null) return;
        
        List<Line> visualSegments = new ArrayList<>();
        Point2D wireStart = new Point2D(wireLine.getStartX(), wireLine.getStartY());
        Point2D wireEnd = new Point2D(wireLine.getEndX(), wireLine.getEndY());
        
        for (CollisionSegment segment : segments) {
            // Calculate the collision segment with radius limit
            Point2D collisionStart = segment.getStart();
            Point2D collisionEnd = segment.getEnd();
            Point2D collisionMid = collisionStart.midpoint(collisionEnd);
            
            // Calculate direction vector along the wire
            Point2D wireVector = wireEnd.subtract(wireStart);
            double wireLength = wireVector.magnitude();
            
            if (wireLength < 0.001) continue; // Skip degenerate wires
            
            Point2D wireDirection = wireVector.normalize();
            
            // Project collision midpoint onto wire
            Point2D toCollision = collisionMid.subtract(wireStart);
            double projectionDistance = toCollision.dotProduct(wireDirection);
            Point2D projectedMid = wireStart.add(wireDirection.multiply(projectionDistance));
            
            // Create segment around the projected midpoint with radius
            double segmentStartDist = Math.max(0, projectionDistance - COLLISION_SEGMENT_RADIUS);
            double segmentEndDist = Math.min(wireLength, projectionDistance + COLLISION_SEGMENT_RADIUS);
            
            Point2D segmentStart = wireStart.add(wireDirection.multiply(segmentStartDist));
            Point2D segmentEnd = wireStart.add(wireDirection.multiply(segmentEndDist));
            
            // Create blue visual segment
            Line blueSegment = new Line(segmentStart.getX(), segmentStart.getY(), 
                                      segmentEnd.getX(), segmentEnd.getY());
            blueSegment.setStroke(Color.BLUE);
            blueSegment.setStrokeWidth(5.0);
            blueSegment.getStyleClass().add("wire-collision");
            
            visualSegments.add(blueSegment);
            
            // Add to game scene
            if (gameState.getUIUpdateListener() != null) {
                gameState.getUIUpdateListener().getGamePane().getChildren().add(blueSegment);
                blueSegment.toBack(); // Behind other elements but above the wire
            }
        }
        
        collisionVisualSegments.put(connection, visualSegments);
    }
    
    /**
     * Remove collision visual segments for a connection
     */
    private void removeCollisionVisualSegments(Connection connection) {
        List<Line> segments = collisionVisualSegments.get(connection);
        if (segments != null) {
            for (Line segment : segments) {
                        if (gameState.getUIUpdateListener() != null) {
            // UI manipulation should be handled by view layer
            gameState.getUIUpdateListener().render();
        }
            }
            collisionVisualSegments.remove(connection);
        }
    }
    
    /**
     * Project a point onto a line segment
     */
    private Point2D projectPointOnLine(Point2D point, Point2D lineStart, Point2D lineEnd) {
        Point2D lineVector = lineEnd.subtract(lineStart);
        double lineLength = lineVector.magnitude();
        
        if (lineLength < 0.001) return lineStart; // Degenerate line
        
        Point2D unitVector = lineVector.normalize();
        Point2D pointVector = point.subtract(lineStart);
        double projection = pointVector.dotProduct(unitVector);
        
        return lineStart.add(unitVector.multiply(projection));
    }
    
    /**
     * Clamp a point to be within a line segment
     */
    private Point2D clampPointToLine(Point2D point, Point2D lineStart, Point2D lineEnd) {
        Point2D lineVector = lineEnd.subtract(lineStart);
        double lineLength = lineVector.magnitude();
        
        if (lineLength < 0.001) return lineStart;
        
        Point2D unitVector = lineVector.normalize();
        Point2D pointVector = point.subtract(lineStart);
        double projection = Math.max(0, Math.min(lineLength, pointVector.dotProduct(unitVector)));
        
        return lineStart.add(unitVector.multiply(projection));
    }
    
    /**
     * Create bend buttons for collision segments
     */
    private void createBendButtons(Connection connection, List<CollisionSegment> segments) {
        // Check if buttons already exist for this connection
        if (bendButtons.containsKey(connection)) {
            List<Button> existingButtons = bendButtons.get(connection);
            if (existingButtons != null && existingButtons.size() == segments.size()) {
                System.out.println("Bend buttons already exist for this connection, skipping creation");
                return; // Buttons already exist, no need to recreate
            }
        }
        
        System.out.println("Creating bend buttons for connection with " + segments.size() + " collision segments");
        removeBendButtons(connection); // Remove existing buttons first
        
        List<Button> buttons = new ArrayList<>();
        
        for (CollisionSegment segment : segments) {
            Button bendButton = new Button("Bend");
            bendButton.getStyleClass().add("bend-button");
            
            // Position button at the middle of collision segment
            Point2D midPoint = segment.getStart().midpoint(segment.getEnd());
            bendButton.setLayoutX(midPoint.getX() - 15);
            bendButton.setLayoutY(midPoint.getY() - 10);
            
            System.out.println("Created bend button at position: " + midPoint);
            
            // Add click handler with debug logging and immediate UI feedback
            bendButton.setOnAction(e -> {
                System.out.println("=== BEND BUTTON CLICKED! ===");
                System.out.println("Button position: " + midPoint);
                System.out.println("Connection: " + connection);
                System.out.println("Current coins before action: " + gameState.getCoins());
                
                // Disable button immediately to prevent double-clicks
                bendButton.setDisable(true);
                bendButton.setText("Creating...");
                
                // Create toggle point immediately
                javafx.application.Platform.runLater(() -> {
                    createBendPoint(connection, segment, midPoint);
                });
            });
            
            buttons.add(bendButton);
            
            // Add button to game scene
            if (gameState.getUIUpdateListener() != null) {
                gameState.getUIUpdateListener().getGamePane().getChildren().add(bendButton);
                System.out.println("Added bend button to game scene");
            } else {
                System.out.println("WARNING: GameScene is null, cannot add bend button");
            }
        }
        
        bendButtons.put(connection, buttons);
        System.out.println("Total bend buttons created: " + buttons.size());
    }
    
    /**
     * Remove bend buttons for a connection
     */
    private void removeBendButtons(Connection connection) {
        List<Button> buttons = bendButtons.get(connection);
        if (buttons != null) {
            for (Button button : buttons) {
                        if (gameState.getUIUpdateListener() != null) {
            // UI manipulation should be handled by view layer
            gameState.getUIUpdateListener().render();
        }
            }
            bendButtons.remove(connection);
        }
    }
    
    /**
     * Create a toggle point (white draggable circle) for a connection
     */
    private void createBendPoint(Connection connection, CollisionSegment segment, Point2D position) {
        System.out.println("=== CREATE TOGGLE POINT CALLED ===");
        System.out.println("Position: " + position);
        System.out.println("Current coins: " + gameState.getCoins());
        
        // Check if player has enough coins
        if (gameState.getCoins() < BEND_POINT_COST) {
            System.out.println("❌ Not enough coins to create toggle point! Need " + BEND_POINT_COST + ", have " + gameState.getCoins());
            return;
        }
        
        // Check if connection already has maximum bend points
        List<BendPoint> existingBends = bendPoints.getOrDefault(connection, new ArrayList<>());
        if (existingBends.size() >= MAX_BEND_POINTS) {
            System.out.println("❌ Maximum toggle points reached for this connection! Current: " + existingBends.size() + ", Max: " + MAX_BEND_POINTS);
            return;
        }
        
        // Deduct coins temporarily (will be refunded if cancelled)
        gameState.spendCoins(BEND_POINT_COST);
        System.out.println("💰 Coins deducted temporarily! New coin count: " + gameState.getCoins());
        
        // Create toggle point (white draggable circle)
        BendPoint togglePoint = new BendPoint(position, BEND_POINT_RADIUS);
        togglePoint.getVisualCircle().setFill(Color.WHITE); // White circle for toggle
        togglePoint.getVisualCircle().setStroke(Color.BLACK);
        togglePoint.getVisualCircle().setStrokeWidth(2.0);
        
        // Store original wire positions for potential cancellation
        Line wireLine = connection.getLine();
        Point2D originalStart = new Point2D(wireLine.getStartX(), wireLine.getStartY());
        Point2D originalEnd = new Point2D(wireLine.getEndX(), wireLine.getEndY());
        
        System.out.println("✅ Created toggle point object, adding to scene...");
        
        // Add toggle point visual to game scene immediately
        if (gameState.getUIUpdateListener() != null) {
            System.out.println("🎨 Adding toggle point visual to game scene...");
            gameState.getUIUpdateListener().getGamePane().getChildren().add(togglePoint.getVisualCircle());
            
            // Make it draggable with wire following
            makeTogglePointDraggable(togglePoint, connection, segment, originalStart, originalEnd);
            
            // Remove collision segments and buttons for this collision
            removeBendButtons(connection);
            removeCollisionVisualSegments(connection);
            
            // Force immediate HUD update
            javafx.application.Platform.runLater(() -> {
                gameState.getUIUpdateListener().render();
            });
            
            System.out.println("✅ Toggle point setup complete!");
        } else {
            System.out.println("❌ ERROR: GameScene is null, cannot add toggle point visual!");
        }
        
        System.out.println("🎯 Successfully created toggle point at " + position);
    }
    
    /**
     * Update HUD coins display
     */
    private void updateHUDCoins() {
        if (gameState.getUIUpdateListener() != null) {
            // Force immediate HUD update on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                gameState.getUIUpdateListener().render();
                // Also force a scene refresh
                gameState.getUIUpdateListener().getScene().getRoot().requestLayout();
            });
        }
    }
    
    /**
     * Make a toggle point draggable with wire following and confirmation popup
     */
    private void makeTogglePointDraggable(BendPoint togglePoint, Connection connection, CollisionSegment originalSegment, Point2D originalStart, Point2D originalEnd) {
        Circle circle = togglePoint.getVisualCircle();
        Line wireLine = connection.getLine();
        
        System.out.println("🎯 Making toggle point draggable at position: " + togglePoint.getPosition());
        
        // Store original wire coordinates
        final double origStartX = wireLine.getStartX();
        final double origStartY = wireLine.getStartY();
        final double origEndX = wireLine.getEndX();
        final double origEndY = wireLine.getEndY();
        
        // Add hover effect
        circle.setOnMouseEntered(e -> {
            circle.setFill(Color.LIGHTGRAY);
            System.out.println("🎯 Toggle point hover effect activated");
        });
        
        circle.setOnMouseExited(e -> {
            if (!circle.getStyleClass().contains("toggle-point-selected")) {
                circle.setFill(Color.WHITE);
            }
        });
        
        circle.setOnMousePressed(e -> {
            circle.getStyleClass().add("toggle-point-selected");
            circle.setFill(Color.LIGHTBLUE);
            circle.toFront();
            System.out.println("🎯 Toggle point selected for dragging");
            e.consume();
        });
        
        circle.setOnMouseDragged(e -> {
            // Convert scene coordinates to local coordinates
            if (gameState.getUIUpdateListener() != null) {
                javafx.geometry.Point2D localPoint = gameState.getUIUpdateListener().getGamePane().sceneToLocal(e.getSceneX(), e.getSceneY());
                
                // Update toggle point position
                Point2D newPosition = new Point2D(localPoint.getX(), localPoint.getY());
                togglePoint.setPosition(newPosition);
                
                System.out.println("🎯 Dragging toggle point to: " + newPosition);
                
                // Update wire to follow the toggle point (create a bent path)
                updateWireWithTogglePoint(connection, togglePoint, originalStart, originalEnd);
            }
            e.consume();
        });
        
        circle.setOnMouseReleased(e -> {
            circle.getStyleClass().remove("toggle-point-selected");
            circle.setFill(Color.WHITE);
            System.out.println("🎯 Toggle point drag completed - showing confirmation popup");
            
            // Show confirmation popup
            showConfirmationPopup(togglePoint, connection, originalStart, originalEnd, origStartX, origStartY, origEndX, origEndY);
            e.consume();
        });
    }
    
    /**
     * Update wire to create a bent path through the toggle point
     */
    private void updateWireWithTogglePoint(Connection connection, BendPoint togglePoint, Point2D originalStart, Point2D originalEnd) {
        Line wireLine = connection.getLine();
        if (wireLine != null) {
            // For now, we'll create a simple bent wire by updating the end point
            // In a full implementation, this would create a polyline
            Point2D togglePos = togglePoint.getPosition();
            
            // Calculate which end of the wire to bend towards the toggle point
            double distToStart = originalStart.distance(togglePos);
            double distToEnd = originalEnd.distance(togglePos);
            
            if (distToStart < distToEnd) {
                // Bend from start towards toggle point
                wireLine.setStartX(togglePos.getX());
                wireLine.setStartY(togglePos.getY());
            } else {
                // Bend from end towards toggle point
                wireLine.setEndX(togglePos.getX());
                wireLine.setEndY(togglePos.getY());
            }
            
            // Make wire yellow during dragging
            wireLine.setStroke(Color.YELLOW);
            wireLine.setStrokeWidth(4.0);
            
            // Recalculate wire length and update HUD during dragging
            updateWireLengthAndHUD(connection);
            
            System.out.println("🔄 Wire updated to follow toggle point");
        }
    }
    
    /**
     * Show confirmation popup with checkmark and cancel options
     */
    private void showConfirmationPopup(BendPoint togglePoint, Connection connection, Point2D originalStart, Point2D originalEnd, 
                                     double origStartX, double origStartY, double origEndX, double origEndY) {
        if (gameState.getUIUpdateListener() == null) return;
        
        // Create popup container
        javafx.scene.layout.VBox popup = new javafx.scene.layout.VBox(10);
        popup.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-background-radius: 10; -fx-padding: 20;");
        popup.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Add title
        javafx.scene.control.Label title = new javafx.scene.control.Label("Confirm Wire Bend");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        
        // Create buttons container
        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(20);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Create checkmark button (confirm)
        javafx.scene.control.Button confirmButton = new javafx.scene.control.Button("✅ Confirm");
        confirmButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        
        // Create cancel button
        javafx.scene.control.Button cancelButton = new javafx.scene.control.Button("❌ Cancel");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        
        // Position popup at toggle point
        Point2D togglePos = togglePoint.getPosition();
        popup.setLayoutX(togglePos.getX() - 100);
        popup.setLayoutY(togglePos.getY() - 80);
        
        // Add confirm button action
        confirmButton.setOnAction(event -> {
            System.out.println("✅ Bend confirmed - making permanent");
            confirmBend(togglePoint, connection);
            gameState.getUIUpdateListener().getGamePane().getChildren().remove(popup);
        });
        
        // Add cancel button action
        cancelButton.setOnAction(event -> {
            System.out.println("❌ Bend cancelled - restoring original");
            cancelBend(togglePoint, connection, origStartX, origStartY, origEndX, origEndY);
            gameState.getUIUpdateListener().getGamePane().getChildren().remove(popup);
        });
        
        buttons.getChildren().addAll(confirmButton, cancelButton);
        popup.getChildren().addAll(title, buttons);
        
        // Add popup to scene
        gameState.getUIUpdateListener().getGamePane().getChildren().add(popup);
        popup.toFront();
        
        System.out.println("🎯 Confirmation popup displayed");
    }
    
    /**
     * Confirm the bend - make it permanent and turn wire yellow
     */
    private void confirmBend(BendPoint togglePoint, Connection connection) {
        // Add to permanent bend points
        List<BendPoint> existingBends = bendPoints.getOrDefault(connection, new ArrayList<>());
        // Change toggle point to permanent yellow bend point
        togglePoint.getVisualCircle().setFill(Color.YELLOW);
        togglePoint.getVisualCircle().setStroke(Color.BLACK);
        existingBends.add(togglePoint);
        bendPoints.put(connection, existingBends);
        // Update wire to permanent yellow and redraw as polyline
        updateWirePath(connection);
        // Recalculate wire length and update HUD
        updateWireLengthAndHUD(connection);
        // Make the bend point normally draggable
        makeBendPointDraggable(togglePoint, connection);
        // Re-check for any remaining collisions
        checkWireCollision(connection);
        System.out.println("✅ Bend confirmed and made permanent");
    }
    
    /**
     * Cancel the bend - restore original wire position and refund coin
     */
    private void cancelBend(BendPoint togglePoint, Connection connection, double origStartX, double origStartY, double origEndX, double origEndY) {
        // Remove toggle point from scene
        gameState.getUIUpdateListener().getGamePane().getChildren().remove(togglePoint.getVisualCircle());
        
        // Restore original wire position
        Line wireLine = connection.getLine();
        if (wireLine != null) {
            wireLine.setStartX(origStartX);
            wireLine.setStartY(origStartY);
            wireLine.setEndX(origEndX);
            wireLine.setEndY(origEndY);
            wireLine.setStroke(Color.LIME); // Original green
            wireLine.setStrokeWidth(2.0);
            wireLine.getStyleClass().removeAll("wire-modified");
        }
        
        // Refund coin
        gameState.addCoins(BEND_POINT_COST);
        System.out.println("💰 Coin refunded! New coin count: " + gameState.getCoins());
        
        // Recalculate wire length and update HUD
        updateWireLengthAndHUD(connection);
        
        // Re-check collisions to show bend buttons again
        checkWireCollision(connection);
        
        System.out.println("❌ Bend cancelled and coin refunded");
    }
    
    /**
     * Make a permanent bend point draggable (for confirmed bends)
     */
    private void makeBendPointDraggable(BendPoint bendPoint, Connection connection) {
        Circle circle = bendPoint.getVisualCircle();
        
        System.out.println("🎯 Making permanent bend point draggable at position: " + bendPoint.getPosition());
        
        // Add hover effect
        circle.setOnMouseEntered(e -> {
            circle.getStyleClass().add("bend-point-hover");
            circle.setFill(Color.GOLD);
        });
        
        circle.setOnMouseExited(e -> {
            if (!circle.getStyleClass().contains("bend-point-selected")) {
                circle.getStyleClass().remove("bend-point-hover");
                circle.setFill(Color.YELLOW);
            }
        });
        
        circle.setOnMousePressed(e -> {
            circle.getStyleClass().add("bend-point-selected");
            circle.setFill(Color.ORANGE);
            circle.toFront();
            System.out.println("🎯 Permanent bend point selected for dragging");
            e.consume();
        });
        
        circle.setOnMouseDragged(e -> {
            // Convert scene coordinates to local coordinates
            if (gameState.getUIUpdateListener() != null) {
                javafx.geometry.Point2D localPoint = gameState.getUIUpdateListener().getGamePane().sceneToLocal(e.getSceneX(), e.getSceneY());
                
                // Update bend point position
                Point2D newPosition = new Point2D(localPoint.getX(), localPoint.getY());
                bendPoint.setPosition(newPosition);
                
                // Update wire path with immediate visual feedback
                updateWirePath(connection);
                
                // Recalculate wire length and update HUD
                updateWireLengthAndHUD(connection);
                
                // Check for new collisions
                List<CollisionSegment> segments = new ArrayList<>();
                Line wireLine = connection.getLine();
                if (wireLine != null) {
                    Point2D wireStart = new Point2D(wireLine.getStartX(), wireLine.getStartY());
                    Point2D wireEnd = new Point2D(wireLine.getEndX(), wireLine.getEndY());
                    
                    for (NetworkSystem system : gameState.getSystems()) {
                        if (system == connection.getSourcePort().getSystem() || 
                            system == connection.getTargetPort().getSystem()) {
                            continue;
                        }
                        
                        CollisionSegment segment = checkLineSystemIntersection(wireStart, wireEnd, system);
                        if (segment != null) {
                            segments.add(segment);
                        }
                    }
                    
                    collisionSegments.put(connection, segments);
                    updateConnectionVisuals(connection, segments);
                }
            }
            e.consume();
        });
        
        circle.setOnMouseReleased(e -> {
            circle.getStyleClass().remove("bend-point-selected");
            circle.setFill(Color.YELLOW);
            System.out.println("🎯 Permanent bend point drag completed");
            e.consume();
        });
    }
    
    /**
     * Update wire length and HUD when bend points are moved
     */
    private void updateWireLengthAndHUD(Connection connection) {
        // Recalculate total wire length used by all connections
        double totalWireUsed = 0;
        for (Connection conn : gameState.getConnections()) {
            totalWireUsed += conn.getLength();
        }
        
        // Update the game state with new total
        gameState.setTotalWireUsed(totalWireUsed);
        
        // Update HUD if game scene is available
        if (gameState.getUIUpdateListener() != null) {
            gameState.getUIUpdateListener().render();
        }
        
        System.out.println("🔄 Wire length updated. Total used: " + totalWireUsed + 
                          ", Remaining: " + gameState.getRemainingWireLength());
    }
    
    /**
     * Update wire path with bend points using smooth, continuous Bezier curves
     */
    private void updateWirePath(Connection connection) {
        List<BendPoint> bends = bendPoints.get(connection);
        Line wireLine = connection.getLine();
        Polyline polyline = wirePolylines.get(connection);
        
        // Remove any existing polyline from the scene
        if (polyline != null && gameState.getUIUpdateListener() != null) {
            gameState.getUIUpdateListener().getGamePane().getChildren().remove(polyline);
            wirePolylines.remove(connection);
        }
        if (wireLine != null && gameState.getUIUpdateListener() != null) {
            gameState.getUIUpdateListener().getGamePane().getChildren().remove(wireLine);
        }
        
        // Get actual port positions
        Point2D startPoint = connection.getSourcePort().getPosition();
        Point2D endPoint = connection.getTargetPort().getPosition();
        
        if (bends == null || bends.isEmpty()) {
            // No bend points, restore original wire appearance and show the straight line
            if (wireLine != null && gameState.getUIUpdateListener() != null) {
                if (!gameState.getUIUpdateListener().getGamePane().getChildren().contains(wireLine)) {
                    gameState.getUIUpdateListener().getGamePane().getChildren().add(wireLine);
                }
                wireLine.getStyleClass().removeAll("wire-modified");
                wireLine.setStroke(Color.rgb(50, 205, 50)); // Original green color
                wireLine.setStrokeWidth(3.0); // Original width
                wireLine.setStartX(startPoint.getX());
                wireLine.setStartY(startPoint.getY());
                wireLine.setEndX(endPoint.getX());
                wireLine.setEndY(endPoint.getY());
            }
            System.out.println("🔄 Wire restored to original appearance");
        } else {
            // Has bend points, create curved wire path
            createCurvedWirePath(connection, bends, startPoint, endPoint);
            System.out.println("🔄 Wire marked as modified with " + bends.size() + " bend points (curved)");
        }
    }
    
    /**
     * Create curved wire path with bend points using smooth, continuous Bezier curves
     */
    private void createCurvedWirePath(Connection connection, List<BendPoint> bends, Point2D startPoint, Point2D endPoint) {
        // Remove any existing curves first
        removeWireCurves(connection);

        // Gather all points: start, bends, end
        List<Point2D> points = new ArrayList<>();
        points.add(startPoint);
        for (BendPoint bend : bends) points.add(bend.getPosition());
        points.add(endPoint);

        List<CubicCurve> curves = new ArrayList<>();

        // Precompute tangents for each point
        List<Point2D> tangents = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Point2D prev = (i == 0) ? points.get(i + 1) : points.get(i - 1);
            Point2D next = (i == points.size() - 1) ? points.get(i - 1) : points.get(i + 1);
            Point2D tangent = next.subtract(prev).normalize();
            tangents.add(tangent);
        }

        // Compute the original wire direction and perpendicular for convexity logic
        Point2D wireDir = endPoint.subtract(startPoint).normalize();
        Point2D wirePerp = new Point2D(-wireDir.getY(), wireDir.getX());

        // For each segment, create a smooth cubic curve
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D p0 = points.get(i);
            Point2D p1 = points.get(i + 1);
            Point2D t0 = tangents.get(i);
            Point2D t1 = tangents.get(i + 1);
            double d = p0.distance(p1);
            double controlLen = Math.min(CURVE_CONTROL_OFFSET, d * 0.4);

            // Determine convex direction for this segment based on the bend (if any)
            double convexSign = 1.0;
            if (i > 0 && i < points.size() - 2) {
                // For bends, check if above or below original wire
                Point2D bend = points.get(i);
                Point2D toBend = bend.subtract(startPoint);
                double proj = toBend.dotProduct(wirePerp);
                convexSign = (proj > 0) ? 1.0 : -1.0;
            }

            // Control points for smoothness and correct convexity
            Point2D c0 = p0.add(t0.multiply(controlLen * convexSign));
            Point2D c1 = p1.subtract(t1.multiply(controlLen * convexSign));

            CubicCurve curve = new CubicCurve();
            curve.setStartX(p0.getX());
            curve.setStartY(p0.getY());
            curve.setEndX(p1.getX());
            curve.setEndY(p1.getY());
            curve.setControlX1(c0.getX());
            curve.setControlY1(c0.getY());
            curve.setControlX2(c1.getX());
            curve.setControlY2(c1.getY());
            curve.setStroke(Color.YELLOW);
            curve.setStrokeWidth(4.0);
            curve.getStyleClass().add("wire-modified");
            curve.setFill(null);
            if (gameState.getUIUpdateListener() != null) {
                gameState.getUIUpdateListener().getGamePane().getChildren().add(curve);
                curve.toBack();
            }
            curves.add(curve);
        }
        wireCurves.put(connection, curves);
        System.out.println("🔄 Created " + curves.size() + " smooth, continuous curved segments");
    }
    
    /**
     * Check if a connection has collisions with systems
     */
    public boolean hasCollisions(Connection connection) {
        List<CollisionSegment> segments = collisionSegments.get(connection);
        return segments != null && !segments.isEmpty();
    }
    
    /**
     * Get collision segments for a connection
     */
    public List<CollisionSegment> getCollisionSegments(Connection connection) {
        return collisionSegments.getOrDefault(connection, new ArrayList<>());
    }
    
    /**
     * Get bend points for a connection
     */
    public List<BendPoint> getBendPoints(Connection connection) {
        return bendPoints.getOrDefault(connection, new ArrayList<>());
    }
    
    /**
     * Clean up resources for a connection
     */
    public void cleanupConnection(Connection connection) {
        removeBendButtons(connection);
        removeCollisionVisualSegments(connection);
        removeBendPointVisuals(connection);
        collisionSegments.remove(connection);
        bendPoints.remove(connection);
        wireCurves.remove(connection);
    }
    
    /**
     * Remove bend point visuals for a connection
     */
    private void removeBendPointVisuals(Connection connection) {
        List<BendPoint> bends = bendPoints.get(connection);
        if (bends != null) {
            for (BendPoint bendPoint : bends) {
                if (gameState.getUIUpdateListener() != null) {
                    gameState.getUIUpdateListener().getGamePane().getChildren().remove(bendPoint.getVisualCircle());
                }
            }
        }
        // Remove polyline if present
        Polyline polyline = wirePolylines.get(connection);
        if (polyline != null && gameState.getUIUpdateListener() != null) {
            gameState.getUIUpdateListener().getGamePane().getChildren().remove(polyline);
            wirePolylines.remove(connection);
        }
        // Remove curves if present
        removeWireCurves(connection);
    }
    
    /**
     * Remove wire curves for a connection
     */
    private void removeWireCurves(Connection connection) {
        List<CubicCurve> curves = wireCurves.get(connection);
        if (curves != null) {
            for (CubicCurve curve : curves) {
                if (gameState.getUIUpdateListener() != null) {
                    gameState.getUIUpdateListener().getGamePane().getChildren().remove(curve);
                }
            }
            wireCurves.remove(connection);
        }
    }
    
    /**
     * Represents a collision segment between a wire and a system
     */
    public static class CollisionSegment {
        private final Point2D start;
        private final Point2D end;
        private final NetworkSystem system;
        
        public CollisionSegment(Point2D start, Point2D end, NetworkSystem system) {
            this.start = start;
            this.end = end;
            this.system = system;
        }
        
        public Point2D getStart() { return start; }
        public Point2D getEnd() { return end; }
        public NetworkSystem getSystem() { return system; }
    }
    
    /**
     * Represents a bend point on a wire
     */
    public static class BendPoint {
        private Point2D position;
        private final double radius;
        private Circle visualCircle;
        
        public BendPoint(Point2D position, double radius) {
            this.position = position;
            this.radius = radius;
            this.visualCircle = new Circle(position.getX(), position.getY(), 6); // Larger radius for better visibility
            this.visualCircle.getStyleClass().add("bend-point");
            this.visualCircle.setFill(Color.YELLOW);
            this.visualCircle.setStroke(Color.BLACK);
            this.visualCircle.setStrokeWidth(2.0);
            this.visualCircle.setCursor(javafx.scene.Cursor.HAND); // Show hand cursor on hover
        }
        
        public Point2D getPosition() { return position; }
        public void setPosition(Point2D position) { 
            this.position = position;
            visualCircle.setCenterX(position.getX());
            visualCircle.setCenterY(position.getY());
        }
        public double getRadius() { return radius; }
        public Circle getVisualCircle() { return visualCircle; }
    }
} 
