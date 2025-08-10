package serialization.save;

import java.util.Map;

public class PacketSave {
    public String id;
    public String type; // PacketType name

    public double x;
    public double y;
    public double dirX;
    public double dirY;

    public int currentHealth;
    public boolean inSystem;
    public boolean moving;
    public Double startX;
    public Double startY;
    public Double targetX;
    public Double targetY;
    public String currentWireId;
    public double movementProgress;
    public double secondsSinceMovementStart;
    public boolean compatibleWithCurrentPort;

    public double deflectedX;
    public double deflectedY;
    public int noise;
    public boolean trojan;
    public boolean bitFragment;

    public double aergiaFrozenSpeed;
    public double aergiaSecondsRemaining;

    public Map<String, Object> extra; // type-specific fields
}


