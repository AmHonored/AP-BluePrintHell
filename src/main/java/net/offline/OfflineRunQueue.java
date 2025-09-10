package net.offline;

import java.util.ArrayList;
import java.util.List;

/**
 * Queue for storing offline runs to be submitted when connection is restored.
 */
public class OfflineRunQueue {
    private static final OfflineRunQueue INSTANCE = new OfflineRunQueue();
    private final List<Entry> queue = new ArrayList<>();
    
    public static OfflineRunQueue getInstance() {
        return INSTANCE;
    }
    
    private OfflineRunQueue() {}
    
    public void enqueue(String levelCode, long durationMs, int xpGained) {
        queue.add(new Entry(levelCode, durationMs, xpGained));
    }
    
    public List<Entry> getAll() {
        return new ArrayList<>(queue);
    }
    
    public void remove(Entry entry) {
        queue.remove(entry);
    }
    
    public static class Entry {
        public final String levelCode;
        public final long durationMs;
        public final int xpGained;
        
        public Entry(String levelCode, long durationMs, int xpGained) {
            this.levelCode = levelCode;
            this.durationMs = durationMs;
            this.xpGained = xpGained;
        }
    }
}