package server.domain;

import server.persistence.RunRepository;

public class RunService {
    private final ProfileService profileService;

    public RunService(ProfileService profileService) {
        this.profileService = profileService;
    }

    public int computeXp(long durationMs, int clientHint, int packetsCollected, int packetLoss) {
        long par = 120_000L;
        double speed = Math.max(0.6, Math.min(1.8, (double) par / Math.max(1000, durationMs)));
        double reliability = Math.max(0.9, Math.min(1.2, 1.2 - (packetLoss / Math.max(1.0, (packetsCollected + packetLoss)))));
        int base = 100;
        int xp = (int) Math.round(base * speed * reliability);
        xp += Math.min(50, Math.max(0, clientHint / 10));
        return Math.max(0, xp);
    }

    public boolean finishRun(String username, String deviceId, String levelCode, long durationMs, int xpGained) {
        if (durationMs < 1000 || durationMs > 86_400_000) return false;
        try {
            if (levelCode != null) RunRepository.ensureLevel(levelCode, levelCode, 1);
            RunRepository.insertRun(username, levelCode, durationMs, xpGained, true);
        } catch (Exception ignored) {}
        try { profileService.addXp(deviceId, xpGained, levelCode, durationMs); } catch (Exception ignored) {}
        return true;
    }
}




