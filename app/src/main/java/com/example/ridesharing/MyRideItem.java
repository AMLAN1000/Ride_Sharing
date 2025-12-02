package com.example.ridesharing;

public class MyRideItem {
    private String id;
    private String status;
    private String pickupLocation;
    private String dropLocation;
    private double fare;
    private String vehicleType;
    private int passengers;
    private Long departureTime;
    private Long acceptedAt;

    // Info about the other person (driver if you're passenger, passenger if you're driver)
    private String otherPersonName;
    private String otherPersonPhone;
    private String otherPersonId;

    // Role indicator
    private boolean isPassengerView; // true = viewing as passenger, false = viewing as driver

    public MyRideItem(String id, String status, String pickupLocation, String dropLocation,
                      double fare, String vehicleType, int passengers,
                      Long departureTime, Long acceptedAt,
                      String otherPersonName, String otherPersonPhone, String otherPersonId,
                      boolean isPassengerView) {
        this.id = id;
        this.status = status;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.fare = fare;
        this.vehicleType = vehicleType;
        this.passengers = passengers;
        this.departureTime = departureTime;
        this.acceptedAt = acceptedAt;
        this.otherPersonName = otherPersonName;
        this.otherPersonPhone = otherPersonPhone;
        this.otherPersonId = otherPersonId;
        this.isPassengerView = isPassengerView;
    }

    // Getters
    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getPickupLocation() { return pickupLocation; }
    public String getDropLocation() { return dropLocation; }
    public double getFare() { return fare; }
    public String getVehicleType() { return vehicleType; }
    public int getPassengers() { return passengers; }
    public Long getDepartureTime() { return departureTime; }
    public Long getAcceptedAt() { return acceptedAt; }
    public String getOtherPersonName() { return otherPersonName; }
    public String getOtherPersonPhone() { return otherPersonPhone; }
    public String getOtherPersonId() { return otherPersonId; }
    public boolean isPassengerView() { return isPassengerView; }

    // Helper methods
    public String getRoleLabel() {
        return isPassengerView ? "Driver" : "Passenger";
    }
}