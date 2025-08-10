package model.entity.packets;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

public abstract class MassivePacket extends Packet {
    protected MassivePacket(String id, PacketType type, int coinValue, int health,
                            Point2D position, Point2D direction) {
        super(id, type, coinValue, position, direction, health);
    }

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
        double radius = 8.0;
        return new Circle(getPosition().getX(), getPosition().getY(), radius);
    }

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

    public static class Type2 extends MassivePacket {
        private static final double BASE_SPEED = 55.0;
        private static final double DEFLECTION_AMPLITUDE = 6.0; 

        public Type2(String id, Point2D position, Point2D direction) {
            super(id, PacketType.MASSIVE_TYPE2, 10, 10, position, direction);
        }

        @Override
        public void updateMovement(double deltaTimeSeconds, boolean compatiblePort) {
            super.updateMovement(deltaTimeSeconds, compatiblePort);

            if (getCurrentWire() == null) {
                return;
            }

            double progress = getMovementProgress();
            double amplitude = DEFLECTION_AMPLITUDE * Math.sin(Math.PI * progress);

            double h = 0.003;
            double p0 = Math.max(0.0, progress - h);
            double p1 = Math.min(1.0, progress + h);
            Point2D a = getCurrentWire().getPositionAtProgress(p0);
            Point2D b = getCurrentWire().getPositionAtProgress(p1);
            Point2D tangent = b.subtract(a);

            if (tangent.magnitude() <= 1e-6) {
                resetDeflection();
                return;
            }

            Point2D perp = new Point2D(-tangent.getY(), tangent.getX()).normalize();

            resetDeflection();
            applyDeflection(perp.getX() * amplitude, perp.getY() * amplitude);
        }

        @Override
        public double getSpeed() {
            return BASE_SPEED;
        }
    }
}



