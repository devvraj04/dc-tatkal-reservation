package com.tatkal.replication;

import clock.LogicalClock;
import clock.PhysicalClock;
import com.tatkal.replication.rmi.ReplicationRemote;
import server.DBConnection;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReplicationManager {
    private final ReplicaNode node;
    private final LogicalClock logicalClock;
    private final PhysicalClock physicalClock;
    
    // For demo purposes
    private boolean isFailed = false;
    private long replicationDelayMs = 0;

    // In-memory fallback if DB fails
    private ReplicaState memoryState;
    private List<ReplicationRecord> memoryLogs = new ArrayList<>();

    public ReplicationManager(ReplicaNode node) {
        this.node = node;
        this.logicalClock = new LogicalClock(0);
        this.physicalClock = new PhysicalClock(node.getNodeName(), 0);
        this.memoryState = new ReplicaState(node.getId(), node.getNodeName(), node.getParentNodeName(), 0, 0, ReplicaState.Status.UP_TO_DATE);
        initializeState();
    }

    public String getNodeName() {
        return node.getNodeName();
    }

    public ReplicaNode getNode() {
        return node;
    }

    public void setFailed(boolean failed) {
        this.isFailed = failed;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public void setReplicationDelayMs(long delay) {
        this.replicationDelayMs = delay;
    }

    private void initializeState() {
        String insertState = "INSERT INTO replica_state (node_id, node_name, parent_node_name, last_applied_version, last_lamport_timestamp, status) " +
                             "VALUES (?, ?, ?, 0, 0, 'UP_TO_DATE') " +
                             "ON CONFLICT (node_name) DO NOTHING";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertState)) {
            ps.setInt(1, node.getId());
            ps.setString(2, node.getNodeName());
            ps.setString(3, node.getParentNodeName());
            ps.executeUpdate();
        } catch (Exception e) {
            // Silently fall back to in-memory state for the demo
        }
    }

    public ReplicaState getNodeState() {
        String query = "SELECT * FROM replica_state WHERE node_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, node.getNodeName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ReplicaState(
                            rs.getInt("node_id"),
                            rs.getString("node_name"),
                            rs.getString("parent_node_name"),
                            rs.getLong("last_applied_version"),
                            rs.getLong("last_lamport_timestamp"),
                            ReplicaState.Status.valueOf(rs.getString("status"))
                    );
                }
            }
        } catch (Exception e) {
            // Silently fall back to in-memory state for the demo
        }
        return memoryState;
    }

    private void updateState(long version, long lamportTimestamp) {
        memoryState.setLastAppliedVersion(version);
        memoryState.setLastLamportTimestamp(lamportTimestamp);
        memoryState.setStatus(ReplicaState.Status.UP_TO_DATE);

        String update = "UPDATE replica_state SET last_applied_version = ?, last_lamport_timestamp = ?, last_sync_time = CURRENT_TIMESTAMP WHERE node_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setLong(1, version);
            ps.setLong(2, lamportTimestamp);
            ps.setString(3, node.getNodeName());
            ps.executeUpdate();
        } catch (Exception e) {
            // Silently fall back
        }
    }

    public ReplicationRecord createReplicationRecord(ReplicationRecord.OperationType opType, String entityType, String entityId, String payload) {
        logicalClock.increment();
        ReplicaState currentState = getNodeState();
        long nextVersion = currentState.getLastAppliedVersion() + 1;
        String repId = "REP-" + node.getNodeName() + "-" + nextVersion;

        return new ReplicationRecord(
                repId,
                node.getNodeName(),
                null,
                opType,
                entityType,
                entityId,
                payload,
                nextVersion,
                logicalClock.getValue(),
                physicalClock.getTimeMillis()
        );
    }

    public void logReplication(ReplicationRecord record, String status) {
        // Fallback memory logic
        boolean exists = false;
        for (ReplicationRecord r : memoryLogs) {
            if (r.getReplicationId().equals(record.getReplicationId())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            memoryLogs.add(record);
        }

        String insert = "INSERT INTO replication_log (replication_id, source_node, destination_node, operation_type, entity_type, entity_id, version_number, lamport_timestamp, physical_timestamp, payload, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (replication_id) DO UPDATE SET status = EXCLUDED.status, applied_at = CURRENT_TIMESTAMP";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, record.getReplicationId());
            ps.setString(2, record.getSourceNode());
            ps.setString(3, node.getNodeName()); // destination is self
            ps.setString(4, record.getOperationType().name());
            ps.setString(5, record.getEntityType());
            ps.setString(6, record.getEntityId());
            ps.setLong(7, record.getVersionNumber());
            ps.setLong(8, record.getLamportTimestamp());
            ps.setLong(9, record.getPhysicalTimestamp());
            ps.setString(10, record.getPayload());
            ps.setString(11, status);
            ps.executeUpdate();
        } catch (Exception e) {
            // Silently fall back
        }
    }

    public boolean receiveReplication(ReplicationRecord record) {
        if (isFailed) {
            System.out.println("[" + node.getNodeName() + "] FAILED to process replication - node is currently down/unavailable.");
            return false;
        }

        if (replicationDelayMs > 0) {
            try {
                System.out.println("[" + node.getNodeName() + "] Simulating replication delay of " + replicationDelayMs + "ms...");
                Thread.sleep(replicationDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logicalClock.updateFromRemote(record.getLamportTimestamp());
        System.out.println("[" + node.getNodeName() + "] Received replication " + record.getReplicationId() + " (Version: " + record.getVersionNumber() + ")");
        
        logReplication(record, "APPLIED");
        updateState(record.getVersionNumber(), logicalClock.getValue());
        
        System.out.println("[" + node.getNodeName() + "] Update successful. Applied version: " + record.getVersionNumber());
        return true;
    }

    public boolean sendReplication(ReplicationRecord record, String targetNodeName, boolean strongConsistency) {
        System.out.println("[" + node.getNodeName() + " -> " + targetNodeName + "] Sending replication request...");
        System.out.println("Operation: " + record.getOperationType() + " | Version: " + record.getVersionNumber() + " | Lamport: " + logicalClock.getValue());
        
        if (!strongConsistency) {
            new Thread(() -> doSend(record, targetNodeName)).start();
            return true;
        } else {
            return doSend(record, targetNodeName);
        }
    }

    private boolean doSend(ReplicationRecord record, String targetNodeName) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            ReplicationRemote remote = (ReplicationRemote) registry.lookup("Replication-" + targetNodeName);
            boolean success = remote.receiveReplication(record);
            if (success) {
                remote.acknowledgeReplication(record.getReplicationId());
            } else {
                System.out.println("[" + node.getNodeName() + " -> " + targetNodeName + "] ERROR: REPLICA UNAVAILABLE OR FAILED");
            }
            return success;
        } catch (Exception e) {
            System.out.println("[" + node.getNodeName() + " -> " + targetNodeName + "] ERROR: " + e.getMessage());
            return false;
        }
    }

    public List<ReplicationRecord> getReplicationLogsSince(long fromVersion) {
        List<ReplicationRecord> records = new ArrayList<>();
        String query = "SELECT * FROM replication_log WHERE destination_node = ? AND version_number > ? ORDER BY version_number ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, node.getNodeName());
            ps.setLong(2, fromVersion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new ReplicationRecord(
                            rs.getString("replication_id"),
                            rs.getString("source_node"),
                            rs.getString("destination_node"),
                            ReplicationRecord.OperationType.valueOf(rs.getString("operation_type")),
                            rs.getString("entity_type"),
                            rs.getString("entity_id"),
                            rs.getString("payload"),
                            rs.getLong("version_number"),
                            rs.getLong("lamport_timestamp"),
                            rs.getLong("physical_timestamp")
                    ));
                }
            }
            return records;
        } catch (Exception e) {
            // Silently fall back
            return memoryLogs.stream()
                .filter(r -> r.getVersionNumber() > fromVersion)
                .collect(Collectors.toList());
        }
    }
}
