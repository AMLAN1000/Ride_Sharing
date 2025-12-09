package com.example.ridesharing;

import com.google.android.gms.maps.model.LatLng;

public class RouteStop {
    private String address;
    private LatLng latLng;
    private int orderIndex; // 0 = start, 1 = first middle stop, etc.

    public RouteStop(String address, LatLng latLng, int orderIndex) {
        this.address = address;
        this.latLng = latLng;
        this.orderIndex = orderIndex;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LatLng getLatLng() { return latLng; }
    public void setLatLng(LatLng latLng) { this.latLng = latLng; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}