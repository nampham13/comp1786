package com.example.myapplication.ui.adapter;

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


import android.widget.Filter;
import android.widget.Filterable;
import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> implements Filterable {

    private List<User> userList;
    private List<User> userListFull; // For filtering
    private UserActionListener actionListener;

    public interface UserActionListener {
        void onUserClick(User user);

        void onBanUser(User user);

        void onActivateUser(User user);

        void onDeleteUser(User user);
    }

    public UserAdapter(List<User> userList, UserActionListener actionListener) {
        this.userList = userList;
        this.userListFull = new ArrayList<>(userList);
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use item_user.xml for user items in the recycler
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.textViewName.setText(user.getName());
        holder.textViewEmail.setText(user.getEmail());
        holder.textViewRole.setText(user.getRole());
        holder.textViewStatus.setText(user.getStatus());

        // Show/hide and set listeners for action buttons based on user status/role
        if (holder.buttonBan != null) {
            if ("active".equalsIgnoreCase(user.getStatus())) {
                holder.buttonBan.setVisibility(View.VISIBLE);
            } else {
                holder.buttonBan.setVisibility(View.GONE);
            }
            holder.buttonBan.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onBanUser(user);
            });
        }
        if (holder.buttonActivate != null) {
            if ("banned".equalsIgnoreCase(user.getStatus())) {
                holder.buttonActivate.setVisibility(View.VISIBLE);
            } else {
                holder.buttonActivate.setVisibility(View.GONE);
            }
            holder.buttonActivate.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActivateUser(user);
            });
        }
        if (holder.buttonDelete != null) {
            // Hide delete for super admin, show for others (customize as needed)
            if ("super_admin".equalsIgnoreCase(user.getRole())) {
                holder.buttonDelete.setVisibility(View.GONE);
            } else {
                holder.buttonDelete.setVisibility(View.VISIBLE);
            }
            holder.buttonDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDeleteUser(user);
            });
        }

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

    @Override
    public Filter getFilter() {
        return userFilter;
    }

    private final Filter userFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<User> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(userListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (User user : userListFull) {
                    if ((user.getName() != null && user.getName().toLowerCase().contains(filterPattern)) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(filterPattern)) ||
                        (user.getRole() != null && user.getRole().toLowerCase().contains(filterPattern))) {
                        filteredList.add(user);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            userList.clear();
            //noinspection unchecked
            userList.addAll((List<User>) results.values);
            notifyDataSetChanged();
        }
    };

    // Call this when the full user list changes (e.g., after loading from DB)
    public void updateUserList(List<User> newList) {
        userList.clear();
        userList.addAll(newList);
        userListFull.clear();
        userListFull.addAll(newList);
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewEmail, textViewRole, textViewStatus;
        Button buttonBan, buttonActivate, buttonDelete;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewUserName);
            textViewEmail = itemView.findViewById(R.id.textViewUserEmail);
            textViewRole = itemView.findViewById(R.id.textViewUserRole);
            textViewStatus = itemView.findViewById(R.id.textViewUserStatus);
            buttonBan = itemView.findViewById(R.id.buttonBan);
            buttonActivate = itemView.findViewById(R.id.buttonActivate);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}