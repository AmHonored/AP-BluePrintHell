package view.components.wires;

import javafx.scene.Group;
import javafx.scene.effect.Bloom;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.geometry.Point2D;

import model.wire.Wire;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

public class WireView extends Group {
    private static final Map<Wire, WireView> REGISTRY = new ConcurrentHashMap<>();
    private final Wire wireModel;
    private final List<QuadCurve> curves = new ArrayList<>();
    private final List<Circle> bendPointIndicators = new ArrayList<>();
    private final Text wireLabel;
    private final Text outOfWireWarning;
    
    // Bending constraints
    private static final double MAX_BEND_RADIUS = 300.0;
    private static final double BEND_INDICATOR_RADIUS = 6.0;
    
    // Callback for wire removal and bend point purchase
    private Runnable onRemove;
    private java.util.function.Function<Wire, Boolean> onBendPointPurchase;
    private Runnable onWireLengthChanged;

    public void setOnRemove(Runnable onRemove) {
        this.onRemove = onRemove;
    }
    
    public void setOnBendPointPurchase(java.util.function.Function<Wire, Boolean> onBendPointPurchase) {
        this.onBendPointPurchase = onBendPointPurchase;
    }
    
    public void setOnWireLengthChanged(Runnable onWireLengthChanged) {
        this.onWireLengthChanged = onWireLengthChanged;
    }
    
    public Wire getWireModel() {
        return wireModel;
    }

    public WireView(Wire wireModel) {
        this.wireModel = wireModel;
        REGISTRY.put(wireModel, this);
        this.wireLabel = new Text();
        this.outOfWireWarning = new Text("Out of wire!");
        
        this.outOfWireWarning.getStyleClass().add("out-of-wire");
        this.outOfWireWarning.setVisible(false);

        createWireShape();
        getChildren().addAll(wireLabel, outOfWireWarning);
        setNormal();
        updatePosition();
        
        // Make this node focusable to receive keyboard events
        setFocusTraversable(true);

        // Add right-click handler for wire removal or bend point addition
        this.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                if (event.isShiftDown()) {
                    // Shift + right-click = remove wire
                    if (onRemove != null) {
                        onRemove.run();
                    }
                } else {
                    // Right-click = add bend point
                    addBendPointAtPosition(new Point2D(event.getX(), event.getY()));
                }
                event.consume();
            }
        });
        
        // Add keyboard handler for reset functionality
        this.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.R) {
                resetAllBendPoints();
                event.consume();
            }
        });
        
        // Request focus to enable keyboard events on hover
        this.setOnMouseEntered(event -> {
            requestFocus();
        });
    }

    /**
     * Mark a wire as disabled in the UI by changing its color to red.
     */
    public static void markDisabled(Wire wire) {
        WireView view = REGISTRY.get(wire);
        if (view == null) return;
        for (QuadCurve curve : view.curves) {
            curve.setStroke(Color.RED);
            curve.setStrokeWidth(4);
            curve.setEffect(null);
        }
        view.outOfWireWarning.setVisible(false);
    }

    public void createWireShape() {
        // Clear existing curves and bend indicators
        getChildren().removeAll(curves);
        getChildren().removeAll(bendPointIndicators);
        curves.clear();
        bendPointIndicators.clear();
        
        Point2D start = wireModel.getSource().getPosition();
        Point2D end = wireModel.getDest().getPosition();
        
        if (wireModel.hasBendPoints()) {
            createCurvedWire(start, end);
        } else {
            createStraightWire(start, end);
        }
        
        // Add all curves to the group at the beginning
        for (int i = 0; i < curves.size(); i++) {
            getChildren().add(i, curves.get(i));
        }
        
        // Add bend point indicators
        createBendPointIndicators();
    }
    
    private void createStraightWire(Point2D start, Point2D end) {
        // Create a single straight curve for consistency
        QuadCurve curve = new QuadCurve();
        curve.setStartX(start.getX());
        curve.setStartY(start.getY());
        curve.setEndX(end.getX());
        curve.setEndY(end.getY());
        
        // Set control point to midpoint for straight line
        double midX = (start.getX() + end.getX()) / 2;
        double midY = (start.getY() + end.getY()) / 2;
        curve.setControlX(midX);
        curve.setControlY(midY);
        
        setupCurve(curve, 0);
        curves.add(curve);
    }
    
    private void createCurvedWire(Point2D start, Point2D end) {
        List<Wire.BendPoint> bendPoints = wireModel.getBendPoints();
        
        if (bendPoints.size() == 1) {
            // Single bend point - create one curve from start to end with control point influenced by bend point
            QuadCurve curve = new QuadCurve();
            curve.setStartX(start.getX());
            curve.setStartY(start.getY());
            curve.setEndX(end.getX());
            curve.setEndY(end.getY());
            
            // Calculate control point based on bend point position relative to straight line
            Point2D bendPos = bendPoints.get(0).getPosition();
            Point2D originalBendPos = bendPoints.get(0).getOriginalPosition();
            
            // Find the offset from the original position
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            
            // Use the midpoint of the line plus the offset to create the curve direction
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            
            // Scale the offset to create a more pronounced curve
            double curveStrength = 1.5;
            curve.setControlX(midX + offsetX * curveStrength);
            curve.setControlY(midY + offsetY * curveStrength);
            
            setupCurve(curve, 0);
            curves.add(curve);
        } else {
            // Multiple bend points - create smooth curves through all points
            List<Point2D> pathPoints = new ArrayList<>();
            pathPoints.add(start);
            for (Wire.BendPoint bendPoint : bendPoints) {
                pathPoints.add(bendPoint.getPosition());
            }
            pathPoints.add(end);
            
            // Create curves between consecutive points with smooth control points
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point2D segmentStart = pathPoints.get(i);
                Point2D segmentEnd = pathPoints.get(i + 1);
                
                QuadCurve curve = new QuadCurve();
                curve.setStartX(segmentStart.getX());
                curve.setStartY(segmentStart.getY());
                curve.setEndX(segmentEnd.getX());
                curve.setEndY(segmentEnd.getY());
                
                // Calculate smooth control point for this segment
                Point2D controlPoint = calculateSmoothControlPoint(segmentStart, segmentEnd, i, pathPoints, bendPoints);
                curve.setControlX(controlPoint.getX());
                curve.setControlY(controlPoint.getY());
                
                setupCurve(curve, i);
                curves.add(curve);
            }
        }
    }
    
    private Point2D calculateSmoothControlPoint(Point2D start, Point2D end, int segmentIndex, List<Point2D> pathPoints, List<Wire.BendPoint> bendPoints) {
        // For segments involving bend points, use the bend point offset to create curves
        if (segmentIndex < bendPoints.size()) {
            // This segment goes TO a bend point
            Wire.BendPoint bendPoint = bendPoints.get(segmentIndex);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            
            // Calculate offset from original position
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            
            // Use segment midpoint plus the bend offset to create curve direction
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            
            // Apply the offset with curve strength
            double curveStrength = 1.2;
            return new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
        } else if (segmentIndex > 0 && segmentIndex <= bendPoints.size()) {
            // This segment goes FROM a bend point
            Wire.BendPoint bendPoint = bendPoints.get(segmentIndex - 1);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            
            // Calculate offset from original position
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            
            // Use segment midpoint plus the bend offset
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            
            double curveStrength = 1.2;
            return new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
        }
        
        // Default to midpoint for segments without bend points
        return new Point2D((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
    }
    
    private void setupCurve(QuadCurve curve, int segmentIndex) {
        curve.setStrokeWidth(4);
        curve.setFill(null);
        curve.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        curve.setUserData(wireModel);
        
        // Set segment-specific color if wire has bend points
        if (wireModel.hasBendPoints()) {
            // Don't use CSS class for curved wires - use programmatic colors
            curve.setStroke(getSegmentColor(segmentIndex));
        } else {
            // Use CSS class for straight wires (maintains existing behavior)
            curve.getStyleClass().add("connection");
            curve.setStroke(Color.LIME); // Default color for straight wires
        }
    }
    
    private void createBendPointIndicators() {
        List<Wire.BendPoint> bendPoints = wireModel.getBendPoints();
        
        for (int i = 0; i < bendPoints.size(); i++) {
            Wire.BendPoint bendPoint = bendPoints.get(i);
            // Create visible indicator
            Circle indicator = new Circle(BEND_INDICATOR_RADIUS);
            indicator.setCenterX(bendPoint.getPosition().getX());
            indicator.setCenterY(bendPoint.getPosition().getY());
            indicator.setFill(getBendPointColor(i));
            indicator.setStroke(Color.WHITE);
            indicator.setStrokeWidth(2);
            indicator.getStyleClass().add("bend-point");
            
            // Create larger invisible hit area for easier clicking
            Circle hitArea = new Circle(BEND_INDICATOR_RADIUS * 3);
            hitArea.setCenterX(bendPoint.getPosition().getX());
            hitArea.setCenterY(bendPoint.getPosition().getY());
            hitArea.setFill(Color.TRANSPARENT);
            hitArea.setStroke(Color.TRANSPARENT);
            
            // Make both indicator and hit area draggable
            setupBendPointDragging(indicator, hitArea, i);
            
            // Add hover effects to both
            Runnable onEnter = () -> {
                indicator.setStrokeWidth(3);
                indicator.setStroke(Color.LIGHTBLUE);
            };
            Runnable onExit = () -> {
                indicator.setStrokeWidth(2);
                indicator.setStroke(Color.WHITE);
            };
            
            indicator.setOnMouseEntered(e -> onEnter.run());
            indicator.setOnMouseExited(e -> onExit.run());
            hitArea.setOnMouseEntered(e -> onEnter.run());
            hitArea.setOnMouseExited(e -> onExit.run());
            
            bendPointIndicators.add(indicator);
            getChildren().addAll(indicator, hitArea);
        }
    }
    
    private Color getBendPointColor(int index) {
        // Match the bend point colors with segment colors for consistency
        Color[] colors = {Color.GOLD, Color.MEDIUMPURPLE, Color.CORAL, Color.LIGHTSEAGREEN};
        return colors[index % colors.length];
    }
    
    private Color getSegmentColor(int segmentIndex) {
        // Better colors for wire segments that match the requirement
        Color[] segmentColors = {
            Color.GOLD,        // First segment - golden yellow
            Color.MEDIUMPURPLE, // Second segment - purple  
            Color.CORAL,       // Third segment - coral
            Color.LIGHTSEAGREEN // Fourth segment - sea green
        };
        return segmentColors[segmentIndex % segmentColors.length];
    }
    
    private void setupBendPointDragging(Circle indicator, Circle hitArea, int bendPointIndex) {
        final boolean[] isDragging = {false};
        
        // Setup mouse events for both indicator and hit area
        Runnable onPressed = () -> {
            isDragging[0] = true;
            // Visual feedback
            indicator.setStroke(Color.YELLOW);
            indicator.setStrokeWidth(3);
        };
        
        indicator.setOnMousePressed(event -> {
            event.consume();
            onPressed.run();
        });
        
        hitArea.setOnMousePressed(event -> {
            event.consume();
            onPressed.run();
        });
        
        // Shared drag logic
        Runnable dragUpdate = () -> {
            // This will be called by either indicator or hitArea
        };
        
        javafx.event.EventHandler<javafx.scene.input.MouseEvent> onDragged = event -> {
            event.consume();
            if (isDragging[0]) {
                // Convert scene coordinates to local coordinates of this WireView's parent
                Point2D scenePos = new Point2D(event.getSceneX(), event.getSceneY());
                Point2D localPos = getParent().sceneToLocal(scenePos);
                
                Wire.BendPoint bendPoint = wireModel.getBendPoints().get(bendPointIndex);
                
                // Apply flexible constraint - limit distance from wire line
                Point2D wireStart = wireModel.getSource().getPosition();
                Point2D wireEnd = wireModel.getDest().getPosition();
                
                // Calculate the maximum allowed distance from the wire line
                double wireLength = wireStart.distance(wireEnd);
                double maxOffset = Math.min(300.0, wireLength * 0.6); // More generous constraint
                
                // Find closest point on wire line to mouse position
                double t = Math.max(0.0, Math.min(1.0, 
                    ((localPos.getX() - wireStart.getX()) * (wireEnd.getX() - wireStart.getX()) + 
                     (localPos.getY() - wireStart.getY()) * (wireEnd.getY() - wireStart.getY())) / 
                    (wireLength * wireLength)));
                
                Point2D closestPointOnWire = new Point2D(
                    wireStart.getX() + t * (wireEnd.getX() - wireStart.getX()),
                    wireStart.getY() + t * (wireEnd.getY() - wireStart.getY())
                );
                
                double distanceFromWire = closestPointOnWire.distance(localPos);
                
                Point2D finalPosition;
                if (distanceFromWire <= maxOffset) {
                    finalPosition = localPos;
                } else {
                    // Clamp to maximum distance
                    double ratio = maxOffset / distanceFromWire;
                    finalPosition = new Point2D(
                        closestPointOnWire.getX() + ratio * (localPos.getX() - closestPointOnWire.getX()),
                        closestPointOnWire.getY() + ratio * (localPos.getY() - closestPointOnWire.getY())
                    );
                }
                
                // Update bend point position
                bendPoint.forceSetPosition(finalPosition);
                indicator.setCenterX(finalPosition.getX());
                indicator.setCenterY(finalPosition.getY());
                hitArea.setCenterX(finalPosition.getX());
                hitArea.setCenterY(finalPosition.getY());
                updateCurvesForBendPoint();
            }
        };
        
        javafx.event.EventHandler<javafx.scene.input.MouseEvent> onReleased = event -> {
            event.consume();
            isDragging[0] = false;
            indicator.setStroke(Color.WHITE);
            indicator.setStrokeWidth(2);
        };
        
        // Apply events to both elements
        indicator.setOnMouseDragged(onDragged);
        hitArea.setOnMouseDragged(onDragged);
        indicator.setOnMouseReleased(onReleased);
        hitArea.setOnMouseReleased(onReleased);
    }
    
    private void updateCurvesForBendPoint() {
        // Recreate the entire wire shape to reflect bend point changes
        createWireShape();
        updatePosition();
        
        // Notify that wire length has changed
        if (onWireLengthChanged != null) {
            onWireLengthChanged.run();
        }
    }
    
    private void resetAllBendPoints() {
        List<Wire.BendPoint> bendPoints = wireModel.getBendPoints();
        for (Wire.BendPoint bendPoint : bendPoints) {
            bendPoint.resetToOriginalPosition();
        }
        createWireShape();
        updatePosition();
        System.out.println("All bend points reset to original positions");
        
        // Notify that wire length has changed
        if (onWireLengthChanged != null) {
            onWireLengthChanged.run();
        }
    }
    
    private void addBendPointAtPosition(Point2D localPosition) {
        if (!wireModel.canAddBendPoint()) {
            System.out.println("Cannot add bend point - maximum of 3 bend points per wire");
            return;
        }
        
        // Try to purchase bend point
        if (onBendPointPurchase != null) {
            boolean purchased = onBendPointPurchase.apply(wireModel);
            if (!purchased) {
                System.out.println("Not enough coins to add bend point (Cost: 1 coin)");
                return;
            }
        }
        
        // Convert local position to scene coordinates for bend point
        Point2D scenePosition = localToScene(localPosition.getX(), localPosition.getY());
        Point2D wirePosition = getParent().sceneToLocal(scenePosition);
        
        if (wireModel.addBendPoint(wirePosition, MAX_BEND_RADIUS)) {
            System.out.println("Bend point added successfully!");
            createWireShape();
            updatePosition();
            
            // Notify that wire length has changed
            if (onWireLengthChanged != null) {
                onWireLengthChanged.run();
            }
        }
    }

    public void updatePosition() {
        Point2D start = wireModel.getSource().getPosition();
        Point2D end = wireModel.getDest().getPosition();
        
        // Position labels at midpoint of overall wire
        wireLabel.setX((start.getX() + end.getX()) / 2);
        wireLabel.setY((start.getY() + end.getY()) / 2 - 10);
        
        // Position warning below label
        outOfWireWarning.setX(wireLabel.getX());
        outOfWireWarning.setY(wireLabel.getY() + 18);
    }

    // Visual state methods
    public void setNormal() {
        for (int i = 0; i < curves.size(); i++) {
            QuadCurve curve = curves.get(i);
            if (wireModel.hasBendPoints()) {
                curve.setStroke(getSegmentColor(i));
            } else {
                curve.setStroke(Color.LIME);
            }
            curve.setStrokeWidth(4);
            curve.setEffect(null);
        }
    }

    public void setBusy() {
        for (QuadCurve curve : curves) {
            curve.setStroke(Color.ROYALBLUE);
            curve.setStrokeWidth(5);
            Bloom bloom = new Bloom();
            bloom.setThreshold(0.3);
            curve.setEffect(bloom);
        }
    }

    public void setDragging() {
        for (QuadCurve curve : curves) {
            curve.setStroke(Color.RED);
            curve.setStrokeWidth(4);
            curve.setEffect(null);
        }
    }

    public void setValidTarget() {
        for (int i = 0; i < curves.size(); i++) {
            QuadCurve curve = curves.get(i);
            if (wireModel.hasBendPoints()) {
                curve.setStroke(getSegmentColor(i));
            } else {
                curve.setStroke(Color.LIME);
            }
            curve.setStrokeWidth(5);
            Bloom bloom = new Bloom();
            bloom.setThreshold(0.2);
            curve.setEffect(bloom);
        }
    }

    public void setInvalid() {
        for (QuadCurve curve : curves) {
            curve.setStroke(Color.RED);
            curve.setStrokeWidth(4);
            curve.setEffect(null);
        }
    }

    public void setOutOfWire(boolean out) {
        outOfWireWarning.setVisible(out);
        if (out) setInvalid();
        else setNormal();
    }

    /**
     * Refresh the geometry for a specific wire based on its current model endpoints/bend points.
     * Safe no-op if there is no view registered for the given wire.
     */
    public static void refresh(Wire wire) {
        WireView view = REGISTRY.get(wire);
        if (view == null) return;
        view.createWireShape();
        view.updatePosition();
    }

    public void updateWireLabel(String text, boolean animate) {
        wireLabel.setText(text);
        wireLabel.getStyleClass().add("wire-label");
        if (animate) {
            wireLabel.getStyleClass().add("updating");
        } else {
            wireLabel.getStyleClass().remove("updating");
        }
    }

    // Getter methods
    public List<QuadCurve> getCurves() {
        return new ArrayList<>(curves);
    }
    
    public Shape getWireShape() {
        return curves.isEmpty() ? null : curves.get(0);
    }

    /**
     * Get the curve that is closest to the given local point; used for mark placement hit-testing.
     */
    public javafx.scene.shape.QuadCurve getClosestCurveToLocalPoint(Point2D local) {
        if (curves.isEmpty()) return null;
        javafx.scene.shape.QuadCurve best = curves.get(0);
        double bestDist = distanceToCurve(best, local);
        for (javafx.scene.shape.QuadCurve c : curves) {
            double d = distanceToCurve(c, local);
            if (d < bestDist) { bestDist = d; best = c; }
        }
        return best;
    }

    private double distanceToCurve(javafx.scene.shape.QuadCurve c, Point2D p) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i <= 100; i++) {
            double t = i / 100.0;
            double x = (1-t)*(1-t)*c.getStartX() + 2*(1-t)*t*c.getControlX() + t*t*c.getEndX();
            double y = (1-t)*(1-t)*c.getStartY() + 2*(1-t)*t*c.getControlY() + t*t*c.getEndY();
            double d = p.distance(x, y);
            if (d < min) min = d;
        }
        return min;
    }

    public Text getWireLabel() {
        return wireLabel;
    }

    public Text getOutOfWireWarning() {
        return outOfWireWarning;
    }
}