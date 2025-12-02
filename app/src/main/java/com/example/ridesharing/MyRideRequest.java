package com.example.ridesharing;

public class MyRideRequest {
    private String id;
    private String status;
    private String pickupLocation;
    private String dropLocation;
    private double fare;
    private String vehicleType;
    private int passengers;
    private Long departureTime;
    private Long createdAt;
    private String driverId;
    private String driverName;
    private String driverPhone;
    private Long acceptedAt;
    private boolean notificationShown;

    public MyRideRequest(String id, String status, String pickupLocation,
                         String dropLocation, double fare, String vehicleType,
                         int passengers, Long departureTime, Long createdAt,
                         String driverId, String driverName, String driverPhone,
                         Long acceptedAt, boolean notificationShown) {
        this.id = id;
        this.status = status;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.fare = fare;
        this.vehicleType = vehicleType;
        this.passengers = passengers;
        this.departureTime = departureTime;
        this.createdAt = createdAt;
        this.driverId = driverId;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.acceptedAt = acceptedAt;
        this.notificationShown = notificationShown;
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
    public Long getCreatedAt() { return createdAt; }
    public String getDriverId() { return driverId; }
    public String getDriverName() { return driverName; }
    public String getDriverPhone() { return driverPhone; }
    public Long getAcceptedAt() { return acceptedAt; }
    public boolean isNotificationShown() { return notificationShown; }
}