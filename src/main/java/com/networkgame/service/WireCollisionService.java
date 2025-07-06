package com.networkgame.service;

import javafx.geometry.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Service for handling wire collision calculations without direct dependencies.
 * This breaks the cyclic dependency between Connection and WireCollisionManager.
 */
public class WireCollisionService {
    
    /**
     * Represents a bend point in a wire
     */
    public static class BendPoint {
        private final Point2D position;
        
        public BendPoint(Point2D position) {
            this.position = position;
        }
        
        public Point2D getPosition() {
            return position;
        }
    }
    
    /**
     * Calculate the actual wire length including bend points.
     * @param sourcePosition The source position of the wire
     * @param targetPosition The target position of the wire
     * @param bendPoints List of bend points along the wire
     * @return The total wire length including all segments
     */
    public static double calculateActualWireLength(Point2D sourcePosition, Point2D targetPosition, List<BendPoint> bendPoints) {
        if (bendPoints == null || bendPoints.isEmpty()) {
            // No bend points, return direct distance
            return sourcePosition.distance(targetPosition);
        }
        
        // Calculate total length through all bend points
        double totalLength = 0.0;
        Point2D currentPoint = sourcePosition;
        
        // Add length from source to first bend point, then between bend points, then to target
        for (BendPoint bendPoint : bendPoints) {
            Point2D bendPosition = bendPoint.getPosition();
            totalLength += currentPoint.distance(bendPosition);
            currentPoint = bendPosition;
        }
        
        // Add length from last bend point to target
        totalLength += currentPoint.distance(targetPosition);
        
        return totalLength;
    }
    
    /**
     * Calculate the distance from a point to a line segment
     * @param point The point to check
     * @param lineStart Start of the line segment
     * @param lineEnd End of the line segment
     * @return Distance from point to line segment
     */
    public static double distanceToLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd) {
        double A = point.getX() - lineStart.getX();
        double B = point.getY() - lineStart.getY();
        double C = lineEnd.getX() - lineStart.getX();
        double D = lineEnd.getY() - lineStart.getY();

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        
        if (lenSq == 0) {
            // Line segment is a point
            return point.distance(lineStart);
        }
        
        double param = dot / lenSq;

        Point2D closestPoint;
        if (param < 0) {
            closestPoint = lineStart;
        } else if (param > 1) {
            closestPoint = lineEnd;
        } else {
            closestPoint = new Point2D(
                lineStart.getX() + param * C,
                lineStart.getY() + param * D
            );
        }

        return point.distance(closestPoint);
    }
    
    /**
     * Check if a point lies on a line segment within a tolerance
     * @param point The point to check
     * @param lineStart Start of the line segment
     * @param lineEnd End of the line segment
     * @param tolerance Distance tolerance
     * @return true if point is on the line segment within tolerance
     */
    public static boolean isPointOnLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd, double tolerance) {
        double distanceToLine = distanceToLineSegment(point, lineStart, lineEnd);
        double distanceStartToEnd = lineStart.distance(lineEnd);
        double distanceStartToPoint = lineStart.distance(point);
        double distanceEndToPoint = lineEnd.distance(point);
        
        return distanceToLine < tolerance && 
               distanceStartToPoint + distanceEndToPoint <= distanceStartToEnd + 0.1;
    }
} 