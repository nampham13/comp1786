package com.example.myapplication.util;

import com.example.myapplication.model.Course;

/**
 * Utility class for creating sample data
 */
public class SampleDataUtil {
    public static Course createSampleCourse() {
        Course course = new Course();
        course.setName("Flow Yoga"); // Using type as name since name wasn't provided
        course.setDescription("A dynamic yoga practice that synchronizes movement with breath");
        course.setDuration(12); // Duration in minutes
        course.setLevel("Beginner");
        
        // New fields from the example
        course.setCapacity(12);
        course.setDayOfWeek("Monday");
        course.setType("Flow Yoga");
        course.setPrice(300);
        course.setTime("09:00");
        course.setEquipmentNeeded(null); // Equipment needed was null in the example
        
        return course;
    }
}
