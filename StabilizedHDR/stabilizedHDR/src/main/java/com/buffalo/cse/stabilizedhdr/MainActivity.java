package com.buffalo.cse.stabilizedhdr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements OnClickListener,
 SurfaceHolder.Callback, Camera.PictureCallback, Camera.ShutterCallback, SensorEventListener {
	private static final String TAG = "VirtualJax";
	private SensorManager mgr;
	private Sensor accel;
    private Sensor compass;
	private Sensor orient;
    private TextView picture1, picture2,picture3,picture4,picture5;

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private int ready = 0;
	private float[] accelValues = new float[3];
	private float[] compassValues = new float[3];
	private float[] inR = new float[9];
	private float[] inR1 = new float[9];
	private float[] inR2 = new float[9];
	private float[] inclineMatrix = new float[9];
	private float[] orientationValues = new float[3];
	private float[] prefValues1 = new float[3];
	private float[] prefValues2 = new float[3];
	private float[] angleChange = new float[3];
	private float x,y,z, exp3;
    private int w;
    private int h;
	private long time;
	private double mInclination;
	private int counter, recordsensor;
	private int mRotation;
	private int first,exp1,exp2;
	private Uri image1uri,image2uri;
	Bitmap bmp, bmapRotate;

    SurfaceView cameraView;
    SurfaceHolder surfaceHolder;
    Camera camera;
   
    
    Handler timerHandler = new Handler();
    
    Runnable timerTask = new Runnable() {
        public void run() {
        
        
            exp2 =  camera.getParameters().getExposureCompensation();
            camera.takePicture(MainActivity.this,null,null,MainActivity.this);
            //camera.takePicture(MainActivity.this,null,null,MainActivity.this);
            //sleep(1000);
            

        }
    };

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {
	            	Log.i(TAG, "OpenCV loaded successfully");
	               // mOpenCvCameraView.enableView();
	            } break;
	            default:
	            {
	                super.onManagerConnected(status);
	            } break;
	        }
	    }
	};

    public MainActivity() {
        h = 0;
        w = 0;
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        picture1 = (TextView)findViewById(R.id.picture1);
        picture2 = (TextView)findViewById(R.id.picture2);
        picture3= (TextView)findViewById(R.id.picture3);
        picture4 = (TextView)findViewById(R.id.picture4);
        picture5 = (TextView)findViewById(R.id.picture5);
        
        cameraView = (SurfaceView) this.findViewById(R.id.CameraView);

        surfaceHolder = cameraView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);

        mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        accel = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        compass = mgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        orient = mgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        
        cameraView.setFocusable(true);
       cameraView.setFocusableInTouchMode(true);
        cameraView.setClickable(true);

        cameraView.setOnClickListener(this);
    }
    
    
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
  }

  /** Create a File for saving an image or video */
  private static File getOutputMediaFile(int type){
      // To be safe, you should check that the SDCard is mounted
      // using Environment.getExternalStorageState() before doing this.

      File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
      // This location works best if you want the created images to be shared
      // between applications and persist after your app has been uninstalled.

      // Create the storage directory if it does not exist
      if (! mediaStorageDir.exists()){
          if (! mediaStorageDir.mkdirs()){
              Log.d("MyCameraApp", "failed to create directory");
              return null;
          }
      }

      // Create a media file name
      String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      File mediaFile;
      if (type == MEDIA_TYPE_IMAGE){
          mediaFile = new File(mediaStorageDir.getPath() + File.separator +
          "IMG_"+ timeStamp + ".jpg");
      } else if(type == MEDIA_TYPE_VIDEO) {
          mediaFile = new File(mediaStorageDir.getPath() + File.separator +
          "VID_"+ timeStamp + ".mp4");
      } else {
          return null;
      }

      return mediaFile;
  }

    public void onClick(View v) {
        camera.takePicture(this, null, this);
        //timerHandler.postDelayed(timerTask, 1100);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    	mgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        mgr.registerListener(this, compass, SensorManager.SENSOR_DELAY_FASTEST);
        mgr.registerListener(this, orient, SensorManager.SENSOR_DELAY_FASTEST);
    }
    
    @Override
	public void onShutter() {
    	if(first == 0)
    	{
    		String msg3 = String.format(
                    "Pic 1:azimuth (Z): %7.3f pitch (X): %7.3f roll (Y): %7.3f",
              		orientationValues[0],
              		orientationValues[1],
             			orientationValues[2]);
          picture1.setText(msg3);
          
    	}

        if(first == 1)
        {
            String msg3 = String.format(
                    "Pic 2:azimuth (Z): %7.3f pitch (X): %7.3f roll (Y): %7.3f",
                    orientationValues[0],
                    orientationValues[1],
                    orientationValues[2]);
            picture2.setText(msg3);

        }

        if(first == 2)
        {
            String msg3 = String.format(
                    "Pic 3:azimuth (Z): %7.3f pitch (X): %7.3f roll (Y): %7.3f",
                    orientationValues[0],
                    orientationValues[1],
                    orientationValues[2]);
            picture3.setText(msg3);

        }

        if(first == 3)
        {
            String msg3 = String.format(
                    "Pic 4:azimuth (Z): %7.3f pitch (X): %7.3f roll (Y): %7.3f",
                    orientationValues[0],
                    orientationValues[1],
                    orientationValues[2]);
            picture4.setText(msg3);

        }

        if(first == 4)
        {
            String msg3 = String.format(
                    "Pic 5:azimuth (Z): %7.3f pitch (X): %7.3f roll (Y): %7.3f",
                    orientationValues[0],
                    orientationValues[1],
                    orientationValues[2]);
            picture5.setText(msg3);

        }


        first = first + 1;
      
    }

    @Override
    protected void onPause() {
        mgr.unregisterListener(this, accel);
        mgr.unregisterListener(this, compass);
        mgr.unregisterListener(this, orient);
    	super.onPause();
    }
    
    public void onPictureTaken(byte[] data, Camera camera) {
    	  
    	   
    	  try {

              if(first<6) {
                  Uri imageFileUri = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
                  // Uri imageFileUri = getOutputMediaFileUri(1);
                  // File imFile =  getOutputMediaFile(1);
                  BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
                  bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                  bmp = BitmapFactory.decodeByteArray(data, 0, data.length, bmpFactoryOptions);
                  Matrix matrix = new Matrix();
                  matrix.postRotate(90);
                  bmapRotate = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                  //    OutputStream out = new BufferedOutputStream(new FileOutputStream(imFile));
                  OutputStream imageFileOS = getContentResolver().openOutputStream(imageFileUri);
                  imageFileOS.write(data);
                  imageFileOS.flush();
                  imageFileOS.close();
                  Camera.Parameters parameters = camera.getParameters();
                  //parameters.setAutoExposureLock(true);
                  parameters.setPictureSize(w, h);

                  if (first == 1) {
                      parameters.setExposureCompensation(-6);
                  }

                  if (first == 2) {
                      parameters.setExposureCompensation(0);
                  }
                  if (first == 3) {
                      parameters.setExposureCompensation(6);
                  }
                  if (first == 4) {
                      parameters.setExposureCompensation(12);
                  }

                  if(first<5) {

                      camera.setParameters(parameters);


                      timerHandler.postDelayed(timerTask, 500);
                  }
              }
    	        	
    		  
    	  } catch (FileNotFoundException e) {
    	        Toast t = Toast.makeText(this,e.getMessage(), Toast.LENGTH_SHORT);
    	        t.show();
    	  } catch (IOException e) {
    	        Toast t = Toast.makeText(this,e.getMessage(), Toast.LENGTH_SHORT);
    	        t.show();
    	  }
    	  
    	  
    	  camera.startPreview();
    	}
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        camera.startPreview();
  }

    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        recordsensor = 0;
        try {
            camera.setPreviewDisplay(holder);
            Camera.Parameters parameters = camera.getParameters();
          if (this.getResources().getConfiguration().orientation !=
             Configuration.ORIENTATION_LANDSCAPE)
            {
                 parameters.set("orientation", "portrait");
                 // For Android Version 2.2 and above
                 camera.setDisplayOrientation(90);

                 // For Android Version 2.0 and above
               parameters.setRotation(90);
              }

            // Effects are for Android Version 2.0 and higher
            List<String> colorEffects = parameters.getSupportedColorEffects();
            exp1 = parameters.getMinExposureCompensation();
            exp2 = parameters.getMaxExposureCompensation();
            exp3 = parameters.getExposureCompensationStep();
            Iterator<String> cei = colorEffects.iterator();
            while (cei.hasNext())
            {
                String currentEffect = cei.next();
                if (currentEffect.equals(Camera.Parameters.SCENE_MODE_HDR))
                {
                parameters.setColorEffect(Camera.Parameters.SCENE_MODE_HDR);
                    break;
                }
            }
            

            // Effects are for Android Version 2.0 and higher
            List<Size> csize =  parameters.getSupportedPictureSizes();
            if (csize.size() > 1)
            {
                Iterator<Camera.Size> ei = csize.iterator();
                while (ei.hasNext())
                {
                    Camera.Size aSize = ei.next();
                   if (aSize.width > w)
                    	w = aSize.width;
                   if (aSize.height > h)
                    	h = aSize.height;
                }
            }
            //Size mSize = csize.get(0);
            parameters.setPictureSize(w,h);
            // End Effects for Android Version 2.0 and higher
               // parameters.setAutoExposureLock(true);
                parameters.setExposureCompensation(-12);
                
                camera.setParameters(parameters);
                exp1 =  camera.getParameters().getExposureCompensation();
        }
        catch (IOException exception)
        {
                camera.release();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// ignore
	}

	public void onSensorChanged(SensorEvent event) {
		// Need to get both accelerometer and compass
		// before we can determine our orientationValues
		switch(event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			for(int i=0; i<3; i++) {
	            accelValues[i] = event.values[i];
			}
            if(compassValues[0] != 0)
            	ready = ready | 1;
            break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			for(int i=0; i<3; i++) {
				compassValues[i] = event.values[i];
			}
            if(accelValues[2] != 0)
            	ready = ready | 2;
            break;
		case Sensor.TYPE_ORIENTATION:
			for(int i=0; i<3; i++) {
				orientationValues[i] = event.values[i];
			}
			
			if(recordsensor == 1)
			{
			 time= System.currentTimeMillis();
	         Log.v("values", ";"+ time+";" + orientationValues[0] + ";"+orientationValues[1]+ ";"+ orientationValues[2]);
			}
			
		    break;
		}
		
        if(ready != 3)
        	return;
        
        
	}
	
	
  } // End the Activity

