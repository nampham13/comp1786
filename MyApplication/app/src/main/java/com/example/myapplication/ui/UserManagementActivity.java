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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        userAdapter = new UserAdapter(userList, 
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
                
                // Update the user object with the local database ID
                registeredUser.setId(userId);
                
                // Update the Firebase document with the local database ID
                if (userId > 0 && registeredUser.getFirebaseUid() != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("localId", userId);
                    
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(registeredUser.getFirebaseUid())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Firebase document updated with local ID: " + userId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update Firebase document with local ID: " + e.getMessage());
                            });
                }
                
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
                    if (User.ROLE_SUPER_ADMIN.equals(currentUserRole) || User.ROLE_ADMIN.equals(currentUserRole)) {
                        // Both super admin and admin see all users
                        userList.addAll(users);
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

        if (User.ROLE_SUPER_ADMIN.equals(currentUserRole) || User.ROLE_ADMIN.equals(currentUserRole)) {
            for (User user : allUsers) {
                // Don't show the default super admin in the list if current user is not the default super admin
                if (user.getId() == 1 && currentUserId != 1) {
                    continue;
                }
                userList.add(user);
            }
        }

        userAdapter.notifyDataSetChanged();
    }

    private void showBanUserDialog(User user) {
        // Check if current user is trying to ban themselves
        if (user.getId() == currentUserId || 
            (user.getFirebaseUid() != null && user.getFirebaseUid().equals(currentUserFirebaseUid))) {
            Toast.makeText(this, "You cannot ban yourself", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if super admin is trying to ban another super admin
        if (User.ROLE_SUPER_ADMIN.equals(currentUserRole) && user.isSuperAdmin()) {
            Toast.makeText(this, "Super admins cannot ban other super admins", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if admin is trying to ban another admin or super admin
        if (User.ROLE_ADMIN.equals(currentUserRole) && (user.isAdmin() || user.isSuperAdmin())) {
            Toast.makeText(this, "Admins cannot ban other admins or super admins", Toast.LENGTH_SHORT).show();
            return;
        }
        
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
        // Check if admin is trying to activate another admin or super admin
        if (User.ROLE_ADMIN.equals(currentUserRole) && (user.isAdmin() || user.isSuperAdmin())) {
            Toast.makeText(this, "Admins cannot activate other admins or super admins", Toast.LENGTH_SHORT).show();
            return;
        }
        
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
        // Check if current user is trying to delete themselves
        if (user.getId() == currentUserId || 
            (user.getFirebaseUid() != null && user.getFirebaseUid().equals(currentUserFirebaseUid))) {
            Toast.makeText(this, "You cannot delete your own account", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if super admin is trying to delete another super admin
        if (User.ROLE_SUPER_ADMIN.equals(currentUserRole) && user.isSuperAdmin()) {
            Toast.makeText(this, "Super admins cannot delete other super admins", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if admin is trying to delete another admin or super admin
        if (User.ROLE_ADMIN.equals(currentUserRole) && (user.isAdmin() || user.isSuperAdmin())) {
            Toast.makeText(this, "Admins cannot delete other admins or super admins", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Special protection for the default super admin (ID 1)
        if (user.getId() == 1) {
            Toast.makeText(this, "The default super admin account cannot be deleted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userType = user.isAdmin() ? "Admin" : "User";
        String message = "Are you sure you want to delete " + user.getName() + "? This action cannot be undone.";
        
        // Add specific warning messages based on user role
        if (user.isAdmin()) {
            message += "\n\nWarning: Deleting an admin will remove all their account information and access privileges.";
            
            // Add additional warning for super admin
            if (User.ROLE_SUPER_ADMIN.equals(currentUserRole)) {
                message += "\n\nAs a Super Admin, you can delete admin users.";
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
        
        // Log user details for debugging
        Log.d(TAG, "Starting deletion process for user: ID=" + user.getId() + 
              ", Name=" + user.getName() + 
              ", Email=" + user.getEmail() + 
              ", Role=" + user.getRole() + 
              ", Firebase UID=" + (user.getFirebaseUid() != null ? user.getFirebaseUid() : "null"));
        
        // First delete from Firebase if we have a Firebase UID
        if (user.getFirebaseUid() != null && !user.getFirebaseUid().isEmpty()) {
            // Delete the user document from Firestore
            firebaseAuthService.deleteUserData(user.getFirebaseUid(), new FirebaseAuthService.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully deleted user from Firebase: " + user.getEmail());
                    
                    // Check if user exists in local database
                    if (user.getId() <= 0) {
                        // User doesn't exist in local database, just show success message
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
                        Toast.makeText(UserManagementActivity.this, 
                                roleName + " " + user.getName() + " deleted successfully from Firebase", 
                                Toast.LENGTH_SHORT).show();
                        
                        // Log the action for audit purposes
                        Log.i(TAG, userType + " deleted from Firebase: " + user.getName() + 
                              " (Email: " + user.getEmail() + ", Role: " + user.getRole() + 
                              ") by user ID: " + currentUserId + " with role: " + currentUserRole);
                        
                        // Reload the user list
                        loadUsers();
                    } else {
                        completeUserDeletion(user);
                    }
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
                            if (user.getId() > 0) {
                                completeUserDeletion(user);
                            } else {
                                // User doesn't exist in local database
                                Toast.makeText(UserManagementActivity.this, 
                                        "User doesn't exist in local database", 
                                        Toast.LENGTH_SHORT).show();
                                loadUsers();
                            }
                        });
                    });
                }
            });
        } else {
            // If no Firebase UID, just delete from local database
            if (user.getId() > 0) {
                completeUserDeletion(user);
            } else {
                // Invalid user ID
                hideProgressOverlay();
                Toast.makeText(this, "Cannot delete user: Invalid user ID", Toast.LENGTH_SHORT).show();
                loadUsers();
            }
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
     * Completes user deletion from the local database and updates UI.
     */
    private void completeUserDeletion(User user) {
        // Delete user from local database
        boolean deleted = databaseHelper.deleteUser(user.getId());
        hideProgressOverlay();

        String userType = user.isAdmin() ? "Admin" : "User";
        String roleName = "";
        if (user.isSuperAdmin()) {
            roleName = "Super Admin";
        } else if (user.isAdmin()) {
            roleName = "Admin";
        } else if (user.isCustomer()) {
            roleName = "Customer";
        }

        if (deleted) {
            Toast.makeText(this,
                    roleName + " " + user.getName() + " deleted successfully",
                    Toast.LENGTH_SHORT).show();

            Log.i(TAG, userType + " deleted from local DB: " + user.getName() +
                    " (Email: " + user.getEmail() + ", Role: " + user.getRole() +
                    ") by user ID: " + currentUserId + " with role: " + currentUserRole);
        } else {
            Toast.makeText(this,
                    "Failed to delete user from local database",
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to delete user from local DB: " + user.getName() +
                    " (ID: " + user.getId() + ")");
        }

        loadUsers();
    }
    
    /**
     * Show user details dialog when a user item is clicked
     */
    private void showUserDetailsDialog(User user) {
        // Check if user has a Firebase UID
        if (user.getFirebaseUid() != null && !user.getFirebaseUid().isEmpty()) {
            // Get the latest user data from Firebase to ensure we have the current status
            firebaseAuthService.getUserByUid(user.getFirebaseUid(), new FirebaseAuthService.AuthCallback() {
                @Override
                public void onSuccess(User updatedUser) {
                    // Use the updated user data from Firebase
                    showUserDetailsDialogWithData(updatedUser != null ? updatedUser : user);
                }
                
                @Override
                public void onFailure(String errorMessage) {
                    // If we can't get updated data, use what we have
                    Log.e(TAG, "Failed to get updated user data: " + errorMessage);
                    showUserDetailsDialogWithData(user);
                }
            });
        } else {
            // If user doesn't have a Firebase UID, just show the dialog with the data we have
            showUserDetailsDialogWithData(user);
        }
    }
    
    /**
     * Display the user details dialog with the provided user data
     * 
     * @param user The user whose details will be displayed
     */
    private void showUserDetailsDialogWithData(User user) {
        // Inflate the dialog_user_details layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_details, null);

        // Find views in the dialog layout
        TextView textViewNameValue = dialogView.findViewById(R.id.textViewDialogNameValue);
        TextView textViewEmailValue = dialogView.findViewById(R.id.textViewDialogEmailValue);
        TextView textViewRoleValue = dialogView.findViewById(R.id.textViewDialogRoleValue);
        TextView textViewStatusValue = dialogView.findViewById(R.id.textViewDialogStatusValue);
        Button buttonBan = dialogView.findViewById(R.id.buttonDialogBan);
        Button buttonActivate = dialogView.findViewById(R.id.buttonDialogActivate);
        Button buttonDelete = dialogView.findViewById(R.id.buttonDialogDelete);
        Button buttonClose = dialogView.findViewById(R.id.buttonDialogClose);

        // Set user data to views
        textViewNameValue.setText(user.getName() != null ? user.getName() : "");
        textViewEmailValue.setText(user.getEmail() != null ? user.getEmail() : "");
        
        // Format role for display (convert from code to readable format)
        String displayRole = "";
        if (user.isSuperAdmin()) {
            displayRole = "Super Admin";
        } else if (user.isAdmin()) {
            displayRole = "Admin";
        } else if (user.isCustomer()) {
            displayRole = "Customer";
        } else {
            displayRole = user.getRole() != null ? user.getRole() : "";
        }
        textViewRoleValue.setText(displayRole);
        
        // Format status for display (convert from code to readable format)
        String displayStatus = "";
        if (user.isActive()) {
            displayStatus = "Active";
            textViewStatusValue.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (user.isBanned()) {
            displayStatus = "Banned";
            textViewStatusValue.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            displayStatus = user.getStatus() != null ? user.getStatus() : "";
        }
        textViewStatusValue.setText(displayStatus);

        // Configure action buttons based on user role and status
        configureDialogActionButtons(buttonBan, buttonActivate, buttonDelete, user);

        // Create the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set click listeners for action buttons
        buttonBan.setOnClickListener(v -> {
            dialog.dismiss();
            showBanUserDialog(user);
        });

        buttonActivate.setOnClickListener(v -> {
            dialog.dismiss();
            activateUser(user);
        });

        buttonDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteUserDialog(user);
        });

        buttonClose.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
        dialog.show();
    }
    
    /**
     * Configure the visibility of action buttons in the user details dialog
     * based on user roles and permissions
     * 
     * @param buttonBan The button to ban a user
     * @param buttonActivate The button to activate a user
     * @param buttonDelete The button to delete a user
     * @param user The user whose details are being displayed
     */
    private void configureDialogActionButtons(Button buttonBan, Button buttonActivate, Button buttonDelete, User user) {
        // Hide all buttons by default
        buttonBan.setVisibility(View.GONE);
        buttonActivate.setVisibility(View.GONE);
        buttonDelete.setVisibility(View.GONE);

        // Log user status for debugging
        Log.d(TAG, "Configuring buttons for user: " + user.getName() + 
              ", Status: " + user.getStatus() + 
              ", isActive: " + user.isActive() + 
              ", isBanned: " + user.isBanned());

        // Don't allow actions on self only if the user is an admin or superadmin (not for customers)
        if ((user.isAdmin() || user.isSuperAdmin()) &&
            (user.getId() == currentUserId || 
            (user.getFirebaseUid() != null && user.getFirebaseUid().equals(currentUserFirebaseUid)))) {
            Log.d(TAG, "No actions allowed on self (admin/superadmin)");
            return;
        }
        
        // Special protection for the default super admin (ID 1)
        if (user.getId() == 1 && currentUserId != 1) {
            Log.d(TAG, "No actions allowed on default super admin");
            return;
        }

        // Determine if status action buttons should be shown
        boolean canShowStatusButtons = false;
        boolean canShowDeleteButton = false;

        // Super Admin can manage admins and customers (not other super admins)
        if (User.ROLE_SUPER_ADMIN.equals(currentUserRole)) {
            if (!user.isSuperAdmin()) {
                canShowStatusButtons = true;
                canShowDeleteButton = true;
                Log.d(TAG, "Super admin can manage this user (admin or customer)");
            } else {
                Log.d(TAG, "Super admin cannot manage other super admins");
            }
        }
        // Admin can only manage customers
        else if (User.ROLE_ADMIN.equals(currentUserRole)) {
            if (user.isCustomer()) {
                canShowStatusButtons = true;
                canShowDeleteButton = true;
                Log.d(TAG, "Admin can manage this customer");
            } else {
                Log.d(TAG, "Admin cannot manage admins or super admins");
            }
        }

        // Show appropriate status button based on user's current status
        if (canShowStatusButtons) {
            if (User.STATUS_ACTIVE.equals(user.getStatus())) {
                buttonBan.setVisibility(View.VISIBLE);
                Log.d(TAG, "Showing Ban button for active user");
            } else if (User.STATUS_BANNED.equals(user.getStatus())) {
                buttonActivate.setVisibility(View.VISIBLE);
                Log.d(TAG, "Showing Activate button for banned user");
            }
        }
        
        // Show delete button if allowed
        if (canShowDeleteButton) {
            buttonDelete.setVisibility(View.VISIBLE);
            Log.d(TAG, "Showing Delete button");
        }
    }
}
