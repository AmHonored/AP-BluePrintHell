package config.levels;

import java.util.ArrayList;
import java.util.List;

/**
 * Root object describing a single level.
 */
public class LevelDefinition {
    private int schemaVersion = 1;
    private String id;
    private String name;

    private LevelDisplayDefinition display = new LevelDisplayDefinition();
    private LevelModelDefinition model = new LevelModelDefinition();
    private List<SystemDefinition> systems = new ArrayList<>();

    private String nextLevelId; // optional

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LevelDisplayDefinition getDisplay() { return display; }
    public void setDisplay(LevelDisplayDefinition display) { this.display = display; }

    public LevelModelDefinition getModel() { return model; }
    public void setModel(LevelModelDefinition model) { this.model = model; }

    public List<SystemDefinition> getSystems() { return systems; }
    public void setSystems(List<SystemDefinition> systems) { this.systems = systems; }

    public String getNextLevelId() { return nextLevelId; }
    public void setNextLevelId(String nextLevelId) { this.nextLevelId = nextLevelId; }
}

