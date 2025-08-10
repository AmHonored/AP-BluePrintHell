package serialization.save;

public class PortSave {
    public String id;
    public String systemId; // stable system id
    public String role; // INPUT/OUTPUT
    public String shapeKind; // dynamic shape kind
    public double x;
    public double y;
    public String wireId; // connected wire id or null
}


