package rmi;

import java.io.Serializable;
import java.util.List;

public class BookingHistoryItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pnr;
    private int trainNo;
    private String trainName;
    private String journeyDate;
    private String sourceStation;
    private String destinationStation;
    private String bookingStatus;
    private double totalFare;
    private String bookingTimestamp;
    private List<PassengerDetail> passengers;

    public BookingHistoryItem(String pnr, int trainNo, String trainName, String journeyDate, 
                              String sourceStation, String destinationStation, String bookingStatus, 
                              double totalFare, String bookingTimestamp, List<PassengerDetail> passengers) {
        this.pnr = pnr;
        this.trainNo = trainNo;
        this.trainName = trainName;
        this.journeyDate = journeyDate;
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.bookingStatus = bookingStatus;
        this.totalFare = totalFare;
        this.bookingTimestamp = bookingTimestamp;
        this.passengers = passengers;
    }

    public String getPnr() { return pnr; }
    public int getTrainNo() { return trainNo; }
    public String getTrainName() { return trainName; }
    public String getJourneyDate() { return journeyDate; }
    public String getSourceStation() { return sourceStation; }
    public String getDestinationStation() { return destinationStation; }
    public String getBookingStatus() { return bookingStatus; }
    public double getTotalFare() { return totalFare; }
    public String getBookingTimestamp() { return bookingTimestamp; }
    public List<PassengerDetail> getPassengers() { return passengers; }

    public static class PassengerDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private String passengerName;
        private String coachNumber;
        private int seatNumber;
        private String berthType;
        private String status;

        public PassengerDetail(String passengerName, String coachNumber, int seatNumber, String berthType, String status) {
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
            if (seatNumber <= 0 || coachNumber == null) {
                return String.format("%s (%s)", passengerName, status);
            }
            return String.format("%s (%s, Seat %d %s) - %s", passengerName, coachNumber, seatNumber, berthType, status);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("PNR: %s | Train: %d - %s | Date: %s\n", pnr, trainNo, trainName, journeyDate));
        sb.append(String.format("Route: %s -> %s | Status: %s | Total Fare: Rs. %.2f (Booked at: %s)\n", 
            sourceStation, destinationStation, bookingStatus, totalFare, bookingTimestamp));
        sb.append("Passengers:\n");
        for (PassengerDetail pd : passengers) {
            sb.append("  - ").append(pd.toString()).append("\n");
        }
        return sb.toString();
    }
}
