package com.tatkal.replication;

import java.io.Serializable;

public class ReplicaState implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        UP_TO_DATE,
        STALE,
        SYNCING,
        FAILED
    }

    private int nodeId;
    private String nodeName;
    private String parentNodeName;
    private long lastAppliedVersion;
    private long lastLamportTimestamp;
    private Status status;

    public ReplicaState(int nodeId, String nodeName, String parentNodeName, long lastAppliedVersion, long lastLamportTimestamp, Status status) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.parentNodeName = parentNodeName;
        this.lastAppliedVersion = lastAppliedVersion;
        this.lastLamportTimestamp = lastLamportTimestamp;
        this.status = status;
    }

    public int getNodeId() { return nodeId; }
    public String getNodeName() { return nodeName; }
    public String getParentNodeName() { return parentNodeName; }
    public long getLastAppliedVersion() { return lastAppliedVersion; }
    public void setLastAppliedVersion(long lastAppliedVersion) { this.lastAppliedVersion = lastAppliedVersion; }
    public long getLastLamportTimestamp() { return lastLamportTimestamp; }
    public void setLastLamportTimestamp(long lastLamportTimestamp) { this.lastLamportTimestamp = lastLamportTimestamp; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Override
    public String toString() {
        return "ReplicaState{" +
                "nodeName='" + nodeName + '\'' +
                ", version=" + lastAppliedVersion +
                ", status=" + status +
                '}';
    }
}
