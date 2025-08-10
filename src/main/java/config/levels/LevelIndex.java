package config.levels;

import java.util.ArrayList;
import java.util.List;

/**
 * Index file listing available levels and their resource paths.
 */
public class LevelIndex {
    public static class Entry {
        private String id;
        private String name;
        private String path; // classpath resource path to the level definition JSON

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    private int schemaVersion = 1;
    private List<Entry> levels = new ArrayList<>();

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }

    public List<Entry> getLevels() { return levels; }
    public void setLevels(List<Entry> levels) { this.levels = levels; }
}

