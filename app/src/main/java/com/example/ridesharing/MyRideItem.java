package com.example.ridesharing;

import java.util.List;
import java.util.Map;

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
    private String otherPersonName;
    private String otherPersonPhone;
    private String otherPersonId;
    private boolean isPassengerView;

    // Carpool specific fields
    private boolean isCarpool;
    private double farePerPassenger;
    private int maxSeats;
    private int passengerCount;
    private String allPassengerNames;
    private int unreadCount;

    // NEW: Store individual passenger details for driver view
    private List<PassengerDetail> passengerDetails;

    public MyRideItem(String id, String status, String pickupLocation, String dropLocation,
                      double fare, String vehicleType, int passengers, Long departureTime,
                      Long acceptedAt, String otherPersonName, String otherPersonPhone,
                      String otherPersonId, boolean isPassengerView, boolean isCarpool,
                      double farePerPassenger, int maxSeats, int passengerCount,
                      String allPassengerNames, int unreadCount) {
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
        this.unreadCount = unreadCount;
    }

    // Inner class to hold passenger details
    public static class PassengerDetail {
        private String name;
        private String phone;
        private String pickupLocation;
        private String dropLocation;
        private double fare;

        public PassengerDetail(String name, String phone, String pickupLocation,
                               String dropLocation, double fare) {
            this.name = name;
            this.phone = phone;
            this.pickupLocation = pickupLocation;
            this.dropLocation = dropLocation;
            this.fare = fare;
        }

        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getPickupLocation() { return pickupLocation; }
        public String getDropLocation() { return dropLocation; }
        public double getFare() { return fare; }
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
    public boolean isCarpool() { return isCarpool; }
    public double getFarePerPassenger() { return farePerPassenger; }
    public int getMaxSeats() { return maxSeats; }
    public int getPassengerCount() { return passengerCount; }
    public String getAllPassengerNames() { return allPassengerNames; }
    public int getUnreadCount() { return unreadCount; }
    public List<PassengerDetail> getPassengerDetails() { return passengerDetails; }

    // Setters
    public void setPassengerDetails(List<PassengerDetail> passengerDetails) {
        this.passengerDetails = passengerDetails;
    }
}