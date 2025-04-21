package com.example.myapplication.util;

import android.text.TextUtils;
import android.util.Patterns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class ValidationUtil {
    
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    public static boolean isValidPassword(String password) {
        // Password should be at least 6 characters
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }
    
    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && name.length() >= 2;
    }
    
    public static boolean isValidDescription(String description) {
        return !TextUtils.isEmpty(description);
    }
    
    public static boolean isValidDuration(String duration) {
        if (TextUtils.isEmpty(duration)) {
            return false;
        }
        
        try {
            int durationValue = Integer.parseInt(duration);
            return durationValue > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidCapacity(String capacity) {
        if (TextUtils.isEmpty(capacity)) {
            return false;
        }
        
        try {
            int capacityValue = Integer.parseInt(capacity);
            return capacityValue > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidPrice(String price) {
        if (TextUtils.isEmpty(price)) {
            return false;
        }
        
        try {
            float priceValue = Float.parseFloat(price);
            return priceValue >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidDate(String date) {
        if (TextUtils.isEmpty(date)) {
            return false;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setLenient(false);
        
        try {
            Date parsedDate = sdf.parse(date);
            return parsedDate != null;
        } catch (ParseException e) {
            return false;
        }
    }
    
    public static boolean isValidTime(String time) {
        if (TextUtils.isEmpty(time)) {
            return false;
        }
        
        // Check if time is in HH:mm format
        Pattern pattern = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
        return pattern.matcher(time).matches();
    }
    
    public static boolean isValidDayOfWeek(String date, String dayOfWeek) {
        if (!isValidDate(date) || TextUtils.isEmpty(dayOfWeek)) {
            return false;
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsedDate = sdf.parse(date);
            
            if (parsedDate == null) {
                return false;
            }
            
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            
            int dayOfWeekValue = calendar.get(Calendar.DAY_OF_WEEK);
            String actualDayOfWeek;
            
            switch (dayOfWeekValue) {
                case Calendar.MONDAY:
                    actualDayOfWeek = "Monday";
                    break;
                case Calendar.TUESDAY:
                    actualDayOfWeek = "Tuesday";
                    break;
                case Calendar.WEDNESDAY:
                    actualDayOfWeek = "Wednesday";
                    break;
                case Calendar.THURSDAY:
                    actualDayOfWeek = "Thursday";
                    break;
                case Calendar.FRIDAY:
                    actualDayOfWeek = "Friday";
                    break;
                case Calendar.SATURDAY:
                    actualDayOfWeek = "Saturday";
                    break;
                case Calendar.SUNDAY:
                    actualDayOfWeek = "Sunday";
                    break;
                default:
                    return false;
            }
            
            return actualDayOfWeek.equalsIgnoreCase(dayOfWeek);
        } catch (ParseException e) {
            return false;
        }
    }
}