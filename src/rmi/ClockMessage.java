package rmi;

import java.io.Serializable;

public class ClockMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String senderNode;
    private long logicalTimestamp;
    private long physicalTimestamp;
    private String eventDescription;

    public ClockMessage(String senderNode, long logicalTimestamp, long physicalTimestamp, String eventDescription) {
        this.senderNode = senderNode;
        this.logicalTimestamp = logicalTimestamp;
        this.physicalTimestamp = physicalTimestamp;
        this.eventDescription = eventDescription;
    }

    public String getSenderNode() { return senderNode; }
    public long getLogicalTimestamp() { return logicalTimestamp; }
    public long getPhysicalTimestamp() { return physicalTimestamp; }
    public String getEventDescription() { return eventDescription; }
}
