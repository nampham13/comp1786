package com.example.myapplication.ui;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityUserManagementBinding;
import com.example.myapplication.model.User;
import com.example.myapplication.ui.adapter.UserAdapter;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {
    private static final String TAG = "UserManagementActivity";

    private ActivityUserManagementBinding binding;
    private UserAdapter userAdapter;
    private List<User> userList;
    private long currentUserId;
    private String currentUserRole;
    // Admin is the only role that can be added
    private String selectedRole = User.ROLE_ADMIN;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Get current user info from intent
        Intent intent = getIntent();
        currentUserId = intent.getLongExtra("user_id", -1);
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

        // Setup search bar
        EditText editTextSearchUser = findViewById(R.id.editTextSearchUser);
        ImageButton buttonSearchUser = findViewById(R.id.buttonSearchUser);

        // Text change listener for live search
        editTextSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (userAdapter != null) {
                    userAdapter.getFilter().filter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Optional: search button click triggers filter (useful for keyboard search action)
        buttonSearchUser.setOnClickListener(v -> {
            if (userAdapter != null) {
                userAdapter.getFilter().filter(editTextSearchUser.getText());
            }
        });

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

            // Remove databaseHelper.getUserByEmail(email)
            // User existingUser = databaseHelper.getUserByEmail(email);
            User existingUser = null; // Placeholder: implement your own check if needed

            if (existingUser != null) {
                Toast.makeText(UserManagementActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
            } else {
                createNewUser(email, password, name);
            }
        });

        binding.buttonBack.setOnClickListener(v -> finish());

    }

    /**
     * Create a new user in the database
     */
    private void createNewUser(String email, String password, String name) {
        // Create new user - only admin role is allowed
        User newUser = new User(email, password, name, selectedRole, User.STATUS_ACTIVE);
        showProgressOverlay();
        com.example.myapplication.firebase.FirebaseAuthService.getInstance().registerUser(newUser, new com.example.myapplication.firebase.FirebaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                hideProgressOverlay();
                Toast.makeText(UserManagementActivity.this, "Admin user added successfully", Toast.LENGTH_SHORT).show();
                // Clear input fields
                binding.editTextEmail.setText("");
                binding.editTextPassword.setText("");
                binding.editTextName.setText("");
                // Reload users
                loadUsers();
            }

            @Override
            public void onFailure(String errorMessage) {
                hideProgressOverlay();
                Log.e(TAG, "Failed to add user to Firebase: " + errorMessage);
                Toast.makeText(UserManagementActivity.this, "Failed to add user: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUsers() {
        showProgressOverlay();
        com.example.myapplication.firebase.FirebaseAuthService.getInstance().getAllUsers(new com.example.myapplication.firebase.FirebaseAuthService.UsersCallback() {
            @Override
            public void onSuccess(List<User> allUsers) {
                Log.d(TAG, "Users fetched from Firebase: " + allUsers.size());
                List<User> filteredUsers = new ArrayList<>();
                if (User.ROLE_SUPER_ADMIN.equals(currentUserRole) || User.ROLE_ADMIN.equals(currentUserRole)) {
                    for (User user : allUsers) {
                        // Don't show the default super admin in the list if currentUserId != 1
                        if (user.getId() == 1 && currentUserId != 1) {
                            continue;
                        }
                        filteredUsers.add(user);
                    }
                }
                userList.clear();
                userList.addAll(filteredUsers);
                if (userAdapter != null) {
                    userAdapter.updateUserList(new ArrayList<>(filteredUsers));
                } else {
                    userAdapter.notifyDataSetChanged();
                }
                hideProgressOverlay();
            }

            @Override
            public void onFailure(String errorMessage) {
                hideProgressOverlay();
                Log.e(TAG, "Failed to fetch users from Firebase: " + errorMessage);
                Toast.makeText(UserManagementActivity.this, "Failed to load users: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBanUserDialog(User user) {
        // Check if current user is trying to ban themselves
        if (user.getId() == currentUserId) {
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
        showProgressOverlay();
        com.example.myapplication.firebase.FirebaseAuthService.getInstance().updateUserStatus(user.getFirebaseUid(), User.STATUS_BANNED, new com.example.myapplication.firebase.FirebaseAuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
                hideProgressOverlay();
                String userType = user.isAdmin() ? "Admin" : "User";
                Toast.makeText(UserManagementActivity.this, userType + " banned successfully", Toast.LENGTH_SHORT).show();
                if (user.isAdmin()) {
                    Log.i(TAG, "Admin user banned: " + user.getName() + " (Email: " + user.getEmail() + ") by user ID: " + currentUserId);
                }
                loadUsers();
            }

            @Override
            public void onFailure(String errorMessage) {
                hideProgressOverlay();
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
        showProgressOverlay();
        com.example.myapplication.firebase.FirebaseAuthService.getInstance().updateUserStatus(user.getFirebaseUid(), User.STATUS_ACTIVE, new com.example.myapplication.firebase.FirebaseAuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
                hideProgressOverlay();
                String userType = user.isAdmin() ? "Admin" : "User";
                Toast.makeText(UserManagementActivity.this, userType + " activated successfully", Toast.LENGTH_SHORT).show();
                if (user.isAdmin()) {
                    Log.i(TAG, "Admin user activated: " + user.getName() + " (Email: " + user.getEmail() + ") by user ID: " + currentUserId);
                }
                loadUsers();
            }

            @Override
            public void onFailure(String errorMessage) {
                hideProgressOverlay();
                Toast.makeText(UserManagementActivity.this, "Failed to activate user: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteUserDialog(User user) {
        // Check if current user is trying to delete themselves
        if (user.getId() == currentUserId) {
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
        showProgressOverlay();
        Log.d(TAG, "Starting deletion process for user: Name=" + user.getName() + ", Email=" + user.getEmail() + ", Role=" + user.getRole());
        com.example.myapplication.firebase.FirebaseAuthService.getInstance().deleteUserData(user.getFirebaseUid(), new com.example.myapplication.firebase.FirebaseAuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
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
                Toast.makeText(UserManagementActivity.this, roleName + " " + user.getName() + " deleted successfully", Toast.LENGTH_SHORT).show();
                Log.i(TAG, userType + " deleted: " + user.getName() + " (Email: " + user.getEmail() + ", Role: " + user.getRole() + ") by user ID: " + currentUserId + " with role: " + currentUserRole);
                loadUsers();
            }

            @Override
            public void onFailure(String errorMessage) {
                hideProgressOverlay();
                Toast.makeText(UserManagementActivity.this, "Failed to delete user: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to delete user: " + user.getName() + " (ID: " + user.getId() + ")");
            }
        });
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
}