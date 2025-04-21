package com.example.myapplication.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Instance;
import com.example.myapplication.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class to handle Firebase operations and synchronization
 */
public class FirebaseService {
    private static final String TAG = "FirebaseService";
    
    // Firebase instances
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    
    // Collection names
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_COURSES = "yoga_courses";
    private static final String COLLECTION_INSTANCES = "instances";
    
    // Local database helper
    private final DatabaseHelper dbHelper;
    
    // Singleton instance
    private static FirebaseService instance;
    
    /**
     * Get singleton instance of FirebaseService
     * @param context Application context
     * @return FirebaseService instance
     */
    public static synchronized FirebaseService getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseService(context);
        }
        return instance;
    }
    
    /**
     * Private constructor
     * @param context Application context
     */
    private FirebaseService(Context context) {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbHelper = DatabaseHelper.getInstance(context);
    }
    
    /**
     * Check if user is currently signed in
     * @return true if user is signed in, false otherwise
     */
    public boolean isUserSignedIn() {
        return mAuth.getCurrentUser() != null;
    }
    
    /**
     * Get current user ID
     * @return User ID or null if not signed in
     */
    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Sign in with email and password
     * @param email User email
     * @param password User password
     * @param listener Callback for sign in result
     */
    public void signIn(String email, String password, OnCompleteListener<AuthResult> listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }
    
    /**
     * Sign out current user
     */
    public void signOut() {
        mAuth.signOut();
    }
    
    /**
     * Synchronize all courses from local database to Firebase
     * @param listener Callback for sync result
     */
    public void syncCoursesToFirebase(final OnSyncListener listener) {
        if (!isUserSignedIn()) {
            listener.onSyncFailed("User not signed in");
            return;
        }
        
        // Log the start of sync process
        Log.d(TAG, "Starting sync from local database to Firebase");
        
        List<Course> courses = dbHelper.getAllCourses();
        Log.d(TAG, "Found " + courses.size() + " courses in local database to sync");
        
        if (courses.isEmpty()) {
            listener.onSyncComplete("No courses to sync");
            return;
        }
        
        // Log all courses being synced
        for (Course c : courses) {
            Log.d(TAG, "Will sync course: " + c.getName() + " (ID: " + c.getId() + ")");
        }
        
        final int[] successCount = {0};
        final int totalCount = courses.size();
        
        for (Course course : courses) {
            Map<String, Object> courseMap = courseToMap(course);
            Log.d(TAG, "Course map for " + course.getName() + ": " + courseMap);
            
            // Use course ID as document ID for easy reference
            String documentId = String.valueOf(course.getId());
            Log.d(TAG, "Using document ID: " + documentId);
            
            db.collection(COLLECTION_COURSES)
                    .document(documentId)
                    .set(courseMap, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        successCount[0]++;
                        Log.d(TAG, "Course synced successfully: " + course.getName());
                        
                        // Sync instances for this course
                        syncInstancesForCourse(course.getId());
                        
                        // Check if all courses have been processed
                        if (successCount[0] == totalCount) {
                            Log.d(TAG, "All courses synced successfully: " + successCount[0] + " courses");
                            listener.onSyncComplete("Synced " + successCount[0] + " courses");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error syncing course " + course.getName() + ": " + e.getMessage());
                        if (successCount[0] + 1 == totalCount) {
                            // This was the last one, but it failed
                            Log.d(TAG, "Sync completed with errors: " + successCount[0] + " of " + totalCount + " courses synced");
                            listener.onSyncComplete("Synced " + successCount[0] + " courses, with errors");
                        }
                    });
        }
    }
    
    /**
     * Synchronize all instances for a specific course
     * @param courseId Course ID
     */
    private void syncInstancesForCourse(long courseId) {
        List<Instance> instances = dbHelper.getInstancesByCourseId(courseId);
        
        for (Instance instance : instances) {
            Map<String, Object> instanceMap = instanceToMap(instance);
            
            // Use instance ID as document ID for easy reference
            String documentId = String.valueOf(instance.getId());
            
            db.collection(COLLECTION_INSTANCES)
                    .document(documentId)
                    .set(instanceMap, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> 
                            Log.d(TAG, "Instance synced for course: " + courseId))
                    .addOnFailureListener(e -> 
                            Log.e(TAG, "Error syncing instance: " + e.getMessage()));
        }
    }
    
    /**
     * Synchronize all courses from Firebase to local database
     * @param listener Callback for sync result
     */
    public void syncCoursesFromFirebase(final OnSyncListener listener) {
        if (!isUserSignedIn()) {
            listener.onSyncFailed("User not signed in");
            return;
        }
        
        // Log the start of sync process
        Log.d(TAG, "Starting sync from Firebase to local database");
        
        db.collection(COLLECTION_COURSES)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = 0;
                        Log.d(TAG, "Firebase returned " + task.getResult().size() + " courses");
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "Processing document: " + document.getId() + " with data: " + document.getData());
                            
                            Course course = mapToCourse(document);
                            Log.d(TAG, "Mapped to course: " + course.getName() + " (ID: " + course.getId() + ")");
                            
                            // Check if course already exists in local DB
                            Course existingCourse = dbHelper.getCourseById(course.getId());
                            
                            if (existingCourse == null) {
                                // New course, add it
                                Log.d(TAG, "Adding new course: " + course.getName());
                                long newId = dbHelper.addCourse(course);
                                Log.d(TAG, "Added with ID: " + newId);
                            } else {
                                // Existing course, update it
                                Log.d(TAG, "Updating existing course: " + course.getName());
                                int rowsUpdated = dbHelper.updateCourse(course);
                                Log.d(TAG, "Updated rows: " + rowsUpdated);
                            }
                            
                            // Sync instances for this course
                            syncInstancesFromFirebase(course.getId());
                            
                            count++;
                        }
                        
                        // Log the courses in local database after sync
                        List<Course> localCourses = dbHelper.getAllCourses();
                        Log.d(TAG, "After sync, local database has " + localCourses.size() + " courses");
                        for (Course c : localCourses) {
                            Log.d(TAG, "Local course: " + c.getName() + " (ID: " + c.getId() + ")");
                        }
                        
                        listener.onSyncComplete("Downloaded " + count + " courses");
                    } else {
                        Log.e(TAG, "Error getting courses: " + task.getException());
                        listener.onSyncFailed("Error getting courses: " + task.getException());
                    }
                });
    }
    
    /**
     * Synchronize all instances for a specific course from Firebase
     * @param courseId Course ID
     */
    private void syncInstancesFromFirebase(long courseId) {
        db.collection(COLLECTION_INSTANCES)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Instance instance = mapToInstance(document);
                            
                            // Check if instance already exists in local DB
                            Instance existingInstance = dbHelper.getInstanceById(instance.getId());
                            
                            if (existingInstance == null) {
                                // New instance, add it
                                dbHelper.addInstance(instance);
                            } else {
                                // Existing instance, update it
                                dbHelper.updateInstance(instance);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error getting instances: " + task.getException());
                    }
                });
    }
    
    /**
     * Convert Course object to Map for Firestore
     * @param course Course object
     * @return Map representation of course
     */
    private Map<String, Object> courseToMap(Course course) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", course.getId());
        
        // Handle name - if it's null, use type as name
        String name = course.getName();
        if (name == null || name.isEmpty()) {
            name = course.getType();
            Log.d(TAG, "Using type as name for course: " + name);
        }
        map.put("name", name);
        
        map.put("description", course.getDescription());
        map.put("duration", course.getDuration());
        map.put("level", course.getLevel());
        map.put("photoPath", course.getPhotoPath());
        map.put("capacity", course.getCapacity());
        map.put("dayOfWeek", course.getDayOfWeek());
        map.put("type", course.getType());
        map.put("price", course.getPrice());
        map.put("time", course.getTime());
        map.put("equipmentNeeded", course.getEquipmentNeeded());
        map.put("enrolled", course.getEnrolled());
        map.put("lastUpdated", System.currentTimeMillis());
        return map;
    }
    
    /**
     * Convert Instance object to Map for Firestore
     * @param instance Instance object
     * @return Map representation of instance
     */
    private Map<String, Object> instanceToMap(Instance instance) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", instance.getId());
        map.put("courseId", instance.getCourseId());
        map.put("date", instance.getDate());
        map.put("time", instance.getTime());
        map.put("location", instance.getLocation());
        map.put("capacity", instance.getCapacity());
        map.put("instructor", instance.getInstructor());
        map.put("price", instance.getPrice());
        map.put("lastUpdated", System.currentTimeMillis());
        return map;
    }
    
    /**
     * Convert Firestore document to Course object
     * @param document Firestore document
     * @return Course object
     */
    private Course mapToCourse(DocumentSnapshot document) {
        Course course = new Course();
        
        // Log the document data for debugging
        Log.d(TAG, "Mapping document to course: " + document.getId() + ", data: " + document.getData());
        
        // Get ID as long
        Object idObj = document.get("id");
        if (idObj != null) {
            Log.d(TAG, "ID object type: " + idObj.getClass().getName() + ", value: " + idObj);
            if (idObj instanceof Long) {
                course.setId((Long) idObj);
            } else if (idObj instanceof Integer) {
                course.setId((Integer) idObj);
            } else if (idObj instanceof Double) {
                course.setId(((Double) idObj).longValue());
            } else if (idObj instanceof String) {
                try {
                    course.setId(Long.parseLong((String) idObj));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing course ID: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "Unexpected ID type: " + idObj.getClass().getName());
            }
        } else {
            // If ID is null, try to use document ID
            try {
                course.setId(Long.parseLong(document.getId()));
                Log.d(TAG, "Using document ID as course ID: " + document.getId());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse document ID as long: " + e.getMessage());
            }
        }
        
        // Get name - if it's missing, try to use type
        String name = document.getString("name");
        if (name == null || name.isEmpty()) {
            name = document.getString("type");
            Log.d(TAG, "Name is missing, using type as name: " + name);
        }
        course.setName(name);
        Log.d(TAG, "Course name: " + name);
        
        // Get description
        String description = document.getString("description");
        course.setDescription(description);
        Log.d(TAG, "Course description: " + description);
        
        // Get duration as int
        Object durationObj = document.get("duration");
        if (durationObj != null) {
            Log.d(TAG, "Duration object type: " + durationObj.getClass().getName() + ", value: " + durationObj);
            if (durationObj instanceof Long) {
                course.setDuration(((Long) durationObj).intValue());
            } else if (durationObj instanceof Integer) {
                course.setDuration((Integer) durationObj);
            } else if (durationObj instanceof Double) {
                course.setDuration(((Double) durationObj).intValue());
            } else if (durationObj instanceof String) {
                try {
                    course.setDuration(Integer.parseInt((String) durationObj));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing duration: " + e.getMessage());
                }
            }
        }
        
        // Get level
        String level = document.getString("level");
        course.setLevel(level);
        Log.d(TAG, "Course level: " + level);
        
        // Get photo path
        String photoPath = document.getString("photoPath");
        course.setPhotoPath(photoPath);
        Log.d(TAG, "Course photo path: " + photoPath);
        
        // Get capacity as int
        Object capacityObj = document.get("capacity");
        if (capacityObj != null) {
            Log.d(TAG, "Capacity object type: " + capacityObj.getClass().getName() + ", value: " + capacityObj);
            if (capacityObj instanceof Long) {
                course.setCapacity(((Long) capacityObj).intValue());
            } else if (capacityObj instanceof Integer) {
                course.setCapacity((Integer) capacityObj);
            } else if (capacityObj instanceof Double) {
                course.setCapacity(((Double) capacityObj).intValue());
            } else if (capacityObj instanceof String) {
                try {
                    course.setCapacity(Integer.parseInt((String) capacityObj));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing capacity: " + e.getMessage());
                }
            }
        }
        
        // Get day of week
        String dayOfWeek = document.getString("dayOfWeek");
        course.setDayOfWeek(dayOfWeek);
        Log.d(TAG, "Course day of week: " + dayOfWeek);
        
        // Get type
        String type = document.getString("type");
        course.setType(type);
        Log.d(TAG, "Course type: " + type);
        
        // Get price as float
        Object priceObj = document.get("price");
        if (priceObj != null) {
            Log.d(TAG, "Price object type: " + priceObj.getClass().getName() + ", value: " + priceObj);
            if (priceObj instanceof Double) {
                course.setPrice(((Double) priceObj).floatValue());
            } else if (priceObj instanceof Long) {
                course.setPrice(((Long) priceObj).floatValue());
            } else if (priceObj instanceof Integer) {
                course.setPrice(((Integer) priceObj).floatValue());
            } else if (priceObj instanceof Float) {
                course.setPrice((Float) priceObj);
            } else if (priceObj instanceof String) {
                try {
                    course.setPrice(Float.parseFloat((String) priceObj));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing price: " + e.getMessage());
                }
            }
        }
        
        // Get time
        String time = document.getString("time");
        course.setTime(time);
        Log.d(TAG, "Course time: " + time);
        
        // Get equipment needed
        String equipmentNeeded = document.getString("equipmentNeeded");
        course.setEquipmentNeeded(equipmentNeeded);
        Log.d(TAG, "Course equipment needed: " + equipmentNeeded);
        
        // Get enrolled
        Object enrolledObj = document.get("enrolled");
        if (enrolledObj != null) {
            Log.d(TAG, "Enrolled object type: " + enrolledObj.getClass().getName() + ", value: " + enrolledObj);
            if (enrolledObj instanceof Long) {
                course.setEnrolled(((Long) enrolledObj).intValue());
            } else if (enrolledObj instanceof Integer) {
                course.setEnrolled((Integer) enrolledObj);
            } else if (enrolledObj instanceof Double) {
                course.setEnrolled(((Double) enrolledObj).intValue());
            } else if (enrolledObj instanceof String) {
                try {
                    course.setEnrolled(Integer.parseInt((String) enrolledObj));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing enrolled: " + e.getMessage());
                }
            }
        }
        Log.d(TAG, "Course enrolled: " + course.getEnrolled());
        
        Log.d(TAG, "Mapped course: " + course.getName() + " (ID: " + course.getId() + ")");
        return course;
    }
    
    /**
     * Convert Firestore document to Instance object
     * @param document Firestore document
     * @return Instance object
     */
    private Instance mapToInstance(DocumentSnapshot document) {
        Instance instance = new Instance();
        
        // Get ID as long
        Object idObj = document.get("id");
        if (idObj != null) {
            if (idObj instanceof Long) {
                instance.setId((Long) idObj);
            } else if (idObj instanceof String) {
                try {
                    instance.setId(Long.parseLong((String) idObj));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing instance ID: " + e.getMessage());
                }
            }
        }
        
        // Get courseId as long
        Object courseIdObj = document.get("courseId");
        if (courseIdObj != null) {
            if (courseIdObj instanceof Long) {
                instance.setCourseId((Long) courseIdObj);
            } else if (courseIdObj instanceof String) {
                try {
                    instance.setCourseId(Long.parseLong((String) courseIdObj));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing course ID: " + e.getMessage());
                }
            }
        }
        
        instance.setDate(document.getString("date"));
        instance.setTime(document.getString("time"));
        instance.setLocation(document.getString("location"));
        
        // Get capacity as int
        Object capacityObj = document.get("capacity");
        if (capacityObj != null) {
            if (capacityObj instanceof Long) {
                instance.setCapacity(((Long) capacityObj).intValue());
            } else if (capacityObj instanceof Integer) {
                instance.setCapacity((Integer) capacityObj);
            }
        }
        
        instance.setInstructor(document.getString("instructor"));
        
        // Get price as float
        Object priceObj = document.get("price");
        if (priceObj != null) {
            if (priceObj instanceof Double) {
                instance.setPrice(((Double) priceObj).floatValue());
            } else if (priceObj instanceof Long) {
                instance.setPrice(((Long) priceObj).floatValue());
            } else if (priceObj instanceof Float) {
                instance.setPrice((Float) priceObj);
            }
        }
        
        return instance;
    }
    
    /**
     * Interface for sync operation callbacks
     */
    public interface OnSyncListener {
        void onSyncComplete(String message);
        void onSyncFailed(String errorMessage);
    }
}