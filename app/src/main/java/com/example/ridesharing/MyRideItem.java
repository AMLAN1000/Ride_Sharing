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

    // New fields for carpool support
    private boolean isCarpool;
    private double farePerPassenger;
    private int maxSeats;
    private int passengerCount;
    private String allPassengerNames; // For displaying multiple passengers
    private int unreadMessageCount; // NEW: For unread message badge

    public MyRideItem(String id, String status, String pickupLocation, String dropLocation,
                      double fare, String vehicleType, int passengers,
                      Long departureTime, Long acceptedAt,
                      String otherPersonName, String otherPersonPhone, String otherPersonId,
                      boolean isPassengerView) {
        this(id, status, pickupLocation, dropLocation, fare, vehicleType, passengers,
                departureTime, acceptedAt, otherPersonName, otherPersonPhone, otherPersonId,
                isPassengerView, false, 0.0, 1, 0, "", 0);
    }

    public MyRideItem(String id, String status, String pickupLocation, String dropLocation,
                      double fare, String vehicleType, int passengers,
                      Long departureTime, Long acceptedAt,
                      String otherPersonName, String otherPersonPhone, String otherPersonId,
                      boolean isPassengerView, boolean isCarpool, double farePerPassenger,
                      int maxSeats, int passengerCount, String allPassengerNames, int unreadMessageCount) {
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
        this.isCarpool = isCarpool;
        this.farePerPassenger = farePerPassenger;
        this.maxSeats = maxSeats;
        this.passengerCount = passengerCount;
        this.allPassengerNames = allPassengerNames;
        this.unreadMessageCount = unreadMessageCount;
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

    // New getters
    public boolean isCarpool() { return isCarpool; }
    public double getFarePerPassenger() { return farePerPassenger; }
    public int getMaxSeats() { return maxSeats; }
    public int getPassengerCount() { return passengerCount; }
    public String getAllPassengerNames() { return allPassengerNames; }
    public int getUnreadMessageCount() { return unreadMessageCount; }

    // Helper methods
    public String getRoleLabel() {
        return isPassengerView ? "Driver" : "Passenger";
    }
}