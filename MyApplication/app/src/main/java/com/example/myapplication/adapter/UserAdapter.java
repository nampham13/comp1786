
package com.example.myapplication.adapter;

import android.widget.Button;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private UserActionListener actionListener;

    public interface UserActionListener {
        void onUserClick(User user);
        void onBanUser(User user);
        void onActivateUser(User user);
        void onDeleteUser(User user);
    }

    public UserAdapter(List<User> userList, UserActionListener actionListener) {
        this.userList = userList;
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

        // Action buttons
        if (holder.buttonActivate != null && holder.buttonDelete != null) {
            if (User.STATUS_BANNED.equals(user.getStatus())) {
                // Banned: show delete full width, hide activate
                holder.buttonActivate.setVisibility(View.GONE);
                holder.buttonDelete.setVisibility(View.VISIBLE);
                // Make delete button full width
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.buttonDelete.getLayoutParams();
                params.weight = 2f;
                holder.buttonDelete.setLayoutParams(params);
            } else {
                // Not banned: show both, each half width
                holder.buttonActivate.setVisibility(View.GONE); // Only show if banned
                holder.buttonDelete.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.buttonDelete.getLayoutParams();
                params.weight = 1f;
                holder.buttonDelete.setLayoutParams(params);
            }

            // Show activate button only if banned
            if (User.STATUS_BANNED.equals(user.getStatus())) {
                holder.buttonActivate.setVisibility(View.VISIBLE);
            } else {
                holder.buttonActivate.setVisibility(View.GONE);
            }

            // Button listeners
            holder.buttonDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDeleteUser(user);
            });
            holder.buttonActivate.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActivateUser(user);
            });
        }

        // Set click listener for the whole user item (card)
        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewEmail, textViewRole, textViewStatus;
        ImageView imageViewArrow;
        Button buttonActivate, buttonDelete;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewRole = itemView.findViewById(R.id.textViewRole);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            imageViewArrow = itemView.findViewById(R.id.imageViewArrow);
            buttonActivate = itemView.findViewById(R.id.buttonActivate);
        }
    }
}