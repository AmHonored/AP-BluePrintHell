package server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.protocol.ConnectAck;
import server.protocol.StateUpdate;
import server.protocol.Connect;
import server.protocol.Leaderboard;
import server.domain.LeaderboardService;
import server.domain.ProfileService;
import server.domain.RunService;
import server.domain.UserProfile;
import server.persistence.ProfileRepository;
import server.persistence.json.JsonProfileRepository;
import server.protocol.Profile;
import server.persistence.RunRepository;
import java.util.UUID;
import server.net.ClientSession;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Minimal server to satisfy Online Game requirements: accepts clients, acks connects,
 * and broadcasts dummy StateUpdate snapshots on a fixed tick.
 */
public class ServerMain {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<ClientSession> sessions = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile long tick = 0;
    // Minimal shared state for demo
    private volatile int coins = 0;
    private volatile int packetsCollected = 0;
    private volatile int packetLoss = 0;
    private final LeaderboardService leaderboardService = new LeaderboardService();
    private final ProfileRepository profileRepository = new JsonProfileRepository();
    private final ProfileService profileService = new ProfileService(profileRepository);
    private final RunService runService = new RunService(profileService);

    public static void main(String[] args) throws Exception {
        int port = 5050;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        new ServerMain().start(port);
    }

    public void start(int port) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

        // Broadcast dummy state at ~20 Hz
        scheduler.scheduleAtFixedRate(this::broadcastState, 0, 50, TimeUnit.MILLISECONDS);

            while (true) {
                Socket s = serverSocket.accept();
                System.out.println("Client connected: " + s.getRemoteSocketAddress());
                ClientSession session = new ClientSession(s, new ClientSession.IncomingHandler() {
                @Override
                public void onMessage(ClientSession sess, String json) {
                    handleIncoming(sess, json);
                }

                @Override
                public void onClosed(ClientSession sess) {
                    sessions.remove(sess);
                    System.out.println("Client disconnected: " + s.getRemoteSocketAddress());
                }
            });
                sessions.add(session);
            }
        }
    }

    private void handleIncoming(ClientSession s, String json) {
        try {
            JsonNode n = mapper.readTree(json);
            String type = n.get("type").asText("");
            if ("Connect".equals(type)) {
                Connect req = mapper.readValue(json, Connect.class);
                String username = req.username == null ? "player" : req.username;
                String deviceId = req.deviceId == null ? "" : req.deviceId;
                // resolve/create profile
                UserProfile p = profileService.resolve(deviceId, username);
                // upsert into SQL users for leaderboard foreign keys
                try { RunRepository.upsertUser(p.username, p.deviceId); } catch (Exception ignored) {}
                // send ack and snapshot
                ConnectAck ack = new ConnectAck();
                ack.accepted = true;
                ack.serverTime = Instant.now().toEpochMilli();
                s.send(ack);
                Profile.Snapshot snap = new Profile.Snapshot();
                snap.username = p.username;
                snap.deviceId = p.deviceId;
                snap.xp = p.xp;
                snap.unlockedAbilities = p.unlockedAbilities;
                snap.activeAbility = p.activeAbility;
                snap.squadId = p.squadId;
                s.send(snap);
            } else if ("Input".equals(type)) {
                // Very simple progression effect based on inputs
                try {
                    JsonNode actions = n.get("actions");
                    if (actions != null && actions.isArray()) {
                        for (JsonNode a : actions) {
                            String kind = a.get("kind").asText("");
                            if ("WIRE_CONNECT".equals(kind)) {
                                coins = Math.min(9999, coins + 1);
                                packetsCollected = Math.min(9999, packetsCollected + 1);
                            } else if ("WIRE_DISCONNECT".equals(kind)) {
                                coins = Math.max(0, coins - 1);
                                packetLoss = Math.min(9999, packetLoss + 1);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            } else if ("RunStart".equals(type)) {
                // Issue runId; minimal DIV: timestamped start
                String runId = UUID.randomUUID().toString();
                com.fasterxml.jackson.databind.node.ObjectNode ack = mapper.createObjectNode();
                ack.put("type", "RunStartAck");
                ack.put("runId", runId);
                ack.put("serverStartTime", System.currentTimeMillis());
                s.send(ack);
            } else if ("RunFinish".equals(type)) {
                // Minimal acceptance: require runId and duration bounds
                String runId = n.path("runId").asText("");
                String levelCode = n.path("levelCode").asText((String) null);
                long durationMs = Math.max(0, n.path("durationMs").asLong(0));
                // Recompute XP server-side with a simple formula
                long clientDurationMs = Math.max(0, n.path("durationMs").asLong(0));
                int clientXp = Math.max(0, n.path("xpGained").asInt(0));
                int xpGained = computeXp(clientDurationMs, clientXp);
                if (runId == null || runId.isEmpty()) {
                    runId = java.util.UUID.randomUUID().toString();
                }
                boolean accepted = durationMs >= 1000 && durationMs <= 86_400_000; // 1s..24h
                String username = n.has("username") ? n.get("username").asText("player") : "player";
                String deviceId = n.has("deviceId") ? n.get("deviceId").asText("") : "";
                if (accepted) {
                    runService.finishRun(username, deviceId, levelCode, durationMs, xpGained);
                }
                com.fasterxml.jackson.databind.node.ObjectNode ack = mapper.createObjectNode();
                ack.put("type", "RunFinishAck");
                ack.put("runId", runId);
                ack.put("levelCode", levelCode);
                ack.put("durationMs", durationMs);
                ack.put("xpGained", xpGained);
                ack.put("accepted", accepted);
                if (!accepted) ack.put("reason", "Invalid run parameters");
                s.send(ack);
                // Optionally, send updated profile snapshot
                if (accepted && deviceId != null && !deviceId.isEmpty()) {
                    UserProfile p = profileService.resolve(deviceId, username);
                    Profile.Snapshot snap = new Profile.Snapshot();
                    snap.username = p.username;
                    snap.deviceId = p.deviceId;
                    snap.xp = p.xp;
                    snap.unlockedAbilities = p.unlockedAbilities;
                    snap.activeAbility = p.activeAbility;
                    snap.squadId = p.squadId;
                    s.send(snap);
                }
            } else if ("LeaderboardRequest".equals(type)) {
                Leaderboard.Request req = mapper.readValue(json, Leaderboard.Request.class);
                Leaderboard.Response resp = new Leaderboard.Response();
                resp.mode = req.mode;
                resp.levelCode = req.levelCode;
                java.util.ArrayList<Leaderboard.Entry> out = new java.util.ArrayList<>();
                if ("xp".equalsIgnoreCase(req.mode)) {
                    int rank = 1;
                    for (server.persistence.RunRepository.LeaderboardEntry e : leaderboardService.topXpAllTime(Math.max(1, req.limit))) {
                        Leaderboard.Entry le = new Leaderboard.Entry();
                        le.rank = rank++;
                        le.username = e.username;
                        le.durationMs = e.durationMs;
                        le.xp = e.xp;
                        le.when = e.when;
                        out.add(le);
                    }
                } else if ("campaign".equalsIgnoreCase(req.mode)) {
                    int rank = 1;
                    for (server.persistence.RunRepository.LeaderboardEntry e : leaderboardService.topCampaignTimeAllTime(Math.max(1, req.limit))) {
                        Leaderboard.Entry le = new Leaderboard.Entry();
                        le.rank = rank++;
                        le.username = e.username;
                        le.durationMs = e.durationMs; // total campaign time
                        le.xp = e.xp; // 0 for now
                        le.when = e.when;
                        out.add(le);
                    }
                } else {
                    int rank = 1;
                    for (server.persistence.RunRepository.LeaderboardEntry e : leaderboardService.topTimes(req.levelCode, Math.max(1, req.limit))) {
                        Leaderboard.Entry le = new Leaderboard.Entry();
                        le.rank = rank++;
                        le.username = e.username;
                        le.durationMs = e.durationMs;
                        le.xp = e.xp;
                        le.when = e.when;
                        out.add(le);
                    }
                }
                resp.entries = out;
                s.send(resp);
            }
        } catch (Exception ignored) {}
    }

    private void broadcastState() {
        tick++;
        StateUpdate update = new StateUpdate();
        update.tick = tick;
        update.snapshot = new StateUpdate.Snapshot();
        // broadcast minimal fields to prove progression
        update.snapshot.coins = coins;
        update.snapshot.packetsCollected = packetsCollected;
        update.snapshot.packetLoss = packetLoss;
        update.snapshot.packets = java.util.Collections.emptyList();
        update.snapshot.wires = java.util.Collections.emptyList();
        for (ClientSession s : sessions) {
            try {
                s.send(update);
            } catch (Exception ignored) {}
        }
    }

    private int computeXp(long durationMs, int clientHint) {
        // Simple baseline: par=120s, base=100
        long par = 120_000L;
        double speed = Math.max(0.6, Math.min(1.8, (double) par / Math.max(1000, durationMs)));
        double reliability = Math.max(0.9, Math.min(1.2, 1.2 - (packetLoss / Math.max(1.0, (packetsCollected + packetLoss)))));
        int base = 100;
        int xp = (int) Math.round(base * speed * reliability);
        // Allow small hint from client (e.g., coins) but cap influence
        xp += Math.min(50, Math.max(0, clientHint / 10));
        return Math.max(0, xp);
    }
}


