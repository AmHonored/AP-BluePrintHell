package protocol.messages;

public class Run {
    public static class StartRequest {
        public String type = "RunStart";
        public String levelCode;
    }

    public static class StartAck {
        public String type = "RunStartAck";
        public String runId;
        public long serverStartTime;
    }

    public static class FinishRequest {
        public String type = "RunFinish";
        public String runId;
        public String levelCode;
        public long durationMs;
        public int xpGained; // trust-minimized; server may recompute
    }

    public static class FinishAck {
        public String type = "RunFinishAck";
        public String runId;
        public String levelCode;
        public long durationMs;
        public int xpGained;
        public boolean accepted;
        public String reason;
    }
}







