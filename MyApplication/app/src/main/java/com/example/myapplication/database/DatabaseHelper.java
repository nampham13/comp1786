package com.example.myapplication.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.myapplication.model.Course;
import com.example.myapplication.model.Instance;
import com.example.myapplication.model.User;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    
    // Database Info
    private static final String DATABASE_NAME = "YogaAdminDatabase";
    private static final int DATABASE_VERSION = 4; // Increased version to trigger schema update with user management fields
    
    // Table Names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_COURSES = "courses";
    private static final String TABLE_INSTANCES = "instances";
    
    // User Table Columns
    private static final String KEY_USER_ID = "id";
    private static final String KEY_USER_EMAIL = "email";
    private static final String KEY_USER_PASSWORD = "password"; // In a real app, this would be hashed
    private static final String KEY_USER_NAME = "name";
    private static final String KEY_USER_ROLE = "role";
    private static final String KEY_USER_STATUS = "status";
    private static final String KEY_USER_CREATED_BY = "created_by";
    
    // Course Table Columns
    private static final String KEY_COURSE_ID = "id";
    private static final String KEY_COURSE_NAME = "name";
    private static final String KEY_COURSE_DESCRIPTION = "description";
    private static final String KEY_COURSE_DURATION = "duration";
    private static final String KEY_COURSE_LEVEL = "level";
    private static final String KEY_COURSE_PHOTO_PATH = "photo_path";
    // New columns
    private static final String KEY_COURSE_CAPACITY = "capacity";
    private static final String KEY_COURSE_DAY_OF_WEEK = "day_of_week";
    private static final String KEY_COURSE_TYPE = "type";
    private static final String KEY_COURSE_PRICE = "price";
    private static final String KEY_COURSE_TIME = "time";
    private static final String KEY_COURSE_EQUIPMENT_NEEDED = "equipment_needed";
    private static final String KEY_COURSE_ENROLLED = "enrolled";
    
    // Instance Table Columns
    private static final String KEY_INSTANCE_ID = "id";
    private static final String KEY_INSTANCE_COURSE_ID = "course_id";
    private static final String KEY_INSTANCE_DATE = "date";
    private static final String KEY_INSTANCE_TIME = "time";
    private static final String KEY_INSTANCE_LOCATION = "location";
    private static final String KEY_INSTANCE_CAPACITY = "capacity";
    private static final String KEY_INSTANCE_INSTRUCTOR = "instructor";
    private static final String KEY_INSTANCE_PRICE = "price";
    
    private static DatabaseHelper sInstance;
    
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }
    
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS +
                "(" +
                KEY_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_USER_EMAIL + " TEXT UNIQUE NOT NULL," +
                KEY_USER_PASSWORD + " TEXT NOT NULL," +
                KEY_USER_NAME + " TEXT," +
                KEY_USER_ROLE + " TEXT," +
                KEY_USER_STATUS + " TEXT DEFAULT '" + User.STATUS_ACTIVE + "'," +
                KEY_USER_CREATED_BY + " INTEGER DEFAULT 0" +
                ")";
                
        String CREATE_COURSES_TABLE = "CREATE TABLE " + TABLE_COURSES +
                "(" +
                KEY_COURSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_COURSE_NAME + " TEXT NOT NULL," +
                KEY_COURSE_DESCRIPTION + " TEXT," +
                KEY_COURSE_DURATION + " INTEGER," +
                KEY_COURSE_LEVEL + " TEXT," +
                KEY_COURSE_PHOTO_PATH + " TEXT," +
                // New columns
                KEY_COURSE_CAPACITY + " INTEGER," +
                KEY_COURSE_DAY_OF_WEEK + " TEXT," +
                KEY_COURSE_TYPE + " TEXT," +
                KEY_COURSE_PRICE + " REAL," +
                KEY_COURSE_TIME + " TEXT," +
                KEY_COURSE_EQUIPMENT_NEEDED + " TEXT," +
                KEY_COURSE_ENROLLED + " INTEGER DEFAULT 0" +
                ")";
                
        String CREATE_INSTANCES_TABLE = "CREATE TABLE " + TABLE_INSTANCES +
                "(" +
                KEY_INSTANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_INSTANCE_COURSE_ID + " INTEGER NOT NULL," +
                KEY_INSTANCE_DATE + " TEXT NOT NULL," +
                KEY_INSTANCE_TIME + " TEXT NOT NULL," +
                KEY_INSTANCE_LOCATION + " TEXT," +
                KEY_INSTANCE_CAPACITY + " INTEGER NOT NULL," +
                KEY_INSTANCE_INSTRUCTOR + " TEXT," +
                KEY_INSTANCE_PRICE + " REAL," +
                "FOREIGN KEY (" + KEY_INSTANCE_COURSE_ID + ") REFERENCES " + TABLE_COURSES + "(" + KEY_COURSE_ID + ") ON DELETE CASCADE" +
                ")";
                
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_COURSES_TABLE);
        db.execSQL(CREATE_INSTANCES_TABLE);
        
        // Add a default super admin user
        ContentValues values = new ContentValues();
        values.put(KEY_USER_EMAIL, "admin@yoga.com");
        values.put(KEY_USER_PASSWORD, "admin123"); // In a real app, this would be hashed
        values.put(KEY_USER_NAME, "Super Admin");
        values.put(KEY_USER_ROLE, User.ROLE_SUPER_ADMIN);
        values.put(KEY_USER_STATUS, User.STATUS_ACTIVE);
        values.put(KEY_USER_CREATED_BY, 0); // Created by system
        db.insert(TABLE_USERS, null, values);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_INSTANCES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }
    
    // User CRUD Operations
    
    public long addUser(User user) {
        SQLiteDatabase db = getWritableDatabase();
        long userId = -1;
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_USER_EMAIL, user.getEmail());
            values.put(KEY_USER_PASSWORD, user.getPassword());
            values.put(KEY_USER_NAME, user.getName());
            values.put(KEY_USER_ROLE, user.getRole());
            values.put(KEY_USER_STATUS, user.getStatus() != null ? user.getStatus() : User.STATUS_ACTIVE);
            
            userId = db.insertOrThrow(TABLE_USERS, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add user to database");
        } finally {
            db.endTransaction();
        }
        
        return userId;
    }
    
    public User authenticateUser(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        User user = null;
        
        String USERS_SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?",
                TABLE_USERS, KEY_USER_EMAIL, KEY_USER_PASSWORD);
                
        Cursor cursor = db.rawQuery(USERS_SELECT_QUERY, new String[]{email, password});
        try {
            if (cursor.moveToFirst()) {
                user = new User();
                user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)));
                user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)));
                user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ROLE)));
                
                // Get new fields
                try {
                    user.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_STATUS)));
                } catch (Exception e) {
                    // Handle case where columns might not exist in older database versions
                    user.setStatus(User.STATUS_ACTIVE);
                }
                
                // Check if user is banned
                if (user.isBanned()) {
                    // Return null if user is banned
                    return null;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to authenticate user");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return user;
    }
    
    public User getUserById(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        User user = null;
        
        String USERS_SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ?",
                TABLE_USERS, KEY_USER_ID);
                
        Cursor cursor = db.rawQuery(USERS_SELECT_QUERY, new String[]{String.valueOf(userId)});
        try {
            if (cursor.moveToFirst()) {
                user = new User();
                user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)));
                user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)));
                user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ROLE)));
                
                // Get new fields
                try {
                    user.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_STATUS)));
                } catch (Exception e) {
                    // Handle case where columns might not exist in older database versions
                    user.setStatus(User.STATUS_ACTIVE);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get user by ID");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return user;
    }
    
    public User getUserByEmail(String email) {
        SQLiteDatabase db = getReadableDatabase();
        User user = null;
        
        String USERS_SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ?",
                TABLE_USERS, KEY_USER_EMAIL);
                
        Cursor cursor = db.rawQuery(USERS_SELECT_QUERY, new String[]{email});
        try {
            if (cursor.moveToFirst()) {
                user = new User();
                user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)));
                user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)));
                user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ROLE)));
                
                // Get new fields
                try {
                    user.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_STATUS)));
                } catch (Exception e) {
                    // Handle case where columns might not exist in older database versions
                    user.setStatus(User.STATUS_ACTIVE);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get user by email");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return user;
    }
    
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        
        String USERS_SELECT_QUERY = String.format("SELECT * FROM %s ORDER BY %s",
                TABLE_USERS, KEY_USER_ID);
        
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(USERS_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    User user = new User();
                    user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)));
                    user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)));
                    user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD)));
                    user.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)));
                    user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ROLE)));
                    
                    // Get new fields
                    try {
                        user.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_STATUS)));
                    } catch (Exception e) {
                        // Handle case where columns might not exist in older database versions
                        user.setStatus(User.STATUS_ACTIVE);
                    }
                    
                    users.add(user);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get all users");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return users;
    }
    
    public List<User> getUsersByRole(String role) {
        List<User> users = new ArrayList<>();
        
        String USERS_SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ? ORDER BY %s",
                TABLE_USERS, KEY_USER_ROLE, KEY_USER_ID);
        
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(USERS_SELECT_QUERY, new String[]{role});
        try {
            if (cursor.moveToFirst()) {
                do {
                    User user = new User();
                    user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)));
                    user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)));
                    user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD)));
                    user.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)));
                    user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ROLE)));
                    
                    // Get new fields
                    try {
                        user.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_STATUS)));
                    } catch (Exception e) {
                        // Handle case where columns might not exist in older database versions
                        user.setStatus(User.STATUS_ACTIVE);
                    }
                    
                    users.add(user);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get users by role");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return users;
    }
    
    public int updateUserStatus(long userId, String status) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsAffected = 0;
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_USER_STATUS, status);
            
            rowsAffected = db.update(TABLE_USERS, values, KEY_USER_ID + " = ?", 
                    new String[]{String.valueOf(userId)});
            
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to update user status");
        } finally {
            db.endTransaction();
        }
        
        return rowsAffected;
    }
    
    public int updateUserRole(long userId, String role) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsAffected = 0;
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_USER_ROLE, role);
            
            rowsAffected = db.update(TABLE_USERS, values, KEY_USER_ID + " = ?", 
                    new String[]{String.valueOf(userId)});
            
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to update user role");
        } finally {
            db.endTransaction();
        }
        
        return rowsAffected;
    }
    
    // Course CRUD Operations
    
    public long addCourse(Course course) {
        SQLiteDatabase db = getWritableDatabase();
        long courseId = -1;
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_COURSE_NAME, course.getName());
            values.put(KEY_COURSE_DESCRIPTION, course.getDescription());
            values.put(KEY_COURSE_DURATION, course.getDuration());
            values.put(KEY_COURSE_LEVEL, course.getLevel());
            values.put(KEY_COURSE_PHOTO_PATH, course.getPhotoPath());
            // Add new fields
            values.put(KEY_COURSE_CAPACITY, course.getCapacity());
            values.put(KEY_COURSE_DAY_OF_WEEK, course.getDayOfWeek());
            values.put(KEY_COURSE_TYPE, course.getType());
            values.put(KEY_COURSE_PRICE, course.getPrice());
            values.put(KEY_COURSE_TIME, course.getTime());
            values.put(KEY_COURSE_EQUIPMENT_NEEDED, course.getEquipmentNeeded());
            values.put(KEY_COURSE_ENROLLED, course.getEnrolled());
            
            courseId = db.insertOrThrow(TABLE_COURSES, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add course to database");
        } finally {
            db.endTransaction();
        }
        
        return courseId;
    }
    
    public int updateCourse(Course course) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsAffected = 0;
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_COURSE_NAME, course.getName());
            values.put(KEY_COURSE_DESCRIPTION, course.getDescription());
            values.put(KEY_COURSE_DURATION, course.getDuration());
            values.put(KEY_COURSE_LEVEL, course.getLevel());
            values.put(KEY_COURSE_PHOTO_PATH, course.getPhotoPath());
            // Add new fields
            values.put(KEY_COURSE_CAPACITY, course.getCapacity());
            values.put(KEY_COURSE_DAY_OF_WEEK, course.getDayOfWeek());
            values.put(KEY_COURSE_TYPE, course.getType());
            values.put(KEY_COURSE_PRICE, course.getPrice());
            values.put(KEY_COURSE_TIME, course.getTime());
            values.put(KEY_COURSE_EQUIPMENT_NEEDED, course.getEquipmentNeeded());
            values.put(KEY_COURSE_ENROLLED, course.getEnrolled());
            
            rowsAffected = db.update(TABLE_COURSES, values, KEY_COURSE_ID + " = ?", 
                    new String[]{String.valueOf(course.getId())});
            
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to update course");
        } finally {
            db.endTransaction();
        }
        
        return rowsAffected;
    }
    
    public void deleteCourse(long courseId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_COURSES, KEY_COURSE_ID + " = ?", 
                    new String[]{String.valueOf(courseId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete course");
        } finally {
            db.endTransaction();
        }
    }
    
    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        
        String COURSES_SELECT_QUERY = String.format("SELECT * FROM %s", TABLE_COURSES);
        
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(COURSES_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    Course course = new Course();
                    course.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COURSE_ID)));
                    course.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_NAME)));
                    course.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_DESCRIPTION)));
                    course.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COURSE_DURATION)));
                    course.setLevel(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_LEVEL)));
                    course.setPhotoPath(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_PHOTO_PATH)));
                    
                    // Get new fields
                    try {
                        course.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COURSE_CAPACITY)));
                        course.setDayOfWeek(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_DAY_OF_WEEK)));
                        course.setType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_TYPE)));
                        course.setPrice(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_COURSE_PRICE)));
                        course.setTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_TIME)));
                        course.setEquipmentNeeded(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_EQUIPMENT_NEEDED)));
                        course.setEnrolled(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COURSE_ENROLLED)));
                    } catch (Exception e) {
                        // Handle case where columns might not exist in older database versions
                        Log.d(TAG, "Some new columns might not exist: " + e.getMessage());
                    }
                    
                    courses.add(course);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get courses from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return courses;
    }
    
    public Course getCourseById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Course course = null;
        
        String COURSE_SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ?",
                TABLE_COURSES, KEY_COURSE_ID);
                
        Cursor cursor = db.rawQuery(COURSE_SELECT_QUERY, new String[]{String.valueOf(id)});
        try {
            if (cursor.moveToFirst()) {
                course = new Course();
                course.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COURSE_ID)));
                course.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_NAME)));
                course.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_DESCRIPTION)));
                course.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COURSE_DURATION)));
                course.setLevel(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_LEVEL)));
                course.setPhotoPath(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_PHOTO_PATH)));
                
                // Get new fields
                try {
                    course.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COURSE_CAPACITY)));
                    course.setDayOfWeek(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_DAY_OF_WEEK)));
                    course.setType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_TYPE)));
                    course.setPrice(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_COURSE_PRICE)));
                    course.setTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_TIME)));
                    course.setEquipmentNeeded(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE_EQUIPMENT_NEEDED)));
                    course.setEnrolled(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COURSE_ENROLLED)));
                } catch (Exception e) {
                    // Handle case where columns might not exist in older database versions
                    Log.d(TAG, "Some new columns might not exist: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get course by id");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return course;
    }
    
    // Instance CRUD Operations
    
    public long addInstance(Instance instance) {
        SQLiteDatabase db = getWritableDatabase();
        long instanceId = -1;
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_INSTANCE_COURSE_ID, instance.getCourseId());
            values.put(KEY_INSTANCE_DATE, instance.getDate());
            values.put(KEY_INSTANCE_TIME, instance.getTime());
            values.put(KEY_INSTANCE_LOCATION, instance.getLocation());
            values.put(KEY_INSTANCE_CAPACITY, instance.getCapacity());
            values.put(KEY_INSTANCE_INSTRUCTOR, instance.getInstructor());
            values.put(KEY_INSTANCE_PRICE, instance.getPrice());
            
            instanceId = db.insertOrThrow(TABLE_INSTANCES, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add instance to database");
        } finally {
            db.endTransaction();
        }
        
        return instanceId;
    }
    
    public int updateInstance(Instance instance) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsAffected = 0;
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_INSTANCE_COURSE_ID, instance.getCourseId());
            values.put(KEY_INSTANCE_DATE, instance.getDate());
            values.put(KEY_INSTANCE_TIME, instance.getTime());
            values.put(KEY_INSTANCE_LOCATION, instance.getLocation());
            values.put(KEY_INSTANCE_CAPACITY, instance.getCapacity());
            values.put(KEY_INSTANCE_INSTRUCTOR, instance.getInstructor());
            values.put(KEY_INSTANCE_PRICE, instance.getPrice());
            
            rowsAffected = db.update(TABLE_INSTANCES, values, KEY_INSTANCE_ID + " = ?", 
                    new String[]{String.valueOf(instance.getId())});
            
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to update instance");
        } finally {
            db.endTransaction();
        }
        
        return rowsAffected;
    }
    
    public void deleteInstance(long instanceId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_INSTANCES, KEY_INSTANCE_ID + " = ?", 
                    new String[]{String.valueOf(instanceId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete instance");
        } finally {
            db.endTransaction();
        }
    }
    
    public List<Instance> getInstancesByCourseId(long courseId) {
        List<Instance> instances = new ArrayList<>();
        
        String INSTANCES_SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ?",
                TABLE_INSTANCES, KEY_INSTANCE_COURSE_ID);
                
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(INSTANCES_SELECT_QUERY, new String[]{String.valueOf(courseId)});
        try {
            if (cursor.moveToFirst()) {
                do {
                    Instance instance = new Instance();
                    instance.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_INSTANCE_ID)));
                    instance.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_INSTANCE_COURSE_ID)));
                    instance.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_DATE)));
                    instance.setTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_TIME)));
                    instance.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_LOCATION)));
                    instance.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_INSTANCE_CAPACITY)));
                    instance.setInstructor(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_INSTRUCTOR)));
                    instance.setPrice(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_INSTANCE_PRICE)));
                    
                    instances.add(instance);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get instances by course id");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return instances;
    }
    
    public Instance getInstanceById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Instance instance = null;
        
        String INSTANCE_SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ?",
                TABLE_INSTANCES, KEY_INSTANCE_ID);
                
        Cursor cursor = db.rawQuery(INSTANCE_SELECT_QUERY, new String[]{String.valueOf(id)});
        try {
            if (cursor.moveToFirst()) {
                instance = new Instance();
                instance.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_INSTANCE_ID)));
                instance.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_INSTANCE_COURSE_ID)));
                instance.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_DATE)));
                instance.setTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_TIME)));
                instance.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_LOCATION)));
                instance.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_INSTANCE_CAPACITY)));
                instance.setInstructor(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_INSTRUCTOR)));
                instance.setPrice(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_INSTANCE_PRICE)));
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get instance by id");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return instance;
    }
    
    public List<Instance> getAllInstances() {
        List<Instance> instances = new ArrayList<>();
        
        String INSTANCES_SELECT_QUERY = String.format("SELECT * FROM %s", TABLE_INSTANCES);
        
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(INSTANCES_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    Instance instance = new Instance();
                    instance.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_INSTANCE_ID)));
                    instance.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_INSTANCE_COURSE_ID)));
                    instance.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_DATE)));
                    instance.setTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_TIME)));
                    instance.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_LOCATION)));
                    instance.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_INSTANCE_CAPACITY)));
                    instance.setInstructor(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTANCE_INSTRUCTOR)));
                    instance.setPrice(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_INSTANCE_PRICE)));
                    
                    instances.add(instance);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get all instances");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        
        return instances;
    }
    
    // User management methods
    public boolean updateUser(User user) {
        SQLiteDatabase db = getWritableDatabase();
        boolean success = false;
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_USER_EMAIL, user.getEmail());
            values.put(KEY_USER_PASSWORD, user.getPassword());
            values.put(KEY_USER_NAME, user.getName());
            values.put(KEY_USER_ROLE, user.getRole());
            values.put(KEY_USER_STATUS, user.getStatus());
            int rows = db.update(TABLE_USERS, values, KEY_USER_ID + " = ?", 
                    new String[]{String.valueOf(user.getId())});
            
            success = rows > 0;
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to update user");
        } finally {
            db.endTransaction();
        }
        
        return success;
    }
    
    public boolean deleteUser(long userId) {
        SQLiteDatabase db = getWritableDatabase();
        boolean success = false;
        
        db.beginTransaction();
        try {
            int rows = db.delete(TABLE_USERS, KEY_USER_ID + " = ?", 
                    new String[]{String.valueOf(userId)});
            
            success = rows > 0;
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete user");
        } finally {
            db.endTransaction();
        }
        
        return success;
    }
}