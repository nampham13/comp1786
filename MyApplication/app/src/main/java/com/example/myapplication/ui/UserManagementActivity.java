package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
                    public void onUserClick(User user) {
                        showUserDetailsDialog(user);
                    }
                    
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
        User newUser = new User(email, password, name, selectedRole, User.STATUS_ACTIVE);
        
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
                Log.d(TAG, "Users fetched from Firebase: " + (users != null ? users.size() : 0));
                if (users != null) {
                    if (User.ROLE_SUPER_ADMIN.equals(currentUserRole)) {
                        // Super admin sees all users
                        userList.addAll(users);
                    } else if (User.ROLE_ADMIN.equals(currentUserRole)) {
                        // Admin sees themselves and all customers
                        for (User user : users) {
                            if (user.getRole() != null && user.getRole().equals(User.ROLE_CUSTOMER)) {
                                userList.add(user);
                            } else if (user.getRole() != null && user.getRole().equals(User.ROLE_ADMIN) && user.getFirebaseUid() != null && user.getFirebaseUid().equals(currentUserFirebaseUid)) {
                                userList.add(user);
                            }
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

        if (User.ROLE_SUPER_ADMIN.equals(currentUserRole)) {
            for (User user : allUsers) {
                // Don't show the default super admin in the list if current user is not the default super admin
                if (user.getId() == 1 && currentUserId != 1) {
                    continue;
                }
                userList.add(user);
            }
        } else if (User.ROLE_ADMIN.equals(currentUserRole)) {
            for (User user : allUsers) {
                if (user.getRole() != null && user.getRole().equals(User.ROLE_CUSTOMER)) {
                    userList.add(user);
                } else if (user.getRole() != null && user.getRole().equals(User.ROLE_ADMIN) && user.getId() == currentUserId) {
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
                // Use the ban() method from User model to update status
                user.ban();
                // Also update in local database for backward compatibility
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
                // Use the activate() method from User model to update status
                user.activate();
                // Also update in local database for backward compatibility
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
        
        // Add specific warning messages based on user role
        if (user.isAdmin()) {
            message += "\n\nWarning: Deleting an admin will remove all their account information and access privileges.";
            
            // Add additional warning for super admin
            if (User.ROLE_SUPER_ADMIN.equals(currentUserRole)) {
                message += "\n\nAs a Super Admin, you can delete any admin user.";
            }
        } else if (user.isCustomer()) {
            message += "\n\nWarning: Deleting a customer will remove all their account information and booking history.";

        }
        
        new AlertDialog.Builder(this)
                .setTitle("Delete " + userType)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Show progress and overlay before deletion
                    showProgressOverlay();
                    deleteUser(user);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(User user) {
        // Show deletion in progress
        showProgressOverlay();
        
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
                    // Show error message
                    runOnUiThread(() -> {
                        hideProgressOverlay();
                        showErrorDialog("Firebase Error", "Failed to delete user from Firebase: " + errorMessage + 
                                "\n\nWould you like to try deleting from local database only?", () -> {
                            // Try to delete from local database anyway
                            completeUserDeletion(user);
                        });
                    });
                }
            });
        } else {
            // If no Firebase UID, just delete from local database
            completeUserDeletion(user);
        }
    }
    
    private void showErrorDialog(String title, String message, Runnable onRetry) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (onRetry != null) {
                        onRetry.run();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                    hideProgressOverlay();
                })
                .show();
    }
    
    /**
     * Show progress overlay during operations
     */
    private void showProgressOverlay() {
        binding.overlayView.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.VISIBLE);
    }
    
    /**
     * Hide progress overlay after operations
     */
    private void hideProgressOverlay() {
        binding.overlayView.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
    }
    
    /**
     * Show user details dialog when a user item is clicked
     */
    private void showUserDetailsDialog(User user) {
        // Create the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_details, null);
        
        // Log the layout inflation
        Log.d(TAG, "Dialog layout inflated: " + (dialogView != null ? "success" : "failed"));
        
        // Find views in the dialog layout
        TextView textViewName = dialogView.findViewById(R.id.textViewName);
        TextView textViewEmail = dialogView.findViewById(R.id.textViewEmail);
        TextView textViewRole = dialogView.findViewById(R.id.textViewRole);
        TextView textViewStatus = dialogView.findViewById(R.id.textViewStatus);
        Button buttonEdit = dialogView.findViewById(R.id.buttonEdit);
        Button buttonDelete = dialogView.findViewById(R.id.buttonDelete);
        
        // Log the button references
        Log.d(TAG, "Button references - Edit: " + (buttonEdit != null ? "found" : "not found") + 
              ", Delete: " + (buttonDelete != null ? "found" : "not found"));
        
        // Set user data to views
        textViewName.setText(user.getName());
        textViewEmail.setText(user.getEmail());
        textViewRole.setText("Role: " + user.getRole());
        textViewStatus.setText("Status: " + user.getStatus());
        
        // Log debug information
        Log.d(TAG, "User details dialog - User ID: " + user.getId() + 
              ", Current User ID: " + currentUserId + 
              ", User Role: " + user.getRole() + 
              ", Current User Role: " + currentUserRole + 
              ", User Status: " + user.getStatus());
        
        // Make sure buttons are visible
        if (buttonEdit != null) {
            buttonEdit.setVisibility(View.VISIBLE);
            // Set appropriate text based on current status
            if (user.isActive()) {
                buttonEdit.setText("Ban User");
                buttonEdit.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_light));
            } else {
                buttonEdit.setText("Activate User");
                buttonEdit.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_light));
            }
        } else {
            Log.e(TAG, "Edit button is null!");
        }
        
        if (buttonDelete != null) {
            buttonDelete.setVisibility(View.VISIBLE);
        } else {
            Log.e(TAG, "Delete button is null!");
        }
        
        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder
                .setTitle("User Details")
                .setView(dialogView)
                .create();
        
        dialog.show();
        
        // Set button click listeners with debug logging
        if (buttonEdit != null) {
            buttonEdit.setOnClickListener(v -> {
                Log.d(TAG, "Edit/Status button clicked for user: " + user.getName());
                dialog.dismiss();
                // Show ban or activate dialog based on current status
                if (user.isActive()) {
                    Log.d(TAG, "User is active, showing ban dialog");
                    showBanUserDialog(user);
                } else {
                    Log.d(TAG, "User is not active, activating user");
                    activateUser(user);
                }
            });
        }
        
        if (buttonDelete != null) {
            buttonDelete.setOnClickListener(v -> {
                Log.d(TAG, "Delete button clicked for user: " + user.getName());
                dialog.dismiss();
                showDeleteUserDialog(user);
            });
        }
    }
    
    private void completeUserDeletion(User user) {
        if (databaseHelper.deleteUser(user.getId())) {
            // Hide progress overlay
            hideProgressOverlay();
            
            String userType = user.isAdmin() ? "Admin" : "User";
            String roleName = "";
            
            // Get more specific role name for the toast message
            if (user.isSuperAdmin()) {
                roleName = "Super Admin";
            } else if (user.isAdmin()) {
                roleName = "Admin";
            } else if (user.isCustomer()) {
                roleName = "Customer";
            }
            
            // Show success message with role information
            Toast.makeText(this, roleName + " " + user.getName() + " deleted successfully", Toast.LENGTH_SHORT).show();
            
            // Log the action for audit purposes
            Log.i(TAG, userType + " deleted: " + user.getName() + " (ID: " + user.getId() + 
                  ", Email: " + user.getEmail() + ", Role: " + user.getRole() + 
                  ") by user ID: " + currentUserId + " with role: " + currentUserRole);
            
            // Reload the user list
            loadUsers();
        } else {
            // Hide progress overlay
            hideProgressOverlay();
            
            // Show error message
            Toast.makeText(this, "Failed to delete user from local database", Toast.LENGTH_SHORT).show();
        }
    }
}