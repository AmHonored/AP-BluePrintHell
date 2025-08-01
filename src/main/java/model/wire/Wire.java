package model.wire;

import model.entity.ports.Port;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Wire {
    private final String id;
    private final Port source;
    private final Port dest;
    private boolean active = true;
    private final List<BendPoint> bendPoints = new ArrayList<>();
    
    public static class BendPoint {
        private Point2D position;
        private final Point2D originalPosition;
        private final double maxRadius;
        
        public BendPoint(Point2D originalPosition, double maxRadius) {
            this.originalPosition = originalPosition;
            this.position = originalPosition;
            this.maxRadius = maxRadius;
        }
        
        public Point2D getPosition() { return position; }
        public Point2D getOriginalPosition() { return originalPosition; }
        public double getMaxRadius() { return maxRadius; }
        
        public boolean setPosition(Point2D newPosition) {
            double distance = originalPosition.distance(newPosition);
            if (distance <= maxRadius) {
                this.position = newPosition;
                return true;
            }
            return false;
        }
        
        public void forceSetPosition(Point2D newPosition) {
            this.position = newPosition;
        }
        
        public void resetToOriginalPosition() {
            this.position = originalPosition;
        }
        
        public boolean isAtOriginalPosition() {
            return position.distance(originalPosition) < 1.0;
        }
    }

    public Wire(String id, Port source, Port dest) {
        this.id = id;
        this.source = source;
        this.dest = dest;
    }

    public String getId() {
        return id;
    }

    public Port getSource() {
        return source;
    }

    public Port getDest() {
        return dest;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAvailable() {
        return active;
    }

    public void setAvailable(boolean available) {
        this.active = available;
    }

    public double getLength() {
        if (source == null || dest == null) return 0;
        
        if (bendPoints.isEmpty()) {
            return source.getPosition().distance(dest.getPosition());
        }
        
        // Calculate accurate curved length
        Point2D start = source.getPosition();
        Point2D end = dest.getPosition();
        
        if (bendPoints.size() == 1) {
            // Single bend point - calculate QuadCurve length
            BendPoint bendPoint = bendPoints.get(0);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            double curveStrength = 1.5;
            Point2D controlPoint = new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
            
            return approximateQuadCurveLength(start, controlPoint, end);
        } else {
            // Multiple bend points - sum all curve segment lengths
            double totalLength = 0;
            
            List<Point2D> pathPoints = new ArrayList<>();
            pathPoints.add(start);
            for (BendPoint bendPoint : bendPoints) {
                pathPoints.add(bendPoint.getPosition());
            }
            pathPoints.add(end);
            
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point2D segmentStart = pathPoints.get(i);
                Point2D segmentEnd = pathPoints.get(i + 1);
                Point2D controlPoint = calculateSegmentControlPoint(segmentStart, segmentEnd, i);
                
                totalLength += approximateQuadCurveLength(segmentStart, controlPoint, segmentEnd);
            }
            
            return totalLength;
        }
    }

    public boolean isValid() {
        return source != null && dest != null;
    }

    public Point2D getPositionAtProgress(double progress) {
        if (source == null || dest == null) {
            return new Point2D(0, 0);
        }
        
        // Clamp progress to valid range for precise movement
        progress = Math.max(0.0, Math.min(1.0, progress));
        
        if (bendPoints.isEmpty()) {
            // Simple linear interpolation for straight wires
            Point2D sourcePos = source.getPosition();
            Point2D destPos = dest.getPosition();
            
            double deltaX = destPos.getX() - sourcePos.getX();
            double deltaY = destPos.getY() - sourcePos.getY();
            
            double x = sourcePos.getX() + progress * deltaX;
            double y = sourcePos.getY() + progress * deltaY;
            
            return new Point2D(x, y);
        }
        
        // Complex curved path calculation
        return getPositionOnCurvedPath(progress);
    }
    
    private Point2D getPositionOnCurvedPath(double progress) {
        Point2D start = source.getPosition();
        Point2D end = dest.getPosition();
        
        if (bendPoints.size() == 1) {
            // Single bend point - calculate position on QuadCurve
            BendPoint bendPoint = bendPoints.get(0);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            
            // Calculate control point the same way as in WireView
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            double curveStrength = 1.5;
            Point2D controlPoint = new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
            
            // Calculate position on quadratic Bezier curve
            return getQuadraticBezierPoint(start, controlPoint, end, progress);
        } else {
            // Multiple bend points - create curve segments and find position
            return getPositionOnMultipleCurvedSegments(progress);
        }
    }
    
    private Point2D getPositionOnMultipleCurvedSegments(double progress) {
        Point2D start = source.getPosition();
        Point2D end = dest.getPosition();
        
        // Build path points
        List<Point2D> pathPoints = new ArrayList<>();
        pathPoints.add(start);
        for (BendPoint bendPoint : bendPoints) {
            pathPoints.add(bendPoint.getPosition());
        }
        pathPoints.add(end);
        
        // Calculate approximate curve lengths for each segment
        List<Double> segmentLengths = new ArrayList<>();
        double totalLength = 0;
        
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D segmentStart = pathPoints.get(i);
            Point2D segmentEnd = pathPoints.get(i + 1);
            
            // Calculate control point for this segment
            Point2D controlPoint = calculateSegmentControlPoint(segmentStart, segmentEnd, i);
            
            // Approximate curve length by sampling
            double curveLength = approximateQuadCurveLength(segmentStart, controlPoint, segmentEnd);
            segmentLengths.add(curveLength);
            totalLength += curveLength;
        }
        
        if (totalLength <= 0) {
            return start;
        }
        
        // Find which segment the progress falls into
        double targetDistance = progress * totalLength;
        double accumulatedDistance = 0;
        
        for (int i = 0; i < segmentLengths.size(); i++) {
            double segmentLength = segmentLengths.get(i);
            if (accumulatedDistance + segmentLength >= targetDistance) {
                // Progress is within this segment
                double segmentProgress = (targetDistance - accumulatedDistance) / segmentLength;
                
                Point2D segmentStart = pathPoints.get(i);
                Point2D segmentEnd = pathPoints.get(i + 1);
                Point2D controlPoint = calculateSegmentControlPoint(segmentStart, segmentEnd, i);
                
                return getQuadraticBezierPoint(segmentStart, controlPoint, segmentEnd, segmentProgress);
            }
            accumulatedDistance += segmentLength;
        }
        
        return end;
    }
    
    private Point2D calculateSegmentControlPoint(Point2D start, Point2D end, int segmentIndex) {
        if (segmentIndex < bendPoints.size()) {
            // This segment goes TO a bend point
            BendPoint bendPoint = bendPoints.get(segmentIndex);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            double curveStrength = 1.2;
            
            return new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
        } else if (segmentIndex > 0 && segmentIndex <= bendPoints.size()) {
            // This segment goes FROM a bend point
            BendPoint bendPoint = bendPoints.get(segmentIndex - 1);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            double curveStrength = 1.2;
            
            return new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
        }
        
        // Default to midpoint
        return new Point2D((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
    }
    
    /**
     * Calculate position on a quadratic Bezier curve
     * @param start Start point
     * @param control Control point
     * @param end End point
     * @param t Parameter from 0 to 1
     * @return Point on the curve
     */
    private Point2D getQuadraticBezierPoint(Point2D start, Point2D control, Point2D end, double t) {
        double oneMinusT = 1.0 - t;
        double x = oneMinusT * oneMinusT * start.getX() + 
                   2.0 * oneMinusT * t * control.getX() + 
                   t * t * end.getX();
        double y = oneMinusT * oneMinusT * start.getY() + 
                   2.0 * oneMinusT * t * control.getY() + 
                   t * t * end.getY();
        return new Point2D(x, y);
    }
    
    /**
     * Approximate the length of a quadratic curve by sampling
     */
    private double approximateQuadCurveLength(Point2D start, Point2D control, Point2D end) {
        double length = 0;
        int samples = 100; // Increased samples for more accurate curve length calculation
        Point2D prevPoint = start;
        
        for (int i = 1; i <= samples; i++) {
            double t = (double) i / samples;
            Point2D currentPoint = getQuadraticBezierPoint(start, control, end, t);
            length += prevPoint.distance(currentPoint);
            prevPoint = currentPoint;
        }
        
        return length;
    }
    
    private Point2D interpolatePoints(Point2D start, Point2D end, double progress) {
        double x = start.getX() + progress * (end.getX() - start.getX());
        double y = start.getY() + progress * (end.getY() - start.getY());
        return new Point2D(x, y);
    }
    
    // Bend point management methods
    public boolean canAddBendPoint() {
        return bendPoints.size() < 3;
    }
    
    public boolean addBendPoint(Point2D position, double maxRadius) {
        if (!canAddBendPoint()) return false;
        
        // Calculate default position along the wire
        Point2D defaultPosition = calculateDefaultBendPosition(bendPoints.size());
        BendPoint bendPoint = new BendPoint(defaultPosition, maxRadius);
        
        // Set initial position if provided
        if (position != null) {
            bendPoint.setPosition(position);
        }
        
        bendPoints.add(bendPoint);
        return true;
    }
    
    private Point2D calculateDefaultBendPosition(int index) {
        Point2D sourcePos = source.getPosition();
        Point2D destPos = dest.getPosition();
        
        // Divide wire into equal segments based on number of bend points
        int totalSegments = 3; // Always assume 3 segments for consistency
        double segmentRatio = (index + 1.0) / (totalSegments + 1.0);
        
        double x = sourcePos.getX() + segmentRatio * (destPos.getX() - sourcePos.getX());
        double y = sourcePos.getY() + segmentRatio * (destPos.getY() - sourcePos.getY());
        
        return new Point2D(x, y);
    }
    
    public void removeBendPoint(int index) {
        if (index >= 0 && index < bendPoints.size()) {
            bendPoints.remove(index);
        }
    }
    
    public List<BendPoint> getBendPoints() {
        return new ArrayList<>(bendPoints);
    }
    
    public boolean hasBendPoints() {
        return !bendPoints.isEmpty();
    }
}