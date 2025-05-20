package com.example.myapplication.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.example.myapplication.firebase.FirebaseAuthService;
import com.example.myapplication.model.User;

public class MainActivity extends AppCompatActivity {
    
    private ActivityMainBinding binding;
    private String userName;
    private String userEmail;
    private String userRole;
    private long userId;
    private String userFirebaseUid;
    private boolean isCustomerApp;
    private FirebaseAuthService firebaseAuthService;
    
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuthService = FirebaseAuthService.getInstance();
        
        // Get user data from intent
        Intent intent = getIntent();
        userName = intent.getStringExtra("user_name");
        userEmail = intent.getStringExtra("user_email");
        userRole = intent.getStringExtra("user_role");
        userId = intent.getLongExtra("user_id", -1);
        userFirebaseUid = intent.getStringExtra("user_firebase_uid");
        isCustomerApp = intent.getBooleanExtra("is_customer_app", false);
        
        // Set user info
        binding.textViewWelcome.setText("Welcome, " + (userName != null ? userName : "User"));
        binding.textViewEmail.setText(userEmail != null ? userEmail : "");
        binding.textViewRole.setText("Role: " + (userRole != null ? userRole : ""));
        
        // Configure UI based on user role and app type
        configureUIBasedOnRole();
        
        setupListeners();
    }
    
    private void configureUIBasedOnRole() {
        if (isCustomerApp) {
            // Customer app UI configuration
            binding.cardViewUserManagement.setVisibility(View.GONE);
            // Remove cardViewSync configuration
            // binding.cardViewSync.setVisibility(View.GONE);
            // Add more customer-specific UI configurations here
        } else {
            // Admin app UI configuration
            if (User.ROLE_SUPER_ADMIN.equals(userRole)) {
                // Super admin can see and use all features
                binding.cardViewUserManagement.setVisibility(View.VISIBLE);
            } else if (User.ROLE_ADMIN.equals(userRole)) {
                // Admin can see user management but with limited capabilities
                binding.cardViewUserManagement.setVisibility(View.VISIBLE);
            } else {
                // Hide user management for non-admin roles
                binding.cardViewUserManagement.setVisibility(View.GONE);
            }
        }
    }
    
    private void setupListeners() {
        binding.cardViewCourses.setOnClickListener(v -> {
            // Navigate to CourseListActivity
            Intent intent = new Intent(MainActivity.this, CourseListActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("user_role", userRole);
            intent.putExtra("is_customer_app", isCustomerApp);
            startActivity(intent);
        });
        
        binding.cardViewUserManagement.setOnClickListener(v -> {
            // Navigate to UserManagementActivity
            Intent intent = new Intent(MainActivity.this, UserManagementActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("user_firebase_uid", userFirebaseUid);
            intent.putExtra("user_role", userRole);
            startActivity(intent);
        });
        
        // Remove cardViewSync listener if present
        // binding.cardViewSync.setOnClickListener(...);

        binding.buttonLogout.setOnClickListener(v -> {
            // Sign out from Firebase
            firebaseAuthService.signOut();
            
            // Navigate to login activity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("is_customer_app", isCustomerApp);
            startActivity(intent);
            finish();
        });
    }
}
