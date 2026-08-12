package rmi;

import java.io.Serializable;

public class PassengerInput implements Serializable {
    private static final long serialVersionUID = 1L;

    private long passengerId = -1; // -1 if not pre-saved
    private String name;
    private int age;
    private String gender;
    private String berthPreference;
    private String idProofType;
    private String idProofNumber;

    // Constructor for pre-saved passengers
    public PassengerInput(long passengerId) {
        this.passengerId = passengerId;
    }

    // Constructor for direct input passenger
    public PassengerInput(String name, int age, String gender, String berthPreference, String idProofType, String idProofNumber) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.berthPreference = berthPreference;
        this.idProofType = idProofType;
        this.idProofNumber = idProofNumber;
    }

    public long getPassengerId() { return passengerId; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getBerthPreference() { return berthPreference; }
    public String getIdProofType() { return idProofType; }
    public String getIdProofNumber() { return idProofNumber; }
}
