package net.offline;

import net.GameServerGateway;
import protocol.messages.*;
import java.util.function.Consumer;

/**
 * Offline implementation of GameServerGateway for single-player mode.
 */
public class OfflineGateway implements GameServerGateway {
    private boolean connected = false;

    @Override
    public void connect(String host, int port, String username) {
        // Offline mode - no actual connection
        connected = false;
    }

    @Override
    public void disconnect(String reason) {
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void sendInput(ClientInput input) {
        // No-op in offline mode
    }

    @Override
    public void onStateUpdate(Consumer<StateUpdate> listener) {
        // No-op in offline mode
    }

    @Override
    public void onError(Consumer<ErrorMessage> listener) {
        // No-op in offline mode
    }

    @Override
    public void onConnectionChanged(Consumer<Boolean> listener) {
        // No-op in offline mode
    }

    @Override
    public void onProfileSnapshot(Consumer<Profile.Snapshot> listener) {
        // No-op in offline mode
    }
}

