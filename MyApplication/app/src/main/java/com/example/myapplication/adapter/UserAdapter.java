package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private String currentUserRole;
    private long currentUserId;
    private UserActionListener actionListener;

    public interface UserActionListener {
        void onBanUser(User user);
        void onActivateUser(User user);
        void onDeleteUser(User user);
    }

    public UserAdapter(List<User> userList, String currentUserRole, long currentUserId, UserActionListener actionListener) {
        this.userList = userList;
        this.currentUserRole = currentUserRole;
        this.currentUserId = currentUserId;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        
        holder.textViewName.setText(user.getName());
        holder.textViewEmail.setText(user.getEmail());
        holder.textViewRole.setText("Role: " + user.getRole());
        holder.textViewStatus.setText("Status: " + user.getStatus());
        
        // Configure action buttons based on user role and status
        configureActionButtons(holder, user);
    }

    private void configureActionButtons(UserViewHolder holder, User user) {
        // Hide all buttons by default
        holder.buttonBan.setVisibility(View.GONE);
        holder.buttonActivate.setVisibility(View.GONE);
        holder.buttonDelete.setVisibility(View.GONE);
        
        // Don't allow actions on self
        if (user.getId() == currentUserId) {
            return;
        }
        
        // Super admin can manage all users except other super admins
        if (User.ROLE_SUPER_ADMIN.equals(currentUserRole)) {
            if (!user.isSuperAdmin()) {
                if (user.isActive()) {
                    holder.buttonBan.setVisibility(View.VISIBLE);
                } else {
                    holder.buttonActivate.setVisibility(View.VISIBLE);
                }
                holder.buttonDelete.setVisibility(View.VISIBLE);
            }
        } 
        // Admin can only manage customers they created
        else if (User.ROLE_ADMIN.equals(currentUserRole)) {
            if (user.isCustomer() && user.getCreatedBy() == currentUserId) {
                if (user.isActive()) {
                    holder.buttonBan.setVisibility(View.VISIBLE);
                } else {
                    holder.buttonActivate.setVisibility(View.VISIBLE);
                }
                holder.buttonDelete.setVisibility(View.VISIBLE);
            }
        }
        
        // Set click listeners
        holder.buttonBan.setOnClickListener(v -> actionListener.onBanUser(user));
        holder.buttonActivate.setOnClickListener(v -> actionListener.onActivateUser(user));
        holder.buttonDelete.setOnClickListener(v -> actionListener.onDeleteUser(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewEmail, textViewRole, textViewStatus;
        Button buttonBan, buttonActivate, buttonDelete;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewRole = itemView.findViewById(R.id.textViewRole);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            buttonBan = itemView.findViewById(R.id.buttonBan);
            buttonActivate = itemView.findViewById(R.id.buttonActivate);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}