package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityCourseDetailBinding;
import com.example.myapplication.firebase.FirebaseService;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Instance;
import com.example.myapplication.ui.adapter.InstanceAdapter;

import java.util.ArrayList;
import java.util.List;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "extra_course_id";

    private ActivityCourseDetailBinding binding;
    private Course course;
    private List<Instance> instanceList;
    private InstanceAdapter instanceAdapter;
    private FirebaseService firebaseService;

    private final ActivityResultLauncher<Intent> editCourseLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Course updatedCourse = (Course) result.getData().getSerializableExtra(CourseFormActivity.EXTRA_COURSE);
                    if (updatedCourse != null) {
                        course = updatedCourse;
                        updateCourseDetails();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase service
        firebaseService = FirebaseService.getInstance();
        
        // Get course ID from intent
        long courseId = getIntent().getLongExtra(EXTRA_COURSE_ID, -1);
        if (courseId == -1) {
            Toast.makeText(this, "Error: Course ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loading state
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Load course details from Firebase
        firebaseService.syncCoursesFromFirebase(new FirebaseService.OnCourseSyncListener() {
            @Override
            public void onSyncComplete(List<Course> courses, String message) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    
                    // Find the course with matching ID
                    for (Course c : courses) {
                        if (c.getId() == courseId) {
                            course = c;
                            break;
                        }
                    }
                    
                    if (course == null) {
                        Toast.makeText(CourseDetailActivity.this, "Error: Course not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    
                    // Set toolbar title and update UI
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(course.getName());
                    }
                    updateCourseDetails();
                    loadInstances();
                });
            }

            @Override
            public void onSyncFailed(String errorMessage) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(CourseDetailActivity.this, "Error loading course: " + errorMessage, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });

        // Initialize instance list
        instanceList = new ArrayList<>();
        instanceAdapter = new InstanceAdapter(this, instanceList);
        binding.recyclerViewInstances.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewInstances.setAdapter(instanceAdapter);

        // Setup edit button
        binding.buttonEditCourse.setOnClickListener(v -> editCourse());

        // Setup delete button
        binding.buttonDeleteCourse.setOnClickListener(v -> confirmDeleteCourse());

        // Setup add instance button
        binding.fabAddInstance.setOnClickListener(v -> {
            // TODO: Implement add instance functionality
            Toast.makeText(this, "Add instance functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        // Update UI with course details
        updateCourseDetails();
        loadInstances();
    }

    private void updateCourseDetails() {
        binding.textViewCourseName.setText(course.getName());
        binding.textViewCourseDescription.setText(course.getDescription());
        binding.textViewCourseDuration.setText(course.getDuration() + " minutes");
        binding.textViewCourseLevel.setText(course.getLevel());
        
        // Set new fields
        binding.textViewCourseType.setText(course.getType() != null ? course.getType() : "N/A");
        binding.textViewCourseCapacity.setText(course.getCapacity() > 0 ? String.valueOf(course.getCapacity()) : "N/A");
        binding.textViewCourseDayOfWeek.setText(course.getDayOfWeek() != null ? course.getDayOfWeek() : "N/A");
        binding.textViewCourseTime.setText(course.getTime() != null ? course.getTime() : "N/A");
        binding.textViewCoursePrice.setText(course.getPrice() > 0 ? String.valueOf(course.getPrice()) : "N/A");
        binding.textViewCourseEquipment.setText(course.getEquipmentNeeded() != null ? course.getEquipmentNeeded() : "None");
        
        // Load course image if available
        if (!TextUtils.isEmpty(course.getPhotoPath())) {
            binding.imageViewCourse.setVisibility(View.VISIBLE);
            Glide.with(this)
                .load(course.getPhotoPath())
                .placeholder(R.drawable.ic_launcher_background)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(binding.imageViewCourse);
        } else {
            binding.imageViewCourse.setVisibility(View.GONE);
        }
    }

    private void loadInstances() {
        try {
            // Show loading state
            binding.progressBarInstances.setVisibility(View.VISIBLE);
            
            // Clear current instances
            instanceList.clear();
            
            // Load instances from Firebase
            firebaseService.syncInstancesFromFirebase(course.getId(), new FirebaseService.OnSyncInstancesListener() {
                @Override
                public void onSyncComplete(List<Instance> instances) {
                    runOnUiThread(() -> {
                        binding.progressBarInstances.setVisibility(View.GONE);
                        
                        // Update instance list
                        if (instances != null && !instances.isEmpty()) {
                            instanceList.addAll(instances);
                            Toast.makeText(CourseDetailActivity.this, 
                                instances.size() + " instances loaded", Toast.LENGTH_SHORT).show();
                        }
                        
                        instanceAdapter.notifyDataSetChanged();
                        
                        // Show empty view if no instances
                        if (instanceList.isEmpty()) {
                            binding.textViewNoInstances.setVisibility(View.VISIBLE);
                        } else {
                            binding.textViewNoInstances.setVisibility(View.GONE);
                        }
                    });
                }
                
                @Override
                public void onSyncFailed(String errorMessage) {
                    runOnUiThread(() -> {
                        binding.progressBarInstances.setVisibility(View.GONE);
                        Toast.makeText(CourseDetailActivity.this, 
                            "Error loading instances: " + errorMessage, Toast.LENGTH_SHORT).show();
                        binding.textViewNoInstances.setVisibility(View.VISIBLE);
                        binding.textViewNoInstances.setText("Error loading instances");
                    });
                }
            });
        } catch (Exception e) {
            binding.progressBarInstances.setVisibility(View.GONE);
            Toast.makeText(this, "Error loading instances: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void editCourse() {
        Intent intent = new Intent(this, CourseFormActivity.class);
        intent.putExtra(CourseFormActivity.EXTRA_IS_EDIT_MODE, true);
        intent.putExtra(CourseFormActivity.EXTRA_COURSE, course);
        editCourseLauncher.launch(intent);
    }

    private void confirmDeleteCourse() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete " + course.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCourse();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCourse() {
        if (course == null) {
            Toast.makeText(this, "Error: Course not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        firebaseService.deleteCourseFromFirebase(course.getId(), new FirebaseService.OnSyncListener() {
            @Override
            public void onSyncComplete(String message) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(CourseDetailActivity.this, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onSyncFailed(String errorMessage) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(CourseDetailActivity.this, "Error deleting course: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
