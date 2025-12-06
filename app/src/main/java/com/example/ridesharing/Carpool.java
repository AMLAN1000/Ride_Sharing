package com.example.ridesharing;

public class Carpool {
    private String id;
    private String driverName;
    private String vehicleModel;
    private int availableSeats;
    private double rating;
    private String source;
    private String destination;
    private String departureTime;
    private double farePerPassenger;
    private double totalFare;
    private String driverId;
    private int passengerCount;
    private int maxSeats;
    private boolean userAlreadyJoined;
    private String driverPhone;
    private double distance;

    public Carpool(String id, String driverName, String vehicleModel, int availableSeats,
                   double rating, String source, String destination, String departureTime,
                   double farePerPassenger, double totalFare, String driverId,
                   int passengerCount, int maxSeats) {
        this(id, driverName, vehicleModel, availableSeats, rating, source, destination,
                departureTime, farePerPassenger, totalFare, driverId, passengerCount,
                maxSeats, "", 0.0);
    }

    public Carpool(String id, String driverName, String vehicleModel, int availableSeats,
                   double rating, String source, String destination, String departureTime,
                   double farePerPassenger, double totalFare, String driverId,
                   int passengerCount, int maxSeats, String driverPhone, double distance) {
        this.id = id;
        this.driverName = driverName;
        this.vehicleModel = vehicleModel;
        this.availableSeats = availableSeats;
        this.rating = rating;
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.farePerPassenger = farePerPassenger;
        this.totalFare = totalFare;
        this.driverId = driverId;
        this.passengerCount = passengerCount;
        this.maxSeats = maxSeats;
        this.userAlreadyJoined = false;
        this.driverPhone = driverPhone;
        this.distance = distance;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getDriverName() { return driverName; }
    public String getVehicleModel() { return vehicleModel; }
    public int getAvailableSeats() { return availableSeats; }
    public double getRating() { return rating; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public String getDepartureTime() { return departureTime; }
    public double getFarePerPassenger() { return farePerPassenger; }
    public double getTotalFare() { return totalFare; }
    public String getDriverId() { return driverId; }
    public int getPassengerCount() { return passengerCount; }
    public int getMaxSeats() { return maxSeats; }
    public boolean isUserAlreadyJoined() { return userAlreadyJoined; }
    public String getDriverPhone() { return driverPhone; }
    public double getDistance() { return distance; }

    public void setUserAlreadyJoined(boolean userAlreadyJoined) {
        this.userAlreadyJoined = userAlreadyJoined;
    }

    // ✅ ADD THESE TWO ALIAS METHODS
    public String getPickupLocation() { return source; }
    public String getDropLocation() { return destination; }
}