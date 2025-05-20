package com.example.myapplication.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityCourseListBinding;
import com.example.myapplication.firebase.FirebaseService;
import com.example.myapplication.model.Course;
import com.example.myapplication.ui.adapter.CourseAdapter;
import com.example.myapplication.util.SampleDataUtil;

import java.util.ArrayList;
import java.util.List;

public class CourseListActivity extends AppCompatActivity implements CourseAdapter.OnCourseClickListener {

    private static final int REQUEST_CODE_ADD_COURSE = 100;
    private static final int REQUEST_CODE_EDIT_COURSE = 101;

    private ActivityCourseListBinding binding;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;
    private FirebaseService firebaseService;
    
    private final ActivityResultLauncher<Intent> addCourseLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Course newCourse = (Course) result.getData().getSerializableExtra(CourseFormActivity.EXTRA_COURSE);
                    if (newCourse != null) {
                        // Add the new course to the list and refresh
                        courseList.add(newCourse);
                        courseAdapter.notifyItemInserted(courseList.size() - 1);
                        updateEmptyView();
                    }
                }
            });
            
    private final ActivityResultLauncher<Intent> editCourseLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Course updatedCourse = (Course) result.getData().getSerializableExtra(CourseFormActivity.EXTRA_COURSE);
                    if (updatedCourse != null) {
                        // Find and update the course in the list
                        for (int i = 0; i < courseList.size(); i++) {
                            if (courseList.get(i).getId() == updatedCourse.getId()) {
                                courseList.set(i, updatedCourse);
                                courseAdapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Yoga Courses");
        }

        // Initialize Firebase service
        firebaseService = FirebaseService.getInstance();
        
        // Initialize RecyclerView
        courseList = new ArrayList<>();
        courseAdapter = new CourseAdapter(this, courseList);
        courseAdapter.setOnCourseClickListener(this);
        binding.recyclerViewCourses.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewCourses.setAdapter(courseAdapter);

        // Setup SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadCourses);

        // Setup FAB
        binding.fabAddCourse.setOnClickListener(v -> {
            Intent intent = new Intent(this, CourseFormActivity.class);
            intent.putExtra(CourseFormActivity.EXTRA_IS_EDIT_MODE, false);
            addCourseLauncher.launch(intent);
        });
        


        // Load courses
        loadCourses();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload courses when activity is resumed
        // This ensures that when returning from SyncActivity, the data is refreshed
        loadCourses();
    }

    private void loadCourses() {
        binding.swipeRefreshLayout.setRefreshing(true);
        
        // Load courses from Firebase
        firebaseService.syncCoursesFromFirebase(new FirebaseService.OnCourseSyncListener() {
            @Override
            public void onSyncComplete(List<Course> courses, String message) {
                runOnUiThread(() -> {
                    try {
                        // Update course list
                        courseList.clear();
                        if (courses != null && !courses.isEmpty()) {
                            courseList.addAll(courses);
                            Toast.makeText(CourseListActivity.this, 
                                "Loaded " + courses.size() + " courses", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CourseListActivity.this, 
                                "No courses found in Firebase", Toast.LENGTH_SHORT).show();
                        }
                        
                        // Update UI
                        courseAdapter.notifyDataSetChanged();
                        updateEmptyView();
                    } finally {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onSyncFailed(String errorMessage) {
                runOnUiThread(() -> {
                    // Handle any errors
                    Toast.makeText(CourseListActivity.this, 
                        "Error loading courses: " + errorMessage, Toast.LENGTH_SHORT).show();
                    binding.textViewEmpty.setVisibility(View.VISIBLE);
                    binding.textViewEmpty.setText("Error loading courses");
                    binding.swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }
    
    private void updateEmptyView() {
        if (courseList.isEmpty()) {
            binding.textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.textViewEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCourseClick(Course course) {
        // Show options dialog
        String[] options = {"View Details", "Edit Course", "Delete Course"};
        
        new AlertDialog.Builder(this)
                .setTitle(course.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // View Details
                            viewCourseDetails(course);
                            break;
                        case 1: // Edit Course
                            editCourse(course);
                            break;
                        case 2: // Delete Course
                            confirmDeleteCourse(course);
                            break;
                    }
                })
                .show();
    }
    
    private void viewCourseDetails(Course course) {
        Intent intent = new Intent(this, CourseDetailActivity.class);
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.getId());
        startActivity(intent);
    }
    
    private void editCourse(Course course) {
        Intent intent = new Intent(this, CourseFormActivity.class);
        intent.putExtra(CourseFormActivity.EXTRA_IS_EDIT_MODE, true);
        intent.putExtra(CourseFormActivity.EXTRA_COURSE, course);
        editCourseLauncher.launch(intent);
    }
    
    private void confirmDeleteCourse(Course course) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete " + course.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCourse(course);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteCourse(Course course) {
        if (course == null) {
            Toast.makeText(this, "Error: Course not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.swipeRefreshLayout.setRefreshing(true);
        firebaseService.deleteCourseFromFirebase(course.getId(), new FirebaseService.OnSyncListener() {
            @Override
            public void onSyncComplete(String message) {
                runOnUiThread(() -> {
                    // Remove from UI list
                    int position = -1;
                    for (int i = 0; i < courseList.size(); i++) {
                        if (courseList.get(i).getId() == course.getId()) {
                            position = i;
                            break;
                        }
                    }
                    if (position != -1) {
                        courseList.remove(position);
                        courseAdapter.notifyItemRemoved(position);
                        updateEmptyView();
                    }
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(CourseListActivity.this, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onSyncFailed(String errorMessage) {
                runOnUiThread(() -> {
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(CourseListActivity.this, "Error deleting course: " + errorMessage, Toast.LENGTH_SHORT).show();
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

