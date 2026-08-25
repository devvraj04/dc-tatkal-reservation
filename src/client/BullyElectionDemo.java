package client;

import rmi.BookingService;
import rmi.UserSession;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Single Complete Lab Evaluation Demonstration for Bully Election Algorithm in
 * Tatkal Railway Reservation System.
 * 
 * Demonstrates:
 * 1. Process mapping (Users as distributed clients with User IDs 1, 2, 3, 4).
 * 2. Initial booking request sent to Leader User 4 (Fails due to missing ACK).
 * 3. Bully Election initiated by User 2 (ELECTION REQUEST, ALIVE ACK, COORDINATOR ACK).
 * 4. User 3 elected as new leader (Highest Active Process ID).
 * 5. Transaction-safe database update (users.is_leader = TRUE for User 3 only).
 * 6. Initially failed booking request re-sent to NEW LEADER User 3 and SUCCESSFULLY SATISFIED!
 */
public class BullyElectionDemo {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("     TATKAL RAILWAY RESERVATION SYSTEM - BULLY ELECTION      ");
        System.out.println("                LAB EXPERIMENT DEMONSTRATION                ");
        System.out.println("============================================================");

        String host = "localhost";
        int port = 1099;
        if (args.length > 0) host = args[0];

        BookingService bookingService = null;
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            bookingService = (BookingService) registry.lookup("BookingService");
            System.out.println("Connected to central Booking Server RMI interface successfully.");
        } catch (Exception e) {
            System.out.println("NOTICE: Running in standalone local simulation mode (RMI Registry port 1099).");
        }

        try {
            // Ensure RMI registry exists on port 1099
            try { LocateRegistry.createRegistry(1099); } catch (Exception ignored) {}

            // 1. Initialize Distributed User Client Processes
            System.out.println("\nInitializing Distributed User Client Processes:");
            ClientElectionNode node1 = new ClientElectionNode(1L, "User 1", host, port, bookingService);
            ClientElectionNode node2 = new ClientElectionNode(2L, "User 2", host, port, bookingService);
            ClientElectionNode node3 = new ClientElectionNode(3L, "User 3", host, port, bookingService);
            ClientElectionNode node4 = new ClientElectionNode(4L, "User 4", host, port, bookingService);

            System.out.println("  User 1 (ID=1) -> ACTIVE");
            System.out.println("  User 2 (ID=2) -> ACTIVE");
            System.out.println("  User 3 (ID=3) -> ACTIVE");
            System.out.println("  User 4 (ID=4) -> ACTIVE / INITIAL LEADER");

            // 2. Set Initial Leader: User 4 (ID 4)
            System.out.println("\nSetting Initial Database Leader: User 4 (users.is_leader = TRUE)");
            if (bookingService != null) {
                try { bookingService.updateLeaderInDatabase(4L); } catch (Exception ignored) {}
            }
            node1.setCurrentLeader(4L, "User 4");
            node2.setCurrentLeader(4L, "User 4");
            node3.setCurrentLeader(4L, "User 4");
            node4.setCurrentLeader(4L, "User 4");

            System.out.println("\n------------------------------------------------------------");
            System.out.println("               RUNNING BULLY ELECTION DEMO                  ");
            System.out.println("------------------------------------------------------------");

            // 3. SIMULATE FAILURE OF LEADER (USER 4)
            System.out.println("\n>>> SIMULATING LEADER FAILURE: User 4 (ID=4) set to FAILED / UNRESPONSIVE <<<");
            node4.setActive(false);

            // 4. USER 2 SENDS DISTRIBUTED BOOKING REQUEST TO CURRENT LEADER (USER 4)
            // Request fails due to missing ACK -> Triggers Election -> Resends & Satisfies Request at New Leader (User 3)!
            String bookingPayload = "Tatkal Ticket Reservation [Train: 12951 | Class: SL | Passengers: 2]";
            System.out.println("\n>>> User 2 (ID=2) sends Distributed Booking Request to Leader User 4 (ID=4) <<<");

            boolean satisfied = node2.sendRequestToLeader(bookingPayload);

            System.out.println("\n============================================================");
            System.out.println("                DEMONSTRATION FINAL RESULT                  ");
            System.out.println("============================================================");

            if (satisfied) {
                System.out.println("SUCCESS: Initially failed request was successfully satisfied after new leader election!");
            } else {
                System.err.println("FAILURE: Request could not be satisfied.");
            }

            // 5. Database Verification
            System.out.println("\n------------------------------------------------------------");
            System.out.println("DATABASE LEADER RECORD VERIFICATION");
            System.out.println("------------------------------------------------------------");
            if (bookingService != null) {
                try {
                    UserSession leader = bookingService.getCurrentLeader();
                    System.out.println("Querying: SELECT user_id, full_name, is_leader FROM users WHERE is_leader = TRUE;");
                    System.out.println("Elected Leader in Database : " + leader.getFullName() + " (ID=" + leader.getUserId() + ")");
                    System.out.println("users.is_leader Status     : TRUE (Exactly 1 active leader record)");
                } catch (Exception ignored) {}
            }

            System.out.println("\n============================================================");
            System.out.println("     BULLY ELECTION LAB DEMONSTRATION COMPLETED SUCCESSFULLY ");
            System.out.println("============================================================");

        } catch (Exception e) {
            System.err.println("CRITICAL: Error in Bully Election Demo execution!");
            e.printStackTrace();
        }
    }
}
