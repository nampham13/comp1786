package com.example.myapplication.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class to handle Firebase Authentication and Firestore user management
 */
public class FirebaseAuthService {
    private static final String TAG = "FirebaseAuthService";
    private static final String USERS_COLLECTION = "users";
    
    private static FirebaseAuthService instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    
    private FirebaseAuthService() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }
    
    public static synchronized FirebaseAuthService getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthService();
        }
        return instance;
    }
    
    /**
     * Register a new user with Firebase Authentication and Firestore
     * 
     * @param user User object containing registration details
     * @param callback Callback to handle the result
     */
    public void registerUser(User user, final AuthCallback callback) {
        // First create the user in Firebase Authentication
        firebaseAuth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            // Store additional user data in Firestore
                            String uid = firebaseUser.getUid();
                            storeUserData(uid, user, callback);
                        } else {
                            callback.onFailure("Failed to get user after registration");
                        }
                    } else {
                        callback.onFailure(task.getException() != null ? 
                                task.getException().getMessage() : "Registration failed");
                    }
                });
    }
    
    /**
     * Store additional user data in Firestore
     * 
     * @param uid Firebase user ID
     * @param user User object containing user details
     * @param callback Callback to handle the result
     */
    private void storeUserData(String uid, User user, final AuthCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("role", user.getRole());
        userData.put("status", user.getStatus());
        firestore.collection(USERS_COLLECTION).document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // Set the Firebase UID to the user object
                    user.setFirebaseUid(uid);
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    // If Firestore storage fails, delete the Authentication user
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    if (currentUser != null) {
                        currentUser.delete();
                    }
                    callback.onFailure("Failed to store user data: " + e.getMessage());
                });
    }
    
    /**
     * Authenticate a user with Firebase Authentication and get user data from Firestore
     * 
     * @param email User email
     * @param password User password
     * @param callback Callback to handle the result
     */
    public void loginUser(String email, String password, final AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            // Get additional user data from Firestore
                            getUserData(firebaseUser.getUid(), callback);
                        } else {
                            callback.onFailure("Failed to get user after login");
                        }
                    } else {
                        callback.onFailure(task.getException() != null ? 
                                task.getException().getMessage() : "Authentication failed");
                    }
                });
    }
    
    /**
     * Get user data from Firestore
     * 
     * @param uid Firebase user ID
     * @param callback Callback to handle the result
     */
    private void getUserData(String uid, final AuthCallback callback) {
        firestore.collection(USERS_COLLECTION).document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Create user object from Firestore data
                            User user = new User();
                            user.setFirebaseUid(uid);
                            user.setEmail(document.getString("email"));
                            user.setName(document.getString("name"));
                            user.setRole(document.getString("role"));
                            user.setStatus(document.getString("status"));
                            
                            // Check if user is banned
                            if (user.isBanned()) {
                                // Sign out the user
                                firebaseAuth.signOut();
                                callback.onFailure("Your account has been banned");
                                return;
                            }
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure("User data not found");
                        }
                    } else {
                        callback.onFailure(task.getException() != null ? 
                                task.getException().getMessage() : "Failed to get user data");
                    }
                });
    }
    
    /**
     * Get all users from Firestore
     * 
     * @param callback Callback to handle the result
     */
    public void getAllUsers(final UsersCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            User user = new User();
                            user.setFirebaseUid(document.getId());
                            user.setEmail(document.getString("email"));
                            user.setName(document.getString("name"));
                            user.setRole(document.getString("role"));
                            user.setStatus(document.getString("status"));
                            users.add(user);
                        }
                        callback.onSuccess(users);
                    } else {
                        callback.onFailure(task.getException() != null ? 
                                task.getException().getMessage() : "Failed to get users");
                    }
                });
    }
    
    /**
     * Get user by email from Firestore
     * 
     * @param email User email
     * @param callback Callback to handle the result
     */
    public void getUserByEmail(String email, final AuthCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            User user = new User();
                            user.setFirebaseUid(document.getId());
                            user.setEmail(document.getString("email"));
                            user.setName(document.getString("name"));
                            user.setRole(document.getString("role"));
                            user.setStatus(document.getString("status"));
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure("User not found");
                        }
                    } else {
                        callback.onFailure(task.getException() != null ? 
                                task.getException().getMessage() : "Failed to get user");
                    }
                });
    }
    
    /**
     * Update user status in Firestore
     * 
     * @param uid Firebase user ID
     * @param status New status
     * @param callback Callback to handle the result
     */
    public void updateUserStatus(String uid, String status, final SimpleCallback callback) {
        firestore.collection(USERS_COLLECTION).document(uid)
                .update("status", status)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
    
    /**
     * Delete user data from Firestore
     * 
     * @param uid Firebase user ID
     * @param callback Callback to handle the result
     */
    public void deleteUserData(String uid, final SimpleCallback callback) {
        firestore.collection(USERS_COLLECTION).document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
    
    /**
     * Sign out the current user
     */
    public void signOut() {
        firebaseAuth.signOut();
    }
    
    /**
     * Get the current authenticated user
     * 
     * @return FirebaseUser object or null if not authenticated
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    /**
     * Check if a user is currently authenticated
     * 
     * @return true if a user is authenticated, false otherwise
     */
    public boolean isUserAuthenticated() {
        return firebaseAuth.getCurrentUser() != null;
    }
    
    /**
     * Callback interface for authentication operations
     */
    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }
    
    /**
     * Callback interface for getting multiple users
     */
    public interface UsersCallback {
        void onSuccess(List<User> users);
        void onFailure(String errorMessage);
    }
    
    /**
     * Simple callback interface for operations without return data
     */
    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }
}