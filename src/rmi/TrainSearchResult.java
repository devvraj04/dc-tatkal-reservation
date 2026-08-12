package rmi;

import java.io.Serializable;

public class TrainSearchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private long scheduleId;
    private int trainNo;
    private String trainName;
    private String departureTime;
    private String arrivalTime;
    private int dayDiff;

    public TrainSearchResult(long scheduleId, int trainNo, String trainName, String departureTime, String arrivalTime, int dayDiff) {
        this.scheduleId = scheduleId;
        this.trainNo = trainNo;
        this.trainName = trainName;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.dayDiff = dayDiff;
    }

    public long getScheduleId() { return scheduleId; }
    public int getTrainNo() { return trainNo; }
    public String getTrainName() { return trainName; }
    public String getDepartureTime() { return departureTime; }
    public String getArrivalTime() { return arrivalTime; }
    public int getDayDiff() { return dayDiff; }

    @Override
    public String toString() {
        return String.format("[%d] Train %d: %s | Dep: %s | Arr: %s (+%d days)", 
            scheduleId, trainNo, trainName, departureTime, arrivalTime, dayDiff);
    }
}
