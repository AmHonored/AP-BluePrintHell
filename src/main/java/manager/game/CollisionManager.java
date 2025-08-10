package manager.game;

import javafx.geometry.Point2D;
import model.entity.packets.Packet;
import model.wire.Wire;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public final class CollisionManager {
	private CollisionManager() {}

    private static final double COLLISION_DEFLECT_MIN = 8.0;
    private static final double COLLISION_DEFLECT_MAX = 14.0;

    public static void applyCollisionDeflection(Packet a, Packet b) {
        if (a == null || b == null) return;
        Wire wire = a.getCurrentWire() != null ? a.getCurrentWire() : b.getCurrentWire();
        if (wire == null) return;
        double progressMid = 0.5 * (a.getMovementProgress() + b.getMovementProgress());
        Point2D perp = computeWirePerpendicular(wire, progressMid);
        if (perp == null) return;
        double magnitude = COLLISION_DEFLECT_MIN + new Random().nextDouble() * (COLLISION_DEFLECT_MAX - COLLISION_DEFLECT_MIN);
        a.applyDeflection(perp.getX() * magnitude, perp.getY() * magnitude);
        b.applyDeflection(-perp.getX() * magnitude, -perp.getY() * magnitude);
    }

    public static void cleanupOffWire(List<Packet> movingPackets, controller.PacketController packetController) {
        if (movingPackets == null || movingPackets.isEmpty()) return;
        List<Packet> toRemove = new ArrayList<>();
        for (Packet p : movingPackets) {
            if (isPacketOffWire(p)) toRemove.add(p);
        }
        if (!toRemove.isEmpty()) {
            for (Packet p : new HashSet<>(toRemove)) {
                movingPackets.remove(p);
                if (packetController != null) packetController.removePacket(p);
            }
        }
    }

    private static boolean isPacketOffWire(Packet packet) {
		double dx = packet.getDeflectedX();
		double dy = packet.getDeflectedY();
		double radial = Math.hypot(dx, dy);
		final double OFF_WIRE_THRESHOLD = 18.0;
		return radial > OFF_WIRE_THRESHOLD || packet.isDeflectionTooLarge();
	}

	private static Point2D computeWirePerpendicular(Wire wire, double progress) {
		if (wire == null) return null;
		double h = 0.003;
		double p0 = Math.max(0.0, progress - h);
		double p1 = Math.min(1.0, progress + h);
		Point2D a = wire.getPositionAtProgress(p0);
		Point2D b = wire.getPositionAtProgress(p1);
		Point2D tangent = b.subtract(a);
		if (tangent.magnitude() <= 1e-6) return null;
		return new Point2D(-tangent.getY(), tangent.getX()).normalize();
	}
}


