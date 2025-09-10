package server.persistence.json;

import server.domain.UserProfile;
import server.persistence.ProfileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonProfileRepository implements ProfileRepository {
    private final Path root = Paths.get("serverdata", "profiles");

    @Override
    public UserProfile getByDeviceId(String deviceId) {
        Path file = root.resolve(deviceId + ".json");
        try {
            if (Files.exists(file)) {
                return JsonIo.MAPPER.readValue(Files.readAllBytes(file), UserProfile.class);
            }
        } catch (IOException ignored) {}
        return null;
    }

    @Override
    public synchronized UserProfile upsert(UserProfile profile) {
        long now = System.currentTimeMillis();
        if (profile.createdAt == 0) profile.createdAt = now;
        profile.updatedAt = now;
        Path file = root.resolve(profile.deviceId + ".json");
        try {
            JsonIo.saveAtomic(file, profile);
        } catch (IOException ignored) {}
        return profile;
    }
}




