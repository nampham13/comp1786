package com.example.myapplication.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ItemInstanceBinding;
import com.example.myapplication.model.Instance;

import java.util.List;

public class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.InstanceViewHolder> {

    private final Context context;
    private final List<Instance> instanceList;
    private OnInstanceClickListener listener;

    public interface OnInstanceClickListener {
        void onInstanceClick(Instance instance);
    }

    public InstanceAdapter(Context context, List<Instance> instanceList) {
        this.context = context;
        this.instanceList = instanceList;
    }

    public void setOnInstanceClickListener(OnInstanceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public InstanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInstanceBinding binding = ItemInstanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new InstanceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InstanceViewHolder holder, int position) {
        Instance instance = instanceList.get(position);
        holder.bind(instance);
    }

    @Override
    public int getItemCount() {
        return instanceList.size();
    }

    class InstanceViewHolder extends RecyclerView.ViewHolder {
        private final ItemInstanceBinding binding;

        public InstanceViewHolder(ItemInstanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Instance instance) {
            // Set date and time
            binding.textViewInstanceDate.setText(instance.getDate());
            binding.textViewInstanceTime.setText(instance.getTime());
            
            // Set location
            binding.textViewInstanceLocation.setText(instance.getLocation());
            
            // Set instructor
            binding.textViewInstanceInstructor.setText(instance.getInstructor());
            
            // Set price
            binding.textViewInstancePrice.setText("$" + instance.getPrice());
            
            // Set capacity
            binding.textViewInstanceCapacity.setText(instance.getCapacity() + " spots");
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onInstanceClick(instance);
                }
            });
        }
    }
}