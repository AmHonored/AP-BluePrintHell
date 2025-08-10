package model.logic.Shop;

import javafx.geometry.Point2D;
import model.levels.Level;
import model.wire.Wire;

import java.util.ArrayList;
import java.util.List;

public final class Aergia implements ShopItem {
    public Aergia() {}

    public static final long EFFECT_DURATION_NANOS = 20_000_000_000L;
    public static final long COOLDOWN_DURATION_NANOS = 10_000_000_000L; 

    public static class AergiaMark {
        public final Wire wire;
        public final double progress; 
        public final long effectEndNanos; 

        public AergiaMark(Wire wire, double progress, long effectEndNanos) {
            this.wire = wire;
            this.progress = progress;
            this.effectEndNanos = effectEndNanos;
        }
    }

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

    public static void addMark(Level level, Wire wire, double progress) {
        long now = java.lang.System.nanoTime();
        long effectEnd = now + EFFECT_DURATION_NANOS;
        long cooldownEnd = effectEnd + COOLDOWN_DURATION_NANOS;
        
        level.getAergiaMarks().add(new AergiaMark(wire, progress, effectEnd));
        level.setAergiaCooldownEnd(cooldownEnd);
    }

    public static void pruneExpiredMarks(Level level) {
        long now = java.lang.System.nanoTime();
        boolean wasOnCooldown = level.isAergiaOnCooldown();
        
        List<AergiaMark> toKeep = new ArrayList<>();
        for (AergiaMark m : level.getAergiaMarks()) {
            if (m.effectEndNanos > now) {
                toKeep.add(m);
            }
        }
        level.setAergiaMarks(toKeep);
        
        if (toKeep.isEmpty() && level.getAergiaCooldownEnd() <= now && wasOnCooldown) {
            level.setAergiaCooldownEnd(0);
        }
    }

    @Override
    public String getName() { return "Scroll of Aergia"; }

    @Override
    public int getPrice() { return 10; }

    @Override
    public int getDurationSeconds() { return 0; }

    @Override
    public void apply(Level level) {
        level.addAergiaScrolls(1);
    }
}


