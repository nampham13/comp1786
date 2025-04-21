package com.example.myapplication.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUtil {
    private static final String TAG = "ImageUtil";
    
    public static File createImageFile(Context context) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }
    
    public static String saveImageToInternalStorage(Context context, Uri imageUri, String fileName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            // Create directory if it doesn't exist
            File directory = new File(context.getFilesDir(), "course_photos");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Create file
            File file = new File(directory, fileName + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            
            // Compress bitmap to jpeg with 85% quality
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }
    
    public static Bitmap loadImageFromStorage(String path) {
        try {
            File file = new File(path);
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            return null;
        }
    }
}