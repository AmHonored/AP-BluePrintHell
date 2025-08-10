package serialization.save;

import java.util.Map;

public class PacketSave {
    public String id;
    public String type; 

    public double x;
    public double y;
    public double dirX;
    public double dirY;

    public int currentHealth;
    public boolean inSystem;
    public boolean moving;
    public String currentWireId;
    public double movementProgress;
    public double secondsSinceMovementStart;
    

    public double aergiaFrozenSpeed;
    public double aergiaSecondsRemaining;

    public Map<String, Object> extra; 
}


