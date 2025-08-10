package service;

import controller.GameController;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.layout.Pane;
import javafx.geometry.Point2D;
import manager.game.LevelManager;
import manager.packets.PacketManager;
import model.entity.packets.Packet;
import model.entity.packets.PacketType;
import model.entity.ports.Port;
import model.entity.systems.IntermediateSystem;
import model.entity.systems.System;
import model.levels.Level;
import model.logic.Shop.AergiaLogic;
import model.wire.Wire;
import repository.SaveRepository;
import repository.json.JsonSaveRepository;
import serialization.save.*;

/**
 * Handles building save-game snapshots and restoring them.
 * Keep code straightforward and readable; avoid over-optimization.
 */
public class SaveService {
    private final SaveRepository repository;
    private final String defaultProfileId;

    private Timeline autosaveTimeline;

    public SaveService() {
        this(new JsonSaveRepository(Paths.get("saves")), "default");
    }

    public SaveService(SaveRepository repository, String defaultProfileId) {
        this.repository = repository;
        this.defaultProfileId = defaultProfileId;
    }

    public void attachAutosave(Level level, String levelId, double intervalSeconds) {
        if (autosaveTimeline != null) {
            autosaveTimeline.stop();
        }
        autosaveTimeline = new Timeline(new KeyFrame(Duration.seconds(intervalSeconds), e -> saveNow(level, defaultProfileId, levelId)));
        autosaveTimeline.setCycleCount(Timeline.INDEFINITE);
        autosaveTimeline.play();
    }

    public void stopAutosave() {
        if (autosaveTimeline != null) autosaveTimeline.stop();
    }

    public void saveNow(Level level, String profileId, String levelId) {
        SaveGame root = new SaveGame();
        root.schemaVersion = 1;
        root.profileId = profileId;
        root.levelId = levelId;
        root.savedAtEpochMillis = Instant.now().toEpochMilli();
        root.level = snapshotLevel(level);
        repository.saveAtomic(profileId, levelId, root);
    }

    public Optional<LevelSave> tryLoadLevelSave(String profileId, String levelId) {
        return repository.loadLatest(profileId, levelId).map(s -> s.level);
    }

    /**
     * Apply core dynamic state only. This keeps implementation minimal and safe to call
     * before controllers/managers are initialized. Full reconstruction (wires/packets)
     * can be layered later after views/managers exist.
     */
    public void applyBasicToLevel(Level level, LevelSave save) {
        if (level == null || save == null) return;

        // Game state
        if (save.gameState != null) {
            level.setPaused(save.gameState.paused);
            level.setGameOver(save.gameState.gameOver);
            level.setGameStarted(save.gameState.gameStarted);
            level.setCurrentTime(save.gameState.currentTime);

            int currentCoins = level.getCoins();
            int targetCoins = save.gameState.coins;
            if (targetCoins > currentCoins) {
                level.addCoins(targetCoins - currentCoins);
            } else if (targetCoins < currentCoins) {
                // Use GameState API to subtract since Level doesn't expose subtract
                level.getGameState().subtractCoins(currentCoins - targetCoins);
            }
        }

        // Level state
        if (save.levelState != null) {
            // Adjust remaining wire length by diff
            double currentRemaining = level.getRemainingWireLength();
            double targetRemaining = save.levelState.remainingWireLength;
            if (targetRemaining > currentRemaining) {
                level.addWireLength(targetRemaining - currentRemaining);
            } else if (targetRemaining < currentRemaining) {
                level.subtractWireLength(currentRemaining - targetRemaining);
            }
            // Stats
            while (level.getPacketsGenerated() < save.levelState.packetsGenerated) level.incrementPacketsGenerated();
            while (level.getPacketLoss() < save.levelState.packetLoss) level.incrementPacketLoss();
            while (level.getPacketsCollected() < save.levelState.packetsCollected) level.incrementPacketsCollected();
            level.setImpactDisabled(save.levelState.impactDisabled);
            level.setCollisionsDisabled(save.levelState.collisionsDisabled);
            long nowN = java.lang.System.nanoTime();
            if (save.levelState.impactSecondsRemaining > 0) {
                level.getLevelState().setImpactDisableEndNanos(nowN + (long)(save.levelState.impactSecondsRemaining * 1_000_000_000L));
            }
            if (save.levelState.collisionsSecondsRemaining > 0) {
                level.getLevelState().setCollisionsDisableEndNanos(nowN + (long)(save.levelState.collisionsSecondsRemaining * 1_000_000_000L));
            }
        }

        // Inventory/effects exact restoration
        int deltaA = save.aergiaScrolls - level.getAergiaScrolls();
        if (deltaA != 0) level.addAergiaScrolls(deltaA);
        int deltaS = save.sisyphusScrolls - level.getSisyphusScrolls();
        if (deltaS != 0) level.addSisyphusScrolls(deltaS);
        int deltaE = save.eliphasScrolls - level.getEliphasScrolls();
        if (deltaE != 0) level.addEliphasScrolls(deltaE);

        long now = java.lang.System.nanoTime();
        if (save.aergiaSecondsRemaining > 0) {
            level.setAergiaCooldownEnd(now + (long) (save.aergiaSecondsRemaining * 1_000_000_000L));
        }
        // Marks will be reconstructed in full restore step (future); skipping here keeps it simple.
    }

    /**
     * Apply saved system positions and adjust their ports by the same delta
     * so wires and hitboxes remain aligned.
     */
    public void applySystemPositions(Level level, LevelSave save) {
        if (level == null || save == null || save.systems == null) return;
        // Index systems by stable id
        Map<String, System> systemsById = new HashMap<>();
        for (System s : level.getSystems()) {
            if (s.getId() != null) systemsById.put(s.getId(), s);
        }
        for (SystemSave ss : save.systems) {
            if (ss == null || ss.id == null) continue;
            System sys = systemsById.get(ss.id);
            if (sys == null) continue;
            javafx.geometry.Point2D current = sys.getPosition();
            javafx.geometry.Point2D target = new javafx.geometry.Point2D(ss.x, ss.y);
            if (current == null) continue;
            double dx = target.getX() - current.getX();
            double dy = target.getY() - current.getY();
            if (Math.abs(dx) < 1e-6 && Math.abs(dy) < 1e-6) continue;
            sys.setPosition(target);
            // Move all ports by the same delta
            for (Port p : sys.getInPorts()) {
                p.setPosition(new javafx.geometry.Point2D(p.getPosition().getX() + dx, p.getPosition().getY() + dy));
            }
            for (Port p : sys.getOutPorts()) {
                p.setPosition(new javafx.geometry.Point2D(p.getPosition().getX() + dx, p.getPosition().getY() + dy));
            }
        }
    }

    private LevelSave snapshotLevel(Level level) {
        LevelSave out = new LevelSave();

        // Game state
        GameStateSave gs = new GameStateSave();
        gs.paused = level.isPaused();
        gs.gameOver = level.isGameOver();
        gs.gameStarted = level.isGameStarted();
        gs.currentTime = level.getCurrentTime();
        gs.coins = level.getCoins();
        out.gameState = gs;

        // Level state
        LevelStateSave ls = new LevelStateSave();
        ls.remainingWireLength = level.getRemainingWireLength();
        ls.packetsGenerated = level.getPacketsGenerated();
        ls.packetLoss = level.getPacketLoss();
        ls.packetsCollected = level.getPacketsCollected();
        ls.impactDisabled = level.isImpactDisabled();
        ls.collisionsDisabled = level.isCollisionsDisabled();
        try {
            long nowN = java.lang.System.nanoTime();
            long impactEnd = level.getLevelState().getImpactDisableEndNanos();
            long collEnd = level.getLevelState().getCollisionsDisableEndNanos();
            ls.impactSecondsRemaining = impactEnd > nowN ? (impactEnd - nowN) / 1_000_000_000.0 : 0.0;
            ls.collisionsSecondsRemaining = collEnd > nowN ? (collEnd - nowN) / 1_000_000_000.0 : 0.0;
        } catch (Throwable ignored) {}
        out.levelState = ls;

        // Inventory/effects
        out.aergiaScrolls = level.getAergiaScrolls();
        long now = java.lang.System.nanoTime();
        long cooldownEnd = level.getAergiaCooldownEnd();
        out.aergiaSecondsRemaining = cooldownEnd > now ? (cooldownEnd - now) / 1_000_000_000.0 : 0.0;

        out.aergiaMarks = new java.util.ArrayList<>();
        for (AergiaLogic.AergiaMark m : level.getAergiaMarks()) {
            AergiaMarkSave ms = new AergiaMarkSave();
            ms.wireId = (m.wire != null ? m.wire.getId() : null);
            ms.progress = m.progress;
            ms.secondsRemaining = m.effectEndNanos > now ? (m.effectEndNanos - now) / 1_000_000_000.0 : 0.0;
            out.aergiaMarks.add(ms);
        }
        out.sisyphusScrolls = level.getSisyphusScrolls();
        out.eliphasScrolls = level.getEliphasScrolls();
        out.eliphasMarks = new java.util.ArrayList<>();
        for (model.logic.Shop.EliphasLogic.EliphasMark em : level.getEliphasMarks()) {
            EliphasMarkSave ems = new EliphasMarkSave();
            ems.wireId = (em.wire != null ? em.wire.getId() : null);
            ems.progress = em.progress;
            ems.secondsRemaining = em.effectEndNanos > now ? (em.effectEndNanos - now) / 1_000_000_000.0 : 0.0;
            out.eliphasMarks.add(ems);
        }

        // Systems
        out.systems = new java.util.ArrayList<>();
        for (System s : level.getSystems()) {
            SystemSave ss = new SystemSave();
            ss.id = (s.getId() != null ? s.getId() : s.getType().name() + "@" + Integer.toHexString(java.lang.System.identityHashCode(s)));
            ss.type = s.getType().name();
            ss.x = s.getPosition().getX();
            ss.y = s.getPosition().getY();
            ss.ready = s.isReady();
            out.systems.add(ss);
        }

        // Ports
        out.ports = new java.util.ArrayList<>();
        for (System s : level.getSystems()) {
            for (Port p : s.getInPorts()) out.ports.add(portToSave(p, s));
            for (Port p : s.getOutPorts()) out.ports.add(portToSave(p, s));
        }

        // Wires (collect via ConnectionManager if accessible, else via port references)
        out.wires = new java.util.ArrayList<>();
        java.util.Set<Wire> wires = new java.util.HashSet<>();
        for (System s : level.getSystems()) {
            for (Port p : s.getInPorts()) if (p.getWire() != null) wires.add(p.getWire());
            for (Port p : s.getOutPorts()) if (p.getWire() != null) wires.add(p.getWire());
        }
        for (Wire w : wires) {
            WireSave ws = new WireSave();
            ws.id = w.getId();
            ws.sourcePortId = w.getSource() != null ? w.getSource().getId() : null;
            ws.destPortId = w.getDest() != null ? w.getDest().getId() : null;
            ws.active = w.isActive();
            ws.massivePacketRunCount = w.getMassivePacketRunCount();
            ws.bendPoints = new java.util.ArrayList<>();
            for (Wire.BendPoint bp : w.getBendPoints()) {
                WireSave.BendPointSave bps = new WireSave.BendPointSave();
                bps.x = bp.getPosition().getX();
                bps.y = bp.getPosition().getY();
                bps.maxRadius = bp.getMaxRadius();
                ws.bendPoints.add(bps);
            }
            out.wires.add(ws);
        }

        // Packets
        out.packets = new java.util.ArrayList<>();
        for (Packet p : level.getPackets()) {
            out.packets.add(packetToSave(p));
        }

        // Queues
        out.systemPacketQueues = new HashMap<>();
        for (System s : level.getSystems()) {
            if (s instanceof IntermediateSystem) {
                List<String> ids = new java.util.ArrayList<>();
                for (Packet p : ((IntermediateSystem) s).getPackets()) ids.add(p.getId());
                String key = (s.getId() != null ? s.getId() : s.getType().name());
                out.systemPacketQueues.put(key, ids);
            }
        }

        return out;
    }

    /**
     * Restore wires (model + visuals) from a saved snapshot.
     * Keeps it simple: sets wires on ports and draws basic WireView without extra callbacks.
     */
    public void restoreWires(Level level, LevelSave save, Pane gamePane) {
        if (level == null || save == null || save.wires == null || gamePane == null) return;

        // Build lookup of ports by id
        Map<String, Port> portById = new HashMap<>();
        for (System s : level.getSystems()) {
            for (Port p : s.getInPorts()) portById.put(p.getId(), p);
            for (Port p : s.getOutPorts()) portById.put(p.getId(), p);
        }

        for (WireSave ws : save.wires) {
            Port src = ws.sourcePortId != null ? portById.get(ws.sourcePortId) : null;
            Port dst = ws.destPortId != null ? portById.get(ws.destPortId) : null;
            if (src == null || dst == null) continue;

            Wire wire = new Wire(ws.id != null ? ws.id : java.util.UUID.randomUUID().toString(), src, dst);
            wire.setActive(ws.active);
            // Attach to ports
            src.setWire(wire);
            dst.setWire(wire);

            // Restore bend points
            if (ws.bendPoints != null) {
                for (WireSave.BendPointSave bps : ws.bendPoints) {
                    Point2D pos = new Point2D(bps.x, bps.y);
                    double maxR = bps.maxRadius > 0 ? bps.maxRadius : 300.0;
                    wire.addBendPoint(pos, maxR);
                }
            }

            // Draw basic wire view
            try {
                view.components.wires.WireView view = new view.components.wires.WireView(wire);
                gamePane.getChildren().add(view);
            } catch (Throwable ignored) {
                // If view cannot be created (e.g., not in JavaFX thread), skip visuals
            }
        }
        // Recompute remaining wire length based on restored wires
        double used = 0.0;
        for (System s : level.getSystems()) {
            for (Port p : s.getInPorts()) if (p.getWire() != null) used += p.getWire().getLength();
        }
        // Each wire counted once via input ports; ensure no double counting
        double targetRemaining = Math.max(0.0, level.getWireLength() - used);
        double currentRemaining = level.getRemainingWireLength();
        if (Math.abs(targetRemaining - currentRemaining) > 1e-6) {
            if (targetRemaining > currentRemaining) level.addWireLength(targetRemaining - currentRemaining);
            else level.subtractWireLength(currentRemaining - targetRemaining);
        }
    }

    /**
     * Restore moving packets and system queues.
     * This must be called after wires are restored so wire ids resolve.
     */
    public void restorePackets(Level level, LevelSave save) {
        if (level == null || save == null) return;

        // Build quick lookups
        Map<String, Wire> wireById = new HashMap<>();
        for (System s : level.getSystems()) {
            for (Port p : s.getInPorts()) if (p.getWire() != null) wireById.put(p.getWire().getId(), p.getWire());
            for (Port p : s.getOutPorts()) if (p.getWire() != null) wireById.put(p.getWire().getId(), p.getWire());
        }

        // Recreate packets-in-network
        if (save.packets != null) {
            for (PacketSave ps : save.packets) {
                Packet packet = createPacketFromSave(ps);
                if (packet == null) continue;
                packet.setCurrentHealth(ps.currentHealth);
                packet.setInSystem(ps.inSystem);
                packet.setMoving(false);

                // Aergia effect
                if (ps.aergiaSecondsRemaining > 0.0) {
                    long end = java.lang.System.nanoTime() + (long)(ps.aergiaSecondsRemaining * 1_000_000_000L);
                    double frozen = ps.aergiaFrozenSpeed >= 0.0 ? ps.aergiaFrozenSpeed : packet.getSpeed();
                    packet.setAergiaFreeze(frozen, end);
                }

                // If assigned to a wire and was moving, resume movement from saved progress
                if (ps.currentWireId != null && ps.moving) {
                    Wire w = wireById.get(ps.currentWireId);
                    if (w != null) {
                        PacketManager.startMovement(packet, w, true);
                        // Apply saved movement state
                        packet.setMovementProgress(ps.movementProgress);
                        long now = java.lang.System.nanoTime();
                        long start = now - (long)(ps.secondsSinceMovementStart * 1_000_000_000L);
                        packet.setMovementStartTime(start);
                        // Set position to match progress
                        javafx.geometry.Point2D pos = w.getPositionAtProgress(ps.movementProgress);
                        packet.setPosition(pos);

                        // Hexagon-specific fields
                        if (ps.type != null && ps.type.contains("HEXAGON")) {
                            double total = w.getLength();
                            trySetDouble(packet, "totalPathLength", total);
                            trySetDouble(packet, "distanceTraveled", Math.max(0.0, ps.movementProgress * total));
                            if (ps.extra != null && ps.extra.get("movementState") instanceof String) {
                                trySetEnum(packet, "movementState", (String) ps.extra.get("movementState"));
                            }
                            if (ps.extra != null && ps.extra.get("currentSpeed") instanceof Number) {
                                trySetDouble(packet, "currentSpeed", ((Number) ps.extra.get("currentSpeed")).doubleValue());
                            }
                        }
                        if (ps.type != null && ps.type.contains("TRIANGLE") && ps.extra != null && ps.extra.get("currentSpeed") instanceof Number) {
                            trySetDouble(packet, "currentSpeed", ((Number) ps.extra.get("currentSpeed")).doubleValue());
                        }
                    }
                }
            }
        }

        // Restore system queues (Intermediate, etc.)
        if (save.systemPacketQueues != null) {
            // Map packets by id from level list
            Map<String, Packet> byId = new HashMap<>();
            for (Packet p : level.getPackets()) byId.put(p.getId(), p);

            Map<String, System> systemsById = new HashMap<>();
            for (System s : level.getSystems()) if (s.getId() != null) systemsById.put(s.getId(), s);

            for (Map.Entry<String, java.util.List<String>> e : save.systemPacketQueues.entrySet()) {
                System sys = systemsById.get(e.getKey());
                if (sys instanceof IntermediateSystem) {
                    IntermediateSystem isys = (IntermediateSystem) sys;
                    for (String pid : e.getValue()) {
                        Packet p = byId.get(pid);
                        if (p != null) isys.enqueuePacket(p);
                    }
                }
            }
        }
    }

    private Packet createPacketFromSave(PacketSave s) {
        if (s == null || s.type == null || s.id == null) return null;
        javafx.geometry.Point2D pos = new javafx.geometry.Point2D(s.x, s.y);
        javafx.geometry.Point2D dir = new javafx.geometry.Point2D(s.dirX, s.dirY);
        try {
            switch (model.entity.packets.PacketType.valueOf(s.type)) {
                case SQUARE:
                    return new model.entity.packets.SquarePacket(s.id, pos, dir);
                case TRIANGLE:
                    return new model.entity.packets.TrianglePacket(s.id, pos, dir);
                case HEXAGON:
                    return new model.entity.packets.HexagonPacket(s.id, pos, dir);
                default:
                    return new model.entity.packets.SquarePacket(s.id, pos, dir);
            }
        } catch (IllegalArgumentException ex) {
            return new model.entity.packets.SquarePacket(s.id, pos, dir);
        }
    }

    private void trySetDouble(Object obj, String field, double value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException ignored) {}
    }

    private void trySetEnum(Object obj, String field, String enumName) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Class<?> enumType = f.getType();
            Object enumValue = java.lang.Enum.valueOf((Class) enumType, enumName);
            f.set(obj, enumValue);
        } catch (ReflectiveOperationException ignored) {}
    }

    private PortSave portToSave(Port p, System system) {
        PortSave ps = new PortSave();
        ps.id = p.getId();
        ps.systemId = (system.getId() != null ? system.getId() : system.getType().name());
        ps.role = p.getType().name();
        ps.shapeKind = p.getShapeKind().name();
        ps.x = p.getPosition().getX();
        ps.y = p.getPosition().getY();
        ps.wireId = p.getWire() != null ? p.getWire().getId() : null;
        return ps;
    }

    private Wire findWireById(Level level, String wireId) {
        // Currently wires aren't globally registered; derive from ports
        for (System s : level.getSystems()) {
            for (Port p : s.getInPorts()) if (p.getWire() != null && p.getWire().getId().equals(wireId)) return p.getWire();
            for (Port p : s.getOutPorts()) if (p.getWire() != null && p.getWire().getId().equals(wireId)) return p.getWire();
        }
        return null;
    }

    private PacketSave packetToSave(Packet p) {
        PacketSave s = new PacketSave();
        s.id = p.getId();
        s.type = p.getType().name();
        s.x = p.getPosition().getX();
        s.y = p.getPosition().getY();
        s.dirX = p.getDirection().getX();
        s.dirY = p.getDirection().getY();
        s.currentHealth = p.getCurrentHealth();
        s.inSystem = p.isInSystem();
        s.moving = p.isMoving();
        if (p.getStartPosition() != null) {
            s.startX = p.getStartPosition().getX();
            s.startY = p.getStartPosition().getY();
        }
        if (p.getTargetPosition() != null) {
            s.targetX = p.getTargetPosition().getX();
            s.targetY = p.getTargetPosition().getY();
        }
        s.currentWireId = p.getCurrentWire() != null ? p.getCurrentWire().getId() : null;
        s.movementProgress = p.getMovementProgress();
        long now = java.lang.System.nanoTime();
        long start = p.getMovementStartTime();
        s.secondsSinceMovementStart = start > 0 ? (now - start) / 1_000_000_000.0 : 0.0;
        s.compatibleWithCurrentPort = p.isCompatibleWithCurrentPort();
        s.deflectedX = p.getDeflectedX();
        s.deflectedY = p.getDeflectedY();
        s.noise = p.getNoise();
        s.trojan = p.isTrojan();
        s.bitFragment = p.isBitFragment();
        s.aergiaFrozenSpeed = p.getAergiaFrozenSpeedOrNegative();
        double aergiaRemaining = p.getAergiaEffectEndNanos() > now ? (p.getAergiaEffectEndNanos() - now) / 1_000_000_000.0 : 0.0;
        s.aergiaSecondsRemaining = Math.max(0.0, aergiaRemaining);

        // Type-specific extras
        s.extra = new HashMap<>();
        if (p.getType() == PacketType.TRIANGLE) {
            s.extra.put("currentSpeed", tryGetDouble(p, "currentSpeed"));
        } else if (p.getType() == PacketType.HEXAGON) {
            s.extra.put("currentSpeed", tryGetDouble(p, "currentSpeed"));
            s.extra.put("movementState", tryGetEnumName(p, "movementState"));
            s.extra.put("distanceTraveled", tryGetDouble(p, "distanceTraveled"));
            s.extra.put("totalPathLength", tryGetDouble(p, "totalPathLength"));
        }

        return s;
    }

    // Reflection helpers: keep simple; return null if not present
    private Double tryGetDouble(Object obj, String field) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v instanceof Number ? ((Number) v).doubleValue() : null;
        } catch (ReflectiveOperationException e) { return null; }
    }

    private String tryGetEnumName(Object obj, String field) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v != null ? v.toString() : null;
        } catch (ReflectiveOperationException e) { return null; }
    }
}


