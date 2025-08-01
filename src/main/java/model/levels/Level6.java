package model.levels;

public class Level6 extends Level {
    public Level6() {
        super(20000); // Wire length suitable for testing AntiVirus system with multiple paths
        
        // Enable all mechanics for comprehensive AntiVirus testing
        setCollisionsDisabled(false);
        setImpactDisabled(false);
    }
} 