package client;

import rmi.BookingResult;
import rmi.BookingService;
import rmi.CoachAvailability;
import rmi.PassengerInput;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;

public class TatkalConcurrencyTest {

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        
        // Read environment variables or default values (Default 50 clients to test heavy concurrency)
        int totalClients = 50;
        String envClients = System.getenv("TEST_CLIENTS");
        if (envClients != null && !envClients.trim().isEmpty()) {
            try {
                totalClients = Integer.parseInt(envClients.trim());
            } catch (NumberFormatException ignored) {}
        }
        
        long scheduleId = 1;
        String envSched = System.getenv("TEST_SCHEDULE_ID");
        if (envSched != null && !envSched.trim().isEmpty()) {
            try {
                scheduleId = Long.parseLong(envSched.trim());
            } catch (NumberFormatException ignored) {}
        }

        String coachType = "SL";
        String envCoach = System.getenv("TEST_COACH_TYPE");
        if (envCoach != null && !envCoach.trim().isEmpty()) {
            coachType = envCoach.trim().toUpperCase();
        }

        int passengersPerReq = 1;
        String envPass = System.getenv("TEST_PASSENGERS");
        if (envPass != null && !envPass.trim().isEmpty()) {
            try {
                passengersPerReq = Integer.parseInt(envPass.trim());
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("==========================================================");
        System.out.println("       Tatkal Multithreading Concurrency Test Suite       ");
        System.out.println("==========================================================");
        System.out.println("RMI Host            : " + host);
        System.out.println("Target Schedule ID  : " + scheduleId);
        System.out.println("Target Coach Class  : " + coachType);
        System.out.println("Passengers Per Req  : " + passengersPerReq);
        System.out.println("Concurrent Users    : " + totalClients);
        System.out.println("==========================================================");

        try {
            // 1. Connect to RMI Registry and lookup TatkalService (with fallback to BookingService)
            Registry registry = LocateRegistry.getRegistry(host, 1099);
            BookingService service;
            try {
                service = (BookingService) registry.lookup("TatkalService");
                System.out.println("RMI: Successfully looked up 'TatkalService'.");
            } catch (Exception e) {
                service = (BookingService) registry.lookup("BookingService");
                System.out.println("RMI: Looked up 'BookingService' (fallback).");
            }

            // 2. Query initial seat availability
            System.out.println("\nQuerying initial seat availability for Schedule ID " + scheduleId + "...");
            try {
                List<CoachAvailability> avail = service.checkAvailability(12951, "2026-08-15");
                for (CoachAvailability ca : avail) {
                    System.out.println("  Class " + ca.getCoachType() + ": " + ca.getAvailableSeats() + " seat(s) available");
                }
            } catch (Exception e) {
                System.out.println("  Notice: Could not fetch initial availability overview: " + e.getMessage());
            }

            // 3. Setup CountDownLatch barrier for synchronized release
            CountDownLatch readyLatch = new CountDownLatch(totalClients);
            CountDownLatch startLatch = new CountDownLatch(1);
            
            ExecutorService clientPool = Executors.newFixedThreadPool(totalClients);
            List<Future<UserBookingOutcome>> futures = new ArrayList<>();

            System.out.println("\nInitializing " + totalClients + " concurrent client threads...");
            
            final BookingService finalService = service;
            final long targetScheduleId = scheduleId;
            final String targetCoachType = coachType;
            final int targetPassengersCount = passengersPerReq;

            for (int i = 1; i <= totalClients; i++) {
                final int userIndex = i;
                Callable<UserBookingOutcome> task = () -> {
                    String username = String.format("ConcurrentUser-%02d", userIndex);
                    long userId = 1; // Devraj (default user)
                    
                    List<PassengerInput> passengers = new ArrayList<>();
                    for (int p = 1; p <= targetPassengersCount; p++) {
                        String passName = username + (targetPassengersCount > 1 ? "-P" + p : "");
                        passengers.add(new PassengerInput(passName, 25, "MALE", "LOWER", "AADHAAR", "ID-" + userIndex + "-" + p));
                    }

                    // Signal ready and wait for synchronized start latch
                    readyLatch.countDown();
                    startLatch.await();

                    long reqStart = System.currentTimeMillis();
                    try {
                        BookingResult result = finalService.bookTatkalTicket(
                            userId, targetScheduleId, targetCoachType, passengers, "UPI"
                        );
                        long reqDuration = System.currentTimeMillis() - reqStart;
                        return new UserBookingOutcome(userIndex, username, result, null, reqDuration);
                    } catch (Exception e) {
                        long reqDuration = System.currentTimeMillis() - reqStart;
                        return new UserBookingOutcome(userIndex, username, null, e.getMessage(), reqDuration);
                    }
                };
                futures.add(clientPool.submit(task));
            }

            // Wait for all worker threads to reach the starting line
            readyLatch.await();
            System.out.println("All " + totalClients + " worker threads are READY.");
            System.out.println("Releasing start latch! Launching concurrent booking requests...\n");
            
            long testStartTime = System.currentTimeMillis();
            startLatch.countDown(); // Synchronized release!

            // 4. Collect results
            List<UserBookingOutcome> outcomes = new ArrayList<>();
            for (Future<UserBookingOutcome> f : futures) {
                outcomes.add(f.get());
            }
            long totalExecutionTime = System.currentTimeMillis() - testStartTime;

            clientPool.shutdown();

            // 5. Output individual outcomes and process assertions
            System.out.println("==========================================================");
            System.out.println("               INDIVIDUAL BOOKING RESULTS                 ");
            System.out.println("==========================================================");
            
            int confirmedBookings = 0;
            int waitlistedBookings = 0;
            int failedBookings = 0;
            int totalConfirmedSeats = 0;
            Set<String> allocatedSeats = new HashSet<>();
            boolean duplicateDetected = false;

            for (UserBookingOutcome outcome : outcomes) {
                if (outcome.result != null && outcome.result.isSuccess()) {
                    String status = outcome.result.getStatus();
                    List<BookingResult.SeatAssignment> assignments = outcome.result.getSeatAssignments();
                    
                    StringBuilder seatsDesc = new StringBuilder();
                    for (BookingResult.SeatAssignment sa : assignments) {
                        if ("CONFIRMED".equalsIgnoreCase(sa.getStatus())) {
                            totalConfirmedSeats++;
                            String seatUniqueKey = sa.getCoachNumber() + "-" + sa.getSeatNumber();
                            if (!allocatedSeats.add(seatUniqueKey)) {
                                duplicateDetected = true;
                                System.err.println("CRITICAL CONCURRENCY ERROR: Duplicate seat allocated: " + seatUniqueKey);
                            }
                            if (seatsDesc.length() > 0) seatsDesc.append(", ");
                            seatsDesc.append(String.format("Coach %s Seat %d", sa.getCoachNumber(), sa.getSeatNumber()));
                        }
                    }

                    if ("CONFIRMED".equalsIgnoreCase(status)) {
                        confirmedBookings++;
                        System.out.println(String.format("User %02d (%s) -> CONFIRMED -> PNR: %s -> Seats: [%s] (%d ms)",
                            outcome.userIndex, outcome.username, outcome.result.getPnr(), seatsDesc, outcome.durationMs));
                    } else if ("PARTIAL".equalsIgnoreCase(status)) {
                        confirmedBookings++;
                        System.out.println(String.format("User %02d (%s) -> PARTIAL   -> PNR: %s -> Confirmed Seats: [%s] (%d ms)",
                            outcome.userIndex, outcome.username, outcome.result.getPnr(), seatsDesc, outcome.durationMs));
                    } else {
                        waitlistedBookings++;
                        System.out.println(String.format("User %02d (%s) -> WAITLIST  -> PNR: %s -> Status: %s (%d ms)",
                            outcome.userIndex, outcome.username, outcome.result.getPnr(), status, outcome.durationMs));
                    }
                } else {
                    failedBookings++;
                    String reason = (outcome.result != null) ? outcome.result.getMessage() : outcome.errorMessage;
                    System.out.println(String.format("User %02d (%s) -> FAILED    -> Reason: %s (%d ms)",
                        outcome.userIndex, outcome.username, reason, outcome.durationMs));
                }
            }

            // 6. Print summary
            System.out.println("\n==========================================================");
            System.out.println("            TATKAL MULTITHREADING TEST RESULT             ");
            System.out.println("==========================================================");
            System.out.println(String.format("Total Client Requests : %d", totalClients));
            System.out.println(String.format("Confirmed Bookings    : %d", confirmedBookings));
            System.out.println(String.format("Waitlisted Bookings   : %d", waitlistedBookings));
            System.out.println(String.format("Failed Requests       : %d", failedBookings));
            System.out.println(String.format("Total Confirmed Seats : %d", totalConfirmedSeats));
            System.out.println(String.format("Unique Allocated Seats: %d", allocatedSeats.size()));
            System.out.println(String.format("Total Execution Time  : %d ms", totalExecutionTime));
            System.out.println("==========================================================");

            // Assertions
            boolean totalMatches = (confirmedBookings + waitlistedBookings + failedBookings == totalClients);
            boolean seatsUnique = (allocatedSeats.size() == totalConfirmedSeats) && !duplicateDetected;

            if (totalMatches && seatsUnique) {
                System.out.println("STATUS: CONCURRENCY TEST PASSED");
                System.out.println("VERIFICATION: Zero duplicate seats allocated under high concurrent load.");
            } else if (duplicateDetected) {
                System.out.println("STATUS: CONCURRENCY TEST FAILED: DUPLICATE SEAT ALLOCATION");
            } else {
                System.out.println("STATUS: CONCURRENCY TEST FAILED: UNEXPECTED RESULT COUNT");
            }
            System.out.println("==========================================================");

        } catch (Exception e) {
            System.err.println("\nCRITICAL TEST FAILURE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class UserBookingOutcome {
        final int userIndex;
        final String username;
        final BookingResult result;
        final String errorMessage;
        final long durationMs;

        UserBookingOutcome(int userIndex, String username, BookingResult result, String errorMessage, long durationMs) {
            this.userIndex = userIndex;
            this.username = username;
            this.result = result;
            this.errorMessage = errorMessage;
            this.durationMs = durationMs;
        }
    }
}

