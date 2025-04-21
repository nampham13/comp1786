package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.databinding.ActivityRegisterBinding;
import com.example.myapplication.firebase.FirebaseAuthService;
import com.example.myapplication.model.User;
import com.example.myapplication.util.ValidationUtil;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    
    private ActivityRegisterBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuthService firebaseAuthService;
    private long creatorUserId;
    private User creatorUser;
    private String creatorFirebaseUid;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        databaseHelper = DatabaseHelper.getInstance(this);
        firebaseAuthService = FirebaseAuthService.getInstance();
        
        // Get creator user ID from intent
        Intent intent = getIntent();
        creatorUserId = intent.getLongExtra("creator_user_id", -1);
        creatorFirebaseUid = intent.getStringExtra("creator_firebase_uid");
        
        // If no creator user ID provided, finish activity
        if (creatorUserId == -1) {
            Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get creator user from database
        creatorUser = databaseHelper.getUserById(creatorUserId);
        
        // If creator user is not a super admin, finish activity
        if (creatorUser == null || !creatorUser.isSuperAdmin()) {
            Toast.makeText(this, "Only super admins can create accounts", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupRoleSpinner();
        setupListeners();
    }
    
    private void setupRoleSpinner() {
        // Create role options
        String[] roles = {User.ROLE_ADMIN, User.ROLE_SUPER_ADMIN, User.ROLE_CUSTOMER};
        
        // Create adapter for spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // Set adapter to spinner
        binding.spinnerRole.setAdapter(adapter);
        
        // Set default selection to admin
        binding.spinnerRole.setSelection(0);
    }
    
    private void setupListeners() {
        binding.buttonRegister.setOnClickListener(v -> registerUser());
        
        binding.textViewLogin.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });
    }
    
    private void registerUser() {
        String name = binding.editTextName.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString().trim();
        String role = binding.spinnerRole.getSelectedItem().toString();
        
        // Validate input
        if (!validateInput(name, email, password, confirmPassword)) {
            return;
        }
        
        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Create user object with creator ID
        User user = new User(email, password, name, role, creatorUserId);
        
        // Register user with Firebase
        firebaseAuthService.registerUser(user, new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(User registeredUser) {
                // Also add to local database for backward compatibility during transition
                long userId = databaseHelper.addUser(registeredUser);
                
                // Hide progress
                binding.progressBar.setVisibility(View.GONE);
                
                // Registration successful
                Toast.makeText(RegisterActivity.this, "User created successfully", Toast.LENGTH_SHORT).show();
                
                // Navigate back to previous activity
                finish();
            }
            
            @Override
            public void onFailure(String errorMessage) {
                // Hide progress
                binding.progressBar.setVisibility(View.GONE);
                
                // Registration failed
                Log.e(TAG, "Registration failed: " + errorMessage);
                Toast.makeText(RegisterActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        boolean isValid = true;
        
        if (!ValidationUtil.isValidName(name)) {
            binding.textInputLayoutName.setError("Please enter a valid name");
            isValid = false;
        } else {
            binding.textInputLayoutName.setError(null);
        }
        
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
        
        if (!password.equals(confirmPassword)) {
            binding.textInputLayoutConfirmPassword.setError("Passwords do not match");
            isValid = false;
        } else {
            binding.textInputLayoutConfirmPassword.setError(null);
        }
        
        return isValid;
    }
}