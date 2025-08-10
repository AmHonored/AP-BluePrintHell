package manager.packets;

public final class TrianglePacketManager {
	private TrianglePacketManager() {}

	public static void onStart(model.entity.packets.TrianglePacket packet, boolean isCompatible) {
		if (packet == null) return;
		if (!isCompatible) {
			packet.resetSpeed();
		}
	}
}


