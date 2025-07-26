package view.components.wires;

import javafx.scene.Group;
import javafx.scene.effect.Bloom;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.geometry.Point2D;
import model.wire.Wire;

public class WireView extends Group {
    private final Wire wireModel;
    private Shape wireShape;
    private final Text wireLabel;
    private final Text outOfWireWarning;
    private boolean useBezier = false;
    // Callback for wire removal (set by controller or parent)
    private Runnable onRemove;

    public void setOnRemove(Runnable onRemove) {
        this.onRemove = onRemove;
    }

    public WireView(Wire wireModel) {
        this.wireModel = wireModel;
        this.wireLabel = new Text();
        this.outOfWireWarning = new Text("Out of wire!");
        this.outOfWireWarning.getStyleClass().add("out-of-wire");
        this.outOfWireWarning.setVisible(false);

        createWireShape();
        // wireShape is now added by createWireShape(), just add label and warning
        getChildren().addAll(wireLabel, outOfWireWarning);
        setNormal();
        updatePosition();

        // Add right-click handler for removal
        this.setOnMouseClicked(event -> {
            if (event.isSecondaryButtonDown()) {
                if (onRemove != null) {
                    onRemove.run();
                }
                event.consume();
            }
        });
    }

    public void createWireShape() {
        Point2D start = wireModel.getSource().getPosition();
        Point2D end = wireModel.getDest().getPosition();
        
        // Remove the old wire shape if it exists
        if (wireShape != null) {
            getChildren().remove(wireShape);
        }
        
        if (useBezier) {
            wireShape = createCurve(start, end);
        } else {
            wireShape = createLine(start, end);
        }
        wireShape.getStyleClass().add("connection");
        wireShape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        // Store the wire model in UserData for access by controllers
        wireShape.setUserData(wireModel);
        
        // Add the new wire shape at the beginning so it's behind other elements
        getChildren().add(0, wireShape);
    }

    private Line createLine(Point2D start, Point2D end) {
        Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
        line.setStrokeWidth(3);
        return line;
    }

    private CubicCurve createCurve(Point2D start, Point2D end) {
        CubicCurve curve = new CubicCurve();
        curve.setStartX(start.getX());
        curve.setStartY(start.getY());
        curve.setEndX(end.getX());
        curve.setEndY(end.getY());
        double controlX1 = start.getX() + (end.getX() - start.getX()) / 3;
        double controlY1 = start.getY();
        double controlX2 = end.getX() - (end.getX() - start.getX()) / 3;
        double controlY2 = end.getY();
        curve.setControlX1(controlX1);
        curve.setControlY1(controlY1);
        curve.setControlX2(controlX2);
        curve.setControlY2(controlY2);
        curve.setStrokeWidth(3);
        curve.setFill(null);
        return curve;
    }

    public void updatePosition() {
        Point2D start = wireModel.getSource().getPosition();
        Point2D end = wireModel.getDest().getPosition();
        if (wireShape instanceof Line) {
            Line line = (Line) wireShape;
            line.setStartX(start.getX());
            line.setStartY(start.getY());
            line.setEndX(end.getX());
            line.setEndY(end.getY());
        } else if (wireShape instanceof CubicCurve) {
            CubicCurve curve = (CubicCurve) wireShape;
            curve.setStartX(start.getX());
            curve.setStartY(start.getY());
            curve.setEndX(end.getX());
            curve.setEndY(end.getY());
            double controlX1 = start.getX() + (end.getX() - start.getX()) / 3;
            double controlY1 = start.getY();
            double controlX2 = end.getX() - (end.getX() - start.getX()) / 3;
            double controlY2 = end.getY();
            curve.setControlX1(controlX1);
            curve.setControlY1(controlY1);
            curve.setControlX2(controlX2);
            curve.setControlY2(controlY2);
        }
        // Position label at midpoint
        wireLabel.setX((start.getX() + end.getX()) / 2);
        wireLabel.setY((start.getY() + end.getY()) / 2 - 10);
        // Position warning below label
        outOfWireWarning.setX(wireLabel.getX());
        outOfWireWarning.setY(wireLabel.getY() + 18);
        
        // Force visual update
        wireShape.autosize();
    }

    // Visual state methods
    public void setNormal() {
        wireShape.setStroke(Color.LIME);
        wireShape.setStrokeWidth(3);
        wireShape.setEffect(null);
    }

    public void setBusy() {
        wireShape.setStroke(Color.ROYALBLUE);
        wireShape.setStrokeWidth(4);
        Bloom bloom = new Bloom();
        bloom.setThreshold(0.3);
        wireShape.setEffect(bloom);
    }

    public void setDragging() {
        wireShape.setStroke(Color.RED);
        wireShape.setStrokeWidth(3);
        wireShape.setEffect(null);
    }

    public void setValidTarget() {
        wireShape.setStroke(Color.LIME);
        wireShape.setStrokeWidth(4);
        Bloom bloom = new Bloom();
        bloom.setThreshold(0.2);
        wireShape.setEffect(bloom);
    }

    public void setInvalid() {
        wireShape.setStroke(Color.RED);
        wireShape.setStrokeWidth(3);
        wireShape.setEffect(null);
    }

    public void setOutOfWire(boolean out) {
        outOfWireWarning.setVisible(out);
        if (out) setInvalid();
        else setNormal();
    }

    public void updateWireLabel(String text, boolean animate) {
        wireLabel.setText(text);
        wireLabel.getStyleClass().add("wire-label");
        if (animate) {
            wireLabel.getStyleClass().add("updating");
            // Animation logic (e.g., Timeline) can be added in controller
        } else {
            wireLabel.getStyleClass().remove("updating");
        }
    }

    // For future: enable curve mode
    public void setUseBezier(boolean useBezier) {
        if (this.useBezier != useBezier) {
            this.useBezier = useBezier;
            getChildren().remove(wireShape);
            createWireShape();
            getChildren().add(0, wireShape);
            updatePosition();
        }
    }

    public Shape getWireShape() {
        return wireShape;
    }

    public Text getWireLabel() {
        return wireLabel;
    }

    public Text getOutOfWireWarning() {
        return outOfWireWarning;
    }
} 