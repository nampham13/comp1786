package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivitySyncBinding;
import com.example.myapplication.firebase.FirebaseService;
import com.google.firebase.auth.FirebaseAuth;

public class SyncActivity extends AppCompatActivity {

    private ActivitySyncBinding binding;
    private FirebaseService firebaseService;
    private FirebaseAuth mAuth;
    private Button buttonViewCourses;

    private boolean dataHasBeenSynced = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySyncBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sync with Firebase");
        }

        // Initialize Firebase services
        firebaseService = FirebaseService.getInstance(this);
        mAuth = FirebaseAuth.getInstance();

        // Setup login button
        binding.buttonLogin.setOnClickListener(v -> loginToFirebase());

        // Setup sync buttons
        binding.buttonSyncToFirebase.setOnClickListener(v -> syncToFirebase());
        binding.buttonSyncFromFirebase.setOnClickListener(v -> syncFromFirebase());
        binding.buttonLogout.setOnClickListener(v -> logoutFromFirebase());

        // Add a button to view courses after sync
        buttonViewCourses = new Button(this);
        buttonViewCourses.setText("View Updated Courses");
        buttonViewCourses.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonViewCourses.setOnClickListener(v -> navigateToCourseList());
        
        // Initially hide the button
        buttonViewCourses.setVisibility(View.GONE);
        
        // Add the button to the layout
        ((android.view.ViewGroup) binding.layoutSync).addView(buttonViewCourses, 
                ((android.view.ViewGroup) binding.layoutSync).getChildCount() - 1); // Add before logout button

        // Check if user is already signed in
        updateUIBasedOnAuthState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in when activity starts
        updateUIBasedOnAuthState();
    }

    private void updateUIBasedOnAuthState() {
        if (firebaseService.isUserSignedIn()) {
            // User is signed in, show sync options
            binding.layoutLogin.setVisibility(View.GONE);
            binding.layoutSync.setVisibility(View.VISIBLE);
            binding.textViewLoggedInAs.setText("Logged in as: " + mAuth.getCurrentUser().getEmail());
            
            // Show the view courses button if data has been synced
            buttonViewCourses.setVisibility(dataHasBeenSynced ? View.VISIBLE : View.GONE);
        } else {
            // User is not signed in, show login form
            binding.layoutLogin.setVisibility(View.VISIBLE);
            binding.layoutSync.setVisibility(View.GONE);
        }
    }

    private void loginToFirebase() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        // Validate input
        if (email.isEmpty()) {
            binding.editTextEmail.setError("Email is required");
            binding.editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.editTextPassword.setError("Password is required");
            binding.editTextPassword.requestFocus();
            return;
        }

        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);

        // Attempt login
        firebaseService.signIn(email, password, task -> {
            binding.progressBar.setVisibility(View.GONE);
            
            if (task.isSuccessful()) {
                // Login successful
                Toast.makeText(SyncActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                updateUIBasedOnAuthState();
            } else {
                // Login failed
                Toast.makeText(SyncActivity.this, "Login failed: " + task.getException().getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncToFirebase() {
        // Show progress
        binding.progressBarSync.setVisibility(View.VISIBLE);
        binding.textViewSyncStatus.setText("Syncing to Firebase...");

        // Log the sync operation
        android.util.Log.d("SyncActivity", "Starting sync to Firebase");

        // Perform sync
        firebaseService.syncCoursesToFirebase(new FirebaseService.OnSyncListener() {
            @Override
            public void onSyncComplete(String message) {
                android.util.Log.d("SyncActivity", "Sync to Firebase completed: " + message);
                runOnUiThread(() -> {
                    binding.progressBarSync.setVisibility(View.GONE);
                    binding.textViewSyncStatus.setText("Sync Status: " + message);
                    Toast.makeText(SyncActivity.this, message, Toast.LENGTH_SHORT).show();
                    
                    // Data has been synced
                    dataHasBeenSynced = true;
                    buttonViewCourses.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onSyncFailed(String errorMessage) {
                android.util.Log.e("SyncActivity", "Sync to Firebase failed: " + errorMessage);
                runOnUiThread(() -> {
                    binding.progressBarSync.setVisibility(View.GONE);
                    binding.textViewSyncStatus.setText("Sync Failed: " + errorMessage);
                    Toast.makeText(SyncActivity.this, "Sync failed: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void syncFromFirebase() {
        // Show progress
        binding.progressBarSync.setVisibility(View.VISIBLE);
        binding.textViewSyncStatus.setText("Syncing from Firebase...");

        // Log the sync operation
        android.util.Log.d("SyncActivity", "Starting sync from Firebase");

        // Perform sync
        firebaseService.syncCoursesFromFirebase(new FirebaseService.OnSyncListener() {
            @Override
            public void onSyncComplete(String message) {
                android.util.Log.d("SyncActivity", "Sync from Firebase completed: " + message);
                runOnUiThread(() -> {
                    binding.progressBarSync.setVisibility(View.GONE);
                    binding.textViewSyncStatus.setText("Sync Status: " + message);
                    Toast.makeText(SyncActivity.this, message, Toast.LENGTH_SHORT).show();
                    
                    // Data has been synced
                    dataHasBeenSynced = true;
                    buttonViewCourses.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onSyncFailed(String errorMessage) {
                android.util.Log.e("SyncActivity", "Sync from Firebase failed: " + errorMessage);
                runOnUiThread(() -> {
                    binding.progressBarSync.setVisibility(View.GONE);
                    binding.textViewSyncStatus.setText("Sync Failed: " + errorMessage);
                    Toast.makeText(SyncActivity.this, "Sync failed: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void navigateToCourseList() {
        // Navigate to CourseListActivity to see the updated courses
        Intent intent = new Intent(this, CourseListActivity.class);
        startActivity(intent);
    }

    private void logoutFromFirebase() {
        firebaseService.signOut();
        updateUIBasedOnAuthState();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}