package com.example.myapplication.model;

public class User {
    // Role constants
    public static final String ROLE_SUPER_ADMIN = "super_admin";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_CUSTOMER = "customer";
    
    // Status constants
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_BANNED = "banned";
    
    private long id; // Local database ID
    private String firebaseUid; // Firebase user ID
    private String email;
    private String password;
    private String name;
    private String role;
    private String status;
    

    public User() {
        this.status = STATUS_ACTIVE; // Default status is active
    }
    
    public User(String email, String name, String role, String password) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.status = STATUS_ACTIVE; // Default status is active
    }

    // New constructor to support creation with id.
    public User(String email, String password, String name, String role, long id) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.status = STATUS_ACTIVE; // Default status is active
        this.id = id;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    

    
    public String getFirebaseUid() {
        return firebaseUid;
    }
    
    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
    
    // Helper methods
    public boolean isSuperAdmin() {
        return ROLE_SUPER_ADMIN.equals(role);
    }
    
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
    
    public boolean isCustomer() {
        return ROLE_CUSTOMER.equals(role);
    }
    
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }
    
    public boolean isBanned() {
        return STATUS_BANNED.equals(status);
    }
}
