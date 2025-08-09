package model.entity.packets;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

/**
 * Massive packets: circular packets with higher size/health and special behaviors.
 * - Type1 (MASSIVE_TYPE1): constant speed on straight wires; accelerates on curved wires.
 * - Type2 (MASSIVE_TYPE2): constant speed but visually deflected from the wire while moving.
 */
public abstract class MassivePacket extends Packet {
    protected MassivePacket(String id, PacketType type, int coinValue, int health,
                            Point2D position, Point2D direction) {
        super(id, type, coinValue, position, direction, health);
    }

    /**
     * Split this massive packet into bit packets.
     * Type1 → 8 circle bits (size=1), Type2 → 10 rect bits (size=1)
     */
    public java.util.List<Packet> splitIntoBits() {
        java.util.List<Packet> bits = new java.util.ArrayList<>();
        int count = (getType() == PacketType.MASSIVE_TYPE1) ? 8 : 10;
        boolean circleBits = (getType() == PacketType.MASSIVE_TYPE1);
        for (int i = 0; i < count; i++) {
            Packet bit;
            if (circleBits) {
                bit = new model.entity.packets.bits.BitCirclePacket(getId()+"-b"+i, getPosition(), getDirection());
            } else {
                bit = new model.entity.packets.bits.BitRectPacket(getId()+"-b"+i, getPosition(), getDirection());
            }
            bit.setBitFragment(true);
            bits.add(bit);
        }
        return bits;
    }

    @Override
    public Shape getCollisionShape() {
        // Circle collision matching visual representation
        double radius = 8.0;
        return new Circle(getPosition().getX(), getPosition().getY(), radius);
    }

    /**
     * Massive Packet Type 1: constant speed on straight wires, accelerates on curved wires.
     * Size/health: 8
     */
    public static class Type1 extends MassivePacket {
        private static final double BASE_SPEED = 55.0;
        private static final double MAX_SPEED = 120.0;
        private static final double ACCELERATION = 28.0;
        private double currentSpeed = BASE_SPEED;

        public Type1(String id, Point2D position, Point2D direction) {
            super(id, PacketType.MASSIVE_TYPE1, 8, 8, position, direction);
        }

        @Override
        public void updateMovement(double deltaTimeSeconds, boolean compatiblePort) {
            super.updateMovement(deltaTimeSeconds, compatiblePort);
            // Accelerate only if the wire is curved (has bend points). Otherwise, keep constant speed.
            if (isAergiaFrozenActive()) {
                double frozen = getAergiaFrozenSpeedOrNegative();
                if (frozen >= 0.0) currentSpeed = frozen;
            } else if (getCurrentWire() != null && getCurrentWire().hasBendPoints()) {
                currentSpeed += ACCELERATION * deltaTimeSeconds;
                if (currentSpeed > MAX_SPEED) currentSpeed = MAX_SPEED;
            } else {
                currentSpeed = BASE_SPEED;
            }
        }

        @Override
        public double getSpeed() {
            return currentSpeed;
        }
    }

    /**
     * Massive Packet Type 2: constant speed; smoothly deflected from the wire center along its path.
     * Deflection follows a single smooth lobe over the wire using progress-based easing (no shaking).
     * Size/health: 10
     */
    public static class Type2 extends MassivePacket {
        private static final double BASE_SPEED = 55.0;
        private static final double DEFLECTION_AMPLITUDE = 6.0; // pixels (max lateral offset)

        public Type2(String id, Point2D position, Point2D direction) {
            super(id, PacketType.MASSIVE_TYPE2, 10, 10, position, direction);
        }

        @Override
        public void updateMovement(double deltaTimeSeconds, boolean compatiblePort) {
            super.updateMovement(deltaTimeSeconds, compatiblePort);

            if (getCurrentWire() == null) {
                return;
            }

            // Smooth, progress-locked amplitude: 0 at ends, peak at middle
            double progress = getMovementProgress();
            double amplitude = DEFLECTION_AMPLITUDE * Math.sin(Math.PI * progress);

            // Central difference for stable tangent estimation
            double h = 0.003;
            double p0 = Math.max(0.0, progress - h);
            double p1 = Math.min(1.0, progress + h);
            Point2D a = getCurrentWire().getPositionAtProgress(p0);
            Point2D b = getCurrentWire().getPositionAtProgress(p1);
            Point2D tangent = b.subtract(a);

            if (tangent.magnitude() <= 1e-6) {
                // Fallback: no deflection if tangent is unreliable to avoid jitter
                resetDeflection();
                return;
            }

            Point2D perp = new Point2D(-tangent.getY(), tangent.getX()).normalize();

            // Reset and apply smooth lateral offset
            resetDeflection();
            applyDeflection(perp.getX() * amplitude, perp.getY() * amplitude);
        }

        @Override
        public double getSpeed() {
            return BASE_SPEED;
        }
    }
}



