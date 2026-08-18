package clock;

import rmi.BookingService;
import rmi.ClockMessage;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DistributedNode {
    private final String nodeId;
    private final PhysicalClock physicalClock;
    private final LogicalClock logicalClock;

    public DistributedNode(String nodeId, long initialOffsetMillis) {
        this.nodeId = nodeId;
        this.physicalClock = new PhysicalClock(nodeId, initialOffsetMillis);
        this.logicalClock = new LogicalClock(0);
    }

    public String getNodeId() {
        return nodeId;
    }

    public PhysicalClock getPhysicalClock() {
        return physicalClock;
    }

    public LogicalClock getLogicalClock() {
        return logicalClock;
    }

    public String getFormattedPhysicalTime() {
        return physicalClock.getFormattedTime();
    }

    /**
     * Perform Cristian's Algorithm Physical Clock Synchronization
     */
    public CristianSyncResult synchronizeWithServer(BookingService timeServer, long syncWindowMs) {
        try {
            if (timeServer != null) {
                long t0 = physicalClock.getTimeMillis();
                long serverTime = timeServer.getServerPhysicalTime();
                long t1 = physicalClock.getTimeMillis();

                long rtt = t1 - t0;
                long estimatedNetworkDelay = rtt / 2;
                long estimatedCorrectTime = serverTime + estimatedNetworkDelay;
                long adjustment = estimatedCorrectTime - t1;

                // Apply adjustment to local physical clock
                physicalClock.adjustClock(adjustment);

                long updatedClientTime = physicalClock.getTimeMillis();
                long difference = Math.abs(updatedClientTime - serverTime);
                boolean withinWindow = difference <= syncWindowMs;

                return new CristianSyncResult(
                    nodeId, t0, serverTime, t1, rtt, estimatedNetworkDelay,
                    estimatedCorrectTime, t1, adjustment, difference, withinWindow
                );
            }
        } catch (Exception ignored) {}

        // Fallback for standalone / simulated mode
        long t0 = physicalClock.getTimeMillis();
        // Simulate Mumbai server time (+5000 ms offset)
        long serverTime = System.currentTimeMillis() + 5000L;
        // Simulate 20ms RTT
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        long t1 = physicalClock.getTimeMillis();

        long rtt = t1 - t0;
        long estimatedNetworkDelay = rtt / 2;
        long estimatedCorrectTime = serverTime + estimatedNetworkDelay;
        long adjustment = estimatedCorrectTime - t1;

        physicalClock.adjustClock(adjustment);

        long updatedClientTime = physicalClock.getTimeMillis();
        long difference = Math.abs(updatedClientTime - serverTime);
        boolean withinWindow = difference <= syncWindowMs;

        return new CristianSyncResult(
            nodeId, t0, serverTime, t1, rtt, estimatedNetworkDelay,
            estimatedCorrectTime, t1, adjustment, difference, withinWindow
        );
    }

    /**
     * Rule 1: Local Event
     */
    public long executeLocalEvent(String eventDescription) {
        long newL = logicalClock.increment();
        return newL;
    }

    /**
     * Rule 2: Send Event
     */
    public ClockMessage createSendEvent(String eventDescription) {
        long newL = logicalClock.increment();
        return new ClockMessage(nodeId, newL, physicalClock.getTimeMillis(), eventDescription);
    }

    /**
     * Rule 3: Receive Event
     */
    public long processReceiveEvent(ClockMessage message) {
        long oldL = logicalClock.getValue();
        long remoteL = message.getLogicalTimestamp();
        long newL = logicalClock.updateFromRemote(remoteL);
        return newL;
    }

    public static class CristianSyncResult {
        public final String nodeId;
        public final long t0;
        public final long serverTime;
        public final long t1;
        public final long rtt;
        public final long estimatedNetworkDelay;
        public final long estimatedCorrectTime;
        public final long localTimeBeforeAdj;
        public final long adjustment;
        public final long differenceMs;
        public final boolean withinWindow;

        public CristianSyncResult(String nodeId, long t0, long serverTime, long t1, long rtt, 
                                  long estimatedNetworkDelay, long estimatedCorrectTime, 
                                  long localTimeBeforeAdj, long adjustment, long differenceMs, boolean withinWindow) {
            this.nodeId = nodeId;
            this.t0 = t0;
            this.serverTime = serverTime;
            this.t1 = t1;
            this.rtt = rtt;
            this.estimatedNetworkDelay = estimatedNetworkDelay;
            this.estimatedCorrectTime = estimatedCorrectTime;
            this.localTimeBeforeAdj = localTimeBeforeAdj;
            this.adjustment = adjustment;
            this.differenceMs = differenceMs;
            this.withinWindow = withinWindow;
        }

        public String formatTimestamp(long timeMs) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return sdf.format(new Date(timeMs));
        }
    }
}
