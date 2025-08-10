package serialization.save;

public class SaveGame {
    public int schemaVersion;
    public String profileId;
    public String levelId;
    public long savedAtEpochMillis;

    public LevelSave level;
}


