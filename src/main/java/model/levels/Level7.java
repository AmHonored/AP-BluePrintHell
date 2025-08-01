package model.levels;

public class Level7 extends Level {
    public Level7() {
        super(15000); // Medium wire length for testing confidential packet behaviors
        
        // Enable all mechanics for comprehensive confidential packet testing
        setCollisionsDisabled(false);
        setImpactDisabled(false);
    }
}