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

    private static boolean showLoggedInMenu() {
        System.out.println("\n--- Dashboard (Logged in as: " + session.getFullName() + ") ---");
        System.out.println("1. Search Trains");
        System.out.println("2. Check Seat Availability");
        System.out.println("3. Book Tatkal Ticket");
        System.out.println("4. Cancel Ticket");
        System.out.println("5. View Booking History");
        System.out.println("6. Logout");
        System.out.println("7. Exit");
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
                System.out.println("Logging out...");
                session = null;
                break;
            case 7:
                return false;
            default:
                System.out.println("Invalid option. Please try again.\n");
        }
        return true;
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
            System.out.println("Sending booking request to server...");
            BookingResult result = service.bookTatkalTicket(session.getUserId(), schedId, coachType, passengers, payMode);
            System.out.println("\n----------------------------------------------");
            System.out.println(result);
            System.out.println("----------------------------------------------");
        } catch (Exception e) {
            System.err.println("Booking failed with remote error: " + e.getMessage());
        }
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
