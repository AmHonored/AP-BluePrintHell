package model.wire;

import model.entity.ports.Port;
import javafx.geometry.Point2D;
import java.util.List;

public class Wire {
    private final String id;
    private final Port source;
    private final Port dest;
    private boolean active = true;
    private final BendPath bendPath = new BendPath();
    private int massivePacketRunCount = 0;
    
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

    public void incrementMassivePacketRunCount() {
        massivePacketRunCount++;
    }

    public int getMassivePacketRunCount() {
        return massivePacketRunCount;
    }

    public boolean hasReachedMassiveRunLimit() {
        return massivePacketRunCount >= 3;
    }

    public void detachAndDeactivate() {
        if (source != null) {
            source.setWire(null);
        }
        if (dest != null) {
            dest.setWire(null);
        }
        this.active = false;
    }

    public double getLength() {
        if (source == null || dest == null) return 0;
        return bendPath.computeLength(source.getPosition(), dest.getPosition());
    }

    public Point2D getPositionAtProgress(double progress) {
        if (source == null || dest == null) {
            return new Point2D(0, 0);
        }
        
        progress = Math.max(0.0, Math.min(1.0, progress));
        
        return bendPath.positionAt(progress, source.getPosition(), dest.getPosition());
    }
        
    // Bend point management methods
    public boolean canAddBendPoint() { return bendPath.canAddBendPoint(); }
    
    public boolean addBendPoint(Point2D position, double maxRadius) {
        return bendPath.addBendPoint(position, maxRadius, source.getPosition(), dest.getPosition());
    }
    
    public void removeBendPoint(int index) {
        bendPath.removeBendPoint(index);
    }
    
    public List<BendPoint> getBendPoints() {
        return bendPath.getBendPoints();
    }
    
    public boolean hasBendPoints() {
        return bendPath.hasBendPoints();
    }
}