package server.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side representation of a multiplayer room.
 */
public class Room {
    private final String roomCode;
    private final String creatorUsername;
    private final List<String> players;
    private final List<String> logs;
    private final Instant createdAt;
    
    public Room(String roomCode, String creatorUsername) {
        this.roomCode = roomCode;
        this.creatorUsername = creatorUsername;
        this.players = new CopyOnWriteArrayList<>();
        this.logs = new CopyOnWriteArrayList<>();
        this.createdAt = Instant.now();
        
        // Add creator to the room
        this.players.add(creatorUsername);
        addLog("Room created by " + creatorUsername);
    }
    
    public boolean addPlayer(String username) {
        if (players.contains(username)) {
            return false; // Already in room
        }
        if (players.size() >= 2) {
            return false; // Room full
        }
        
        players.add(username);
        addLog("Player " + username + " joined the room!");
        return true;
    }
    
    public boolean removePlayer(String username) {
        if (players.remove(username)) {
            addLog("Player " + username + " left the game!");
            return true;
        }
        return false;
    }
    
    public void addLog(String message) {
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            .format(java.time.LocalTime.now());
        logs.add("[" + timestamp + "] " + message);
        
        // Keep only last 20 log entries
        if (logs.size() > 20) {
            logs.remove(0);
        }
    }
    
    public boolean isEmpty() {
        return players.isEmpty();
    }
    
    public boolean isFull() {
        return players.size() >= 2;
    }
    
    // Getters
    public String getRoomCode() { return roomCode; }
    public String getCreatorUsername() { return creatorUsername; }
    public List<String> getPlayers() { return new ArrayList<>(players); }
    public List<String> getLogs() { return new ArrayList<>(logs); }
    public Instant getCreatedAt() { return createdAt; }
}
