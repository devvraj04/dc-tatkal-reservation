package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for distributed client-to-client communication
 * in the Bully Election Algorithm.
 */
public interface ClientElectionService extends Remote {

    /**
     * Check if the process is active and responsive.
     */
    boolean isAlive() throws RemoteException;

    /**
     * Receive an ELECTION message from a lower-ID initiating process.
     * 
     * @param senderId   User/Process ID of the sender
     * @param senderName User/Process name of the sender
     * @return true if active and responding with ALIVE
     */
    boolean receiveElection(long senderId, String senderName) throws RemoteException;

    /**
     * Receive a COORDINATOR message from the newly elected leader.
     * 
     * @param leaderId   User/Process ID of the new leader
     * @param leaderName User/Process name of the new leader
     */
    void receiveCoordinator(long leaderId, String leaderName) throws RemoteException;

    /**
     * Get the process/user ID.
     */
    long getUserId() throws RemoteException;

    /**
     * Get the process/user full name.
     */
    String getUserName() throws RemoteException;

    /**
     * Check if the process is currently in a simulated failed state.
     */
    boolean isFailed() throws RemoteException;

    /**
     * Process a distributed application request sent to this leader process.
     * 
     * @param requesterId     ID of the requesting process
     * @param requesterName   Name of the requesting process
     * @param requestPayload  Details of the requested operation
     * @return ACK confirmation string if request was processed successfully
     */
    String processLeaderRequest(long requesterId, String requesterName, String requestPayload) throws RemoteException;
}

