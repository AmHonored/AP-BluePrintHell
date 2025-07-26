package manager.game;

import javafx.animation.AnimationTimer;
import manager.packets.PacketManager;

public class MovementManager extends AnimationTimer {
    private long lastUpdateTime = 0;
    
    @Override
    public void handle(long currentTimeNanos) {
        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTimeNanos;
            return;
        }
        
        double deltaTimeSeconds = (currentTimeNanos - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = currentTimeNanos;
        
        PacketManager.updateMovingPackets(deltaTimeSeconds);
    }
    
    public void startMovementUpdates() {
        start();
    }
    
    public void stopMovementUpdates() {
        stop();
    }
    
    public boolean isRunning() {
        // AnimationTimer doesn't have a direct isRunning method, 
        // so we track state through start/stop calls
        // For now, we assume it's running if start() has been called
        return true;
    }
} 