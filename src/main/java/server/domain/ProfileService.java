package server.domain;

import server.persistence.ProfileRepository;

public class ProfileService {
    private final ProfileRepository repo;

    public ProfileService(ProfileRepository repo) {
        this.repo = repo;
    }

    public UserProfile resolve(String deviceId, String usernameIfNew) {
        UserProfile p = repo.getByDeviceId(deviceId);
        if (p == null) {
            p = new UserProfile();
            p.deviceId = deviceId;
            p.username = (usernameIfNew == null || usernameIfNew.isEmpty()) ? "player" : usernameIfNew;
            p.xp = 0;
            repo.upsert(p);
        } else if (p.username == null || p.username.isEmpty()) {
            if (usernameIfNew != null && !usernameIfNew.isEmpty()) {
                p.username = usernameIfNew;
                repo.upsert(p);
            }
        }
        return p;
    }

    public UserProfile addXp(String deviceId, int delta, String levelCode, long durationMs) {
        if (delta <= 0) return repo.getByDeviceId(deviceId);
        UserProfile p = repo.getByDeviceId(deviceId);
        if (p == null) return null;
        p.xp = Math.max(0, p.xp + delta);
        UserProfile.Score s = new UserProfile.Score();
        s.levelCode = levelCode;
        s.durationMs = durationMs;
        s.xpGained = delta;
        s.when = System.currentTimeMillis();
        p.scoreHistory.add(s);
        repo.upsert(p);
        return p;
    }
}



