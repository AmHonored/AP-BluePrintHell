package manager.packets;

import model.entity.packets.HexagonPacket;
import model.wire.Wire;

public final class HexagonPacketManager {
	private HexagonPacketManager() {}

	public static void initOnStart(HexagonPacket packet, Wire wire) {
		if (packet == null || wire == null) return;
		packet.setTotalPathLength(wire.getLength());
		packet.setDistanceTraveled(0.0);
	}

	public static double updateDistance(HexagonPacket packet, Wire wire, double deltaTimeSeconds, boolean inputCompatible) {
		if (packet == null || wire == null) return 0.0;
		
		double distanceTraveled = packet.getDistanceTraveled();
		if (distanceTraveled < 0.0) distanceTraveled = 0.0;
		double max = wire.getLength();
		if (distanceTraveled > max) distanceTraveled = max;
		packet.setDistanceTraveled(distanceTraveled);
		return distanceTraveled;
	}
}


