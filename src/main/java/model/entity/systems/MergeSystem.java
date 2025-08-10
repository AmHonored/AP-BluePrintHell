package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.packets.Packet;
import model.entity.packets.PacketType;
import model.entity.packets.bits.BitCirclePacket;
import model.entity.packets.bits.BitRectPacket;

import java.util.ArrayList;
import java.util.List;

public class MergeSystem extends System {
    private final List<BitCirclePacket> circleBits = new ArrayList<>();
    private final List<BitRectPacket> rectBits = new ArrayList<>();
    
    private List<Integer> sourceCounts = new ArrayList<>();
    
    private static final int CIRCLE_BITS_NEEDED = 8;
    private static final int RECT_BITS_NEEDED = 10;

    public MergeSystem(Point2D position) {
        super(position, SystemType.MergeSystem);
    }

    public void addBitPacket(Packet packet) {
        if (packet instanceof BitCirclePacket) {
            circleBits.add((BitCirclePacket) packet);
        } else if (packet instanceof BitRectPacket) {
            rectBits.add((BitRectPacket) packet);
        }
    }

    public boolean canCreateMassivePacket() {
        return circleBits.size() >= CIRCLE_BITS_NEEDED || 
               rectBits.size() >= RECT_BITS_NEEDED;
    }

    public PacketType getMassivePacketType() {
        if (circleBits.size() >= CIRCLE_BITS_NEEDED) {
            return PacketType.MASSIVE_TYPE1;
        } else if (rectBits.size() >= RECT_BITS_NEEDED) {
            return PacketType.MASSIVE_TYPE2;
        }
        return null;
    }

    public void removeBitsForMassive(PacketType type) {
        if (type == PacketType.MASSIVE_TYPE1 && circleBits.size() >= CIRCLE_BITS_NEEDED) {
            for (int i = 0; i < CIRCLE_BITS_NEEDED; i++) {
                circleBits.remove(0);
            }
        } else if (type == PacketType.MASSIVE_TYPE2 && rectBits.size() >= RECT_BITS_NEEDED) {
            for (int i = 0; i < RECT_BITS_NEEDED; i++) {
                rectBits.remove(0);
            }
        }
    }

    public double calculatePacketLoss() {
        if (sourceCounts.size() < 2) {
            return 0; 
        }
        
        int N = 0;
        for (int count : sourceCounts) {
            N += count;
        }

        int n1 = sourceCounts.get(0);
        int n2 = sourceCounts.get(1);
        int k = 2; 
        
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

    public void setSourceCounts(List<Integer> counts) {
        this.sourceCounts = new ArrayList<>(counts);
    }
}
