package model.wire;

import model.entity.ports.Port;
import javafx.geometry.Point2D;

public class Wire {
    private final String id;
    private final Port source;
    private final Port dest;
    private boolean active = true;

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
        return source.getPosition().distance(dest.getPosition());
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
        
        Point2D sourcePos = source.getPosition();
        Point2D destPos = dest.getPosition();
        
        // Calculate precise interpolated position for packet center
        double deltaX = destPos.getX() - sourcePos.getX();
        double deltaY = destPos.getY() - sourcePos.getY();
        
        double x = sourcePos.getX() + progress * deltaX;
        double y = sourcePos.getY() + progress * deltaY;
        
        return new Point2D(x, y);
    }
}
