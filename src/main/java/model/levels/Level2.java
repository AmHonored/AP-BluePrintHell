package model.levels;

public class Level2 extends Level {
    public Level2() {
        super(20000); // Increased wire length for Level 2's complex routing
        // Ensure collisions and impact waves are enabled for this level
        setCollisionsDisabled(false);
        setImpactDisabled(false);
    }
}
