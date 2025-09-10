package net;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight in-memory room service used for local gameplay/testing.
 * Maintains a map of roomCode -> list of usernames.
 */
public class RoomService {
    private static final RoomService INSTANCE = new RoomService();

    public static RoomService getInstance() { return INSTANCE; }

    private final Map<String, List<String>> roomCodeToPlayers = new HashMap<>();
    private final Map<String, List<String>> roomCodeToLogs = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    private RoomService() {}

    /**
     * Create a new room (optionally with a user-specified code) and add the creator.
     * If the provided code is null/empty, a random code will be generated.
     * If the provided code already exists, throws IllegalArgumentException.
     */
    public synchronized String createRoom(String desiredCode, String username) {
        String code = desiredCode == null || desiredCode.trim().isEmpty()
                ? generateRoomCode()
                : desiredCode.trim().toUpperCase(Locale.ROOT);
        if (roomCodeToPlayers.containsKey(code)) {
            throw new IllegalArgumentException("Room already exists");
        }
        List<String> players = new ArrayList<>();
        players.add(username);
        roomCodeToPlayers.put(code, players);
        
        // Initialize logs for the room
        List<String> logs = new ArrayList<>();
        roomCodeToLogs.put(code, logs);
        addLog(code, "Room created by " + username);
        
        return code;
    }

    /**
     * Join an existing room by code.
     * @param code room code (case-insensitive)
     * @param username player username
     * @return current players in the room after join
     * @throws IllegalArgumentException if the room doesn't exist
     */
    public synchronized List<String> joinRoom(String code, String username) {
        if (code == null) throw new IllegalArgumentException("Room doesn't exist");
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        List<String> players = roomCodeToPlayers.get(normalized);
        if (players == null) {
            throw new IllegalArgumentException("Room doesn't exist");
        }
        if (!players.contains(username)) {
            players.add(username);
            addLog(normalized, "Player " + username + " joined the room!");
        }
        return new ArrayList<>(players);
    }

    /**
     * Get the players currently in a room.
     */
    public synchronized List<String> getPlayers(String code) {
        if (code == null) return Collections.emptyList();
        List<String> players = roomCodeToPlayers.get(code.toUpperCase(Locale.ROOT));
        return players == null ? Collections.emptyList() : new ArrayList<>(players);
    }

    /**
     * Check if the room exists.
     */
    public synchronized boolean roomExists(String code) {
        if (code == null) return false;
        return roomCodeToPlayers.containsKey(code.toUpperCase(Locale.ROOT));
    }

    /**
     * Get the logs for a room.
     */
    public synchronized List<String> getLogs(String code) {
        if (code == null) return Collections.emptyList();
        List<String> logs = roomCodeToLogs.get(code.toUpperCase(Locale.ROOT));
        return logs == null ? Collections.emptyList() : new ArrayList<>(logs);
    }

    /**
     * Add a log entry to a room.
     */
    public synchronized void addLog(String code, String message) {
        if (code == null) return;
        String normalized = code.toUpperCase(Locale.ROOT);
        List<String> logs = roomCodeToLogs.get(normalized);
        if (logs != null) {
            String timestamp = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(java.time.LocalTime.now());
            logs.add("[" + timestamp + "] " + message);
            
            // Keep only last 20 log entries to prevent memory issues
            if (logs.size() > 20) {
                logs.remove(0);
            }
        }
    }

    private String generateRoomCode() {
        // 5-char uppercase alphanumeric code
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // skip ambiguous
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        String code = sb.toString();
        // ensure uniqueness (very unlikely to collide)
        if (roomCodeToPlayers.containsKey(code)) {
            return generateRoomCode();
        }
        return code;
    }
}


