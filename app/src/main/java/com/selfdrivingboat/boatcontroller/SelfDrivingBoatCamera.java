package com.selfdrivingboat.boatcontroller;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Date;

public class SelfDrivingBoatCamera {


    public CameraDevice mCameraDevice;
    private MainActivity mainActivity;
    private Context context;
    List<Surface> outputSurfaces;
    CameraManager manager;
    CameraCharacteristics characteristics;
    CaptureRequest.Builder captureBuilder;
    private Size[] jpegSizes;
    int width = 640;
    int height = 480;
    ImageReader reader;
    Handler backgroudHandler;
    File file;

    static int MEDIA_TYPE_IMAGE = 1;
    static int MEDIA_TYPE_VIDEO = 2;
    private UploadTask uploadTask;

    public SelfDrivingBoatCamera(MainActivity activity) throws CameraAccessException {
        mainActivity = activity;
        context = mainActivity.getApplicationContext();
        manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        String[] cameraIds = manager.getCameraIdList();
        String cameraId = cameraIds[0];

        try {
            characteristics = manager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

        if (jpegSizes != null && 0 < jpegSizes.length) {
            width = jpegSizes[0].getWidth();
            height = jpegSizes[0].getHeight();
        }
        reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        outputSurfaces = new ArrayList<Surface>(2);
        outputSurfaces.add(reader.getSurface());

        HandlerThread thread = new HandlerThread("CameraPicture");
        thread.start();
        backgroudHandler = new Handler(thread.getLooper());
        reader.setOnImageAvailableListener(readerListener, backgroudHandler);

        openCamera();

    }

    //setup for actually taking the picture.
    // alex: this is the actual code that saves the image to file
    ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

            Image image = null;
            try {
                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                save(bytes);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }

        private void save(byte[] bytes) throws IOException {
            OutputStream output = null;
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            try {
                output = new FileOutputStream(file);
                output.write(bytes);
                StorageReference reference = FirebaseStorage.getInstance().getReference().child(timeStamp);
                UploadTask uploadTask = reference.putBytes(bytes);
            } finally {
                if (null != output) {
                    output.close();
                }
            }
        }
    };


    /*
     This is the callback necessary for the manager.openCamera Call back needed above.
    */
    // the callback says "when the camera is opened" give me the camera and put it on mCameraDevice
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            //setup the capture of the current surface.
            //is the setup to take the picture, now the mCameraDevice is initialized.
            //configure the catureBuilder, which is built in listener later on.
            try {
                captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        }


        @Override
        public void onDisconnected(CameraDevice camera) {
        }

        @Override
        public void onError(CameraDevice camera, int error) {
        }

    };

    // alex: openCamera basically set up the camera with the callback
    private void openCamera() {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mainActivity.logger.i("openCamera() asking permissions");
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.CAMERA}, 1231);
        }
        try {
            String cameraId = manager.getCameraIdList()[0];

            // setup the camera perview.  should wrap this in a checkpermissions, which studio is bitching about
            // except it has been done before this fragment is called.
            manager.openCamera(cameraId, mStateCallback, null);

        } catch (CameraAccessException e) {
            mainActivity.logger.e("openCamera() failed: CameraAccessException");
            e.printStackTrace();
        }
    }

    CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {

            super.onCaptureCompleted(session, request, result);
            //new tell the system that the file exists , so it will show up in gallery/photos/whatever
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.MediaColumns.DATA, file.toString());
            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // alex: here we saved the image
        }

    };

    CameraCaptureSession.StateCallback mCaptureStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {

            try {
                session.capture(captureBuilder.build(), captureListener, backgroudHandler);
            } catch (CameraAccessException e) {

                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };


    // take a picture from mCameraDevice, this must be called only after the onOpened function
    // from the callback run otherwise mCameraDevice is gonna be empty
    public void takePicture(){
        file = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if( mCameraDevice == null) {
            mainActivity.logger.e("takePicture() failed: mCameraDevice is null");
            return;
        }
        try {
            mCameraDevice.createCaptureSession(outputSurfaces, mCaptureStateCallback, backgroudHandler);
            mainActivity.logger.i("takePicture() success");
        } catch (CameraAccessException e) {
            mainActivity.logger.e("takePicture() failed: CameraAccessException");
            e.printStackTrace();
        }
    }

    /**
     * Create a File for saving an image or video
     */
    public File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        //creates a directory in pictures.
        //File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");

        File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

}