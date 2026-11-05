package com.pgconnect.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "pgs")
@CompoundIndex(name = "gender_rent", def = "{'gender': 1, 'rent': 1}")
public class Pg {

    @Id
    private String id;

    @TextIndexed
    private String name;
    private String gender;              // male | female | unisex
    private int rent;
    private List<Integer> sharing = new ArrayList<>();
    private List<String> amenities = new ArrayList<>();

    @TextIndexed
    private String area;
    private String address;
    private String phone;

    // GeoJSON Point: coordinates are [longitude, latitude]
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private List<Review> reviews = new ArrayList<>();   // embedded documents
    private double avgRating;
    private Date createdAt = new Date();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public GeoJsonPoint getLocation() { return location; }
    public void setLocation(GeoJsonPoint location) { this.location = location; }
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
    public double getAvgRating() { return avgRating; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
