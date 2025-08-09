package model.logic.Shop;

import javafx.geometry.Point2D;
import model.levels.Level;
import model.wire.Wire;

import java.util.ArrayList;
import java.util.List;

/**
 * Core logic for Aergia effects and mark placement.
 */
public final class AergiaLogic {
    private AergiaLogic() {}

    public static final long EFFECT_DURATION_NANOS = 20_000_000_000L; // 20s
    public static final long COOLDOWN_DURATION_NANOS = 10_000_000_000L; // 10s cooldown

    /**
     * Aergia mark data kept in the level to inform movement logic.
     */
    public static class AergiaMark {
        public final Wire wire;
        public final double progress; // 0..1 along the wire
        public final long effectEndNanos; // absolute nano time when effect ends

        public AergiaMark(Wire wire, double progress, long effectEndNanos) {
            this.wire = wire;
            this.progress = progress;
            this.effectEndNanos = effectEndNanos;
        }
    }

    /**
     * Utility to estimate the closest progress parameter on a wire to a given point.
     * Uses sampling; sufficient for placement accuracy.
     */
    public static double findClosestProgress(Wire wire, Point2D point) {
        if (wire == null || point == null) return 0.0;
        int samples = 250;
        double bestT = 0.0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            Point2D p = wire.getPositionAtProgress(t);
            double d = p.distance(point);
            if (d < bestDist) {
                bestDist = d;
                bestT = t;
            }
        }
        return bestT;
    }

    /**
     * Add a mark to the level and configure cooldown.
     */
    public static void addMark(Level level, Wire wire, double progress) {
        long now = java.lang.System.nanoTime();
        long effectEnd = now + EFFECT_DURATION_NANOS;
        long cooldownEnd = effectEnd + COOLDOWN_DURATION_NANOS;
        
        level.getAergiaMarks().add(new AergiaMark(wire, progress, effectEnd));
        // Cooldown starts after effect ends
        level.setAergiaCooldownEnd(cooldownEnd);
        
        java.lang.System.out.println("DEBUG: addMark() - now: " + now);
        java.lang.System.out.println("DEBUG: addMark() - effectEnd: " + effectEnd);
        java.lang.System.out.println("DEBUG: addMark() - cooldownEnd: " + cooldownEnd);
        java.lang.System.out.println("DEBUG: addMark() - effect duration: " + (EFFECT_DURATION_NANOS / 1_000_000_000.0) + "s");
        java.lang.System.out.println("DEBUG: addMark() - cooldown duration: " + (COOLDOWN_DURATION_NANOS / 1_000_000_000.0) + "s");
        java.lang.System.out.println("DEBUG: addMark() - total marks: " + level.getAergiaMarks().size());
    }

    /**
     * Remove expired marks.
     */
    public static void pruneExpiredMarks(Level level) {
        long now = java.lang.System.nanoTime();
        int marksBeforePrune = level.getAergiaMarks().size();
        boolean wasOnCooldown = level.isAergiaOnCooldown();
        
        List<AergiaMark> toKeep = new ArrayList<>();
        for (AergiaMark m : level.getAergiaMarks()) {
            if (m.effectEndNanos > now) {
                toKeep.add(m);
            } else {
                java.lang.System.out.println("DEBUG: AERGIA MARK EXPIRED → wire=" + m.wire.getId() + 
                    ", progress=" + String.format("%.3f", m.progress));
            }
        }
        level.setAergiaMarks(toKeep);
        int marksAfterPrune = toKeep.size();
        
        // Clear cooldown if all marks expired and cooldown period is over
        if (toKeep.isEmpty() && level.getAergiaCooldownEnd() <= now && wasOnCooldown) {
            level.setAergiaCooldownEnd(0);
            java.lang.System.out.println("DEBUG: AERGIA COOLDOWN CLEARED → all marks expired and cooldown period over");
        }
        
        if (marksBeforePrune > marksAfterPrune || wasOnCooldown != level.isAergiaOnCooldown()) {
            java.lang.System.out.println("DEBUG: pruneExpiredMarks → marks: " + marksBeforePrune + " → " + marksAfterPrune + 
                ", cooldown: " + wasOnCooldown + " → " + level.isAergiaOnCooldown() +
                ", cooldownEnd: " + level.getAergiaCooldownEnd() + ", now: " + now);
        }
    }
}


