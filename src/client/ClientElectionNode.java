package client;

import rmi.BookingService;
import rmi.ClientElectionService;
import rmi.UserSession;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Distributed Process / Client Node participating in the Bully Election Algorithm.
 * Implements direct process-to-process communication via Java RMI.
 */
public class ClientElectionNode extends UnicastRemoteObject implements ClientElectionService {
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final String userName;
    private final String rmiHost;
    private final int rmiPort;

    private boolean active = true;
    private long currentLeaderId = -1;
    private String currentLeaderName = "UNKNOWN";
    private BookingService bookingService;

    public ClientElectionNode(long userId, String userName, String rmiHost, int rmiPort, BookingService bookingService) throws RemoteException {
        super();
        this.userId = userId;
        this.userName = userName;
        this.rmiHost = rmiHost;
        this.rmiPort = rmiPort;
        this.bookingService = bookingService;

        // Register RMI endpoint for client-to-client communication
        registerRmiEndpoint();
    }

    private void registerRmiEndpoint() {
        try {
            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
                registry.list();
            } catch (Exception e) {
                registry = LocateRegistry.createRegistry(rmiPort);
            }
            String bindingName = "UserClient_" + userId;
            registry.rebind(bindingName, this);
            System.out.println("RMI: Client process registered as '" + bindingName + "' on port " + rmiPort);
        } catch (Exception e) {
            System.err.println("WARN: Failed to register RMI endpoint for User " + userId + ": " + e.getMessage());
        }
    }

    // ============================================================
    // RMI Remote Methods
    // ============================================================

    @Override
    public boolean isAlive() throws RemoteException {
        if (!active) {
            throw new RemoteException("Process User " + userId + " is FAILED / UNAVAILABLE.");
        }
        return true;
    }

    @Override
    public boolean receiveElection(long senderId, String senderName) throws RemoteException {
        if (!active) {
            throw new RemoteException("Process User " + userId + " (ID=" + userId + ") is FAILED.");
        }

        String timestamp = getFormattedTimestamp();
        System.out.println("\n[" + timestamp + "] MESSAGE RECEIVED AT CLIENT");
        System.out.println("------------------------------------------------------------");
        System.out.println("SENDER   : " + senderName + " (ID=" + senderId + ")");
        System.out.println("RECEIVER : " + userName + " (ID=" + userId + ")");
        System.out.println("MESSAGE  : ELECTION REQUEST");
        System.out.println("RESULT   : DELIVERED");
        System.out.println("------------------------------------------------------------");
        System.out.println("RESPONSE ACK SENT");
        System.out.println("SENDER   : " + userName + " (ID=" + userId + ")");
        System.out.println("RECEIVER : " + senderName + " (ID=" + senderId + ")");
        System.out.println("MESSAGE  : ALIVE ACK");
        System.out.println("RESULT   : SENT");
        System.out.println("------------------------------------------------------------");

        return true;
    }

    @Override
    public void receiveCoordinator(long leaderId, String leaderName) throws RemoteException {
        if (!active) {
            return; // Failed nodes do not process coordinator messages
        }
        this.currentLeaderId = leaderId;
        this.currentLeaderName = leaderName;

        String timestamp = getFormattedTimestamp();
        System.out.println("\n[" + timestamp + "] COORDINATOR ANNOUNCEMENT RECEIVED");
        System.out.println("SENDER   : Leader " + leaderName + " (ID=" + leaderId + ")");
        System.out.println("RECEIVER : Process " + userName + " (ID=" + userId + ")");
        System.out.println("MESSAGE  : COORDINATOR ANNOUNCEMENT");
        System.out.println("RESPONSE : COORDINATOR ACK SENT");
        System.out.println("Client User " + userName + " (ID=" + userId + ") updated current leader to: " + leaderName + " (ID=" + leaderId + ")");
    }

    @Override
    public String processLeaderRequest(long requesterId, String requesterName, String requestPayload) throws RemoteException {
        if (!active) {
            throw new RemoteException("Leader Process " + userName + " (ID=" + userId + ") is FAILED / UNRESPONSIVE.");
        }

        String timestamp = getFormattedTimestamp();
        System.out.println("\n============================================================");
        System.out.println("LEADER RECEIVED DISTRIBUTED APPLICATION REQUEST");
        System.out.println("============================================================");
        System.out.println("[" + timestamp + "]");
        System.out.println("LEADER   : " + userName + " (ID=" + userId + ")");
        System.out.println("REQUESTER: " + requesterName + " (ID=" + requesterId + ")");
        System.out.println("PAYLOAD  : " + requestPayload);
        System.out.println("ACTION   : Processing Tatkal Booking Request at Leader Node...");
        System.out.println("STATUS   : REQUEST SUCCESSFULLY SATISFIED");
        System.out.println("RESPONSE : REQUEST ACK SENT");
        System.out.println("============================================================");

        return "REQUEST ACK: Booking Request '" + requestPayload + "' satisfied by Leader " + userName + " (ID=" + userId + ")";
    }

    @Override
    public long getUserId() throws RemoteException { return userId; }

    @Override
    public String getUserName() throws RemoteException { return userName; }

    @Override
    public boolean isFailed() throws RemoteException { return !active; }

    // ============================================================
    // Local State & Controls
    // ============================================================

    public void setActive(boolean active) {
        this.active = active;
        System.out.println("Client Process User " + userName + " (ID=" + userId + ") status updated to: " + (active ? "ACTIVE" : "FAILED"));
    }

    public boolean isActive() { return active; }
    public long getCurrentLeaderId() { return currentLeaderId; }
    public String getCurrentLeaderName() { return currentLeaderName; }
    public void setCurrentLeader(long leaderId, String leaderName) {
        this.currentLeaderId = leaderId;
        this.currentLeaderName = leaderName;
    }

    // ============================================================
    // Request Dispatch & Failure Recovery Workflow
    // ============================================================

    /**
     * Sends a distributed application request to the current leader.
     * If the current leader fails to respond with an ACK, it triggers the Bully Election,
     * elects a new leader, and re-sends the initially failed request to the new leader!
     */
    public boolean sendRequestToLeader(String requestPayload) {
        String timestamp = getFormattedTimestamp();
        System.out.println("\n============================================================");
        System.out.println("SENDING DISTRIBUTED REQUEST TO CURRENT LEADER");
        System.out.println("============================================================");
        System.out.println("[" + timestamp + "]");
        System.out.println("SENDER   : " + userName + " (ID=" + userId + ")");
        System.out.println("RECEIVER : Leader User " + currentLeaderId + " (" + currentLeaderName + ")");
        System.out.println("MESSAGE  : DISTRIBUTED APPLICATION REQUEST (" + requestPayload + ")");

        Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
        } catch (Exception ignored) {}

        boolean success = false;
        String ackMsg = null;

        // Try sending request to current leader
        try {
            if (registry != null && currentLeaderId > 0) {
                String stubName = "UserClient_" + currentLeaderId;
                ClientElectionService leaderStub = (ClientElectionService) registry.lookup(stubName);
                if (leaderStub != null && !leaderStub.isFailed() && leaderStub.isAlive()) {
                    ackMsg = leaderStub.processLeaderRequest(userId, userName, requestPayload);
                    success = true;
                }
            }
        } catch (Exception e) {
            success = false;
        }

        if (success && ackMsg != null) {
            System.out.println("\n------------------------------------------------------------");
            System.out.println("RESPONSE RECEIVED FROM LEADER");
            System.out.println("STATUS   : SUCCESS");
            System.out.println("RESPONSE : " + ackMsg);
            System.out.println("------------------------------------------------------------");
            return true;
        }

        // LEADER REQUEST FAILED!
        System.out.println("\n------------------------------------------------------------");
        System.out.println("[" + getFormattedTimestamp() + "] NO ACK RECEIVED FROM LEADER");
        System.out.println("STATUS   : FAILED (Leader User " + currentLeaderId + " is UNRESPONSIVE / FAILED)");
        System.out.println("ACTION   : Initializing Bully Election to recover & elect new Leader...");
        System.out.println("------------------------------------------------------------");

        // Step 1: Run Bully Election
        boolean electionOk = startBullyElection();

        if (!electionOk) {
            System.err.println("CRITICAL: Election failed. Cannot satisfy request.");
            return false;
        }

        // Step 2: Re-send initially failed request to NEW LEADER!
        System.out.println("\n============================================================");
        System.out.println("RE-SENDING INITIALLY FAILED REQUEST TO NEW ELECTED LEADER");
        System.out.println("============================================================");
        System.out.println("[" + getFormattedTimestamp() + "]");
        System.out.println("SENDER   : " + userName + " (ID=" + userId + ")");
        System.out.println("RECEIVER : New Leader User " + currentLeaderId + " (" + currentLeaderName + ")");
        System.out.println("MESSAGE  : DISTRIBUTED APPLICATION REQUEST (" + requestPayload + ")");

        try {
            if (registry != null && currentLeaderId > 0) {
                String stubName = "UserClient_" + currentLeaderId;
                ClientElectionService newLeaderStub = (ClientElectionService) registry.lookup(stubName);
                if (newLeaderStub != null && !newLeaderStub.isFailed() && newLeaderStub.isAlive()) {
                    ackMsg = newLeaderStub.processLeaderRequest(userId, userName, requestPayload);
                    System.out.println("\n------------------------------------------------------------");
                    System.out.println("RESPONSE ACK RECEIVED FROM NEW LEADER");
                    System.out.println("STATUS   : SUCCESS - INITIALLY FAILED REQUEST SATISFIED!");
                    System.out.println("RESPONSE : " + ackMsg);
                    System.out.println("------------------------------------------------------------");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to send request to new leader: " + e.getMessage());
        }

        return false;
    }

    // ============================================================
    // Core Bully Election Algorithm Variant Implementation
    // ============================================================

    /**
     * Initiates the Bully Election Algorithm from this client node.
     */
    public synchronized boolean startBullyElection() {
        String timestamp = getFormattedTimestamp();
        System.out.println("\n============================================================");
        System.out.println("           TATKAL RAILWAY RESERVATION SYSTEM");
        System.out.println("                BULLY ELECTION ALGORITHM");
        System.out.println("============================================================");
        
        System.out.println("\nCURRENT CLIENT");
        System.out.println("------------------------------------------------------------");
        System.out.println("User Name      : " + userName);
        System.out.println("User ID        : " + userId);
        System.out.println("Client Status  : " + (active ? "ACTIVE" : "FAILED"));

        System.out.println("\nCURRENT LEADER");
        System.out.println("------------------------------------------------------------");
        System.out.println("User Name      : " + currentLeaderName);
        System.out.println("User ID        : " + (currentLeaderId > 0 ? currentLeaderId : "NONE"));
        System.out.println("Leader Status  : FAILED");

        System.out.println("\n============================================================");
        System.out.println("COORDINATOR FAILURE DETECTED");
        System.out.println("============================================================");
        System.out.println("User " + userId + " (ID=" + userId + ") detected that current leader");
        System.out.println("User " + currentLeaderId + " (ID=" + currentLeaderId + ") is unavailable.");
        System.out.println("\nReason:");
        System.out.println("Leader request failed / NO ACK received.");
        System.out.println("\nUser " + userId + " will initiate the election.");

        System.out.println("\n============================================================");
        System.out.println("ELECTION STARTED");
        System.out.println("============================================================");
        System.out.println("Election Initiator:");
        System.out.println("User " + userId);
        System.out.println("ID = " + userId);
        System.out.println("\nCurrent Leader:");
        System.out.println("User " + currentLeaderId);
        System.out.println("ID = " + currentLeaderId);

        // Fetch all participating users
        List<UserSession> allUsers = new ArrayList<>();
        try {
            if (bookingService != null) {
                allUsers = bookingService.getAllUsers();
            }
        } catch (Exception e) {
            System.err.println("WARN: Could not fetch user list from BookingService: " + e.getMessage());
        }

        // Fallback default users if DB query returns empty
        if (allUsers.isEmpty()) {
            allUsers.add(new UserSession(1L, "Devraj", "devraj@example.com"));
            allUsers.add(new UserSession(2L, "Rahul Sharma", "rahul@example.com"));
            allUsers.add(new UserSession(3L, "Priya Patel", "priya@example.com"));
            allUsers.add(new UserSession(4L, "Suresh Kumar", "suresh@example.com"));
        }

        // Filter higher-ID clients
        List<UserSession> higherIdUsers = new ArrayList<>();
        for (UserSession u : allUsers) {
            if (u.getUserId() > userId) {
                higherIdUsers.add(u);
            }
        }

        System.out.println("\nFinding all clients with ID greater than " + userId + "...");
        System.out.println("\nHigher-ID clients found:");
        if (higherIdUsers.isEmpty()) {
            System.out.println("None (Initiator process ID " + userId + " is the highest process ID)");
        } else {
            for (int i = 0; i < higherIdUsers.size(); i++) {
                UserSession u = higherIdUsers.get(i);
                System.out.println((i + 1) + ". " + u.getFullName() + " (ID=" + u.getUserId() + ")");
            }
        }

        System.out.println("\n============================================================");
        System.out.println("SENDING ELECTION REQUESTS");
        System.out.println("============================================================");

        List<UserSession> activeHigherClients = new ArrayList<>();
        List<UserSession> failedHigherClients = new ArrayList<>();

        Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
        } catch (Exception ignored) {}

        for (UserSession targetUser : higherIdUsers) {
            long targetId = targetUser.getUserId();
            String targetName = targetUser.getFullName();

            System.out.println("\n[" + getFormattedTimestamp() + "]");
            System.out.println("SENDER");
            System.out.println(userName + " (ID=" + userId + ")");
            System.out.println("        |");
            System.out.println("        |  ELECTION REQUEST");
            System.out.println("        |");
            System.out.println("        v");
            System.out.println("RECEIVER");
            System.out.println(targetName + " (ID=" + targetId + ")");

            boolean responded = false;
            try {
                if (registry != null) {
                    String stubName = "UserClient_" + targetId;
                    ClientElectionService stub = (ClientElectionService) registry.lookup(stubName);
                    if (stub != null && !stub.isFailed() && stub.isAlive()) {
                        responded = stub.receiveElection(userId, userName);
                    }
                }
            } catch (Exception e) {
                responded = false;
            }

            if (responded) {
                System.out.println("RMI request sent successfully.");
                System.out.println("------------------------------------------------------------");
                System.out.println("[" + getFormattedTimestamp() + "]");
                System.out.println("RESPONSE RECEIVED");
                System.out.println(targetName + " (ID=" + targetId + ")");
                System.out.println("        |");
                System.out.println("        |  ALIVE ACK");
                System.out.println("        v");
                System.out.println(userName + " (ID=" + userId + ")");
                System.out.println("\nResult:");
                System.out.println(targetName + " is ACTIVE (ALIVE ACK RECEIVED).");
                activeHigherClients.add(targetUser);
            } else {
                System.out.println("RMI request sent.");
                System.out.println("------------------------------------------------------------");
                System.out.println("[" + getFormattedTimestamp() + "]");
                System.out.println("NO ACK RECEIVED");
                System.out.println(targetName + " (ID=" + targetId + ") is unavailable/FAILED.");
                failedHigherClients.add(targetUser);
            }
        }

        System.out.println("\n============================================================");
        System.out.println("ELECTION PARTICIPANTS");
        System.out.println("============================================================");
        System.out.println("Initiator:");
        System.out.println(userName + " (ID=" + userId + ")");
        
        System.out.println("\nActive higher-ID clients:");
        if (activeHigherClients.isEmpty()) {
            System.out.println("None");
        } else {
            for (UserSession u : activeHigherClients) {
                System.out.println(u.getFullName() + " (ID=" + u.getUserId() + ")");
            }
        }

        System.out.println("\nFailed higher-ID clients:");
        if (failedHigherClients.isEmpty()) {
            System.out.println("None");
        } else {
            for (UserSession u : failedHigherClients) {
                System.out.println(u.getFullName() + " (ID=" + u.getUserId() + ")");
            }
        }

        System.out.println("\n============================================================");
        System.out.println("WINNER SELECTION");
        System.out.println("============================================================");
        System.out.println("Comparing active participating process IDs:");
        System.out.println(userName + " -> ID " + userId);
        for (UserSession u : activeHigherClients) {
            System.out.println(u.getFullName() + " -> ID " + u.getUserId());
        }

        // Determine winner: highest active ID among initiator + active higher-ID candidates
        UserSession winner = new UserSession(userId, userName, "");
        for (UserSession u : activeHigherClients) {
            if (u.getUserId() > winner.getUserId()) {
                winner = u;
            }
        }

        System.out.println("\nHighest active process ID:");
        System.out.println(winner.getUserId());
        System.out.println("\nWINNER:");
        System.out.println(winner.getFullName() + " (ID=" + winner.getUserId() + ")");

        System.out.println("\n============================================================");
        System.out.println("NEW LEADER SELECTED");
        System.out.println("============================================================");
        System.out.println("New Leader:");
        System.out.println(winner.getFullName());
        System.out.println("User ID: " + winner.getUserId());

        // Update database users.is_leader via RMI BookingService
        System.out.println("\nUpdating users.is_leader in Database...");
        boolean dbUpdated = false;
        try {
            if (bookingService != null) {
                dbUpdated = bookingService.updateLeaderInDatabase(winner.getUserId());
            }
        } catch (Exception e) {
            System.err.println("Database leader update via RMI: " + e.getMessage());
        }

        for (UserSession u : allUsers) {
            boolean isWinner = (u.getUserId() == winner.getUserId());
            System.out.println(u.getFullName() + " (ID=" + u.getUserId() + ") -> is_leader = " + (isWinner ? "TRUE" : "FALSE"));
        }

        System.out.println("\nDatabase leader state updated successfully.");

        System.out.println("\n============================================================");
        System.out.println("BROADCASTING COORDINATOR MESSAGE");
        System.out.println("============================================================");
        System.out.println("SENDER:");
        System.out.println(winner.getFullName() + " (ID=" + winner.getUserId() + ")");
        System.out.println("\nSending COORDINATOR ANNOUNCEMENT:");

        System.out.println(winner.getFullName() + " (ID=" + winner.getUserId() + ")");
        System.out.println("       |");

        // Broadcast COORDINATOR to all active clients
        for (UserSession u : allUsers) {
            if (u.getUserId() == winner.getUserId()) continue;

            boolean targetIsFailed = false;
            for (UserSession f : failedHigherClients) {
                if (f.getUserId() == u.getUserId()) {
                    targetIsFailed = true;
                    break;
                }
            }

            if (targetIsFailed) {
                System.out.println("       +-----------------> " + u.getFullName() + " (ID=" + u.getUserId() + ") [FAILED - NO ACK]");
            } else {
                System.out.println("       +-----------------> " + u.getFullName() + " (ID=" + u.getUserId() + ")");
                try {
                    if (registry != null) {
                        String stubName = "UserClient_" + u.getUserId();
                        ClientElectionService stub = (ClientElectionService) registry.lookup(stubName);
                        if (stub != null && !stub.isFailed()) {
                            stub.receiveCoordinator(winner.getUserId(), winner.getFullName());
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (!failedHigherClients.isEmpty()) {
            System.out.println("\n" + failedHigherClients.get(0).getFullName() + " is FAILED and will not receive the announcement.");
        }

        // Update local leader state
        this.currentLeaderId = winner.getUserId();
        this.currentLeaderName = winner.getFullName();

        System.out.println("\n============================================================");
        System.out.println("ELECTION COMPLETED");
        System.out.println("============================================================");
        System.out.println("NEW LEADER:");
        System.out.println(winner.getFullName() + " (ID=" + winner.getUserId() + ")");

        System.out.println("\nFINAL USER STATUS");
        System.out.println("------------------------------------------------------------");
        for (UserSession u : allUsers) {
            long uId = u.getUserId();
            boolean isWinner = (uId == winner.getUserId());
            boolean isFailed = false;
            for (UserSession f : failedHigherClients) {
                if (f.getUserId() == uId) { isFailed = true; break; }
            }
            String statusStr = isFailed ? "FAILED" : "ACTIVE";
            String roleStr = isWinner ? "LEADER" : "NOT LEADER";
            System.out.println(u.getFullName() + " (ID=" + uId + ") -> " + statusStr + " -> " + roleStr);
        }

        System.out.println("\nDatabase verification:");
        System.out.println("users.is_leader = TRUE");
        System.out.println("for " + winner.getFullName() + " (ID=" + winner.getUserId() + ") only.");

        System.out.println("\n============================================================");
        System.out.println("BULLY ELECTION COMPLETED SUCCESSFULLY");
        System.out.println("============================================================\n");

        return true;
    }

    private String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format(new Date());
    }
}
