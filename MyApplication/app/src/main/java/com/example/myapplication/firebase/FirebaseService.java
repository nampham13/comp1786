package com.example.myapplication.firebase;

import android.util.Log;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Instance;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.OnCompleteListener;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static final String COLLECTION_INSTANCES = "instances";
    private final FirebaseFirestore db;

    private static FirebaseService instance;

    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    private FirebaseService() {
        db = FirebaseFirestore.getInstance();
    }

    // Fix syncCoursesToFirebase to accept a List<Course>
    public void syncCoursesToFirebase(List<Course> localCourses) {
        for (Course course : localCourses) {
            db.collection("yoga_courses")
                .document(String.valueOf(course.getId()))
                .set(courseToMap(course), SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Course synced: " + course.getName()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync course: " + course.getName(), e));
        }
    }

    // Add this method to support syncing courses from Firebase
    public void syncCoursesFromFirebase(OnCourseSyncListener listener) {
        db.collection("yoga_courses")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Course> courses = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Course course = mapToCourse(document);
                    courses.add(course);
                }
                listener.onSyncComplete(courses, "Courses synced from Firebase successfully.");
            })
            .addOnFailureListener(e -> {
                listener.onSyncFailed("Failed to sync courses from Firebase: " + e.getMessage());
            });
    }

    /**
     * Convert Firestore document to Course object
     * @param document Firestore document
     * @return Course object
     */
    private Course mapToCourse(QueryDocumentSnapshot document) {
        Course course = new Course();

        // Set ID
        Object idObj = document.get("id");
        if (idObj != null) {
            if (idObj instanceof Long) {
                course.setId((Long) idObj);
            } else if (idObj instanceof String) {
                try {
                    course.setId(Long.parseLong((String) idObj));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing course ID: " + e.getMessage());
                }
            }
        } else {
            // Use document ID if no ID field
            course.setId(Long.parseLong(document.getId()));
        }

        // Set basic fields
        course.setName(document.getString("name"));
        course.setDescription(document.getString("description"));

        // Set duration
        Object durationObj = document.get("duration");
        if (durationObj != null) {
            if (durationObj instanceof Long) {
                course.setDuration(((Long) durationObj).intValue());
            } else if (durationObj instanceof Integer) {
                course.setDuration((Integer) durationObj);
            }
        }

        course.setLevel(document.getString("level"));
        course.setPhotoPath(document.getString("photoPath"));

        // Set capacity
        Object capacityObj = document.get("capacity");
        if (capacityObj != null) {
            if (capacityObj instanceof Long) {
                course.setCapacity(((Long) capacityObj).intValue());
            } else if (capacityObj instanceof Integer) {
                course.setCapacity((Integer) capacityObj);
            }
        }

        course.setDayOfWeek(document.getString("dayOfWeek"));
        course.setType(document.getString("type"));

        // Set price
        Object priceObj = document.get("price");
        if (priceObj != null) {
            if (priceObj instanceof Double) {
                course.setPrice(((Double) priceObj).floatValue());
            } else if (priceObj instanceof Long) {
                course.setPrice(((Long) priceObj).floatValue());
            } else if (priceObj instanceof Float) {
                course.setPrice((Float) priceObj);
            }
        }

        course.setTime(document.getString("time"));
        course.setEquipmentNeeded(document.getString("equipmentNeeded"));

        // Set enrolled
        Object enrolledObj = document.get("enrolled");
        if (enrolledObj != null) {
            if (enrolledObj instanceof Long) {
                course.setEnrolled(((Long) enrolledObj).intValue());
            } else if (enrolledObj instanceof Integer) {
                course.setEnrolled((Integer) enrolledObj);
            }
        }

        return course;
    }

    // Helper to convert Course to Map (if not already present)
    private Map<String, Object> courseToMap(Course course) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", course.getId());
        map.put("name", course.getName());
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
     * Synchronize all instances for a specific course from Firebase
     * @param courseId Course ID
     * @param listener Callback for sync result
     */
    public void syncInstancesFromFirebase(long courseId, OnSyncInstancesListener listener) {
        db.collection(COLLECTION_INSTANCES)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Instance> instances = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Instance instance = mapToInstance(document);
                            instances.add(instance);
                        }
                        listener.onSyncComplete(instances);
                    } else {
                        Log.e(TAG, "Error getting instances: " + task.getException());
                        listener.onSyncFailed("Error getting instances: " + task.getException());
                    }
                });
    }

    /**
     * Convert Firestore document to Instance object
     * @param document Firestore document
     * @return Instance object
     */
    private Instance mapToInstance(QueryDocumentSnapshot document) {
        Instance instance = new Instance();
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
        Object capacityObj = document.get("capacity");
        if (capacityObj != null) {
            if (capacityObj instanceof Long) {
                instance.setCapacity(((Long) capacityObj).intValue());
            } else if (capacityObj instanceof Integer) {
                instance.setCapacity((Integer) capacityObj);
            }
        }
        instance.setInstructor(document.getString("instructor"));
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


    public void syncUsersToFirebase() {

    }

    // Add this method to check if a user is signed in
    public boolean isUserSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    // Add signIn method for Firebase authentication
    public void signIn(String email, String password, OnCompleteListener<com.google.firebase.auth.AuthResult> listener) {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(listener);
    }

    // Add signOut method for Firebase authentication
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    public void deleteCourseFromFirebase(long courseId, OnSyncListener listener) {
        db.collection("yoga_courses")
                .document(String.valueOf(courseId))
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Course deleted from Firebase: " + courseId);
                    if (listener != null) listener.onSyncComplete("Course deleted successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete course from Firebase: " + courseId, e);
                    if (listener != null) listener.onSyncFailed("Failed to delete course: " + e.getMessage());
                });
    }

    public interface OnSyncInstancesListener {
        void onSyncComplete(List<Instance> instances);
        void onSyncFailed(String errorMessage);
    }

    // Add OnSyncListener interface for sync callbacks
    public interface OnSyncListener {
        void onSyncComplete(String message);
        void onSyncFailed(String errorMessage);
    }

    /**
     * Callback interface for syncing courses
     */
    public interface OnCourseSyncListener {
        void onSyncComplete(List<Course> courses, String message);
        void onSyncFailed(String errorMessage);
    }
}
