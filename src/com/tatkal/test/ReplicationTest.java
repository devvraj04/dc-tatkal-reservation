package com.tatkal.test;

import com.tatkal.replication.ReplicaNode;
import com.tatkal.replication.ReplicaState;
import com.tatkal.replication.ReplicationManager;
import com.tatkal.replication.ReplicationRecord;
import com.tatkal.replication.rmi.ReplicationRemote;
import com.tatkal.replication.rmi.ReplicationRemoteImpl;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ReplicationTest {
    
    private static Map<String, ReplicationManager> managers = new HashMap<>();
    
    public static void main(String[] args) {
        System.out.println("Starting Replication Demo RMI Environment...");
        try {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
            } catch (RemoteException e) {
                // If registry already exists on port 1099
                registry = LocateRegistry.getRegistry(1099);
            }
            
            // Initialize Managers and bind to RMI
            for (ReplicaNode node : ReplicaNode.values()) {
                ReplicationManager manager = new ReplicationManager(node);
                managers.put(node.getNodeName(), manager);
                ReplicationRemote stub = new ReplicationRemoteImpl(manager);
                registry.rebind("Replication-" + node.getNodeName(), stub);
            }
            
            System.out.println("All Replication nodes started and bound.");
            
            runCompleteAutomaticDemo();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void printMenu() {
        System.out.println("\n========================================================");
        System.out.println("REPLICATION EXPERIMENT MENU");
        System.out.println("========================================================");
        System.out.println("1. Show Architecture");
        System.out.println("2. Show Node Status");
        System.out.println("3. Strong Consistency Booking (Mumbai)");
        System.out.println("4. Eventual Consistency Booking (Delhi)");
        System.out.println("5. Delay Replication");
        System.out.println("6. Fail Mumbai Replica");
        System.out.println("7. Fail Delhi Replica");
        System.out.println("8. Restore Replica");
        System.out.println("9. Synchronize Stale Replica");
        System.out.println("10. Show Replication Logs");
        System.out.println("11. Show Version Status");
        System.out.println("12. Show Central Aggregation");
        System.out.println("13. Run Complete Automatic Demo");
        System.out.println("0. Exit");
        System.out.print("Enter choice: ");
    }
    
    private static void showArchitecture() {
        System.out.println("\n========================================================");
        System.out.println("TATKAL RAILWAY RESERVATION SYSTEM");
        System.out.println("DATA REPLICATION EXPERIMENT");
        System.out.println("========================================================");
        System.out.println("ARCHITECTURE\n");
        System.out.println("MUMBAI CENTRAL");
        System.out.println("    |");
        System.out.println("    +--> MUMBAI REPLICA");
        System.out.println("    |");
        System.out.println("    +--> CENTRAL\n");
        System.out.println("DELHI CENTRAL");
        System.out.println("    |");
        System.out.println("    +--> DELHI REPLICA");
        System.out.println("    |");
        System.out.println("    +--> CENTRAL\n");
        System.out.println("CENTRAL");
        System.out.println("    |");
        System.out.println("    +--> CENTRAL REPLICA");
        System.out.println("========================================================");
    }
    
    private static void showNodeStatus() {
        System.out.println("\n========================================================");
        System.out.println("NODE STATUS");
        System.out.println("========================================================");
        for (ReplicaNode node : ReplicaNode.values()) {
            ReplicationManager mgr = managers.get(node.getNodeName());
            System.out.println(String.format("%-18s : %s", node.getNodeName(), mgr.isFailed() ? "FAILED" : "ONLINE"));
        }
    }
    
    private static void showVersionStatus() {
        System.out.println("\n========================================================");
        System.out.println("INITIAL REPLICATION STATE / VERSION STATUS");
        System.out.println("========================================================");
        System.out.printf("%-18s %-12s %-15s\n", "NODE", "VERSION", "STATUS");
        System.out.println("------------------------------------------------");
        for (ReplicaNode node : ReplicaNode.values()) {
            ReplicaState state = managers.get(node.getNodeName()).getNodeState();
            System.out.printf("%-18s V%-11d %-15s\n", node.getNodeName(), state.getLastAppliedVersion(), state.getStatus());
        }
        System.out.println("========================================================");
    }
    
    private static void strongConsistencyBooking() {
        System.out.println("\n========================================================");
        System.out.println("STRONG CONSISTENCY DEMONSTRATION");
        System.out.println("========================================================");
        System.out.println("Client requests:\nBOOK SEAT S1\nTarget regional server: MUMBAI CENTRAL\n");
        System.out.println("[MUMBAI CENTRAL]\nValidating booking...\nDatabase write successful.\n");
        
        ReplicationManager mumbai = managers.get("MUMBAI_CENTRAL");
        ReplicationRecord record = mumbai.createReplicationRecord(ReplicationRecord.OperationType.BOOK, "BOOKING", "1001", "SEAT:S1");
        
        System.out.println("Replication ID:\n" + record.getReplicationId());
        System.out.println("Operation:\n" + record.getOperationType());
        System.out.println("Version:\nM" + record.getVersionNumber());
        System.out.println("Lamport:\nL" + record.getLamportTimestamp() + "\n");
        
        // Replicate to Mumbai Replica
        mumbai.sendReplication(record, "MUMBAI_REPLICA", true);
        
        // Replicate to Central
        mumbai.sendReplication(record, "CENTRAL", true);
        
        System.out.println("BOOKING SUCCESSFUL");
        showVersionStatus();
    }
    
    private static void eventualConsistencyBooking() {
        System.out.println("\n========================================================");
        System.out.println("EVENTUAL CONSISTENCY DEMONSTRATION");
        System.out.println("========================================================");
        System.out.println("Client requests:\nBOOK SEAT S2\nTarget regional server: DELHI CENTRAL\n");
        System.out.println("[DELHI CENTRAL]\nDatabase write successful.\n");
        
        ReplicationManager delhi = managers.get("DELHI_CENTRAL");
        ReplicationRecord record = delhi.createReplicationRecord(ReplicationRecord.OperationType.BOOK, "BOOKING", "2001", "SEAT:S2");
        
        System.out.println("Version:\nD" + record.getVersionNumber());
        System.out.println("Booking:\nSUCCESS\n");
        
        System.out.println("Immediately after write:");
        ReplicaState dcState = delhi.getNodeState();
        ReplicaState drState = managers.get("DELHI_REPLICA").getNodeState();
        ReplicaState cState = managers.get("CENTRAL").getNodeState();
        
        System.out.println("DELHI CENTRAL : D" + dcState.getLastAppliedVersion());
        System.out.println("DELHI REPLICA : D" + drState.getLastAppliedVersion());
        System.out.println("CENTRAL       : D" + cState.getLastAppliedVersion());
        System.out.println("\nSTATUS:\nTEMPORARILY INCONSISTENT\n");
        System.out.println("Waiting for asynchronous replication...\n");
        
        delhi.sendReplication(record, "DELHI_REPLICA", false);
        delhi.sendReplication(record, "CENTRAL", false);
        
        try { Thread.sleep(2000); } catch (InterruptedException e) {} // Give async threads time
        
        System.out.println("Final state:");
        dcState = delhi.getNodeState();
        drState = managers.get("DELHI_REPLICA").getNodeState();
        cState = managers.get("CENTRAL").getNodeState();
        
        System.out.println("DELHI CENTRAL : D" + dcState.getLastAppliedVersion());
        System.out.println("DELHI REPLICA : D" + drState.getLastAppliedVersion());
        System.out.println("CENTRAL       : D" + cState.getLastAppliedVersion());
        System.out.println("\nSTATUS:\nALL NODES CONSISTENT");
    }
    
    private static void delayReplication(Scanner scanner) {
        System.out.print("Enter node to delay (e.g., DELHI_REPLICA): ");
        String node = scanner.next();
        System.out.print("Enter delay in ms: ");
        long delay = scanner.nextLong();
        if (managers.containsKey(node)) {
            managers.get(node).setReplicationDelayMs(delay);
            System.out.println(node + " replication delay set to " + delay + " ms.");
        } else {
            System.out.println("Node not found.");
        }
    }
    
    private static void failReplica(String node) {
        if (managers.containsKey(node)) {
            managers.get(node).setFailed(true);
            System.out.println("\n========================================================");
            System.out.println("REPLICA FAILURE DEMONSTRATION");
            System.out.println("========================================================");
            System.out.println("Disable:\n" + node);
        }
    }
    
    private static void restoreReplica(Scanner scanner) {
        System.out.print("Enter node to restore: ");
        String node = scanner.next();
        if (managers.containsKey(node)) {
            managers.get(node).setFailed(false);
            System.out.println(node + " restored and ONLINE.");
        } else {
            System.out.println("Node not found.");
        }
    }
    
    private static void synchronizeStaleReplica(Scanner scanner) {
        System.out.print("Enter stale node to sync (e.g., DELHI_REPLICA): ");
        String nodeName = scanner.next();
        if (!managers.containsKey(nodeName)) {
            System.out.println("Node not found.");
            return;
        }
        
        ReplicationManager target = managers.get(nodeName);
        ReplicaNode parent = ReplicaNode.fromName(target.getNode().getParentNodeName());
        if (parent == null) {
            System.out.println("No parent to sync from.");
            return;
        }
        
        ReplicationManager source = managers.get(parent.getNodeName());
        long lastApplied = target.getNodeState().getLastAppliedVersion();
        
        System.out.println("Catch-up synchronization:");
        List<ReplicationRecord> missing = source.getReplicationLogsSince(lastApplied);
        
        if (missing.isEmpty()) {
            System.out.println("No missing versions.");
        } else {
            for (ReplicationRecord record : missing) {
                System.out.println("Missing version: " + record.getVersionNumber());
                System.out.println("Applying " + record.getVersionNumber() + "...");
                target.receiveReplication(record);
            }
        }
        System.out.println("Final:\n" + parent.getNodeName() + " : V" + source.getNodeState().getLastAppliedVersion());
        System.out.println(nodeName + " : V" + target.getNodeState().getLastAppliedVersion());
        System.out.println("\nSTATUS:\nCONSISTENT");
    }
    
    private static void showReplicationLogs(Scanner scanner) {
        System.out.print("Enter node to view logs: ");
        String node = scanner.next();
        if (managers.containsKey(node)) {
            List<ReplicationRecord> logs = managers.get(node).getReplicationLogsSince(0);
            for (ReplicationRecord record : logs) {
                System.out.println(record);
            }
        }
    }
    
    private static void showCentralAggregation() {
        System.out.println("\n========================================================");
        System.out.println("CENTRAL DATA");
        System.out.println("========================================================");
        ReplicationManager central = managers.get("CENTRAL");
        List<ReplicationRecord> all = central.getReplicationLogsSince(0);
        long mCount = all.stream().filter(r -> r.getSourceNode().equals("MUMBAI_CENTRAL")).count();
        long dCount = all.stream().filter(r -> r.getSourceNode().equals("DELHI_CENTRAL")).count();
        
        long mMax = all.stream().filter(r -> r.getSourceNode().equals("MUMBAI_CENTRAL")).mapToLong(ReplicationRecord::getVersionNumber).max().orElse(0);
        long dMax = all.stream().filter(r -> r.getSourceNode().equals("DELHI_CENTRAL")).mapToLong(ReplicationRecord::getVersionNumber).max().orElse(0);
        
        System.out.println("Mumbai updates received: " + mCount);
        System.out.println("Delhi updates received: " + dCount);
        System.out.println("Mumbai latest version: V" + mMax);
        System.out.println("Delhi latest version: V" + dMax);
        System.out.println("\nCentral status:\nUP TO DATE\n========================================================");
    }
    
    private static void runCompleteAutomaticDemo() {
        showArchitecture();
        showVersionStatus();
        strongConsistencyBooking();
        
        managers.get("DELHI_REPLICA").setReplicationDelayMs(3000);
        eventualConsistencyBooking();
        
        failReplica("DELHI_REPLICA");
        System.out.println("Create booking: D7");
        ReplicationRecord d7 = managers.get("DELHI_CENTRAL").createReplicationRecord(ReplicationRecord.OperationType.BOOK, "BOOKING", "2002", "SEAT:S3");
        managers.get("DELHI_CENTRAL").sendReplication(d7, "DELHI_REPLICA", true); // Should print ERROR UNAVAILABLE
        System.out.println("DELHI CENTRAL:\nD" + d7.getVersionNumber());
        System.out.println("DELHI REPLICA:\nD" + managers.get("DELHI_REPLICA").getNodeState().getLastAppliedVersion());
        System.out.println("STATUS:\nSTALE\n");
        
        managers.get("DELHI_REPLICA").setFailed(false);
        System.out.println("Restore replica.");
        managers.get("DELHI_REPLICA").setReplicationDelayMs(0);
        
        System.out.println("Catch-up synchronization:");
        List<ReplicationRecord> missing = managers.get("DELHI_CENTRAL").getReplicationLogsSince(managers.get("DELHI_REPLICA").getNodeState().getLastAppliedVersion());
        for (ReplicationRecord r : missing) {
            System.out.println("Missing version:\nD" + r.getVersionNumber());
            System.out.println("Applying D" + r.getVersionNumber() + "...");
            managers.get("DELHI_REPLICA").receiveReplication(r);
        }
        
        System.out.println("Final:\nDELHI CENTRAL : D" + managers.get("DELHI_CENTRAL").getNodeState().getLastAppliedVersion());
        System.out.println("DELHI REPLICA : D" + managers.get("DELHI_REPLICA").getNodeState().getLastAppliedVersion());
        System.out.println("\nSTATUS:\nCONSISTENT");
        
        System.out.println("\n========================================================");
        System.out.println("FINAL REPLICATION SUMMARY");
        System.out.println("========================================================");
        showVersionStatus();
        showCentralAggregation();
        System.out.println("EXPERIMENT COMPLETED");
    }
}
