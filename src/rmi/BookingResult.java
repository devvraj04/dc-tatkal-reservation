package rmi;

import java.io.Serializable;
import java.util.List;

public class BookingResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String pnr;
    private String status; // CONFIRMED, WAITLIST, FAILED
    private double totalFare;
    private String message;
    private List<SeatAssignment> seatAssignments;

    public BookingResult(boolean success, String pnr, String status, double totalFare, String message, List<SeatAssignment> seatAssignments) {
        this.success = success;
        this.pnr = pnr;
        this.status = status;
        this.totalFare = totalFare;
        this.message = message;
        this.seatAssignments = seatAssignments;
    }

    public boolean isSuccess() { return success; }
    public String getPnr() { return pnr; }
    public String getStatus() { return status; }
    public double getTotalFare() { return totalFare; }
    public String getMessage() { return message; }
    public List<SeatAssignment> getSeatAssignments() { return seatAssignments; }

    public static class SeatAssignment implements Serializable {
        private static final long serialVersionUID = 1L;
        private String passengerName;
        private String coachNumber;
        private int seatNumber;
        private String berthType;
        private String status; // CONFIRMED, WAITLIST

        public SeatAssignment(String passengerName, String coachNumber, int seatNumber, String berthType, String status) {
            this.passengerName = passengerName;
            this.coachNumber = coachNumber;
            this.seatNumber = seatNumber;
            this.berthType = berthType;
            this.status = status;
        }

        public String getPassengerName() { return passengerName; }
        public String getCoachNumber() { return coachNumber; }
        public int getSeatNumber() { return seatNumber; }
        public String getBerthType() { return berthType; }
        public String getStatus() { return status; }

        @Override
        public String toString() {
            if ("WAITING".equalsIgnoreCase(status) || "WAITLIST".equalsIgnoreCase(status)) {
                return String.format("%s: Waitlisted", passengerName);
            }
            return String.format("%s: Coach %s, Seat %d (%s) - %s", passengerName, coachNumber, seatNumber, berthType, status);
        }
    }

    @Override
    public String toString() {
        if (!success) {
            return "Booking Failed: " + message;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Booking Successful! PNR: %s | Status: %s | Fare: Rs. %.2f\n", pnr, status, totalFare));
        sb.append("Seat Allocations:\n");
        for (SeatAssignment sa : seatAssignments) {
            sb.append("  - ").append(sa.toString()).append("\n");
        }
        return sb.toString();
    }
}
