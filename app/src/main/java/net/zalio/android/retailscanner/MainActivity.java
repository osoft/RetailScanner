package net.zalio.android.retailscanner;

import android.app.Activity;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    final static String TARGET_BASE_PATH = "/sdcard/RetailScanner/";
    private static final String TAG = MainActivity.class.getSimpleName();
    View.OnClickListener mOnShutterClickedListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            mCamera.takePicture(
                    null,
                    null,
                    new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            mCamera.startPreview();
                            new ASyncTask_OCR().execute(data);
                            //File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                            //if (pictureFile == null) {
                            //    Log.d(TAG, "Error creating media file, check storage permissions: ");
                            //    return;
                            //}
                            //try {
                            //    FileOutputStream fos = new FileOutputStream(pictureFile);
                            //    fos.write(data);
                            //    fos.close();
                            //
                            //
                            //    Log.i(TAG, "START OCR");
                            //    mTess.setImage(pictureFile);
                            //    Log.i(TAG, "text:" + mTess.getUTF8Text());
                            //
                            //    Toast.makeText(MainActivity.this, mTess.getUTF8Text(), Toast.LENGTH_LONG).show();
                            //
                            //} catch (FileNotFoundException e) {
                            //    Log.d(TAG, "File not found: " + e.getMessage());
                            //} catch (IOException e) {
                            //    Log.d(TAG, "Error accessing file: " + e.getMessage());
                            //} catch (NullPointerException npe) {
                            //    Log.d(TAG, "OCR failed!");
                            //}
                        }
                    }
            );


        }
    };
    private Camera mCamera;
    private FrameLayout mFlCameraHolder;
    private CameraSurface mCameraSurface;
    private Button mBtnShutter;
    private List<Camera.Size> supportedSizes;
    private TessBaseAPI mTess;
    private Button mBtnFocus;

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
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

        Log.i(TAG, mediaFile.getAbsolutePath());

        return mediaFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getActionBar().hide();

        setContentView(R.layout.activity_main);

        mFlCameraHolder = (FrameLayout) findViewById(R.id.flCameraHolder);
        mBtnShutter = (Button) findViewById(R.id.btnShutter);
        mBtnFocus = (Button) findViewById(R.id.btnFocus);

        new ASyncTask_LoadTessData().execute();
        mTess = new TessBaseAPI();

        toggleHideyBar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCamera = getCameraInstance();
        if (mCamera == null) {
            finish();
            return;
        }

        initCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, e.getLocalizedMessage());
            Toast.makeText(this, "Failed open camera", Toast.LENGTH_LONG).show();
        }
        return c; // returns null if camera is unavailable
    }

    private void initCamera() {
        // get Camera parameters
        Camera.Parameters params = mCamera.getParameters();

        params.setZoom(2);
        //for (String colorEffect:params.getSupportedColorEffects()) {
        //    if (colorEffect.equals(Camera.Parameters.EFFECT_MONO)) {
        //        params.setColorEffect(Camera.Parameters.EFFECT_MONO);
        //    }
        //}
        //for (String colorEffect:params.getSupportedColorEffects()) {
        //    if (colorEffect.equals(Camera.Parameters.EFFECT_WHITEBOARD)) {
        //        params.setColorEffect(Camera.Parameters.EFFECT_WHITEBOARD);
        //    }
        //}
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        supportedSizes = params.getSupportedPictureSizes();
        int w = 0;
        int h = 0;
        for (Camera.Size s : supportedSizes) {
            Log.i(TAG, "Size supported: " + s.width + " " + s.height);
            w = s.width;
            h = s.height;
            if (s.width > 800 && s.width < 1600) {
                break;
            }
        }
        params.setPictureSize(w, h);

        supportedSizes = params.getSupportedPreviewSizes();
        w = 0;
        h = 0;
        for (Camera.Size s : supportedSizes) {
            Log.i(TAG, "Size supported: " + s.width + " " + s.height);
            w = s.width;
            h = s.height;
            if (s.width > 800 && s.width < 1600) {
                break;
            }
        }
        params.setPreviewSize(w, h);
        // set Camera parameters
        mCamera.setParameters(params);

        mCameraSurface = new CameraSurface(this, mCamera);
        mCameraSurface.setKeepScreenOn(true);
        mFlCameraHolder.addView(mCameraSurface);

        mBtnShutter.setOnClickListener(mOnShutterClickedListener);
        mBtnFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.autoFocus(null);
            }
        });
    }

    private void copyFilesToSdCard() {
        copyFileOrDir(""); // copy all files in assets folder in my project
    }

    private void copyFileOrDir(String path) {
        AssetManager assetManager = this.getAssets();
        String assets[] = null;
        try {
            Log.i("tag", "copyFileOrDir() " + path);
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path);
            } else {
                String fullPath = TARGET_BASE_PATH + path;
                Log.i("tag", "path=" + fullPath);
                File dir = new File(fullPath);
                if (!dir.exists() && !path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                    if (!dir.mkdirs())
                        Log.i("tag", "could not create dir " + fullPath);
                for (int i = 0; i < assets.length; ++i) {
                    String p;
                    if (path.equals(""))
                        p = "";
                    else
                        p = path + "/";

                    if (!path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                        copyFileOrDir(p + assets[i]);
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private void copyFile(String filename) {
        AssetManager assetManager = this.getAssets();

        InputStream in = null;
        OutputStream out = null;
        String newFileName = null;
        try {
            Log.i("tag", "copyFile() " + filename);
            in = assetManager.open(filename);
            if (filename.endsWith(".jpg")) // extension was added to avoid compression on APK file
                newFileName = TARGET_BASE_PATH + filename.substring(0, filename.length() - 4);
            else
                newFileName = TARGET_BASE_PATH + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e("tag", "Exception in copyFile() of " + newFileName);
            Log.e("tag", "Exception in copyFile() " + e.toString());
        }

    }

    class ASyncTask_LoadTessData extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (new File(MainActivity.TARGET_BASE_PATH + "data/").isDirectory()) {
                return null;
            }
            copyFilesToSdCard();
            mTess.init(TARGET_BASE_PATH + "data/", "eng", TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(MainActivity.this, "OCR Ready", Toast.LENGTH_LONG).show();
            mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        }
    }

    class ASyncTask_OCR extends AsyncTask<byte[], Void, ArrayList<String>> {


        @Override
        protected ArrayList<String> doInBackground(byte[]... params) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return null;
            }
            byte[] data = params[0];

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                Log.i(TAG, "START OCR");
                mTess.setImage(pictureFile);
                mTess.setRectangle(0,0,200,200);
                //Log.i(TAG, "text:" + mTess.getUTF8Text());
                //runOnUiThread(new Runnable() {
                //    @Override
                //    public void run() {
                //        Toast.makeText(MainActivity.this, mTess.getUTF8Text(), Toast.LENGTH_LONG).show();
                //    }
                //});
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
                return null;
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
                return null;
            } catch (NullPointerException npe) {
                Log.d(TAG, "OCR failed!");
                return null;
            }

            Log.i(TAG, "text:" + mTess.getUTF8Text());
            Log.i(TAG, "regions: " + mTess.getRegions().getBoxRects().size());
            Log.i(TAG, "Confidence: " + mTess.meanConfidence());
            for (int conf:mTess.wordConfidences()) {
                Log.i(TAG, "Word Confidence: " + conf);
            }

            ArrayList<String> resultList = new ArrayList<String>();

            ResultIterator result = mTess.getResultIterator();
            int level = TessBaseAPI.PageIteratorLevel.RIL_WORD;
            if (result != null) {
                result.begin();
                do {
                    Log.i(TAG, "Confidence: " + result.confidence(level));
                    Log.i(TAG, "Text: " + result.getUTF8Text(level));
                    if (result.confidence(level) > 90 && !result.getUTF8Text(level).trim().isEmpty()) {
                        resultList.add(result.getUTF8Text(level));
                    }
                } while (result.next(level));

            } else {
                Log.e(TAG, "Iterator is null");
            }

            return resultList;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            Log.i(TAG, "vvvvvvvvvv RESULT vvvvvvvvvvv");
            for (String s:strings) {
                Log.i(TAG, "Result String: " + s);
            }
            Log.i(TAG, "^^^^^^^^^^ RESULT ^^^^^^^^^^^");
        }
    }

    /**
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     */
    public void toggleHideyBar() {

        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i(TAG, "Turning immersive mode mode off. ");
        } else {
            Log.i(TAG, "Turning immersive mode mode on.");
        }

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
}
