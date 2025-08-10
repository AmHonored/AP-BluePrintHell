package model.wire;

import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;

public final class BendPath {
    private final List<Wire.BendPoint> bendPoints = new ArrayList<>();

    public boolean hasBendPoints() {
        return !bendPoints.isEmpty();
    }

    public List<Wire.BendPoint> getBendPoints() {
        return new ArrayList<>(bendPoints);
    }

    public boolean canAddBendPoint() {
        return bendPoints.size() < 3;
    }

    public boolean addBendPoint(Point2D position, double maxRadius, Point2D sourcePos, Point2D destPos) {
        if (!canAddBendPoint()) return false;
        Point2D defaultPosition = calculateDefaultBendPosition(sourcePos, destPos, bendPoints.size());
        Wire.BendPoint bendPoint = new Wire.BendPoint(defaultPosition, maxRadius);
        if (position != null) {
            bendPoint.setPosition(position);
        }
        bendPoints.add(bendPoint);
        return true;
    }

    public void removeBendPoint(int index) {
        if (index >= 0 && index < bendPoints.size()) {
            bendPoints.remove(index);
        }
    }

    public double computeLength(Point2D start, Point2D end) {
        if (start == null || end == null) return 0.0;
        if (bendPoints.isEmpty()) {
            return start.distance(end);
        }

        if (bendPoints.size() == 1) {
            Wire.BendPoint bendPoint = bendPoints.get(0);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            double curveStrength = 1.5;
            Point2D controlPoint = new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
            return approximateQuadCurveLength(start, controlPoint, end);
        }

        double totalLength = 0.0;
        List<Point2D> pathPoints = new ArrayList<>();
        pathPoints.add(start);
        for (Wire.BendPoint bendPoint : bendPoints) {
            pathPoints.add(bendPoint.getPosition());
        }
        pathPoints.add(end);

        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D segmentStart = pathPoints.get(i);
            Point2D segmentEnd = pathPoints.get(i + 1);
            Point2D controlPoint = calculateSegmentControlPoint(segmentStart, segmentEnd, i);
            totalLength += approximateQuadCurveLength(segmentStart, controlPoint, segmentEnd);
        }
        return totalLength;
    }

    public Point2D positionAt(double progress, Point2D start, Point2D end) {
        if (start == null || end == null) return new Point2D(0, 0);
        progress = Math.max(0.0, Math.min(1.0, progress));
        if (bendPoints.isEmpty()) {

            double x = start.getX() + progress * (end.getX() - start.getX());
            double y = start.getY() + progress * (end.getY() - start.getY());
            return new Point2D(x, y);
        }

        if (bendPoints.size() == 1) {
            Wire.BendPoint bendPoint = bendPoints.get(0);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            double curveStrength = 1.5;
            Point2D controlPoint = new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
            return getQuadraticBezierPoint(start, controlPoint, end, progress);
        }

        // Multiple segments: start -> bendPoints -> end
        List<Point2D> pathPoints = new ArrayList<>();
        pathPoints.add(start);
        for (Wire.BendPoint bendPoint : bendPoints) {
            pathPoints.add(bendPoint.getPosition());
        }
        pathPoints.add(end);

        List<Double> segmentLengths = new ArrayList<>();
        double totalLength = 0.0;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D s = pathPoints.get(i);
            Point2D e = pathPoints.get(i + 1);
            Point2D c = calculateSegmentControlPoint(s, e, i);
            double len = approximateQuadCurveLength(s, c, e);
            segmentLengths.add(len);
            totalLength += len;
        }
        if (totalLength <= 0.0) return start;

        double target = progress * totalLength;
        double acc = 0.0;
        for (int i = 0; i < segmentLengths.size(); i++) {
            double segLen = segmentLengths.get(i);
            if (acc + segLen >= target) {
                double segT = (target - acc) / segLen;
                Point2D s = pathPoints.get(i);
                Point2D e = pathPoints.get(i + 1);
                Point2D c = calculateSegmentControlPoint(s, e, i);
                return getQuadraticBezierPoint(s, c, e, segT);
            }
            acc += segLen;
        }
        return end;
    }

    private Point2D calculateSegmentControlPoint(Point2D start, Point2D end, int segmentIndex) {
        if (segmentIndex < bendPoints.size()) {
            Wire.BendPoint bendPoint = bendPoints.get(segmentIndex);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            double curveStrength = 1.2;
            return new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
        } else if (segmentIndex > 0 && segmentIndex <= bendPoints.size()) {
            Wire.BendPoint bendPoint = bendPoints.get(segmentIndex - 1);
            Point2D bendPos = bendPoint.getPosition();
            Point2D originalBendPos = bendPoint.getOriginalPosition();
            double offsetX = bendPos.getX() - originalBendPos.getX();
            double offsetY = bendPos.getY() - originalBendPos.getY();
            double midX = (start.getX() + end.getX()) / 2;
            double midY = (start.getY() + end.getY()) / 2;
            double curveStrength = 1.2;
            return new Point2D(midX + offsetX * curveStrength, midY + offsetY * curveStrength);
        }
        return new Point2D((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
    }

    private Point2D calculateDefaultBendPosition(Point2D sourcePos, Point2D destPos, int index) {
        int totalSegments = 3; // Always assume 3 segments for consistency
        double segmentRatio = (index + 1.0) / (totalSegments + 1.0);
        double x = sourcePos.getX() + segmentRatio * (destPos.getX() - sourcePos.getX());
        double y = sourcePos.getY() + segmentRatio * (destPos.getY() - sourcePos.getY());
        return new Point2D(x, y);
    }

    private Point2D getQuadraticBezierPoint(Point2D start, Point2D control, Point2D end, double t) {
        double oneMinusT = 1.0 - t;
        double x = oneMinusT * oneMinusT * start.getX() +
                   2.0 * oneMinusT * t * control.getX() +
                   t * t * end.getX();
        double y = oneMinusT * oneMinusT * start.getY() +
                   2.0 * oneMinusT * t * control.getY() +
                   t * t * end.getY();
        return new Point2D(x, y);
    }

    private double approximateQuadCurveLength(Point2D start, Point2D control, Point2D end) {
        double length = 0.0;
        int samples = 100;
        Point2D prev = start;
        for (int i = 1; i <= samples; i++) {
            double t = (double) i / samples;
            Point2D cur = getQuadraticBezierPoint(start, control, end, t);
            length += prev.distance(cur);
            prev = cur;
        }
        return length;
    }
}


