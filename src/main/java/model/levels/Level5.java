package model.levels;

public class Level5 extends Level {
    public Level5() {
        super(2000); // Wire length for VPN system testing
        
        // Enable all mechanics for comprehensive testing
        setCollisionsDisabled(false);
        setImpactDisabled(false);
    }
} 