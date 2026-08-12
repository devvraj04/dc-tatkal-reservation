package rmi;

import java.io.Serializable;

public class CoachAvailability implements Serializable {
    private static final long serialVersionUID = 1L;

    private String coachType;
    private int availableSeats;
    private int totalSeats;

    public CoachAvailability(String coachType, int availableSeats, int totalSeats) {
        this.coachType = coachType;
        this.availableSeats = availableSeats;
        this.totalSeats = totalSeats;
    }

    public String getCoachType() { return coachType; }
    public int getAvailableSeats() { return availableSeats; }
    public int getTotalSeats() { return totalSeats; }

    @Override
    public String toString() {
        return String.format("Coach Class: %-5s | Available: %d / %d", coachType, availableSeats, totalSeats);
    }
}
