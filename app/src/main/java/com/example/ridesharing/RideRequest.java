package com.example.ridesharing;

public class RideRequest {
    private String id;
    private String passengerName;
    private String userType;
    private double rating;
    private String source;
    private String destination;
    private String departureTime;
    private String timeRemaining;
    private double offeredFare;
    private String passengerId;
    private int passengers;
    private String specialRequest;
    private String passengerPhoto;
    private String passengerPhone;
    private String vehicleType;
    private Double pickupLat;
    private Double pickupLng;
    private Double dropLat;
    private Double dropLng;
    private Double distance;
    private Double duration;
    private String trafficLevel;

    public RideRequest(String id, String passengerName, String userType, double rating,
                       String source, String destination, String departureTime,
                       String timeRemaining, double offeredFare, String passengerId,
                       int passengers, String specialRequest) {
        this.id = id;
        this.passengerName = passengerName;
        this.userType = userType;
        this.rating = rating;
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.timeRemaining = timeRemaining;
        this.offeredFare = offeredFare;
        this.passengerId = passengerId;
        this.passengers = passengers;
        this.specialRequest = specialRequest;
    }

    // Enhanced constructor with all fields
    public RideRequest(String id, String passengerName, String userType, double rating,
                       String source, String destination, String departureTime,
                       String timeRemaining, double offeredFare, String passengerId,
                       int passengers, String specialRequest, String passengerPhoto,
                       String passengerPhone, String vehicleType, Double pickupLat,
                       Double pickupLng, Double dropLat, Double dropLng, Double distance,
                       Double duration, String trafficLevel) {
        this(id, passengerName, userType, rating, source, destination, departureTime,
                timeRemaining, offeredFare, passengerId, passengers, specialRequest);
        this.passengerPhoto = passengerPhoto;
        this.passengerPhone = passengerPhone;
        this.vehicleType = vehicleType;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropLat = dropLat;
        this.dropLng = dropLng;
        this.distance = distance;
        this.duration = duration;
        this.trafficLevel = trafficLevel;
    }

    // Getters
    public String getId() { return id; }
    public String getPassengerName() { return passengerName; }
    public String getUserType() { return userType; }
    public double getRating() { return rating; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public String getDepartureTime() { return departureTime; }
    public String getTimeRemaining() { return timeRemaining; }
    public double getOfferedFare() { return offeredFare; }
    public String getPassengerId() { return passengerId; }
    public int getPassengers() { return passengers; }
    public String getSpecialRequest() { return specialRequest; }
    public String getPassengerPhoto() { return passengerPhoto; }
    public String getPassengerPhone() { return passengerPhone; }
    public String getVehicleType() { return vehicleType; }
    public Double getPickupLat() { return pickupLat; }
    public Double getPickupLng() { return pickupLng; }
    public Double getDropLat() { return dropLat; }
    public Double getDropLng() { return dropLng; } // FIXED: was dropLng()
    public Double getDistance() { return distance; }
    public Double getDuration() { return duration; }
    public String getTrafficLevel() { return trafficLevel; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    public void setUserType(String userType) { this.userType = userType; }
    public void setRating(double rating) { this.rating = rating; }
    public void setSource(String source) { this.source = source; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public void setTimeRemaining(String timeRemaining) { this.timeRemaining = timeRemaining; }
    public void setOfferedFare(double offeredFare) { this.offeredFare = offeredFare; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public void setPassengers(int passengers) { this.passengers = passengers; }
    public void setSpecialRequest(String specialRequest) { this.specialRequest = specialRequest; }
    public void setPassengerPhoto(String passengerPhoto) { this.passengerPhoto = passengerPhoto; }
    public void setPassengerPhone(String passengerPhone) { this.passengerPhone = passengerPhone; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
}