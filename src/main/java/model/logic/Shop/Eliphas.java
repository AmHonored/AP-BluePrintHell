package model.logic.Shop;

import model.levels.Level;
import model.wire.Wire;

public final class Eliphas implements ShopItem {
    public Eliphas() {}

    public static final long EFFECT_DURATION_NANOS = 30_000_000_000L; // 30s

    public static class EliphasMark {
        public final Wire wire;
        public final double progress; 
        public final long effectEndNanos; 

        public EliphasMark(Wire wire, double progress, long effectEndNanos) {
            this.wire = wire;
            this.progress = progress;
            this.effectEndNanos = effectEndNanos;
        }
    }

    public static void addMark(Level level, Wire wire, double progress) {
        if (level == null || wire == null) return;
        long now = java.lang.System.nanoTime();
        long effectEnd = now + EFFECT_DURATION_NANOS;
        level.getEliphasMarks().add(new EliphasMark(wire, progress, effectEnd));
    }

    public static void pruneExpiredMarks(Level level) {
        if (level == null) return;
        long now = java.lang.System.nanoTime();
        java.util.List<EliphasMark> toKeep = new java.util.ArrayList<>();
        for (EliphasMark m : level.getEliphasMarks()) {
            if (m.effectEndNanos > now) toKeep.add(m);
        }
        level.setEliphasMarks(toKeep);
    }

    @Override
    public String getName() { return "Scroll of Eliphas"; }

    @Override
    public int getPrice() { return 20; }

    @Override
    public int getDurationSeconds() { return 0; }

    @Override
    public void apply(Level level) {
        level.addEliphasScrolls(1);
    }
}


