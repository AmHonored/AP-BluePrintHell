package net.client;

/**
 * Resolves the effective username and profile for this client process.
 * Prefers JVM system properties, then environment variables, then OS/user defaults.
 */
public final class UserIdentity {
    private UserIdentity() {}

    public static String getEffectiveProfile() {
        String prop = System.getProperty("networkgame.profile");
        if (prop != null && !prop.isEmpty()) return prop;
        String env = System.getenv("NETWORKGAME_PROFILE");
        if (env != null && !env.isEmpty()) return env;
        try { return ProfileManager.getLastOrDefault(); } catch (Exception ignored) { return "default"; }
    }

    public static boolean isProfileOverridden() {
        String prop = System.getProperty("networkgame.profile");
        if (prop != null && !prop.isEmpty()) return true;
        String env = System.getenv("NETWORKGAME_PROFILE");
        return env != null && !env.isEmpty();
    }

    public static String getEffectiveUsername() {
        String prop = System.getProperty("networkgame.username");
        if (prop != null && !prop.isEmpty()) return prop;
        String env = System.getenv("NETWORKGAME_USERNAME");
        if (env != null && !env.isEmpty()) return env;
        return System.getProperty("user.name", "player");
    }
}



