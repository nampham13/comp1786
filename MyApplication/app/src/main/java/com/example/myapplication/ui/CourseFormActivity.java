package com.example.myapplication.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.databinding.ActivityCourseFormBinding;
import com.example.myapplication.model.Course;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CourseFormActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE = "extra_course";
    public static final String EXTRA_IS_EDIT_MODE = "extra_is_edit_mode";
    
    private ActivityCourseFormBinding binding;
    private DatabaseHelper databaseHelper;
    private boolean isEditMode = false;
    private Course course;
    private Calendar selectedTime = Calendar.getInstance();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);

        // Check if we're in edit mode
        isEditMode = getIntent().getBooleanExtra(EXTRA_IS_EDIT_MODE, false);
        
        if (isEditMode) {
            // Get course from intent
            course = (Course) getIntent().getSerializableExtra(EXTRA_COURSE);
            if (course != null) {
                // Set toolbar title
                getSupportActionBar().setTitle("Edit Course");
                
                // Populate form fields
                binding.editTextCourseName.setText(course.getName());
                binding.editTextCourseDescription.setText(course.getDescription());
                binding.editTextCourseDuration.setText(String.valueOf(course.getDuration()));
                
                // Set level spinner selection
                String level = course.getLevel();
                if (level != null) {
                    String[] levels = getResources().getStringArray(com.example.myapplication.R.array.course_levels);
                    for (int i = 0; i < levels.length; i++) {
                        if (levels[i].equals(level)) {
                            binding.spinnerCourseLevel.setSelection(i);
                            break;
                        }
                    }
                }
                
                // Populate new fields
                // Set course type spinner selection
                String type = course.getType();
                if (type != null) {
                    String[] types = getResources().getStringArray(com.example.myapplication.R.array.course_types);
                    for (int i = 0; i < types.length; i++) {
                        if (types[i].equals(type)) {
                            binding.spinnerCourseType.setSelection(i);
                            break;
                        }
                    }
                }
                
                if (course.getCapacity() > 0) {
                    binding.editTextCourseCapacity.setText(String.valueOf(course.getCapacity()));
                }
                // Set day of week spinner selection
                String dayOfWeek = course.getDayOfWeek();
                if (dayOfWeek != null) {
                    String[] daysOfWeek = getResources().getStringArray(com.example.myapplication.R.array.days_of_week);
                    for (int i = 0; i < daysOfWeek.length; i++) {
                        if (daysOfWeek[i].equals(dayOfWeek)) {
                            binding.spinnerDayOfWeek.setSelection(i);
                            break;
                        }
                    }
                }
                // Set the time
                String timeStr = course.getTime();
                if (timeStr != null && !timeStr.isEmpty()) {
                    binding.textViewSelectedTime.setText(timeStr);
                    
                    // Try to parse the time to set the selectedTime calendar
                    try {
                        String[] timeParts = timeStr.split(":");
                        if (timeParts.length == 2) {
                            int hour = Integer.parseInt(timeParts[0]);
                            int minute = Integer.parseInt(timeParts[1]);
                            selectedTime.set(Calendar.HOUR_OF_DAY, hour);
                            selectedTime.set(Calendar.MINUTE, minute);
                        }
                    } catch (Exception e) {
                        // If parsing fails, keep the default time
                    }
                }
                if (course.getPrice() > 0) {
                    binding.editTextCoursePrice.setText(String.valueOf(course.getPrice()));
                }
                binding.editTextCourseEquipment.setText(course.getEquipmentNeeded());
            } else {
                // Something went wrong, finish activity
                Toast.makeText(this, "Error: Course not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            // Create new course
            getSupportActionBar().setTitle("Add Course");
            course = new Course();
        }

        // Setup time picker button
        binding.buttonSelectTime.setOnClickListener(v -> showTimePickerDialog());
        
        // Setup save button
        binding.buttonSave.setOnClickListener(v -> saveCourse());
    }

    private void saveCourse() {
        // Validate form
        String name = binding.editTextCourseName.getText().toString().trim();
        String description = binding.editTextCourseDescription.getText().toString().trim();
        String durationStr = binding.editTextCourseDuration.getText().toString().trim();
        String level = binding.spinnerCourseLevel.getSelectedItem().toString();
        
        // Get values for new fields
        String type = binding.spinnerCourseType.getSelectedItem().toString();
        String capacityStr = binding.editTextCourseCapacity.getText().toString().trim();
        String dayOfWeek = binding.spinnerDayOfWeek.getSelectedItem().toString();
        String time = binding.textViewSelectedTime.getText().toString().trim();
        String priceStr = binding.editTextCoursePrice.getText().toString().trim();
        String equipmentNeeded = binding.editTextCourseEquipment.getText().toString().trim();

        if (name.isEmpty()) {
            binding.editTextCourseName.setError("Course name is required");
            binding.editTextCourseName.requestFocus();
            return;
        }

        if (durationStr.isEmpty()) {
            binding.editTextCourseDuration.setError("Duration is required");
            binding.editTextCourseDuration.requestFocus();
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(durationStr);
            if (duration <= 0) {
                binding.editTextCourseDuration.setError("Duration must be greater than 0");
                binding.editTextCourseDuration.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            binding.editTextCourseDuration.setError("Invalid duration");
            binding.editTextCourseDuration.requestFocus();
            return;
        }
        
        // Validate capacity
        int capacity = 0;
        if (!capacityStr.isEmpty()) {
            try {
                capacity = Integer.parseInt(capacityStr);
                if (capacity < 0) {
                    binding.editTextCourseCapacity.setError("Capacity must be a positive number");
                    binding.editTextCourseCapacity.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                binding.editTextCourseCapacity.setError("Invalid capacity");
                binding.editTextCourseCapacity.requestFocus();
                return;
            }
        }
        
        // Validate time
        if (time.isEmpty() || time.equals("Select time")) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate price
        float price = 0;
        if (!priceStr.isEmpty()) {
            try {
                price = Float.parseFloat(priceStr);
                if (price < 0) {
                    binding.editTextCoursePrice.setError("Price must be a positive number");
                    binding.editTextCoursePrice.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                binding.editTextCoursePrice.setError("Invalid price");
                binding.editTextCoursePrice.requestFocus();
                return;
            }
        }

        // Update course object
        course.setName(name);
        course.setDescription(description);
        course.setDuration(duration);
        course.setLevel(level);
        
        // Set new fields
        course.setType(type);
        course.setCapacity(capacity);
        course.setDayOfWeek(dayOfWeek);
        course.setTime(time);
        course.setPrice(price);
        course.setEquipmentNeeded(equipmentNeeded);

        // Save to database
        long result;
        if (isEditMode) {
            result = databaseHelper.updateCourse(course);
            if (result > 0) {
                Toast.makeText(this, "Course updated successfully", Toast.LENGTH_SHORT).show();
                // Return updated course to calling activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_COURSE, course);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Failed to update course", Toast.LENGTH_SHORT).show();
            }
        } else {
            result = databaseHelper.addCourse(course);
            if (result != -1) {
                course.setId(result);
                Toast.makeText(this, "Course added successfully", Toast.LENGTH_SHORT).show();
                // Return new course to calling activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_COURSE, course);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Failed to add course", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    // Set the selected time
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);
                    
                    // Update the TextView with the selected time
                    binding.textViewSelectedTime.setText(timeFormat.format(selectedTime.getTime()));
                },
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE),
                true // 24-hour format
        );
        timePickerDialog.show();
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