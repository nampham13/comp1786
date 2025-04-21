package com.example.myapplication.model;

import java.io.Serializable;

public class Instance implements Serializable {
    private long id;
    private long courseId;
    private String date;
    private String time;
    private String location;
    private int capacity;
    private String instructor;
    private float price;
    
    public Instance() {
    }
    
    public Instance(long courseId, String date, String time, String location, int capacity, String instructor, float price) {
        this.courseId = courseId;
        this.date = date;
        this.time = time;
        this.location = location;
        this.capacity = capacity;
        this.instructor = instructor;
        this.price = price;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getCourseId() {
        return courseId;
    }
    
    public void setCourseId(long courseId) {
        this.courseId = courseId;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public String getInstructor() {
        return instructor;
    }
    
    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }
    
    public float getPrice() {
        return price;
    }
    
    public void setPrice(float price) {
        this.price = price;
    }
}