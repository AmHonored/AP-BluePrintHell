package serialization.save;

import java.util.List;
import java.util.Map;

public class LevelSave {
    public GameStateSave gameState;
    public LevelStateSave levelState;

    public int aergiaScrolls;
    public double aergiaSecondsRemaining; 
    public List<AergiaMarkSave> aergiaMarks;

    public int sisyphusScrolls;
    public int eliphasScrolls;
    public List<EliphasMarkSave> eliphasMarks;

    public List<SystemSave> systems;
    public List<WireSave> wires;
    public List<PacketSave> packets;

    public Map<String, List<String>> systemPacketQueues;
}


