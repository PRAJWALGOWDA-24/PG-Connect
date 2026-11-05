package com.pgconnect.model;

import java.util.ArrayList;
import java.util.List;

/** What the frontend sends when adding or editing a PG (plain lat/lng instead of GeoJSON). */
public class PgRequest {
    private String name;
    private String gender;
    private int rent;
    private List<Integer> sharing = new ArrayList<>();
    private List<String> amenities = new ArrayList<>();
    private String area;
    private String address;
    private String phone;
    private double lat;
    private double lng;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getRent() { return rent; }
    public void setRent(int rent) { this.rent = rent; }
    public List<Integer> getSharing() { return sharing; }
    public void setSharing(List<Integer> sharing) { this.sharing = sharing; }
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
}
