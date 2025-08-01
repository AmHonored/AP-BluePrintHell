package manager.systems;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import model.entity.systems.StartSystem;
import model.entity.ports.Port;
import model.entity.packets.Packet;
import java.util.ArrayList;
import model.levels.Level;
import manager.packets.PacketManager;

public class StartSystemManager {
    private final StartSystem system;
    private final Timeline timer;
    private final ArrayList<Packet> generatedPackets = new ArrayList<>();
    private final Level level;
    private boolean started = false;

    public StartSystemManager(StartSystem system, Level level) {
        this.system = system;
        this.level = level;
        this.timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> transfer()));
        this.timer.setCycleCount(Timeline.INDEFINITE);
        // Don't auto-start the timer - wait for play button click
    }

    public void startPacketGeneration() {
        if (!started) {
            started = true;
            timer.play();
        }
    }

    public void stopPacketGeneration() {
        if (started) {
            started = false;
            timer.stop();
        }
    }

    public boolean isStarted() {
        return started;
    }

    private void transfer() {
        // Only generate packets if the game has been started
        if (level.isPaused() || !level.isGameStarted()) return;
        
        for (Port port : system.getOutPorts()) {
            if (port.isConnected()) {
                Packet packet = system.generatePacketIfPossible(port);
                if (packet != null) {
                    Port bestPort = system.findBestOutPort(packet);
                    if (bestPort != null) {
                        boolean sent = PacketManager.sendPacket(bestPort, packet);
                        if (sent) {
                            level.addPacket(packet);
                            
                            // Deliver to destination system
                            model.entity.systems.System destSystem = bestPort.getWire().getDest().getSystem();
                            deliverToSystem(packet, destSystem);
                        }
                    }
                }
            }
        }
    }
    
    private void deliverToSystem(Packet packet, model.entity.systems.System destSystem) {
        if (destSystem instanceof model.entity.systems.IntermediateSystem) {
            // Find the appropriate manager and deliver
            manager.systems.IntermediateSystemManager manager = 
                new manager.systems.IntermediateSystemManager((model.entity.systems.IntermediateSystem) destSystem);
            manager.receivePacket(packet);
        } else if (destSystem instanceof model.entity.systems.DDosSystem) {
            // Find the appropriate manager and deliver
            manager.systems.DDosSystemManager manager = 
                new manager.systems.DDosSystemManager((model.entity.systems.DDosSystem) destSystem);
            manager.receivePacket(packet);
        } else if (destSystem instanceof model.entity.systems.SpySystem) {
            // Find the appropriate manager and deliver
            manager.systems.SpySystemManager manager = 
                new manager.systems.SpySystemManager((model.entity.systems.SpySystem) destSystem);
            manager.receivePacket(packet);
        } else if (destSystem instanceof model.entity.systems.VPNSystem) {
            // Find the appropriate manager and deliver
            manager.systems.VPNSystemManager manager = 
                new manager.systems.VPNSystemManager((model.entity.systems.VPNSystem) destSystem);
            manager.setLevel(level);
            manager.receivePacket(packet);
        } else if (destSystem instanceof model.entity.systems.EndSystem) {
            manager.systems.EndSystemManager manager = 
                new manager.systems.EndSystemManager((model.entity.systems.EndSystem) destSystem, level);
            manager.receivePacket(packet);
        }
    }

    public ArrayList<Packet> getGeneratedPackets() {
        return generatedPackets;
    }
}
