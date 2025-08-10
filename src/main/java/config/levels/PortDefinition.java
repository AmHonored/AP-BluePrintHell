package config.levels;

/**
 * Data model for a port defined in a level configuration file.
 * Designed to work with JSON mappers via standard getters/setters.
 */
public class PortDefinition {
    public enum PortRole { INPUT, OUTPUT }
    public enum PortShape { SQUARE, TRIANGLE, HEXAGON }

    /** Unique identifier of the port within the level. */
    private String id;
    /** INPUT or OUTPUT. */
    private PortRole role;
    /** Port visual/model shape/type. */
    private PortShape shape;
    /** Absolute position in the game pane coordinate space. */
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

