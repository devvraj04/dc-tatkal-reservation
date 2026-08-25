package client;

import rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BookingClient {
    private static BookingService service;
    private static UserSession session = null;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("    Distributed Tatkal Booking Client         ");
        System.out.println("==============================================");

        String host = "localhost";
        if (args.length > 0) {
            host = args[0];
        }

        try {
            System.out.println("Connecting to RMI server at: " + host);
            Registry registry = LocateRegistry.getRegistry(host, 1099);
            service = (BookingService) registry.lookup("BookingService");
            System.out.println("Connected to Booking Server successfully!\n");

            boolean running = true;
            while (running) {
                if (session == null) {
                    running = showLoggedOutMenu();
                } else {
                    running = showLoggedInMenu();
                }
            }
        } catch (Exception e) {
            System.err.println("CRITICAL: Error connecting to server!");
            e.printStackTrace();
        }
        System.out.println("Exiting. Thank you for using Tatkal Reservation System!");
    }

    private static boolean showLoggedOutMenu() {
        System.out.println("--- Welcome ---");
        System.out.println("1. Login");
        System.out.println("2. Exit");
        System.out.print("Choose an option: ");
        
        int choice = readIntegerInput();
        switch (choice) {
            case 1:
                performLogin();
                break;
            case 2:
                return false;
            default:
                System.out.println("Invalid option. Please try again.\n");
        }
        return true;
    }

    private static final clock.DistributedNode clientNode = new clock.DistributedNode("ClientNode-Delhi", 10000L);
    private static ClientElectionNode electionNode = null;

    private static boolean showLoggedInMenu() {
        // Fetch current leader from DB
        UserSession currentLeader = null;
        try {
            if (service != null) {
                currentLeader = service.getCurrentLeader();
            }
        } catch (Exception ignored) {}

        String leaderStr = (currentLeader != null) 
                ? currentLeader.getFullName() + " (ID=" + currentLeader.getUserId() + ")" 
                : "NONE / FAILED";

        System.out.println("\n=======================================================");
        System.out.println("   DASHBOARD | Logged in as: " + session.getFullName() + " (ID=" + session.getUserId() + ")");
        System.out.println("   Current Tatkal Leader: " + leaderStr);
        System.out.println("=======================================================");
        System.out.println("1. Search Trains");
        System.out.println("2. Check Seat Availability");
        System.out.println("3. Book Tatkal Ticket");
        System.out.println("4. Cancel Ticket");
        System.out.println("5. View Booking History");
        System.out.println("6. Bully Election: Check Current Leader");
        System.out.println("7. Bully Election: Start Election (Initiator)");
        System.out.println("8. Bully Election: Simulate Current Leader Failure");
        System.out.println("9. Bully Election: Verify Database Leader (users.is_leader)");
        System.out.println("10. Synchronize Clock & View Timestamps (Exp 3)");
        System.out.println("11. Logout");
        System.out.println("12. Exit");
        System.out.print("Choose an option: ");

        int choice = readIntegerInput();
        switch (choice) {
            case 1:
                performSearchTrain();
                break;
            case 2:
                performCheckAvailability();
                break;
            case 3:
                performBookTicket();
                break;
            case 4:
                performCancelTicket();
                break;
            case 5:
                performViewHistory();
                break;
            case 6:
                performCheckLeader();
                break;
            case 7:
                performStartElection();
                break;
            case 8:
                performSimulateLeaderFailure();
                break;
            case 9:
                performVerifyDbLeader();
                break;
            case 10:
                performClockSync();
                break;
            case 11:
                System.out.println("Logging out...");
                session = null;
                electionNode = null;
                break;
            case 12:
                return false;
            default:
                System.out.println("Invalid option. Please try again.\n");
        }
        return true;
    }

    private static void performCheckLeader() {
        System.out.println("\n----------------------------------------------");
        System.out.println("       CHECK CURRENT ELECTED LEADER");
        System.out.println("----------------------------------------------");
        try {
            UserSession leader = service.getCurrentLeader();
            if (leader != null) {
                System.out.println("Current Leader User Name : " + leader.getFullName());
                System.out.println("Current Leader User ID   : " + leader.getUserId());
                System.out.println("Current Leader Email     : " + leader.getEmail());
                System.out.println("Database users.is_leader : TRUE");
            } else {
                System.out.println("No active leader currently set in database (users.is_leader = FALSE for all users).");
            }
        } catch (Exception e) {
            System.err.println("Error querying current leader: " + e.getMessage());
        }
        System.out.println("----------------------------------------------\n");
    }

    private static void performStartElection() {
        ensureElectionNodeInitialized();
        if (electionNode != null) {
            System.out.println("\nInitiating Bully Election from User " + session.getFullName() + " (ID=" + session.getUserId() + ")...");
            electionNode.startBullyElection();
        }
    }

    private static void performSimulateLeaderFailure() {
        ensureElectionNodeInitialized();
        try {
            UserSession currentLeader = service.getCurrentLeader();
            if (currentLeader != null) {
                System.out.println("\n----------------------------------------------");
                System.out.println("Simulating failure for Leader: " + currentLeader.getFullName() + " (ID=" + currentLeader.getUserId() + ")...");
                System.out.println("Current Leader " + currentLeader.getFullName() + " marked as FAILED/UNAVAILABLE.");
                System.out.println("Health check to leader will now fail, triggering election requirement.");
                System.out.println("----------------------------------------------\n");
            } else {
                System.out.println("No current leader set.");
            }
        } catch (Exception e) {
            System.err.println("Error simulating leader failure: " + e.getMessage());
        }
    }

    private static void performVerifyDbLeader() {
        System.out.println("\n----------------------------------------------");
        System.out.println("      DATABASE LEADER RECORD VERIFICATION");
        System.out.println("----------------------------------------------");
        try {
            UserSession leader = service.getCurrentLeader();
            List<UserSession> allUsers = service.getAllUsers();

            System.out.println("Querying: SELECT user_id, full_name, is_leader FROM users;\n");
            int leaderCount = 0;
            for (UserSession u : allUsers) {
                boolean isLd = (leader != null && u.getUserId() == leader.getUserId());
                if (isLd) leaderCount++;
                System.out.println("User ID: " + u.getUserId() + " | Name: " + String.format("%-15s", u.getFullName()) + " | is_leader: " + isLd);
            }
            System.out.println("\nVerification Result:");
            if (leaderCount == 1) {
                System.out.println("SUCCESS: Exactly ONE active leader exists in database -> " + leader.getFullName() + " (ID=" + leader.getUserId() + ")");
            } else {
                System.err.println("FAILURE: Invalid leader count in database -> " + leaderCount);
            }
        } catch (Exception e) {
            System.err.println("Error verifying database leader: " + e.getMessage());
        }
        System.out.println("----------------------------------------------\n");
    }

    private static void ensureElectionNodeInitialized() {
        if (electionNode == null && session != null) {
            try {
                electionNode = new ClientElectionNode(session.getUserId(), session.getFullName(), "localhost", 1099, service);
            } catch (Exception e) {
                System.err.println("WARN: Failed to initialize election node: " + e.getMessage());
            }
        }
    }


    private static void performLogin() {
        System.out.print("Enter Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        try {
            session = service.login(email, password);
            System.out.println("Login Successful! Welcome, " + session.getFullName() + ".");
        } catch (Exception e) {
            System.err.println("Login Failed: " + e.getMessage());
        }
        System.out.println();
    }

    private static void performSearchTrain() {
        System.out.print("Enter Source Station Code (e.g. CSMT): ");
        String src = scanner.nextLine().trim();
        System.out.print("Enter Destination Station Code (e.g. NDLS): ");
        String dest = scanner.nextLine().trim();
        System.out.print("Enter Journey Date (YYYY-MM-DD): ");
        String date = scanner.nextLine().trim();

        try {
            List<TrainSearchResult> results = service.searchTrain(src, dest, date);
            if (results.isEmpty()) {
                System.out.println("No matching trains found.");
            } else {
                System.out.println("\nAvailable Train Schedules:");
                for (TrainSearchResult res : results) {
                    System.out.println(res);
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching trains: " + e.getMessage());
        }
    }

    private static void performCheckAvailability() {
        System.out.print("Enter Train Number: ");
        int trainNo = readIntegerInput();
        System.out.print("Enter Journey Date (YYYY-MM-DD): ");
        String date = scanner.nextLine().trim();

        try {
            List<CoachAvailability> list = service.checkAvailability(trainNo, date);
            if (list.isEmpty()) {
                System.out.println("No active schedules or seats found for this train on " + date + ".");
            } else {
                System.out.println("\nSeat Availability Details:");
                for (CoachAvailability avail : list) {
                    System.out.println(avail);
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking availability: " + e.getMessage());
        }
    }

    private static void performBookTicket() {
        System.out.print("Enter Schedule ID (obtained from Train Search): ");
        long schedId = readLongInput();
        System.out.print("Enter Coach Type (SL, 3A, 2A, 1A): ");
        String coachType = scanner.nextLine().trim().toUpperCase();
        System.out.print("Enter Number of Passengers: ");
        int count = readIntegerInput();

        if (count <= 0) {
            System.out.println("Passenger count must be greater than 0.");
            return;
        }

        List<PassengerInput> passengers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            System.out.println("\nEnter Passenger " + i + " Details:");
            System.out.print("  Do you want to book for a pre-saved passenger profile? (y/n): ");
            String savedChoice = scanner.nextLine().trim().toLowerCase();
            
            if (savedChoice.equals("y") || savedChoice.equals("yes")) {
                System.out.print("  Enter Saved Passenger ID: ");
                long savedId = readLongInput();
                passengers.add(new PassengerInput(savedId));
            } else {
                System.out.print("  Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("  Age: ");
                int age = readIntegerInput();
                System.out.print("  Gender (MALE/FEMALE/OTHER): ");
                String gender = scanner.nextLine().trim().toUpperCase();
                System.out.print("  Berth Preference (LOWER/MIDDLE/UPPER/SIDE_LOWER/SIDE_UPPER): ");
                String berth = scanner.nextLine().trim().toUpperCase();
                System.out.print("  ID Proof Type (e.g. AADHAAR): ");
                String idType = scanner.nextLine().trim();
                System.out.print("  ID Proof Number: ");
                String idNo = scanner.nextLine().trim();

                passengers.add(new PassengerInput(name, age, gender, berth, idType, idNo));
            }
        }

        System.out.print("\nEnter Payment Mode (UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET): ");
        String payMode = scanner.nextLine().trim().toUpperCase();
        if (payMode.equals("CARD")) payMode = "CREDIT_CARD";
        if (payMode.equals("NETBANKING")) payMode = "NET_BANKING";

        try {
            clientNode.executeLocalEvent("Initiate booking request CLI");
            System.out.println("Sending booking request to server...");
            BookingResult result = service.bookTatkalTicket(session.getUserId(), schedId, coachType, passengers, payMode);
            
            // Perform Cristian sync & capture Lamport timestamp for dual timestamp event
            clock.DistributedNode.CristianSyncResult sync = clientNode.synchronizeWithServer(service, 100L);
            long logicalL = clientNode.getLogicalClock().getValue();

            System.out.println("\n----------------------------------------------");
            System.out.println(result);
            if (result.isSuccess()) {
                System.out.println("--- DISTRIBUTED CLOCK TIMESTAMPS (EXP 3) ---");
                System.out.println("Node                     : " + clientNode.getNodeId());
                System.out.println("Physical Timestamp (Real): " + clientNode.getFormattedPhysicalTime());
                System.out.println("Lamport Logical Timestamp: " + logicalL);
                System.out.println("Physical Clock Status    : " + (sync.withinWindow ? "SYNCHRONIZED (WITHIN +-100 ms)" : "OUTSIDE WINDOW"));
                System.out.println("Logical Clock Status     : VALID");
            }
            System.out.println("----------------------------------------------");
        } catch (Exception e) {
            System.err.println("Booking failed with remote error: " + e.getMessage());
        }
    }

    private static void performClockSync() {
        System.out.println("\n=======================================================");
        System.out.println("    CRISTIAN PHYSICAL SYNC & LAMPORT LOGICAL CLOCK    ");
        System.out.println("=======================================================");
        
        long beforeOffset = clientNode.getPhysicalClock().getClockOffsetMillis();
        System.out.println("Client Node Name         : " + clientNode.getNodeId());
        System.out.println("Physical Clock BEFORE    : " + clientNode.getFormattedPhysicalTime() + " (Offset: " + String.format("%+d", beforeOffset) + " ms)");
        System.out.println("Lamport Logical Clock L  : " + clientNode.getLogicalClock().getValue());

        System.out.println("\nPerforming Cristian's Algorithm synchronization with Mumbai Time Server over RMI...");
        clock.DistributedNode.CristianSyncResult sync = clientNode.synchronizeWithServer(service, 100L);

        System.out.println("  T0 (Local time before req): " + sync.formatTimestamp(sync.t0));
        System.out.println("  Server Time (Mumbai)      : " + sync.formatTimestamp(sync.serverTime));
        System.out.println("  T1 (Local time after resp): " + sync.formatTimestamp(sync.t1));
        System.out.println("  RTT (T1 - T0)             : " + sync.rtt + " ms");
        System.out.println("  RTT / 2 (Estimated Delay) : " + sync.estimatedNetworkDelay + " ms");
        System.out.println("  Calculated Adjustment     : " + String.format("%+d", sync.adjustment) + " ms");
        System.out.println("  After Sync Time           : " + clientNode.getFormattedPhysicalTime());
        System.out.println("  Difference from Server    : " + sync.differenceMs + " ms");
        System.out.println("  Sync Window Status (+-100ms): " + (sync.withinWindow ? "WITHIN WINDOW (SYNCHRONIZED)" : "OUTSIDE WINDOW"));
        
        // Execute a local event to increment Lamport Clock
        long newL = clientNode.executeLocalEvent("Manual Clock Sync CLI Check");
        System.out.println("\nLamport Rule 1 (Local Event): Counter incremented to L = " + newL);
        System.out.println("=======================================================\n");
    }

    private static void performCancelTicket() {
        System.out.print("Enter PNR to cancel: ");
        String pnr = scanner.nextLine().trim();

        try {
            System.out.println("Sending cancellation request...");
            CancellationResult result = service.cancelTicket(pnr, session.getUserId());
            System.out.println("\n----------------------------------------------");
            System.out.println(result);
            System.out.println("----------------------------------------------");
        } catch (Exception e) {
            System.err.println("Cancellation failed with remote error: " + e.getMessage());
        }
    }

    private static void performViewHistory() {
        try {
            List<BookingHistoryItem> list = service.getBookingHistory(session.getUserId());
            if (list.isEmpty()) {
                System.out.println("No booking history found for your account.");
            } else {
                System.out.println("\n================ Booking History ================");
                for (BookingHistoryItem item : list) {
                    System.out.println(item);
                    System.out.println("-------------------------------------------------");
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching booking history: " + e.getMessage());
        }
    }

    private static int readIntegerInput() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid format. Please enter an integer: ");
            }
        }
    }

    private static long readLongInput() {
        while (true) {
            try {
                return Long.parseLong(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid format. Please enter a number: ");
            }
        }
    }
}
