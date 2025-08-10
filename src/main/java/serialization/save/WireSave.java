package serialization.save;

import java.util.List;

public class WireSave {
    public String id;
    public String sourcePortId;
    public String destPortId;
    public boolean active;
    public int massivePacketRunCount;
    public List<BendPointSave> bendPoints;

    public static class BendPointSave {
        public double x;
        public double y;
        public double maxRadius;
    }
}


