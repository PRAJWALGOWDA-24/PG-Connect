package com.pgconnect.model;

import java.util.Date;

/** Embedded inside a Pg document (no separate collection). */
public class Review {
    private String userName;
    private int rating;      // 1 to 5
    private String comment;
    private Date date = new Date();

    public Review() {}

    public Review(String userName, int rating, String comment) {
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
}
