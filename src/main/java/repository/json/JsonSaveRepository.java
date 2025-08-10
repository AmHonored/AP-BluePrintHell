package repository.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;
import repository.SaveRepository;
import serialization.save.SaveGame;

public class JsonSaveRepository implements SaveRepository {
    private final ObjectMapper objectMapper;
    private final Path rootDir;

    public JsonSaveRepository(Path rootDir) {
        this.rootDir = rootDir;
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public Optional<SaveGame> loadLatest(String profileId, String levelId) {
        try {
            Path file = savePath(profileId, levelId);
            if (Files.exists(file)) {
                return Optional.of(objectMapper.readValue(Files.readAllBytes(file), SaveGame.class));
            }
            Path bak = backupPath(profileId, levelId);
            if (Files.exists(bak)) {
                return Optional.of(objectMapper.readValue(Files.readAllBytes(bak), SaveGame.class));
            }
            return Optional.empty();
        } catch (IOException e) {
            try {
                Path bak = backupPath(profileId, levelId);
                if (Files.exists(bak)) {
                    return Optional.of(objectMapper.readValue(Files.readAllBytes(bak), SaveGame.class));
                }
            } catch (IOException ignored) {}
            return Optional.empty();
        }
    }

    @Override
    public void saveAtomic(String profileId, String levelId, SaveGame save) {
        try {
            Path dir = rootDir.resolve(profileId);
            Files.createDirectories(dir);
            Path file = savePath(profileId, levelId);
            Path tmp = dir.resolve(levelId + ".json.tmp");
            Path bak = backupPath(profileId, levelId);

            if (save.savedAtEpochMillis == 0L) {
                save.savedAtEpochMillis = Instant.now().toEpochMilli();
            }

            byte[] bytes = objectMapper.writeValueAsBytes(save);
            Files.write(tmp, bytes);

            if (Files.exists(file)) {
                Files.move(file, bak, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist save file", e);
        }
    }

    @Override
    public void delete(String profileId, String levelId) {
        try {
            Files.deleteIfExists(savePath(profileId, levelId));
            Files.deleteIfExists(backupPath(profileId, levelId));
        } catch (IOException ignored) {}
    }


    private Path savePath(String profileId, String levelId) {
        return rootDir.resolve(profileId).resolve(levelId + ".json");
    }

    private Path backupPath(String profileId, String levelId) {
        return rootDir.resolve(profileId).resolve(levelId + ".json.bak");
    }
}


