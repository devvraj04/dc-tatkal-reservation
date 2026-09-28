package com.tatkal.replication;

import java.io.Serializable;
import java.util.Objects;

public class ReplicationRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum OperationType {
        BOOK, CANCEL, SEAT_UPDATE, UPDATE, INSERT
    }

    private String replicationId;
    private String sourceNode;
    private String destinationNode;
    private OperationType operationType;
    private String entityType;
    private String entityId;
    private String payload;
    private long versionNumber;
    private long lamportTimestamp;
    private long physicalTimestamp;

    public ReplicationRecord() {}

    public ReplicationRecord(String replicationId, String sourceNode, String destinationNode, 
                             OperationType operationType, String entityType, String entityId, 
                             String payload, long versionNumber, long lamportTimestamp, long physicalTimestamp) {
        this.replicationId = replicationId;
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.operationType = operationType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.payload = payload;
        this.versionNumber = versionNumber;
        this.lamportTimestamp = lamportTimestamp;
        this.physicalTimestamp = physicalTimestamp;
    }

    public String getReplicationId() { return replicationId; }
    public String getSourceNode() { return sourceNode; }
    public String getDestinationNode() { return destinationNode; }
    public OperationType getOperationType() { return operationType; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getPayload() { return payload; }
    public long getVersionNumber() { return versionNumber; }
    public long getLamportTimestamp() { return lamportTimestamp; }
    public long getPhysicalTimestamp() { return physicalTimestamp; }

    public void setDestinationNode(String destinationNode) {
        this.destinationNode = destinationNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplicationRecord that = (ReplicationRecord) o;
        return Objects.equals(replicationId, that.replicationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replicationId);
    }

    @Override
    public String toString() {
        return "ReplicationRecord{" +
                "replicationId='" + replicationId + '\'' +
                ", sourceNode='" + sourceNode + '\'' +
                ", destNode='" + destinationNode + '\'' +
                ", op=" + operationType +
                ", version=" + versionNumber +
                ", lamport=" + lamportTimestamp +
                '}';
    }
}
