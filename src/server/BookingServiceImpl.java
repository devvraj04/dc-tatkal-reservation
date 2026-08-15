package server;

import rmi.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class BookingServiceImpl extends UnicastRemoteObject implements BookingService {
    private static final long serialVersionUID = 1L;
    private final Random random = new Random();
    private final ExecutorService bookingExecutor;

    public BookingServiceImpl() throws RemoteException {
        super();
        // Core pool: 8, Max pool: 32, KeepAlive: 60s, Bounded Queue: 100 with CallerRunsPolicy
        this.bookingExecutor = new ThreadPoolExecutor(
            8, 
            32, 
            60L, 
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Register JVM shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownExecutor));
        System.out.println("BookingServiceImpl: Remote object instantiated with ThreadPoolExecutor (8 core, 32 max).");
    }

    private void shutdownExecutor() {
        System.out.println("BookingServiceImpl: Shutting down ExecutorService...");
        bookingExecutor.shutdown();
        try {
            if (!bookingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                bookingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            bookingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("BookingServiceImpl: ExecutorService shutdown completed.");
    }

    // 1. User login
    @Override
    public UserSession login(String email, String password) throws RemoteException {
        System.out.println("Server: Login attempt for " + email);
        String query = "SELECT user_id, full_name, email, password_hash FROM users WHERE email = ? AND account_status = 'ACTIVE'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPass = rs.getString("password_hash");
                    // Check credentials (plain text check for simplicity of experiment demo, or standard hash check)
                    if (dbPass.equals(password)) {
                        long userId = rs.getLong("user_id");
                        String fullName = rs.getString("full_name");
                        
                        // Update last login
                        String updateLogin = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateLogin)) {
                            updatePs.setLong(1, userId);
                            updatePs.executeUpdate();
                        }
                        
                        System.out.println("Server: Login successful for " + email + " (ID: " + userId + ")");
                        return new UserSession(userId, fullName, email);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Database error during login: " + e.getMessage());
        }
        throw new RemoteException("Invalid email or password.");
    }

    // 2. Search train schedule
    @Override
    public List<TrainSearchResult> searchTrain(String sourceStation, String destinationStation, String dateStr) throws RemoteException {
        System.out.println("Server: Searching train from " + sourceStation + " to " + destinationStation + " on " + dateStr);
        List<TrainSearchResult> results = new ArrayList<>();
        
        String query = "SELECT ts.schedule_id, t.train_no, t.train_name, " +
                       "       r1.departure_time, r2.arrival_time, " +
                       "       (r2.day_number - r1.day_number) as day_diff " +
                       "FROM train_schedules ts " +
                       "JOIN trains t ON ts.train_no = t.train_no " +
                       "JOIN routes r1 ON (t.train_no = r1.train_no AND r1.station_code = ?) " +
                       "JOIN routes r2 ON (t.train_no = r2.train_no AND r2.station_code = ?) " +
                       "WHERE r1.stop_number < r2.stop_number " +
                       "  AND ts.journey_date = CAST(? AS DATE) " +
                       "  AND ts.schedule_status = 'SCHEDULED' " +
                       "ORDER BY r1.departure_time ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, sourceStation.toUpperCase());
            ps.setString(2, destinationStation.toUpperCase());
            ps.setString(3, dateStr);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new TrainSearchResult(
                        rs.getLong("schedule_id"),
                        rs.getInt("train_no"),
                        rs.getString("train_name"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time"),
                        rs.getInt("day_diff")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Database error searching trains: " + e.getMessage());
        }
        return results;
    }

    // 3. Check seat availability for a schedule/train on a given date
    @Override
    public List<CoachAvailability> checkAvailability(int trainNo, String dateStr) throws RemoteException {
        System.out.println("Server: Checking availability for Train " + trainNo + " on " + dateStr);
        List<CoachAvailability> availabilities = new ArrayList<>();
        
        String query = "SELECT c.coach_type, " +
                       "       COUNT(CASE WHEN sa.booking_status = 'AVAILABLE' THEN 1 END) as available_seats, " +
                       "       COUNT(sa.allocation_id) as total_seats " +
                       "FROM train_schedules ts " +
                       "JOIN coaches c ON ts.train_no = c.train_no " +
                       "JOIN seats s ON c.coach_id = s.coach_id " +
                       "LEFT JOIN seat_allocations sa ON (ts.schedule_id = sa.schedule_id AND s.seat_id = sa.seat_id) " +
                       "WHERE ts.train_no = ? AND ts.journey_date = CAST(? AS DATE) " +
                       "GROUP BY c.coach_type " +
                       "ORDER BY c.coach_type DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, trainNo);
            ps.setString(2, dateStr);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    availabilities.add(new CoachAvailability(
                        rs.getString("coach_type"),
                        rs.getInt("available_seats"),
                        rs.getInt("total_seats")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Database error checking availability: " + e.getMessage());
        }
        return availabilities;
    }

    // 4. Book Tatkal ticket (locks available seats using transaction and FOR UPDATE SKIP LOCKED processed concurrently via ThreadPoolExecutor)
    @Override
    public BookingResult bookTatkalTicket(long userId, long scheduleId, String coachType, List<PassengerInput> passengers, String paymentMode) throws RemoteException {
        if (passengers.isEmpty()) {
            return new BookingResult(false, null, "FAILED", 0, "No passengers specified.", new ArrayList<>());
        }

        Callable<BookingResult> bookingTask = () -> {
            String threadName = Thread.currentThread().getName();
            System.out.println(String.format("[%s] Booking request received for user %d | Schedule: %d | Coach: %s | Passengers: %d", 
                threadName, userId, scheduleId, coachType, passengers.size()));

            Connection conn = null;
            try {
                conn = DBConnection.getConnection();
                conn.setAutoCommit(false); // Begin transaction

                // 1. Get schedule info and check if valid
                String scheduleQuery = "SELECT t.source_station_code, t.destination_station_code FROM train_schedules ts JOIN trains t ON ts.train_no = t.train_no WHERE ts.schedule_id = ?";
                String srcStation = null;
                String destStation = null;
                try (PreparedStatement ps = conn.prepareStatement(scheduleQuery)) {
                    ps.setLong(1, scheduleId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            srcStation = rs.getString("source_station_code");
                            destStation = rs.getString("destination_station_code");
                        } else {
                            conn.rollback();
                            System.out.println(String.format("[%s] Failed: Invalid schedule ID %d", threadName, scheduleId));
                            return new BookingResult(false, null, "FAILED", 0, "Invalid train schedule ID.", new ArrayList<>());
                        }
                    }
                }

                // 2. Query available seats and LOCK them using FOR UPDATE SKIP LOCKED to avoid deadlock/contention delays
                String seatQuery = "SELECT sa.allocation_id, sa.seat_id, s.seat_number, c.coach_number, s.berth_type " +
                                   "FROM seat_allocations sa " +
                                   "JOIN seats s ON sa.seat_id = s.seat_id " +
                                   "JOIN coaches c ON s.coach_id = c.coach_id " +
                                   "WHERE sa.schedule_id = ? " +
                                   "  AND c.coach_type = CAST(? AS coach_type_enum) " +
                                   "  AND sa.booking_status = 'AVAILABLE' " +
                                   "ORDER BY sa.allocation_id " +
                                   "LIMIT ? " +
                                   "FOR UPDATE OF sa SKIP LOCKED"; // Lock ONLY seat_allocations row, ignoring joined tables
                
                List<LockedSeat> lockedSeats = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(seatQuery)) {
                    ps.setLong(1, scheduleId);
                    ps.setString(2, coachType.toUpperCase());
                    ps.setInt(3, passengers.size());
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            lockedSeats.add(new LockedSeat(
                                rs.getLong("allocation_id"),
                                rs.getLong("seat_id"),
                                rs.getInt("seat_number"),
                                rs.getString("coach_number"),
                                rs.getString("berth_type")
                            ));
                        }
                    }
                }

                System.out.println(String.format("[%s] Allocated %d seat(s) for user %d", threadName, lockedSeats.size(), userId));

                // 3. Generate unique PNR (10 digits)
                String pnr = String.valueOf(1000000000L + random.nextInt(900000000));
                
                // 4. Calculate fare based on coach type
                double farePerSeat;
                switch (coachType.toUpperCase()) {
                    case "1A": farePerSeat = 1800.00; break;
                    case "2A": farePerSeat = 1100.00; break;
                    case "3A": farePerSeat = 650.00; break;
                    case "SL": farePerSeat = 250.00; break;
                    default: farePerSeat = 200.00; break;
                }
                double totalFare = farePerSeat * passengers.size();

                // 5. Insert passenger profiles if they don't exist, and collect passenger IDs
                List<Long> resolvedPassengerIds = new ArrayList<>();
                List<String> passengerNames = new ArrayList<>();
                for (PassengerInput pi : passengers) {
                    long passId = pi.getPassengerId();
                    if (passId == -1) {
                        String insertPass = "INSERT INTO passengers (user_id, passenger_name, age, gender, berth_preference, id_proof_type, id_proof_number) " +
                                            "VALUES (?, ?, ?, CAST(? AS gender_enum), CAST(? AS berth_preference_enum), ?, ?) RETURNING passenger_id";
                        try (PreparedStatement ps = conn.prepareStatement(insertPass)) {
                            ps.setLong(1, userId);
                            ps.setString(2, pi.getName());
                            ps.setInt(3, pi.getAge());
                            ps.setString(4, pi.getGender() != null ? pi.getGender().toUpperCase().trim() : "MALE");
                            ps.setString(5, pi.getBerthPreference() != null && !pi.getBerthPreference().trim().isEmpty() 
                                                    ? pi.getBerthPreference().toUpperCase().trim() 
                                                    : "NO_PREFERENCE");
                            ps.setString(6, pi.getIdProofType());
                            ps.setString(7, pi.getIdProofNumber());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    passId = rs.getLong(1);
                                }
                            }
                        }
                        passengerNames.add(pi.getName());
                    } else {
                        String fetchName = "SELECT passenger_name FROM passengers WHERE passenger_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(fetchName)) {
                            ps.setLong(1, passId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    passengerNames.add(rs.getString("passenger_name"));
                                } else {
                                    passengerNames.add("Passenger #" + passId);
                                }
                            }
                        }
                    }
                    resolvedPassengerIds.add(passId);
                }

                // Determine if booking is confirmed or waitlisted
                boolean allConfirmed = lockedSeats.size() == passengers.size();
                String bookingStatus = allConfirmed ? "CONFIRMED" : (lockedSeats.isEmpty() ? "WAITING" : "PARTIAL");

                // 6. Insert Booking record
                String insertBooking = "INSERT INTO bookings (pnr, user_id, schedule_id, source_station_code, destination_station_code, coach_type, booking_type, quota, booking_status, total_fare) " +
                                       "VALUES (?, ?, ?, ?, ?, CAST(? AS coach_type_enum), 'TATKAL', 'TATKAL', CAST(? AS booking_status_enum), ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertBooking)) {
                    ps.setString(1, pnr);
                    ps.setLong(2, userId);
                    ps.setLong(3, scheduleId);
                    ps.setString(4, srcStation);
                    ps.setString(5, destStation);
                    ps.setString(6, coachType.toUpperCase());
                    ps.setString(7, bookingStatus);
                    ps.setDouble(8, totalFare);
                    ps.executeUpdate();
                }

                // 7. Associate passengers and assign seats or waitlist
                List<BookingResult.SeatAssignment> seatAssignments = new ArrayList<>();
                for (int i = 0; i < passengers.size(); i++) {
                    long passId = resolvedPassengerIds.get(i);
                    String passName = passengerNames.get(i);

                    if (i < lockedSeats.size()) {
                        LockedSeat seat = lockedSeats.get(i);
                        
                        String updateAlloc = "UPDATE seat_allocations SET booking_status = 'BOOKED', booking_type = 'TATKAL', pnr = ?, allocated_at = CURRENT_TIMESTAMP WHERE allocation_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(updateAlloc)) {
                            ps.setString(1, pnr);
                            ps.setLong(2, seat.allocationId);
                            ps.executeUpdate();
                        }

                        String insertBp = "INSERT INTO booking_passengers (pnr, passenger_id, allocation_id, passenger_status, fare) " +
                                           "VALUES (?, ?, ?, 'CONFIRMED', ?)";
                        try (PreparedStatement ps = conn.prepareStatement(insertBp)) {
                            ps.setString(1, pnr);
                            ps.setLong(2, passId);
                            ps.setLong(3, seat.allocationId);
                            ps.setDouble(4, farePerSeat);
                            ps.executeUpdate();
                        }

                        seatAssignments.add(new BookingResult.SeatAssignment(
                            passName, seat.coachNumber, seat.seatNumber, seat.berthType, "CONFIRMED"
                        ));
                    } else {
                        String insertBp = "INSERT INTO booking_passengers (pnr, passenger_id, allocation_id, passenger_status, fare) " +
                                           "VALUES (?, ?, NULL, 'WAITING', ?)";
                        try (PreparedStatement ps = conn.prepareStatement(insertBp)) {
                            ps.setString(1, pnr);
                            ps.setLong(2, passId);
                            ps.setDouble(3, farePerSeat);
                            ps.executeUpdate();
                        }

                        int nextWlNo = 1;
                        String maxWlQuery = "SELECT COALESCE(MAX(waitlist_number), 0) FROM waitlists WHERE schedule_id = ? AND coach_type = CAST(? AS coach_type_enum)";
                        try (PreparedStatement ps = conn.prepareStatement(maxWlQuery)) {
                            ps.setLong(1, scheduleId);
                            ps.setString(2, coachType.toUpperCase());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    nextWlNo = rs.getInt(1) + 1;
                                }
                            }
                        }

                        String insertWl = "INSERT INTO waitlists (pnr, schedule_id, coach_type, waitlist_number, current_position, waitlist_type, status) " +
                                          "VALUES (?, ?, CAST(? AS coach_type_enum), ?, ?, 'TQWL', 'WAITING')";
                        try (PreparedStatement ps = conn.prepareStatement(insertWl)) {
                            ps.setString(1, pnr);
                            ps.setLong(2, scheduleId);
                            ps.setString(3, coachType.toUpperCase());
                            ps.setInt(4, nextWlNo);
                            ps.setInt(5, nextWlNo);
                            ps.executeUpdate();
                        }

                        seatAssignments.add(new BookingResult.SeatAssignment(
                            passName, null, 0, null, "WAITING"
                        ));
                    }
                }

                // 8. Log Payment
                String insertPayment = "INSERT INTO payments (pnr, amount, payment_mode, transaction_id, payment_status) " +
                                       "VALUES (?, ?, CAST(? AS payment_mode_enum), ?, 'SUCCESS')";
                try (PreparedStatement ps = conn.prepareStatement(insertPayment)) {
                    ps.setString(1, pnr);
                    ps.setDouble(2, totalFare);
                    ps.setString(3, paymentMode.toUpperCase());
                    ps.setString(4, "TXN" + System.currentTimeMillis() + random.nextInt(100));
                    ps.executeUpdate();
                }

                // 9. Send Notification
                String insertNotify = "INSERT INTO notifications (user_id, pnr, notification_type, message) " +
                                      "VALUES (?, ?, 'BOOKING', ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertNotify)) {
                    ps.setLong(1, userId);
                    ps.setString(2, pnr);
                    ps.setString(3, "Ticket booked successfully for " + passengers.size() + " passengers. PNR: " + pnr);
                    ps.executeUpdate();
                }

                conn.commit(); // Commit transaction
                System.out.println(String.format("[%s] Booking committed for user %d | PNR: %s | Status: %s", threadName, userId, pnr, bookingStatus));
                
                String outcomeMsg = allConfirmed ? "All passengers confirmed." : "Some/all passengers waitlisted.";
                return new BookingResult(true, pnr, bookingStatus, totalFare, outcomeMsg, seatAssignments);

            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                        System.err.println(String.format("[%s] Transaction rolled back due to error: %s", Thread.currentThread().getName(), e.getMessage()));
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                e.printStackTrace();
                throw new RemoteException("Transaction failed: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        try {
            Future<BookingResult> future = bookingExecutor.submit(bookingTask);
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Server execution interrupted: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RemoteException) {
                throw (RemoteException) cause;
            }
            throw new RemoteException("Booking task execution failed: " + (cause != null ? cause.getMessage() : e.getMessage()), cause);
        } catch (RejectedExecutionException e) {
            throw new RemoteException("Server high load: Booking request rejected.", e);
        }
    }

    // 5. Cancel ticket (moves confirmed seats to AVAILABLE and promotes waitlisted passenger if present)
    @Override
    public CancellationResult cancelTicket(String pnr, long userId) throws RemoteException {
        System.out.println("Server: Cancellation request. PNR: " + pnr + " | User: " + userId);
        Connection conn = null;
        
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Fetch booking with locking
            String bookingQuery = "SELECT user_id, schedule_id, coach_type, booking_status, total_fare FROM bookings WHERE pnr = ? FOR UPDATE";
            long scheduleId;
            String coachType;
            String bookingStatus;
            double totalFare;
            try (PreparedStatement ps = conn.prepareStatement(bookingQuery)) {
                ps.setString(1, pnr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long dbUserId = rs.getLong("user_id");
                        if (dbUserId != userId) {
                            conn.rollback();
                            return new CancellationResult(false, pnr, 0, 0, "Access denied: PNR does not belong to this user.");
                        }
                        scheduleId = rs.getLong("schedule_id");
                        coachType = rs.getString("coach_type");
                        bookingStatus = rs.getString("booking_status");
                        totalFare = rs.getDouble("total_fare");
                    } else {
                        conn.rollback();
                        return new CancellationResult(false, pnr, 0, 0, "PNR not found.");
                    }
                }
            }

            if ("CANCELLED".equals(bookingStatus)) {
                conn.rollback();
                return new CancellationResult(false, pnr, 0, 0, "Ticket is already cancelled.");
            }

            // 2. Calculate refund (Deduct flat cancellation charge of Rs 100 per seat/passenger)
            // Wait, let's see how many booking passengers there are
            int passengerCount = 0;
            String countQuery = "SELECT COUNT(*) FROM booking_passengers WHERE pnr = ?";
            try (PreparedStatement ps = conn.prepareStatement(countQuery)) {
                ps.setString(1, pnr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        passengerCount = rs.getInt(1);
                    }
                }
            }
            double cancelCharge = 100.00 * passengerCount;
            double refundAmt = Math.max(0, totalFare - cancelCharge);

            // 3. Update bookings table status to CANCELLED
            String updateBooking = "UPDATE bookings SET booking_status = 'CANCELLED', cancellation_time = CURRENT_TIMESTAMP WHERE pnr = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateBooking)) {
                ps.setString(1, pnr);
                ps.executeUpdate();
            }

            // 4. Update waitlists if this booking was waitlisted (set status = CANCELLED)
            String updateWlStatus = "UPDATE waitlists SET status = 'CANCELLED' WHERE pnr = ? AND status = 'WAITING'";
            try (PreparedStatement ps = conn.prepareStatement(updateWlStatus)) {
                ps.setString(1, pnr);
                ps.executeUpdate();
            }

            // 5. Fetch passenger seat allocations to free them
            String fetchSeats = "SELECT bp.booking_passenger_id, sa.seat_id, bp.allocation_id, bp.passenger_status " +
                                "FROM booking_passengers bp " +
                                "LEFT JOIN seat_allocations sa ON bp.allocation_id = sa.allocation_id " +
                                "WHERE bp.pnr = ?";
            List<PassengerSeatInfo> seatsToFree = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(fetchSeats)) {
                ps.setString(1, pnr);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        seatsToFree.add(new PassengerSeatInfo(
                            rs.getLong("booking_passenger_id"),
                            rs.getLong("seat_id"),
                            rs.getLong("allocation_id"),
                            rs.getString("passenger_status")
                        ));
                    }
                }
            }

            // Update booking passengers status to CANCELLED
            String updateBps = "UPDATE booking_passengers SET passenger_status = 'CANCELLED' WHERE pnr = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateBps)) {
                ps.setString(1, pnr);
                ps.executeUpdate();
            }

            // 6. Free allocations and check waitlist promotions
            for (PassengerSeatInfo psi : seatsToFree) {
                if ("CONFIRMED".equalsIgnoreCase(psi.passengerStatus) && psi.allocationId > 0) {
                    
                    // We check if there's any active waitlisted passenger for this schedule & coach type
                    String wlSelect = "SELECT w.waitlist_id, w.pnr, bp.booking_passenger_id, b.user_id " +
                                       "FROM waitlists w " +
                                       "JOIN bookings b ON w.pnr = b.pnr " +
                                       "JOIN booking_passengers bp ON (w.pnr = bp.pnr AND bp.passenger_status = 'WAITING') " +
                                       "WHERE w.schedule_id = ? " +
                                       "  AND w.coach_type = CAST(? AS coach_type_enum) " +
                                       "  AND w.status = 'WAITING' " +
                                       "ORDER BY w.waitlist_number ASC " +
                                       "LIMIT 1 " +
                                       "FOR UPDATE"; // Exclusive lock to prevent race on waitlist promotion
                    
                    long promoteWlId = -1;
                    String promotePnr = null;
                    long promoteBpId = -1;
                    long promoteUserId = -1;

                    try (PreparedStatement ps = conn.prepareStatement(wlSelect)) {
                        ps.setLong(1, scheduleId);
                        ps.setString(2, coachType);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                promoteWlId = rs.getLong("waitlist_id");
                                promotePnr = rs.getString("pnr");
                                promoteBpId = rs.getLong("booking_passenger_id");
                                promoteUserId = rs.getLong("user_id");
                            }
                        }
                    }

                    if (promoteWlId != -1) {
                        // We promote the waitlisted passenger directly to this seat!
                        System.out.println("Server: Promoting waitlist ID " + promoteWlId + " (PNR: " + promotePnr + ") to freed seat ID: " + psi.seatId);
                        
                        // Update seat allocation to the new PNR
                        String updateAlloc = "UPDATE seat_allocations SET pnr = ?, allocated_at = CURRENT_TIMESTAMP WHERE allocation_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(updateAlloc)) {
                            ps.setString(1, promotePnr);
                            ps.setLong(2, psi.allocationId);
                            ps.executeUpdate();
                        }

                        // Update booking_passengers table for promoted passenger
                        String updateBpPromoted = "UPDATE booking_passengers SET allocation_id = ?, passenger_status = 'CONFIRMED' WHERE booking_passenger_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(updateBpPromoted)) {
                            ps.setLong(1, psi.allocationId);
                            ps.setLong(2, promoteBpId);
                            ps.executeUpdate();
                        }

                        // Update waitlist entry
                        String updateWlEntry = "UPDATE waitlists SET status = 'CONFIRMED', confirmed_at = CURRENT_TIMESTAMP WHERE waitlist_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(updateWlEntry)) {
                            ps.setLong(1, promoteWlId);
                            ps.executeUpdate();
                        }

                        // Check if all passengers under the promoted PNR are confirmed, to update parent booking status
                        String checkPnrStatus = "SELECT COUNT(*) FROM booking_passengers WHERE pnr = ? AND passenger_status = 'WAITING'";
                        boolean pnrNowAllConfirmed = false;
                        try (PreparedStatement ps = conn.prepareStatement(checkPnrStatus)) {
                            ps.setString(1, promotePnr);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next() && rs.getInt(1) == 0) {
                                    pnrNowAllConfirmed = true;
                                }
                            }
                        }
                        
                        if (pnrNowAllConfirmed) {
                            String updatePromotedBooking = "UPDATE bookings SET booking_status = 'CONFIRMED' WHERE pnr = ?";
                            try (PreparedStatement ps = conn.prepareStatement(updatePromotedBooking)) {
                                ps.setString(1, promotePnr);
                                ps.executeUpdate();
                            }
                        } else {
                            String updatePromotedBooking = "UPDATE bookings SET booking_status = 'PARTIAL' WHERE pnr = ?";
                            try (PreparedStatement ps = conn.prepareStatement(updatePromotedBooking)) {
                                ps.setString(1, promotePnr);
                                ps.executeUpdate();
                            }
                        }

                        // Notify promoted user
                        String insertNotify = "INSERT INTO notifications (user_id, pnr, notification_type, message) " +
                                              "VALUES (?, ?, 'WAITLIST', ?)";
                        try (PreparedStatement ps = conn.prepareStatement(insertNotify)) {
                            ps.setLong(1, promoteUserId);
                            ps.setString(2, promotePnr);
                            ps.setString(3, "Congratulations! Your waitlist ticket under PNR " + promotePnr + " has been CONFIRMED.");
                            ps.executeUpdate();
                        }

                    } else {
                        // No waitlist to promote, so just free the allocation!
                        String freeAlloc = "UPDATE seat_allocations SET booking_status = 'AVAILABLE', pnr = NULL, allocated_at = NULL WHERE allocation_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(freeAlloc)) {
                            ps.setLong(1, psi.allocationId);
                            ps.executeUpdate();
                        }
                    }
                }
            }

            // 7. Log Cancellation details
            String insertCancel = "INSERT INTO cancellations (pnr, cancelled_by, refund_amount, cancellation_charge) " +
                                  "VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertCancel)) {
                ps.setString(1, pnr);
                ps.setLong(2, userId);
                ps.setDouble(3, refundAmt);
                ps.setDouble(4, cancelCharge);
                ps.executeUpdate();
            }

            // 8. Notification for cancellation
            String insertNotify = "INSERT INTO notifications (user_id, pnr, notification_type, message) " +
                                  "VALUES (?, ?, 'CANCELLATION', ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertNotify)) {
                ps.setLong(1, userId);
                ps.setString(2, pnr);
                ps.setString(3, "Ticket successfully cancelled for PNR " + pnr + ". Refund amount: Rs. " + refundAmt);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("Server: Cancellation transaction committed for PNR: " + pnr + ". Refund: Rs. " + refundAmt);
            return new CancellationResult(true, pnr, refundAmt, cancelCharge, "Ticket cancelled successfully.");

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Server: Cancellation rolled back due to error.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            throw new RemoteException("Cancellation failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 6. Get booking history for a user
    @Override
    public List<BookingHistoryItem> getBookingHistory(long userId) throws RemoteException {
        System.out.println("Server: Retrieving booking history for user: " + userId);
        List<BookingHistoryItem> history = new ArrayList<>();
        
        String query = "SELECT b.pnr, b.schedule_id, b.source_station_code, b.destination_station_code, " +
                       "       b.booking_status, b.total_fare, b.booking_timestamp, " +
                       "       ts.train_no, t.train_name, ts.journey_date " +
                       "FROM bookings b " +
                       "JOIN train_schedules ts ON b.schedule_id = ts.schedule_id " +
                       "JOIN trains t ON ts.train_no = t.train_no " +
                       "WHERE b.user_id = ? " +
                       "ORDER BY b.booking_timestamp DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String pnr = rs.getString("pnr");
                    
                    // Fetch passenger details for this PNR
                    List<BookingHistoryItem.PassengerDetail> passList = new ArrayList<>();
                    String passQuery = "SELECT bp.passenger_status, bp.fare, p.passenger_name, " +
                                       "       s.seat_number, c.coach_number, s.berth_type " +
                                       "FROM booking_passengers bp " +
                                       "JOIN passengers p ON bp.passenger_id = p.passenger_id " +
                                       "LEFT JOIN seat_allocations sa ON bp.allocation_id = sa.allocation_id " +
                                       "LEFT JOIN seats s ON sa.seat_id = s.seat_id " +
                                       "LEFT JOIN coaches c ON s.coach_id = c.coach_id " +
                                       "WHERE bp.pnr = ?";
                    
                    try (PreparedStatement psPass = conn.prepareStatement(passQuery)) {
                        psPass.setString(1, pnr);
                        try (ResultSet rsPass = psPass.executeQuery()) {
                            while (rsPass.next()) {
                                passList.add(new BookingHistoryItem.PassengerDetail(
                                    rsPass.getString("passenger_name"),
                                    rsPass.getString("coach_number"),
                                    rsPass.getInt("seat_number"),
                                    rsPass.getString("berth_type"),
                                    rsPass.getString("passenger_status")
                                ));
                            }
                        }
                    }
                    
                    history.add(new BookingHistoryItem(
                        pnr,
                        rs.getInt("train_no"),
                        rs.getString("train_name"),
                        rs.getString("journey_date"),
                        rs.getString("source_station_code"),
                        rs.getString("destination_station_code"),
                        rs.getString("booking_status"),
                        rs.getDouble("total_fare"),
                        rs.getString("booking_timestamp"),
                        passList
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Database error fetching booking history: " + e.getMessage());
        }
        return history;
    }

    // Helper classes for transaction locking representation
    private static class LockedSeat {
        long allocationId;
        long seatId;
        int seatNumber;
        String coachNumber;
        String berthType;

        LockedSeat(long allocationId, long seatId, int seatNumber, String coachNumber, String berthType) {
            this.allocationId = allocationId;
            this.seatId = seatId;
            this.seatNumber = seatNumber;
            this.coachNumber = coachNumber;
            this.berthType = berthType;
        }
    }

    private static class PassengerSeatInfo {
        long bpId;
        long seatId;
        long allocationId;
        String passengerStatus;

        PassengerSeatInfo(long bpId, long seatId, long allocationId, String passengerStatus) {
            this.bpId = bpId;
            this.seatId = seatId;
            this.allocationId = allocationId;
            this.passengerStatus = passengerStatus;
        }
    }
}
