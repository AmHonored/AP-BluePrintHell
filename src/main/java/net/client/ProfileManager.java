package net.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists profiles under saves/ and remembers the last-used profile.
 */
public class ProfileManager {
    private static final String DEFAULT_PROFILE = "default";
    private static final Path ROOT = Paths.get("saves");
    private static final Path LAST_FILE = ROOT.resolve("last-profile.txt");

    public static List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(ROOT);
            try (java.util.stream.Stream<Path> s = Files.list(ROOT)) {
                s.filter(Files::isDirectory).forEach(p -> out.add(p.getFileName().toString()));
            }
        } catch (Exception ignored) {}
        if (out.isEmpty()) out.add(DEFAULT_PROFILE);
        return out;
    }

    public static String getLastOrDefault() {
        try {
            if (Files.exists(LAST_FILE)) {
                String v = Files.readString(LAST_FILE).trim();
                if (!v.isEmpty()) return v;
            }
        } catch (Exception ignored) {}
        return DEFAULT_PROFILE;
    }

    public static void setLast(String profile) {
        try {
            Files.createDirectories(ROOT);
            Files.writeString(LAST_FILE, profile == null ? DEFAULT_PROFILE : profile);
        } catch (Exception ignored) {}
    }
}


