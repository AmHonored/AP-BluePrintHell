package config.levels;

/**
 * Model-side static settings for a level.
 */
public class LevelModelDefinition {
    private int wireLength;
    private int initialCoins = 20;
    private boolean impactDisabled = false;
    private boolean collisionsDisabled = false;

    public int getWireLength() { return wireLength; }
    public void setWireLength(int wireLength) { this.wireLength = wireLength; }

    public int getInitialCoins() { return initialCoins; }
    public void setInitialCoins(int initialCoins) { this.initialCoins = initialCoins; }

    public boolean isImpactDisabled() { return impactDisabled; }
    public void setImpactDisabled(boolean impactDisabled) { this.impactDisabled = impactDisabled; }

    public boolean isCollisionsDisabled() { return collisionsDisabled; }
    public void setCollisionsDisabled(boolean collisionsDisabled) { this.collisionsDisabled = collisionsDisabled; }
}

