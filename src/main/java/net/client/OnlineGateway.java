package net.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import net.GameServerGateway;
import protocol.messages.*;
import protocol.messages.Leaderboard;
import protocol.messages.Run;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OnlineGateway implements GameServerGateway {
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Consumer<StateUpdate>> stateListeners = new ArrayList<>();
    private final List<Consumer<ErrorMessage>> errorListeners = new ArrayList<>();
    private final List<Consumer<Boolean>> connectionListeners = new ArrayList<>();
    private Consumer<Leaderboard.Response> leaderboardCallback;
    private Consumer<Run.FinishAck> finishCallback;
    private String currentRunId;

    private ClientConnection conn;
    private final List<Consumer<Profile.Snapshot>> profileListeners = new ArrayList<>();
    private volatile boolean connected = false;
    private volatile boolean awaitingAck = false;
    // reserved for metrics/telemetry if needed later
    // private volatile long connectStartNanos = 0L;
    // private static final long HANDSHAKE_TIMEOUT_NANOS = 2_500_000_000L; // 2.5s

    @Override
    public void connect(String host, int port, String username) {
        disconnect("reconnect");
        conn = new ClientConnection();
        try {
            conn.connect(host, port, 2000);
            conn.start(new ClientConnection.MessageHandler() {
                @Override
                public void onMessage(String json) {
                    handleIncoming(json);
                }

                @Override
                public void onClosed(IOException error) {
                    Platform.runLater(() -> notifyConnection(false));
                }
            });
            awaitingAck = true;
            conn.send(new Connect(username, getDeviceId(), getClientVersion()));
            // Handshake timeout watchdog
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {}
                if (awaitingAck) {
                    // handshake hung -> fail fast to reduce perceived lag
                    Platform.runLater(() -> {
                        ErrorMessage err = new ErrorMessage();
                        err.code = "HANDSHAKE_TIMEOUT";
                        err.message = "Server did not acknowledge connection.";
                        errorListeners.forEach(l -> l.accept(err));
                        notifyConnection(false);
                    });
                    disconnect("handshake-timeout");
                }
            }, "handshake-timeout").start();
        } catch (IOException e) {
            notifyConnection(false);
        }
    }

    @Override
    public void disconnect(String reason) {
        if (conn != null) {
            try { conn.close(); } catch (Exception ignored) {}
            conn = null;
        }
        if (connected) {
            connected = false;
            notifyConnection(false);
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void sendInput(ClientInput input) {
        if (conn != null && connected) {
            conn.send(input);
        }
    }

    @Override
    public void onStateUpdate(Consumer<StateUpdate> listener) {
        stateListeners.add(listener);
    }

    @Override
    public void onError(Consumer<ErrorMessage> listener) {
        errorListeners.add(listener);
    }

    @Override
    public void onConnectionChanged(Consumer<Boolean> listener) {
        connectionListeners.add(listener);
    }

    @Override
    public void onProfileSnapshot(Consumer<Profile.Snapshot> listener) {
        profileListeners.add(listener);
    }

    private void handleIncoming(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            String type = node.get("type").asText("");
            switch (type) {
                case "ConnectAck":
                    awaitingAck = false;
                    connected = true;
                    Platform.runLater(() -> notifyConnection(true));
                    break;
                case "ProfileSnapshot":
                    Profile.Snapshot snap = mapper.readValue(json, Profile.Snapshot.class);
                    Platform.runLater(() -> profileListeners.forEach(l -> l.accept(snap)));
                    break;
                case "StateUpdate":
                    StateUpdate update = mapper.readValue(json, StateUpdate.class);
                    Platform.runLater(() -> stateListeners.forEach(l -> l.accept(update)));
                    break;
                case "LeaderboardResponse":
                    Leaderboard.Response resp = mapper.readValue(json, Leaderboard.Response.class);
                    Consumer<Leaderboard.Response> cb = leaderboardCallback;
                    if (cb != null) Platform.runLater(() -> cb.accept(resp));
                    break;
                case "RunStartAck":
                    Run.StartAck sa = mapper.readValue(json, Run.StartAck.class);
                    currentRunId = sa.runId;
                    break;
                case "RunFinishAck":
                    Run.FinishAck fa = mapper.readValue(json, Run.FinishAck.class);
                    Consumer<Run.FinishAck> fcb = finishCallback;
                    if (fcb != null) Platform.runLater(() -> fcb.accept(fa));
                    break;
                case "Error":
                    ErrorMessage err = mapper.readValue(json, ErrorMessage.class);
                    Platform.runLater(() -> errorListeners.forEach(l -> l.accept(err)));
                    break;
                default:
                    // ignore unknown
            }
        } catch (Exception ignored) {}
    }

    public void requestLeaderboard(String levelCode, String mode, int limit, Consumer<Leaderboard.Response> cb) {
        this.leaderboardCallback = cb;
        if (conn != null && connected) {
            Leaderboard.Request req = new Leaderboard.Request();
            req.levelCode = levelCode;
            req.mode = mode;
            req.limit = Math.max(1, Math.min(50, limit));
            conn.send(req);
        }
    }

    public void startRun(String levelCode) {
        if (conn != null && connected) {
            Run.StartRequest req = new Run.StartRequest();
            req.levelCode = levelCode;
            conn.send(req);
        }
    }

    public void finishRun(String levelCode, long durationMs, int xp, Consumer<Run.FinishAck> cb) {
        this.finishCallback = cb;
        if (conn != null && connected) {
            Run.FinishRequest r = new Run.FinishRequest();
            r.runId = currentRunId;
            r.levelCode = levelCode;
            r.durationMs = durationMs;
            r.xpGained = xp;
            conn.send(r);
        }
    }

    private void notifyConnection(boolean state) {
        connectionListeners.forEach(l -> l.accept(state));
    }

    private String getDeviceId() {
        String profile = System.getenv("NETWORKGAME_PROFILE");
        if (profile == null || profile.isEmpty()) {
            try { profile = ProfileManager.getLastOrDefault(); } catch (Exception ignored) { profile = "default"; }
        }
        try {
            DeviceIdProvider provider = new ProfileDeviceIdProvider(profile);
            String id = provider.getDeviceId();
            try { ProfileManager.setLast(profile); } catch (Exception ignored) {}
            return id;
        } catch (Exception e) {
            return java.util.UUID.randomUUID().toString();
        }
    }

    private String getClientVersion() {
        return "1.0.0";
    }
}


