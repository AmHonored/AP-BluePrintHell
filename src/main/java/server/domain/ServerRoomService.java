package server.domain;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side room management service.
 */
public class ServerRoomService {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerToRoom = new ConcurrentHashMap<>();
    private final Map<String, Long> playerLastHeartbeat = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private static final long HEARTBEAT_TIMEOUT_MS = 15000; // 15 seconds
    
    public Room createRoom(String desiredCode, String creatorUsername) {
        // Remove player from any existing room
        leaveCurrentRoom(creatorUsername);
        
        String roomCode = desiredCode == null || desiredCode.trim().isEmpty()
                ? generateRoomCode()
                : desiredCode.trim().toUpperCase(Locale.ROOT);
                
        if (rooms.containsKey(roomCode)) {
            throw new IllegalArgumentException("Room already exists");
        }
        
        Room room = new Room(roomCode, creatorUsername);
        rooms.put(roomCode, room);
        playerToRoom.put(creatorUsername, roomCode);
        
        return room;
    }
    
    public Optional<Room> joinRoom(String roomCode, String username) {
        if (roomCode == null) {
            return Optional.empty();
        }
        
        // Remove player from any existing room first
        leaveCurrentRoom(username);
        
        String normalized = roomCode.trim().toUpperCase(Locale.ROOT);
        Room room = rooms.get(normalized);
        
        if (room == null) {
            return Optional.empty(); // Room not found
        }
        // If the player is already listed in this room (stale server state), treat as idempotent join
        if (room.getPlayers().contains(username)) {
            playerToRoom.put(username, normalized);
            updatePlayerHeartbeat(username);
            return Optional.of(room);
        }
        // If room is full and player is not already inside, reject
        if (room.isFull()) {
            return Optional.empty();
        }
        if (room.addPlayer(username)) {
            playerToRoom.put(username, normalized);
            updatePlayerHeartbeat(username);
            return Optional.of(room);
        }
        
        return Optional.empty(); // Could not join (room full)
    }
    
    public void leaveCurrentRoom(String username) {
        String currentRoomCode = playerToRoom.remove(username);
        playerLastHeartbeat.remove(username);
        if (currentRoomCode != null) {
            Room room = rooms.get(currentRoomCode);
            if (room != null) {
                room.removePlayer(username);
                if (room.isEmpty()) {
                    rooms.remove(currentRoomCode); // Clean up empty rooms
                }
            }
        }
    }
    
    public Optional<Room> getCurrentRoom(String username) {
        String roomCode = playerToRoom.get(username);
        if (roomCode != null) {
            return Optional.ofNullable(rooms.get(roomCode));
        }
        return Optional.empty();
    }
    
    public Optional<Room> getRoomByCode(String roomCode) {
        if (roomCode == null) return Optional.empty();
        return Optional.ofNullable(rooms.get(roomCode.toUpperCase(Locale.ROOT)));
    }
    
    private String generateRoomCode() {
        // 5-char uppercase alphanumeric code
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // skip ambiguous chars
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        String code = sb.toString();
        // ensure uniqueness (very unlikely to collide)
        if (rooms.containsKey(code)) {
            return generateRoomCode();
        }
        return code;
    }
    
    public void updatePlayerHeartbeat(String username) {
        if (playerToRoom.containsKey(username)) {
            playerLastHeartbeat.put(username, System.currentTimeMillis());
        }
    }
    
    public java.util.List<String> checkInactivePlayers() {
        java.util.List<String> inactivePlayers = new java.util.ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, Long> entry : playerLastHeartbeat.entrySet()) {
            if (currentTime - entry.getValue() > HEARTBEAT_TIMEOUT_MS) {
                inactivePlayers.add(entry.getKey());
            }
        }
        
        return inactivePlayers;
    }
    
    // Get all rooms (for debugging/admin purposes)
    public Map<String, Room> getAllRooms() {
        return new ConcurrentHashMap<>(rooms);
    }
}
