package com.example.myapplication.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ItemCourseBinding;
import com.example.myapplication.model.Course;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private final Context context;
    private final List<Course> courseList;
    private OnCourseClickListener listener;

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    public CourseAdapter(Context context, List<Course> courseList) {
        this.context = context;
        this.courseList = courseList;
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
            
            // Remove instance count logic that uses databaseHelper
            // You may want to set a placeholder or use a property from Course if available
            binding.textViewInstanceCount.setText(""); // Or set to "N/A" or similar
            
            // Load course image if available
            if (!TextUtils.isEmpty(course.getPhotoPath())) {
                Glide.with(context)
                    .load(course.getPhotoPath())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(binding.imageViewCourse);
            } else {
                // Set default image
                binding.imageViewCourse.setImageResource(android.R.drawable.ic_menu_gallery);
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
