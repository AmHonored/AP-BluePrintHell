package manager.packets;

import model.entity.packets.Packet;

public final class ProtectedPacketManager {
	private ProtectedPacketManager() {}

	public static void convert(Packet oldPacket, Packet newPacket) {
		PacketManager.replacePacket(oldPacket, newPacket);
	}
}


