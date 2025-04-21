package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.databinding.ActivityLoginBinding;
import com.example.myapplication.firebase.FirebaseAuthService;
import com.example.myapplication.model.User;
import com.example.myapplication.util.ValidationUtil;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    private ActivityLoginBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuthService firebaseAuthService;
    private boolean isCustomerApp;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        databaseHelper = DatabaseHelper.getInstance(this);
        firebaseAuthService = FirebaseAuthService.getInstance();
        
        // Check if this is the customer app
        Intent intent = getIntent();
        isCustomerApp = intent.getBooleanExtra("is_customer_app", false);
        
        setupListeners();
    }
    
    private void setupListeners() {
        binding.buttonLogin.setOnClickListener(v -> loginUser());
    }
    
    private void loginUser() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();
        
        // Validate input
        if (!validateInput(email, password)) {
            return;
        }
        
        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Authenticate user with Firebase
        firebaseAuthService.loginUser(email, password, new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                // Hide progress
                binding.progressBar.setVisibility(View.GONE);
                
                // Check if user role is compatible with the app type
                if (isCustomerApp) {
                    // Only customers can access the customer app
                    if (!user.isCustomer()) {
                        Toast.makeText(LoginActivity.this, "Admin accounts cannot access the customer app", Toast.LENGTH_SHORT).show();
                        firebaseAuthService.signOut();
                        return;
                    }
                } else {
                    // Only admins and super admins can access the admin app
                    if (user.isCustomer()) {
                        Toast.makeText(LoginActivity.this, "Customer accounts cannot access the admin app", Toast.LENGTH_SHORT).show();
                        firebaseAuthService.signOut();
                        return;
                    }
                }
                
                // Login successful
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                
                // Navigate to main activity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("user_id", user.getId());
                intent.putExtra("user_firebase_uid", user.getFirebaseUid());
                intent.putExtra("user_name", user.getName());
                intent.putExtra("user_email", user.getEmail());
                intent.putExtra("user_role", user.getRole());
                intent.putExtra("is_customer_app", isCustomerApp);
                startActivity(intent);
                finish();
            }
            
            @Override
            public void onFailure(String errorMessage) {
                // Hide progress
                binding.progressBar.setVisibility(View.GONE);
                
                // Login failed
                Log.e(TAG, "Login failed: " + errorMessage);
                Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                
                // Fallback to local authentication for existing users during transition
                User user = databaseHelper.authenticateUser(email, password);
                if (user != null) {
                    // Migrate user to Firebase
                    migrateUserToFirebase(user, password);
                }
            }
        });
    }
    
    /**
     * Migrate existing local user to Firebase
     */
    private void migrateUserToFirebase(User user, String password) {
        // Set password for migration
        user.setPassword(password);
        
        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Register user in Firebase
        firebaseAuthService.registerUser(user, new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(User migratedUser) {
                // Hide progress
                binding.progressBar.setVisibility(View.GONE);
                
                Toast.makeText(LoginActivity.this, "Account migrated to new system. Please login again.", Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onFailure(String errorMessage) {
                // Hide progress
                binding.progressBar.setVisibility(View.GONE);
                
                Log.e(TAG, "Migration failed: " + errorMessage);
                Toast.makeText(LoginActivity.this, "Account migration failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private boolean validateInput(String email, String password) {
        boolean isValid = true;
        
        if (!ValidationUtil.isValidEmail(email)) {
            binding.textInputLayoutEmail.setError("Please enter a valid email");
            isValid = false;
        } else {
            binding.textInputLayoutEmail.setError(null);
        }
        
        if (!ValidationUtil.isValidPassword(password)) {
            binding.textInputLayoutPassword.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            binding.textInputLayoutPassword.setError(null);
        }
        
        return isValid;
    }
}