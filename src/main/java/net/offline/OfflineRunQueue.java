package net.offline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Simple file-backed queue for offline run submissions.
 */
public class OfflineRunQueue {
    private static final OfflineRunQueue INSTANCE = new OfflineRunQueue();
    public static OfflineRunQueue getInstance() { return INSTANCE; }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path file = Paths.get("saves", "tmp", "default", "offline-runs.json");

    public static class Entry {
        public String levelCode;
        public long durationMs;
        public int xpGained;
        public long createdAt;
    }

    private OfflineRunQueue() {
        ensureDirs();
    }

    private void ensureDirs() {
        try { Files.createDirectories(file.getParent()); } catch (IOException ignored) {}
    }

    public synchronized void enqueue(String levelCode, long durationMs, int xpGained) {
        List<Entry> list = loadAll();
        Entry e = new Entry();
        e.levelCode = levelCode;
        e.durationMs = durationMs;
        e.xpGained = xpGained;
        e.createdAt = System.currentTimeMillis();
        list.add(e);
        saveAll(list);
    }

    public synchronized List<Entry> getAll() {
        return Collections.unmodifiableList(loadAll());
    }

    public synchronized void remove(Entry e) {
        List<Entry> list = loadAll();
        for (Iterator<Entry> it = list.iterator(); it.hasNext();) {
            Entry x = it.next();
            if (x.createdAt == e.createdAt && x.durationMs == e.durationMs && x.xpGained == e.xpGained && safeEq(x.levelCode, e.levelCode)) {
                it.remove();
                break;
            }
        }
        saveAll(list);
    }

    private boolean safeEq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private List<Entry> loadAll() {
        ensureDirs();
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length == 0) return new ArrayList<>();
            return mapper.readValue(bytes, new TypeReference<List<Entry>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void saveAll(List<Entry> entries) {
        ensureDirs();
        try {
            byte[] bytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(entries);
            Files.write(file, bytes);
        } catch (IOException ignored) {}
    }
}


