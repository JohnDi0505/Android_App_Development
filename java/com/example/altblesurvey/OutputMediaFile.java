package com.example.altblesurvey;

import android.hardware.Camera;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.ContentValues.TAG;
import static com.example.altblesurvey.MainActivity.MEDIA_TYPE_IMAGE;
import static com.example.altblesurvey.MainActivity.mCode;
import static com.example.altblesurvey.MainActivity.time;

public class OutputMediaFile {

    private static final String OUTPUT_DIR = "/sdcard"+ File.separator + "BLE_data";

    public static Camera.PictureCallback ImageOutputCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                File pictureFile = SaveImage(MEDIA_TYPE_IMAGE);
                if (pictureFile == null) {
                    Log.d(TAG, "Error creating media file, check storage permissions");
                    return;
                }

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                }
            }
        };
        return picture;
    }

    public static File SaveImage(int type) {

//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());

        File mediaStorageDir = new File(OUTPUT_DIR, "Images");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "ALT_IMG_" + mCode + "_" + time + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

}
