package com.example.myapplication.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Course implements Serializable {
    private long id;
    private String name;
    private String description;
    private int duration; // in minutes
    private String level; // beginner, intermediate, advanced
    private String photoPath;
    private List<Instance> instances;
    
    // New fields based on the example
    private int capacity;
    private String dayOfWeek;
    private String type;
    private float price;
    private String time;
    private String equipmentNeeded;
    
    // Additional field from the previous data
    private int enrolled; // Number of students enrolled
    
    public Course() {
        instances = new ArrayList<>();
    }
    
    public Course(String name, String description, int duration, String level) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.level = level;
        this.instances = new ArrayList<>();
    }
    
    // Extended constructor with new fields
    public Course(String name, String description, int duration, String level, 
                 int capacity, String dayOfWeek, String type, float price, 
                 String time, String equipmentNeeded) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.level = level;
        this.capacity = capacity;
        this.dayOfWeek = dayOfWeek;
        this.type = type;
        this.price = price;
        this.time = time;
        this.equipmentNeeded = equipmentNeeded;
        this.instances = new ArrayList<>();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getPhotoPath() {
        return photoPath;
    }
    
    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
    
    public List<Instance> getInstances() {
        return instances;
    }
    
    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }
    
    public void addInstance(Instance instance) {
        this.instances.add(instance);
    }
    
    // Getters and setters for new fields
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public String getDayOfWeek() {
        return dayOfWeek;
    }
    
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public float getPrice() {
        return price;
    }
    
    public void setPrice(float price) {
        this.price = price;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public String getEquipmentNeeded() {
        return equipmentNeeded;
    }
    
    public void setEquipmentNeeded(String equipmentNeeded) {
        this.equipmentNeeded = equipmentNeeded;
    }
    
    // Getter and setter for enrolled field
    public int getEnrolled() {
        return enrolled;
    }
    
    public void setEnrolled(int enrolled) {
        this.enrolled = enrolled;
    }
}