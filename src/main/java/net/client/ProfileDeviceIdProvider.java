package net.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-profile device identity based on MAC (hashed) with UUID fallback.
 */
public class ProfileDeviceIdProvider implements DeviceIdProvider {
    private final String profileName;
    private final Path idFile;
    private static final Map<String, String> sessionDeviceIds = new HashMap<>();

    public ProfileDeviceIdProvider(String profileName) {
        this.profileName = (profileName == null || profileName.isEmpty()) ? "default" : profileName;
        this.idFile = Paths.get("saves", this.profileName, "device-id.txt");
    }

    @Override
    public String getDeviceId() {
        // Check if we're in session-based mode (for multiple instances)
        boolean sessionBased = "true".equals(System.getProperty("networkgame.sessionbased")) ||
                              profileName.startsWith("temp_");
        
        if (sessionBased) {
            // Use a session-specific ID that persists for this JVM instance
            return sessionDeviceIds.computeIfAbsent(profileName, k -> {
                String sessionId = UUID.randomUUID().toString();
                // Don't persist session-based IDs to disk
                return sessionId;
            });
        }
        
        // Normal mode: use existing ID or derive from MAC
        try {
            if (Files.exists(idFile)) return Files.readString(idFile).trim();
        } catch (IOException ignored) {}

        String derived = deriveFromMac(profileName);
        if (derived == null || derived.isEmpty()) derived = UUID.randomUUID().toString();
        try {
            Files.createDirectories(idFile.getParent());
            Files.writeString(idFile, derived, StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
        return derived;
    }

    private static String deriveFromMac(String profileName) {
        try {
            byte[] mac = tryGetPrimaryMac();
            if (mac == null || mac.length == 0) return null;
            String base = toHex(mac);
            String input = base + ":" + profileName;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] tryGetPrimaryMac() throws Exception {
        java.util.Enumeration<java.net.NetworkInterface> ifs = java.net.NetworkInterface.getNetworkInterfaces();
        while (ifs.hasMoreElements()) {
            java.net.NetworkInterface nif = ifs.nextElement();
            if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) continue;
            byte[] mac = nif.getHardwareAddress();
            if (mac != null && mac.length > 0) return mac;
        }
        return null;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}


