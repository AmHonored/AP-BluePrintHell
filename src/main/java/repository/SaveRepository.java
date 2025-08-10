package repository;

import java.util.List;
import java.util.Optional;
import serialization.save.SaveGame;

public interface SaveRepository {
    Optional<SaveGame> loadLatest(String profileId, String levelId);
    void saveAtomic(String profileId, String levelId, SaveGame save);
    void delete(String profileId, String levelId);
    List<String> listLevelsWithSaves(String profileId);
}


