package config.levels;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads level index and individual level definitions from JSON resources.
 */
public class LevelConfigLoader {
    private final ObjectMapper objectMapper;
    private final Map<String, LevelDefinition> cacheById = new HashMap<>();
    private LevelIndex levelIndex;

    public LevelConfigLoader() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public LevelIndex loadIndex(String classpathResource) {
        try (InputStream in = getResourceStream(classpathResource)) {
            if (in == null) throw new IllegalStateException("Level index not found: " + classpathResource);
            this.levelIndex = objectMapper.readValue(in, LevelIndex.class);
            return this.levelIndex;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load level index: " + classpathResource, e);
        }
    }

    public Optional<LevelDefinition> findLevelById(String id) {
        if (levelIndex == null) throw new IllegalStateException("Level index not loaded");
        if (cacheById.containsKey(id)) return Optional.ofNullable(cacheById.get(id));

        return levelIndex.getLevels().stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst()
                .map(entry -> loadLevel(entry.getPath()));
    }

    public LevelDefinition loadLevel(String classpathResource) {
        try (InputStream in = getResourceStream(classpathResource)) {
            if (in == null) throw new IllegalStateException("Level definition not found: " + classpathResource);
            LevelDefinition def = objectMapper.readValue(in, LevelDefinition.class);
            if (def.getId() != null) cacheById.put(def.getId(), def);
            return def;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load level definition: " + classpathResource, e);
        }
    }

    private InputStream getResourceStream(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(normalized);
        if (in != null) return in;
        return LevelConfigLoader.class.getClassLoader().getResourceAsStream(normalized);
    }
}

