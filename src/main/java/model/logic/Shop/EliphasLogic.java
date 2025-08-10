package model.logic.Shop;

import model.levels.Level;
import model.wire.Wire;

/**
 * Core logic for Eliphas effects and mark placement.
 * Scroll of Eliphas: re-centers packets to the wire path when they pass the mark.
 */
public final class EliphasLogic {
    private EliphasLogic() {}

    // Eliphas effect lasts 30 seconds
    public static final long EFFECT_DURATION_NANOS = 30_000_000_000L; // 30s

    /**
     * Eliphas mark data kept in the level to inform movement logic.
     */
    public static class EliphasMark {
        public final Wire wire;
        public final double progress; // 0..1 along the wire
        public final long effectEndNanos; // absolute nano time when effect ends

        public EliphasMark(Wire wire, double progress, long effectEndNanos) {
            this.wire = wire;
            this.progress = progress;
            this.effectEndNanos = effectEndNanos;
        }
    }

    /**
     * Add a mark to the level.
     */
    public static void addMark(Level level, Wire wire, double progress) {
        if (level == null || wire == null) return;
        long now = java.lang.System.nanoTime();
        long effectEnd = now + EFFECT_DURATION_NANOS;
        level.getEliphasMarks().add(new EliphasMark(wire, progress, effectEnd));
        java.lang.System.out.println("DEBUG: ELIPHAS addMark() → wire=" + wire.getId() +
            ", progress=" + String.format("%.3f", progress) + 
            ", effectEnd=" + effectEnd);
    }

    /**
     * Remove expired Eliphas marks to keep the list small.
     */
    public static void pruneExpiredMarks(Level level) {
        if (level == null) return;
        long now = java.lang.System.nanoTime();
        java.util.List<EliphasMark> toKeep = new java.util.ArrayList<>();
        for (EliphasMark m : level.getEliphasMarks()) {
            if (m.effectEndNanos > now) toKeep.add(m);
        }
        level.setEliphasMarks(toKeep);
    }
}


