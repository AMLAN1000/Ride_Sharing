package com.example.ridesharing;

public class RideRequest {
    private String id;
    private String passengerName;
    private String passengerType;
    private double rating;
    private String source;
    private String destination;
    private String preferredTime;
    private String estimatedArrival;
    private double offeredFare;
    private String passengerId;
    private int passengersCount;
    private String specialRequest;

    // Empty constructor (needed for Firebase)
    public RideRequest() {
    }

    // Constructor with parameters
    public RideRequest(String id, String passengerName, String passengerType, double rating,
                       String source, String destination, String preferredTime,
                       String estimatedArrival, double offeredFare, String passengerId,
                       int passengersCount, String specialRequest) {
        this.id = id;
        this.passengerName = passengerName;
        this.passengerType = passengerType;
        this.rating = rating;
        this.source = source;
        this.destination = destination;
        this.preferredTime = preferredTime;
        this.estimatedArrival = estimatedArrival;
        this.offeredFare = offeredFare;
        this.passengerId = passengerId;
        this.passengersCount = passengersCount;
        this.specialRequest = specialRequest;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getPassengerType() { return passengerType; }
    public void setPassengerType(String passengerType) { this.passengerType = passengerType; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getPreferredTime() { return preferredTime; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }

    public String getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(String estimatedArrival) { this.estimatedArrival = estimatedArrival; }

    public double getOfferedFare() { return offeredFare; }
    public void setOfferedFare(double offeredFare) { this.offeredFare = offeredFare; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public int getPassengersCount() { return passengersCount; }
    public void setPassengersCount(int passengersCount) { this.passengersCount = passengersCount; }

    public String getSpecialRequest() { return specialRequest; }
    public void setSpecialRequest(String specialRequest) { this.specialRequest = specialRequest; }
}