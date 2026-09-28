package com.tatkal.replication.rmi;

import com.tatkal.replication.ReplicaState;
import com.tatkal.replication.ReplicationManager;
import com.tatkal.replication.ReplicationRecord;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ReplicationRemoteImpl extends UnicastRemoteObject implements ReplicationRemote {
    private static final long serialVersionUID = 1L;
    private transient final ReplicationManager manager;

    public ReplicationRemoteImpl(ReplicationManager manager) throws RemoteException {
        super();
        this.manager = manager;
    }

    @Override
    public boolean receiveReplication(ReplicationRecord record) throws RemoteException {
        return manager.receiveReplication(record);
    }

    @Override
    public ReplicaState getNodeState() throws RemoteException {
        return manager.getNodeState();
    }

    @Override
    public List<ReplicationRecord> getMissingRecords(long fromVersion) throws RemoteException {
        return manager.getReplicationLogsSince(fromVersion);
    }

    @Override
    public boolean acknowledgeReplication(String replicationId) throws RemoteException {
        System.out.println("[" + manager.getNodeName() + "] Received ACK for " + replicationId);
        return true;
    }
}
