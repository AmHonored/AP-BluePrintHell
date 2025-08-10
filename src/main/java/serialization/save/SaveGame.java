package serialization.save;

/**
 * Root object persisted to disk for in-level save games.
 * Keep this DTO free of any engine/runtime references.
 */
public class SaveGame {
    public int schemaVersion;
    public String profileId;
    public String levelId;
    public long savedAtEpochMillis;

    public LevelSave level;
}


