package service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.nio.file.Paths;
import java.util.Optional;
import model.levels.Level;
import repository.SaveRepository;
import repository.json.JsonSaveRepository;
import serialization.save.SaveGame;

/**
 * Crash-only autosave manager.
 * - Writes to saves/tmp per profile/level
 * - Atomic writes via JsonSaveRepository
 * - No persistence on menu-driven exits (caller should delete on exit)
 */
public class CrashAutosaveManager {
    private final SaveRepository tmpRepository;
    private final SaveService tmpSaveService;
    private final String profileId;
    private Timeline autosaveTimeline;

    public CrashAutosaveManager() {
        this("default");
    }

    public CrashAutosaveManager(String profileId) {
        this.profileId = profileId;
        // use a separate tmp root
        this.tmpRepository = new JsonSaveRepository(Paths.get("saves", "tmp"));
        this.tmpSaveService = new SaveService(tmpRepository, profileId);
    }

    public void startAutosave(Level level, String levelId, double intervalSeconds) {
        stopAutosave();
        autosaveTimeline = new Timeline(new KeyFrame(Duration.seconds(intervalSeconds), e -> {
            if (level == null) return;
            // Only autosave during active play; skip when paused
            if (level.isPaused() || level.isGameOver()) return;
            tmpSaveService.saveNow(level, profileId, levelId);
        }));
        autosaveTimeline.setCycleCount(Timeline.INDEFINITE);
        autosaveTimeline.play();
    }

    public void stopAutosave() {
        if (autosaveTimeline != null) {
            autosaveTimeline.stop();
            autosaveTimeline = null;
        }
    }

    public Optional<SaveGame> tryLoad(String levelId) {
        return tmpRepository.loadLatest(profileId, levelId);
    }

    public void deleteAutosave(String levelId) {
        tmpRepository.delete(profileId, levelId);
    }
}


