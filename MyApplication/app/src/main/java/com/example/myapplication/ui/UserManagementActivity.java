package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.databinding.ActivityUserManagementBinding;
import com.example.myapplication.firebase.FirebaseAuthService;
import com.example.myapplication.model.User;
import com.example.myapplication.adapter.UserAdapter;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {
    private static final String TAG = "UserManagementActivity";

    private ActivityUserManagementBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuthService firebaseAuthService;
    private UserAdapter userAdapter;
    private List<User> userList;
    private long currentUserId;
    private String currentUserFirebaseUid;
    private String currentUserRole;
    // Admin is the only role that can be added
    private String selectedRole = User.ROLE_ADMIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize database helper and Firebase service
        databaseHelper = DatabaseHelper.getInstance(this);
        firebaseAuthService = FirebaseAuthService.getInstance();

        // Get current user info from intent
        Intent intent = getIntent();
        currentUserId = intent.getLongExtra("user_id", -1);
        currentUserFirebaseUid = intent.getStringExtra("user_firebase_uid");
        currentUserRole = intent.getStringExtra("user_role");

        // Show the Add User card only for super admin to add admins
        if (User.ROLE_SUPER_ADMIN.equals(currentUserRole)) {
            binding.cardViewAddUser.setVisibility(View.VISIBLE);
        } else {
            binding.cardViewAddUser.setVisibility(View.GONE);
        }

        // Setup UI components
        setupRecyclerView();
        setupListeners();

        // Load users
        loadUsers();
    }

    // Role spinner removed as we only allow adding admin users

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, currentUserRole, currentUserId, 
                new UserAdapter.UserActionListener() {
                    @Override
                    public void onBanUser(User user) {
                        showBanUserDialog(user);
                    }

                    @Override
                    public void onActivateUser(User user) {
                        activateUser(user);
                    }

                    @Override
                    public void onDeleteUser(User user) {
                        showDeleteUserDialog(user);
                    }
                });

        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewUsers.setAdapter(userAdapter);
    }

    private void setupListeners() {
        binding.buttonAddUser.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString().trim();
            String name = binding.editTextName.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if email already exists in Firebase
            firebaseAuthService.getUserByEmail(email, new FirebaseAuthService.AuthCallback() {
                @Override
                public void onSuccess(User existingUser) {
                    // Email already exists
                    Toast.makeText(UserManagementActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onFailure(String errorMessage) {
                    // Email doesn't exist, create new user
                    createNewUser(email, password, name);
                }
            });
        });

        binding.buttonBack.setOnClickListener(v -> finish());
    }
    
    /**
     * Create a new user in Firebase
     */
    private void createNewUser(String email, String password, String name) {
        // Create new user - only admin role is allowed
        User newUser = new User(email, password, name, selectedRole, currentUserId);
        
        // Register user with Firebase
        firebaseAuthService.registerUser(newUser, new FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(User registeredUser) {
                // Also add to local database for backward compatibility during transition
                long userId = databaseHelper.addUser(registeredUser);
                
                Toast.makeText(UserManagementActivity.this, 
                        "Admin user added successfully", Toast.LENGTH_SHORT).show();
                
                // Clear input fields
                binding.editTextEmail.setText("");
                binding.editTextPassword.setText("");
                binding.editTextName.setText("");
                
                // Reload users
                loadUsers();
            }
            
            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to add user: " + errorMessage);
                Toast.makeText(UserManagementActivity.this, "Failed to add user: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUsers() {
        userList.clear();
        
        // Get all users from Firebase
        firebaseAuthService.getAllUsers(new FirebaseAuthService.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                // Filter users based on current user's role
                for (User user : users) {
                    // Super admin can see all users
                    if (User.ROLE_SUPER_ADMIN.equals(currentUserRole)) {
                        userList.add(user);
                    } 
                    // Admin can see users they created and other customers
                    else if (User.ROLE_ADMIN.equals(currentUserRole)) {
                        if (user.getCreatedBy() == currentUserId || user.isCustomer()) {
                            userList.add(user);
                        }
                    }
                }
                
                userAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load users: " + errorMessage);
                Toast.makeText(UserManagementActivity.this, "Failed to load users: " + errorMessage, Toast.LENGTH_SHORT).show();
                
                // Fallback to local database during transition
                loadUsersFromLocalDatabase();
            }
        });
    }
    
    private void loadUsersFromLocalDatabase() {
        userList.clear();
        
        // Get all users from local database
        List<User> allUsers = databaseHelper.getAllUsers();
        
        // Filter users based on current user's role
        for (User user : allUsers) {
            // Super admin can see all users
            if (User.ROLE_SUPER_ADMIN.equals(currentUserRole)) {
                // Don't show the default super admin in the list if current user is not the default super admin
                if (user.getId() == 1 && currentUserId != 1) {
                    continue;
                }
                userList.add(user);
            } 
            // Admin can see users they created and other customers
            else if (User.ROLE_ADMIN.equals(currentUserRole)) {
                if (user.getCreatedBy() == currentUserId || user.isCustomer()) {
                    userList.add(user);
                }
            }
        }
        
        userAdapter.notifyDataSetChanged();
    }

    private void showBanUserDialog(User user) {
        String userType = user.isAdmin() ? "Admin" : "User";
        String message = "Are you sure you want to ban " + user.getName() + "?";
        
        // Add warning message if banning an admin
        if (user.isAdmin()) {
            message += "\n\nWarning: Banning an admin will prevent them from accessing the system and managing their customers.";
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Ban " + userType)
                .setMessage(message)
                .setPositiveButton("Ban", (dialog, which) -> {
                    banUser(user);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void banUser(User user) {
        // Update user status in Firebase
        firebaseAuthService.updateUserStatus(user.getFirebaseUid(), User.STATUS_BANNED, new FirebaseAuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Also update in local database for backward compatibility
                user.setStatus(User.STATUS_BANNED);
                databaseHelper.updateUser(user);
                
                String userType = user.isAdmin() ? "Admin" : "User";
                Toast.makeText(UserManagementActivity.this, userType + " banned successfully", Toast.LENGTH_SHORT).show();
                
                // Log the action for audit purposes
                if (user.isAdmin()) {
                    Log.i(TAG, "Admin user banned: " + user.getName() + " (ID: " + user.getId() + 
                          ", Email: " + user.getEmail() + ") by user ID: " + currentUserId);
                }
                
                loadUsers();
            }
            
            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to ban user: " + errorMessage);
                Toast.makeText(UserManagementActivity.this, "Failed to ban user: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void activateUser(User user) {
        // Update user status in Firebase
        firebaseAuthService.updateUserStatus(user.getFirebaseUid(), User.STATUS_ACTIVE, new FirebaseAuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Also update in local database for backward compatibility
                user.setStatus(User.STATUS_ACTIVE);
                databaseHelper.updateUser(user);
                
                String userType = user.isAdmin() ? "Admin" : "User";
                Toast.makeText(UserManagementActivity.this, userType + " activated successfully", Toast.LENGTH_SHORT).show();
                
                // Log the action for audit purposes
                if (user.isAdmin()) {
                    Log.i(TAG, "Admin user activated: " + user.getName() + " (ID: " + user.getId() + 
                          ", Email: " + user.getEmail() + ") by user ID: " + currentUserId);
                }
                
                loadUsers();
            }
            
            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to activate user: " + errorMessage);
                Toast.makeText(UserManagementActivity.this, "Failed to activate user: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteUserDialog(User user) {
        String userType = user.isAdmin() ? "Admin" : "User";
        String message = "Are you sure you want to delete " + user.getName() + "? This action cannot be undone.";
        
        // Add warning message if deleting an admin
        if (user.isAdmin()) {
            message += "\n\nWarning: Deleting an admin will remove all their account information and access privileges.";
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Delete " + userType)
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteUser(user);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(User user) {
        // First delete from Firebase if we have a Firebase UID
        if (user.getFirebaseUid() != null && !user.getFirebaseUid().isEmpty()) {
            // Delete the user document from Firestore
            firebaseAuthService.deleteUserData(user.getFirebaseUid(), new FirebaseAuthService.SimpleCallback() {
                @Override
                public void onSuccess() {
                    // Then delete from local database
                    completeUserDeletion(user);
                }
                
                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Failed to delete user from Firebase: " + errorMessage);
                    // Try to delete from local database anyway
                    completeUserDeletion(user);
                }
            });
        } else {
            // If no Firebase UID, just delete from local database
            completeUserDeletion(user);
        }
    }
    
    private void completeUserDeletion(User user) {
        if (databaseHelper.deleteUser(user.getId())) {
            String userType = user.isAdmin() ? "Admin" : "User";
            Toast.makeText(this, userType + " deleted successfully", Toast.LENGTH_SHORT).show();
            
            // Log the action for audit purposes
            if (user.isAdmin()) {
                Log.i(TAG, "Admin user deleted: " + user.getName() + " (ID: " + user.getId() + 
                      ", Email: " + user.getEmail() + ") by user ID: " + currentUserId);
            }
            
            loadUsers();
        } else {
            Toast.makeText(this, "Failed to delete user from local database", Toast.LENGTH_SHORT).show();
        }
    }
}