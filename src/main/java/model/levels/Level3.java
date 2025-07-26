package model.levels;

public class Level3 extends Level {
    public Level3() {
        super(15000); // Wire length for Level 3 - hexagon packet testing
        // Enable collisions and impact waves for hexagon packet testing
        setCollisionsDisabled(false);
        setImpactDisabled(false);
    }
} 