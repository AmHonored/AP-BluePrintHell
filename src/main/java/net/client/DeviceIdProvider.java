package net.client;

/**
 * Provides a stable device identifier per client profile.
 */
public interface DeviceIdProvider {
    /**
     * Returns a stable deviceId string for the current profile.
     */
    String getDeviceId();
}


