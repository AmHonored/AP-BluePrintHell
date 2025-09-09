package net;

import javafx.application.Platform;
import net.client.OnlineGateway;
import net.offline.OfflineGateway;
import protocol.messages.ClientInput;
import protocol.messages.ErrorMessage;
import protocol.messages.StateUpdate;
import protocol.messages.Profile;
import protocol.messages.Leaderboard;
import protocol.messages.Run;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple global service to manage the active GameServerGateway.
 */
public class NetworkService {
    private static final NetworkService INSTANCE = new NetworkService();

    public static NetworkService getInstance() { return INSTANCE; }

    private GameServerGateway gateway = new OfflineGateway();
    private final List<Consumer<Boolean>> connectionListeners = new ArrayList<>();
    private final List<Consumer<StateUpdate>> stateListeners = new ArrayList<>();
    private final List<Consumer<ErrorMessage>> errorListeners = new ArrayList<>();
    private final List<Consumer<Leaderboard.Response>> leaderboardListeners = new ArrayList<>();
    private final List<Consumer<Profile.Snapshot>> profileListeners = new ArrayList<>();
    private final List<Consumer<Run.FinishAck>> finishListeners = new ArrayList<>();

    private NetworkService() {
        // wire through current gateway
        gateway.onConnectionChanged(this::notifyConnection);
        gateway.onStateUpdate(this::notifyState);
        gateway.onError(this::notifyError);
        gateway.onProfileSnapshot(this::notifyProfile);
    }

    public boolean isConnected() {
        return gateway.isConnected();
    }

    public void connectOnline(String host, int port, String username) {
        OnlineGateway online = new OnlineGateway();
        replaceGateway(online);
        online.connect(host, port, username);
    }

    public void goOffline() {
        replaceGateway(new OfflineGateway());
        notifyConnection(false);
    }

    public void disconnect(String reason) {
        gateway.disconnect(reason);
    }

    public void sendInput(ClientInput input) {
        gateway.sendInput(input);
    }

    public void onConnectionChanged(Consumer<Boolean> listener) {
        connectionListeners.add(listener);
    }

    public void onStateUpdate(Consumer<StateUpdate> listener) {
        stateListeners.add(listener);
    }

    public void onError(Consumer<ErrorMessage> listener) {
        errorListeners.add(listener);
    }

    public void onProfileSnapshot(Consumer<Profile.Snapshot> listener) {
        profileListeners.add(listener);
    }

    public void onLeaderboard(Consumer<Leaderboard.Response> listener) {
        leaderboardListeners.add(listener);
    }

    public void onRunFinished(Consumer<Run.FinishAck> listener) {
        finishListeners.add(listener);
    }

    private void replaceGateway(GameServerGateway next) {
        try { gateway.disconnect("switch"); } catch (Exception ignored) {}
        gateway = next;
        gateway.onConnectionChanged(this::notifyConnection);
        gateway.onStateUpdate(this::notifyState);
        gateway.onError(this::notifyError);
    }

    private void notifyConnection(Boolean state) {
        Platform.runLater(() -> connectionListeners.forEach(l -> l.accept(state)));
        // On connect, attempt to flush any offline runs
        if (state) {
            try {
                java.util.List<net.offline.OfflineRunQueue.Entry> pending = net.offline.OfflineRunQueue.getInstance().getAll();
                for (net.offline.OfflineRunQueue.Entry e : pending) {
                    finishRun(e.levelCode, e.durationMs, e.xpGained);
                    net.offline.OfflineRunQueue.getInstance().remove(e);
                }
            } catch (Throwable ignored) {}
        }
    }

    private void notifyState(StateUpdate update) {
        Platform.runLater(() -> stateListeners.forEach(l -> l.accept(update)));
    }

    private void notifyError(ErrorMessage err) {
        Platform.runLater(() -> errorListeners.forEach(l -> l.accept(err)));
    }

    private void notifyProfile(Profile.Snapshot snapshot) {
        Platform.runLater(() -> profileListeners.forEach(l -> l.accept(snapshot)));
    }

    private void notifyLeaderboard(Leaderboard.Response resp) {
        Platform.runLater(() -> leaderboardListeners.forEach(l -> l.accept(resp)));
    }

    private void notifyRunFinish(Run.FinishAck ack) {
        Platform.runLater(() -> finishListeners.forEach(l -> l.accept(ack)));
    }

    public void requestLeaderboard(String levelCode, String mode, int limit) {
        if (gateway instanceof net.client.OnlineGateway) {
            ((net.client.OnlineGateway) gateway).requestLeaderboard(levelCode, mode, limit, this::notifyLeaderboard);
        }
    }

    public void startRun(String levelCode) {
        if (gateway instanceof net.client.OnlineGateway) {
            ((net.client.OnlineGateway) gateway).startRun(levelCode);
        }
    }

    public void finishRun(String levelCode, long durationMs, int xp) {
        if (gateway instanceof net.client.OnlineGateway) {
            ((net.client.OnlineGateway) gateway).finishRun(levelCode, durationMs, xp, this::notifyRunFinish);
        }
        else {
            try {
                net.offline.OfflineRunQueue.getInstance().enqueue(levelCode, durationMs, xp);
            } catch (Throwable ignored) {}
        }
    }
}


