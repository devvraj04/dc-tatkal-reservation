package com.tatkal.replication.rmi;

import com.tatkal.replication.ReplicaState;
import com.tatkal.replication.ReplicationRecord;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ReplicationRemote extends Remote {
    
    // Receive a single replication record (for strong or eventual consistency)
    boolean receiveReplication(ReplicationRecord record) throws RemoteException;
    
    // Get the current state of the node (version, status)
    ReplicaState getNodeState() throws RemoteException;
    
    // For catch-up synchronization
    List<ReplicationRecord> getMissingRecords(long fromVersion) throws RemoteException;
    
    // Acknowledge a replication (mostly for demonstration logging, the return of receiveReplication is usually the ACK)
    boolean acknowledgeReplication(String replicationId) throws RemoteException;
}
