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
import java.util.Set;
import java.util.HashSet;
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
    
    private static final double MAX_BEND_RADIUS = 300.0;
    private static final double BEND_INDICATOR_RADIUS = 6.0;
    
    private Runnable onRemove;
    private java.util.function.Function<Wire, Boolean> onBendPointPurchase;
    private java.util.function.Function<Wire, Boolean> onBendPointRefund;
    private Runnable onWireLengthChanged;

    public void setOnRemove(Runnable onRemove) {
        this.onRemove = onRemove;
    }
    
    public void setOnBendPointPurchase(java.util.function.Function<Wire, Boolean> onBendPointPurchase) {
        this.onBendPointPurchase = onBendPointPurchase;
    }
    
    public void setOnBendPointRefund(java.util.function.Function<Wire, Boolean> onBendPointRefund) {
        this.onBendPointRefund = onBendPointRefund;
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
        
        setFocusTraversable(true);

        this.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                if (event.isShiftDown()) {
                    if (onRemove != null) {
                        onRemove.run();
                    }
                } else {
                    addBendPointAtPosition(new Point2D(event.getX(), event.getY()));
                }
                event.consume();
            }
        });
        
        this.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.R) {
                resetAllBendPoints();
                event.consume();
            }
        });
        
        this.setOnMouseEntered(event -> {
            requestFocus();
        });
    }

    public static Set<Wire> getRegisteredWires() {
        return new HashSet<>(REGISTRY.keySet());
    }

    public static void markDisabled(Wire wire) {
        WireView view = REGISTRY.get(wire);
        if (view == null) return;
        try { wire.setActive(false); } catch (Throwable ignored) {}
        for (QuadCurve curve : view.curves) {
            curve.setStroke(Color.RED);
            curve.setStrokeWidth(4);
            curve.setEffect(null);
        }
        view.outOfWireWarning.setVisible(false);
    }

    public void createWireShape() {
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
        
        for (int i = 0; i < curves.size(); i++) {
            getChildren().add(i, curves.get(i));
        }
        
        createBendPointIndicators();
    }
    
    private void createStraightWire(Point2D start, Point2D end) {
        QuadCurve curve = new QuadCurve();
        curve.setStartX(start.getX());
        curve.setStartY(start.getY());
        curve.setEndX(end.getX());
        curve.setEndY(end.getY());
        
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
            QuadCurve curve = new QuadCurve();
            curve.setStartX(start.getX());
            curve.setStartY(start.getY());
            curve.setEndX(end.getX());
            curve.setEndY(end.getY());
            
            Point2D bendPos = bendPoints.get(0).getPosition();
            Point2D originalBendPos = bendPoints.get(0).getOriginalPosition();
            
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            
            double curveStrength = 1.5;
            curve.setControlX(midX + offsetX * curveStrength);
            curve.setControlY(midY + offsetY * curveStrength);
            
            setupCurve(curve, 0);
            curves.add(curve);
        } else {
            List<Point2D> pathPoints = new ArrayList<>();
            pathPoints.add(start);
            for (Wire.BendPoint bendPoint : bendPoints) {
                pathPoints.add(bendPoint.getPosition());
            }
            pathPoints.add(end);
            
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point2D segmentStart = pathPoints.get(i);
                Point2D segmentEnd = pathPoints.get(i + 1);
                
                QuadCurve curve = new QuadCurve();
                curve.setStartX(segmentStart.getX());
                curve.setStartY(segmentStart.getY());
                curve.setEndX(segmentEnd.getX());
                curve.setEndY(segmentEnd.getY());
                
                Point2D controlPoint = calculateSmoothControlPoint(segmentStart, segmentEnd, i, pathPoints, bendPoints);
                curve.setControlX(controlPoint.getX());
                curve.setControlY(controlPoint.getY());
                
                setupCurve(curve, i);
                curves.add(curve);
            }
        }
    }
    
    private Point2D calculateSmoothControlPoint(Point2D start, Point2D end, int segmentIndex, List<Point2D> pathPoints, List<Wire.BendPoint> bendPoints) {
        if (segmentIndex < bendPoints.size()) {
            Wire.BendPoint bendPoint = bendPoints.get(segmentIndex);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            
            double curveStrength = 1.2;
            return new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
        } else if (segmentIndex > 0 && segmentIndex <= bendPoints.size()) {
            Wire.BendPoint bendPoint = bendPoints.get(segmentIndex - 1);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            
            double curveStrength = 1.2;
            return new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
        }
        
        return new Point2D((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
    }
    
    private void setupCurve(QuadCurve curve, int segmentIndex) {
        curve.setStrokeWidth(4);
        curve.setFill(null);
        curve.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        curve.setUserData(wireModel);
        
        if (wireModel.hasBendPoints()) {
            curve.setStroke(getSegmentColor(segmentIndex));
        } else {
            curve.setStroke(Color.LIME);
        }
    }
    
    private void createBendPointIndicators() {
        List<Wire.BendPoint> bendPoints = wireModel.getBendPoints();
        
        for (int i = 0; i < bendPoints.size(); i++) {
            final int index = i;
            Wire.BendPoint bendPoint = bendPoints.get(i);
            Circle indicator = new Circle(BEND_INDICATOR_RADIUS);
            indicator.setCenterX(bendPoint.getPosition().getX());
            indicator.setCenterY(bendPoint.getPosition().getY());
            indicator.setFill(getBendPointColor(i));
            indicator.setStroke(Color.WHITE);
            indicator.setStrokeWidth(2);
            indicator.getStyleClass().add("bend-point");
            
            Circle hitArea = new Circle(BEND_INDICATOR_RADIUS * 3);
            hitArea.setCenterX(bendPoint.getPosition().getX());
            hitArea.setCenterY(bendPoint.getPosition().getY());
            hitArea.setFill(Color.TRANSPARENT);
            hitArea.setStroke(Color.TRANSPARENT);
            
            setupBendPointDragging(indicator, hitArea, index);
            
            javafx.event.EventHandler<javafx.scene.input.KeyEvent> onKey = event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.R) {
                    List<Wire.BendPoint> points = wireModel.getBendPoints();
                    if (index < 0 || index >= points.size()) {
                        event.consume();
                        return;
                    }
                    if (onBendPointRefund != null) {
                        try { onBendPointRefund.apply(wireModel); } catch (Throwable ignored) {}
                    }
                    wireModel.removeBendPoint(index);
                    createWireShape();
                    updatePosition();
                    if (onWireLengthChanged != null) onWireLengthChanged.run();
                    event.consume();
                }
            };
            indicator.setOnKeyPressed(onKey);
            hitArea.setOnKeyPressed(onKey);
            indicator.setFocusTraversable(true);
            hitArea.setFocusTraversable(true);
            
            Runnable onEnter = () -> {
                indicator.setStrokeWidth(3);
                indicator.setStroke(Color.LIGHTBLUE);
                try { indicator.requestFocus(); } catch (Throwable ignored) {}
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
        Color[] colors = {Color.GOLD, Color.MEDIUMPURPLE, Color.CORAL, Color.LIGHTSEAGREEN};
        return colors[index % colors.length];
    }
    
    private Color getSegmentColor(int segmentIndex) {
        Color[] segmentColors = {
            Color.GOLD,
            Color.MEDIUMPURPLE,
            Color.CORAL,
            Color.LIGHTSEAGREEN
        };
        return segmentColors[segmentIndex % segmentColors.length];
    }
    
    private void setupBendPointDragging(Circle indicator, Circle hitArea, int bendPointIndex) {
        final boolean[] isDragging = {false};
        
        Runnable onPressed = () -> {
            isDragging[0] = true;
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
        
        javafx.event.EventHandler<javafx.scene.input.MouseEvent> onDragged = event -> {
            event.consume();
            if (isDragging[0]) {
                List<Wire.BendPoint> points = wireModel.getBendPoints();
                if (bendPointIndex < 0 || bendPointIndex >= points.size()) {
                    isDragging[0] = false;
                    return;
                }
                Point2D scenePos = new Point2D(event.getSceneX(), event.getSceneY());
                Point2D localPos = getParent().sceneToLocal(scenePos);
                
                Wire.BendPoint bendPoint = points.get(bendPointIndex);
                
                Point2D wireStart = wireModel.getSource().getPosition();
                Point2D wireEnd = wireModel.getDest().getPosition();
                
                double wireLength = wireStart.distance(wireEnd);
                double maxOffset = Math.min(300.0, wireLength * 0.6);
                
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
                    double ratio = maxOffset / distanceFromWire;
                    finalPosition = new Point2D(
                        closestPointOnWire.getX() + ratio * (localPos.getX() - closestPointOnWire.getX()),
                        closestPointOnWire.getY() + ratio * (localPos.getY() - closestPointOnWire.getY())
                    );
                }
                
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
        
        indicator.setOnMouseDragged(onDragged);
        hitArea.setOnMouseDragged(onDragged);
        indicator.setOnMouseReleased(onReleased);
        hitArea.setOnMouseReleased(onReleased);
    }
    
    private void updateCurvesForBendPoint() {
        createWireShape();
        updatePosition();
        
        if (onWireLengthChanged != null) onWireLengthChanged.run();
    }
    
    private void resetAllBendPoints() {
        List<Wire.BendPoint> bendPoints = wireModel.getBendPoints();
        for (Wire.BendPoint bendPoint : bendPoints) {
            bendPoint.resetToOriginalPosition();
        }
        createWireShape();
        updatePosition();
        if (onWireLengthChanged != null) onWireLengthChanged.run();
    }
    
    private void addBendPointAtPosition(Point2D localPosition) {
        if (!wireModel.canAddBendPoint()) return;
        
        if (onBendPointPurchase != null) {
            boolean purchased = onBendPointPurchase.apply(wireModel);
            if (!purchased) return;
        }
        
        Point2D scenePosition = localToScene(localPosition.getX(), localPosition.getY());
        Point2D wirePosition = getParent().sceneToLocal(scenePosition);
        
        if (wireModel.addBendPoint(wirePosition, MAX_BEND_RADIUS)) {
            createWireShape();
            updatePosition();
            
            if (onWireLengthChanged != null) onWireLengthChanged.run();
        }
    }

    public void updatePosition() {
        Point2D start = wireModel.getSource().getPosition();
        Point2D end = wireModel.getDest().getPosition();
        
        wireLabel.setX((start.getX() + end.getX()) / 2);
        wireLabel.setY((start.getY() + end.getY()) / 2 - 10);
        
        outOfWireWarning.setX(wireLabel.getX());
        outOfWireWarning.setY(wireLabel.getY() + 18);
    }

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

    public List<QuadCurve> getCurves() {
        return new ArrayList<>(curves);
    }
    
    public Shape getWireShape() {
        return curves.isEmpty() ? null : curves.get(0);
    }

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