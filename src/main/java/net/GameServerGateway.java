package net;

import protocol.messages.ClientInput;
import protocol.messages.ErrorMessage;
import protocol.messages.StateUpdate;
import protocol.messages.Profile;

import java.util.function.Consumer;

/**
 * Abstraction for talking to a game server (online) or a local simulator (offline).
 * Keeps controllers decoupled from transport details.
 */
public interface GameServerGateway {
    void connect(String host, int port, String username);
    void disconnect(String reason);
    boolean isConnected();

    void sendInput(ClientInput input);

    void onStateUpdate(Consumer<StateUpdate> listener);
    void onError(Consumer<ErrorMessage> listener);
    void onConnectionChanged(Consumer<Boolean> listener);
    default void onProfileSnapshot(Consumer<Profile.Snapshot> listener) { }
}






