package config.levels;

import java.util.HashMap;
import java.util.Map;

/**
 * View-side display hints to preserve exact look across levels.
 */
public class LevelDisplayDefinition {
    public static class GamePaneSize {
        private int width = 800;
        private int height = 500;

        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
    }

    private GamePaneSize gamePane = new GamePaneSize();
    /** Optional override of port sizes per shape. Keys: SQUARE, TRIANGLE, HEXAGON. */
    private Map<String, Double> portSizes = new HashMap<>();

    public GamePaneSize getGamePane() { return gamePane; }
    public void setGamePane(GamePaneSize gamePane) { this.gamePane = gamePane; }

    public Map<String, Double> getPortSizes() { return portSizes; }
    public void setPortSizes(Map<String, Double> portSizes) { this.portSizes = portSizes; }
}

