package com.example.myapplication.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.databinding.ItemCourseBinding;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Instance;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private final Context context;
    private final List<Course> courseList;
    private final DatabaseHelper databaseHelper;
    private OnCourseClickListener listener;

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    public CourseAdapter(Context context, List<Course> courseList) {
        this.context = context;
        this.courseList = courseList;
        this.databaseHelper = DatabaseHelper.getInstance(context);
    }

    public void setOnCourseClickListener(OnCourseClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCourseBinding binding = ItemCourseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.bind(course);
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    class CourseViewHolder extends RecyclerView.ViewHolder {
        private final ItemCourseBinding binding;

        public CourseViewHolder(ItemCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Course course) {
            // Set course name (with null check)
            binding.textViewCourseName.setText(course.getName() != null ? course.getName() : "Unnamed Course");
            
            // Set course level (with null check)
            binding.textViewCourseLevel.setText(course.getLevel() != null ? course.getLevel() : "N/A");
            
            // Set course duration
            binding.textViewCourseDuration.setText(course.getDuration() + " min");
            
            try {
                // Get instances for this course
                List<Instance> instances = databaseHelper.getInstancesByCourseId(course.getId());
                int instanceCount = instances != null ? instances.size() : 0;
                binding.textViewInstanceCount.setText(instanceCount + " " + 
                        (instanceCount == 1 ? "instance" : "instances"));
            } catch (Exception e) {
                // Handle any database errors
                binding.textViewInstanceCount.setText("0 instances");
            }
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCourseClick(course);
                }
            });
        }
    }
}