package com.networkgame.model;

import javafx.geometry.Point2D;
import javafx.scene.effect.Bloom;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import com.networkgame.model.entity.Connection;

/**
 * Handles visual representation of network connections
 */
public class ConnectionVisualizer {
    private final Connection connection;
    private Shape connectionShape;
    private boolean useBezierCurves = false;

    public ConnectionVisualizer(Connection connection, boolean useBezierCurves) {
        this.connection = connection;
        this.useBezierCurves = useBezierCurves;
        createConnectionShape();
    }

    public void createConnectionShape() {
        Point2D start = connection.getSourcePosition();
        Point2D end = connection.getTargetPosition();
        
        if (useBezierCurves) {
            connectionShape = createCurve(start, end);
        } else {
            connectionShape = createLine(start, end);
        }
        
        updateConnectionColor();
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
    
    private Line createLine(Point2D start, Point2D end) {
        Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
        line.setStrokeWidth(3);
        line.setStroke(Color.BLUE);
        return line;
    }

    public void updateConnectionColor() {
        if (connection.isOutOfWire()) {
            connectionShape.setStroke(Color.RED);
        } else {
            Color baseColor = Color.rgb(50, 205, 50);
            double ratio = Math.max(0, Math.min(1, connection.getRemainingWireLength() / (connection.getMaxWireLength() * 0.3)));
            double opacity = 0.7 + (ratio * 0.3);
            connectionShape.setStroke(new Color(baseColor.getRed(), baseColor.getGreen(), 
                                   baseColor.getBlue(), opacity));
        }
    }
    
    public void applyGlowEffect() {
        if (connectionShape == null) return;
        
        connectionShape.setStrokeWidth(4.0);
        connectionShape.setStroke(Color.ROYALBLUE);
        
        Bloom bloom = new Bloom();
        bloom.setThreshold(0.3);
        connectionShape.setEffect(bloom);
    }
    
    public void clearGlowEffect() {
        if (connectionShape == null) return;
        
        connectionShape.setStrokeWidth(2.0);
        connectionShape.setStroke(connection.isOutOfWire() ? Color.RED : Color.DODGERBLUE);
        connectionShape.setEffect(null);
    }
    
    public void clearAllStyling() {
        if (connectionShape == null) return;
        
        connectionShape.setStrokeWidth(2.0);
        connectionShape.setStroke(Color.BLACK);
        connectionShape.setEffect(null);
    }
    
    public Shape getConnectionShape() {
        return connectionShape;
    }
    
    public Line getLine() {
        return connectionShape instanceof Line ? (Line)connectionShape : null;
    }

    public void updatePosition() {
        Point2D start = connection.getSourcePosition();
        Point2D end = connection.getTargetPosition();
        
        if (connectionShape instanceof Line) {
            Line line = (Line) connectionShape;
            line.setStartX(start.getX());
            line.setStartY(start.getY());
            line.setEndX(end.getX());
            line.setEndY(end.getY());
        } else if (connectionShape instanceof CubicCurve) {
            CubicCurve curve = (CubicCurve) connectionShape;
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
        
        updateConnectionColor();
    }
} 