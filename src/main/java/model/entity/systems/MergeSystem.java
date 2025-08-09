package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.packets.Packet;
import model.entity.packets.PacketType;
import model.entity.packets.bits.BitCirclePacket;
import model.entity.packets.bits.BitRectPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Merge system: collects bit packets and merges them into massive packets.
 * - Collects 8 circle bits to create a Massive Type1 packet
 * - Collects 10 rect bits to create a Massive Type2 packet
 * Packet loss formula: N - k√(n1 × n2) where n1, n2 are counts from different sources
 */
public class MergeSystem extends System {
    private final List<BitCirclePacket> circleBits = new ArrayList<>();
    private final List<BitRectPacket> rectBits = new ArrayList<>();
    
    // For packet loss calculation when merging from multiple sources
    private List<Integer> sourceCounts = new ArrayList<>();
    
    private static final int CIRCLE_BITS_NEEDED = 8;
    private static final int RECT_BITS_NEEDED = 10;

    public MergeSystem(Point2D position) {
        super(position, SystemType.MergeSystem);
    }

    /**
     * Add a bit packet to storage
     */
    public void addBitPacket(Packet packet) {
        if (packet instanceof BitCirclePacket) {
            circleBits.add((BitCirclePacket) packet);
        } else if (packet instanceof BitRectPacket) {
            rectBits.add((BitRectPacket) packet);
        }
    }

    /**
     * Check if system can create a massive packet
     */
    public boolean canCreateMassivePacket() {
        return circleBits.size() >= CIRCLE_BITS_NEEDED || 
               rectBits.size() >= RECT_BITS_NEEDED;
    }

    /**
     * Get the type of massive packet that can be created
     */
    public PacketType getMassivePacketType() {
        if (circleBits.size() >= CIRCLE_BITS_NEEDED) {
            return PacketType.MASSIVE_TYPE1;
        } else if (rectBits.size() >= RECT_BITS_NEEDED) {
            return PacketType.MASSIVE_TYPE2;
        }
        return null;
    }

    /**
     * Remove bits used to create a massive packet
     */
    public List<Packet> removeBitsForMassive(PacketType type) {
        List<Packet> removedBits = new ArrayList<>();
        if (type == PacketType.MASSIVE_TYPE1 && circleBits.size() >= CIRCLE_BITS_NEEDED) {
            for (int i = 0; i < CIRCLE_BITS_NEEDED; i++) {
                removedBits.add(circleBits.remove(0));
            }
        } else if (type == PacketType.MASSIVE_TYPE2 && rectBits.size() >= RECT_BITS_NEEDED) {
            for (int i = 0; i < RECT_BITS_NEEDED; i++) {
                removedBits.add(rectBits.remove(0));
            }
        }
        return removedBits;
    }

    /**
     * Calculate packet loss using formula: N - k√(n1 × n2)
     * For now, this returns 0 as we need multiple merge systems feeding into one
     */
    public double calculatePacketLoss() {
        if (sourceCounts.size() < 2) {
            return 0; // No packet loss for single source
        }
        
        // Calculate N (total bits)
        int N = 0;
        for (int count : sourceCounts) {
            N += count;
        }
        
        // For simplicity, using first two sources for n1 and n2
        // In practice, this might need more complex handling
        int n1 = sourceCounts.get(0);
        int n2 = sourceCounts.get(1);
        int k = 2; // As shown in the example
        
        return N - k * Math.sqrt(n1 * n2);
    }

    public int getCircleBitCount() {
        return circleBits.size();
    }

    public int getRectBitCount() {
        return rectBits.size();
    }

    public int getTotalBitCount() {
        return circleBits.size() + rectBits.size();
    }

    /**
     * Set source counts for packet loss calculation
     */
    public void setSourceCounts(List<Integer> counts) {
        this.sourceCounts = new ArrayList<>(counts);
    }
}
