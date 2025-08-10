package config.levels;

public class PortDefinition {
    public enum PortRole { INPUT, OUTPUT }
    public enum PortShape { SQUARE, TRIANGLE, HEXAGON }

    private String id;
    private PortRole role;
    private PortShape shape;
    private Point position;

    public PortDefinition() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public PortRole getRole() { return role; }
    public void setRole(PortRole role) { this.role = role; }

    public PortShape getShape() { return shape; }
    public void setShape(PortShape shape) { this.shape = shape; }

    public Point getPosition() { return position; }
    public void setPosition(Point position) { this.position = position; }
}

