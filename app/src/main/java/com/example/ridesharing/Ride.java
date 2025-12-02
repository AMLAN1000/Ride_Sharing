package com.example.ridesharing;

public class Ride {
    private String id;
    private String driverName;
    private String vehicleModel;
    private int availableSeats;
    private double rating;
    private String source;
    private String destination;
    private String departureTime;
    private String arrivalTime;
    private double fare;
    private String driverId;

    // Empty constructor (needed for Firebase later)
    public Ride() {
    }

    // Constructor with parameters
    public Ride(String id, String driverName, String vehicleModel, int availableSeats,
                double rating, String source, String destination,
                String departureTime, String arrivalTime, double fare, String driverId) {
        this.id = id;
        this.driverName = driverName;
        this.vehicleModel = vehicleModel;
        this.availableSeats = availableSeats;
        this.rating = rating;
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.fare = fare;
        this.driverId = driverId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
}