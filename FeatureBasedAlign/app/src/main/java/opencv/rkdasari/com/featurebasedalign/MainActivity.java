package opencv.rkdasari.com.featurebasedalign;

import android.app.Activity;
import android.content.ContentValues;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends Activity implements OnClickListener, SurfaceHolder.Callback, Camera.PictureCallback, Camera.ShutterCallback {
    protected static final String TAG = "surfbased";
    private static final int ORB = 5;
    SurfaceView cameraView;
    SurfaceHolder surfaceHolder;
    Camera camera;
    Bitmap bmp1,bmp2,bmpfin;
    private int first = 0;
    Handler timerHandler = new Handler();
    Mat Mat1,Mat2;
    MatOfDMatch matches, gm ;
    LinkedList<DMatch> good_matches;
    LinkedList<Point> objList;
    LinkedList<Point> sceneList;
    MatOfKeyPoint keypoints_object ;
    MatOfKeyPoint keypoints_scene;
    Mat descriptors_object ;
    Mat descriptors_scene ;
    MatOfPoint2f obj ;
    MatOfPoint2f scene ;
    FeatureDetector fd;
    DescriptorExtractor extractor;
    DescriptorMatcher matcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (SurfaceView) this.findViewById(R.id.CameraView);
        surfaceHolder = cameraView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);

        cameraView.setFocusable(true);
        cameraView.setFocusableInTouchMode(true);
        cameraView.setClickable(true);

        cameraView.setOnClickListener(this);
    }

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



    Runnable timerTask = new Runnable() {
        public void run() {
            camera.takePicture(MainActivity.this,null,null,MainActivity.this);
        }
    };
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this, mLoaderCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // TODO Auto-generated method stub
        camera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera = Camera.open();
        try {
            camera.setPreviewDisplay(holder);
            Camera.Parameters parameters = camera.getParameters();
            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            {
                parameters.set("orientation", "portrait");
                // For Android Version 2.2 and above
                camera.setDisplayOrientation(90);

                // For Android Version 2.0 and above
                parameters.setRotation(90);
            }
            // Effects are for Android Version 2.0 and higher
            List<String> colorEffects = parameters.getSupportedColorEffects();
            Iterator<String> cei = colorEffects.iterator();
            while (cei.hasNext())
            {
                String currentEffect = cei.next();
                if (currentEffect.equals(Camera.Parameters.EFFECT_NONE))
                {
                    parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
                    break;
                }
            }
            // End Effects for Android Version 2.0 and higher
            int w = 0;
            int h = 0;
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

            camera.setParameters(parameters);
        }
        catch (IOException exception)
        {
            camera.release();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera.stopPreview();
        camera.release();

    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        camera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        // TODO Auto-generated method stub


        if(first==0)
        {
            Uri imageFileUri =  getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bmp1 = BitmapFactory.decodeByteArray(data,0,data.length,bmpFactoryOptions);
            OutputStream imageFileOS;
            try {
                imageFileOS = getContentResolver().openOutputStream(imageFileUri);
                imageFileOS.write(data);
                imageFileOS.flush();
                imageFileOS.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            first = first + 1;
            timerHandler.postDelayed(timerTask, 500);

        }
        else
        {
            Uri imageFileUri1 =  getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bmp2 = BitmapFactory.decodeByteArray(data,0,data.length,bmpFactoryOptions);
            OutputStream imageFileOS2;
            try {
                imageFileOS2 = getContentResolver().openOutputStream(imageFileUri1);
                imageFileOS2.write(data);
                imageFileOS2.flush();
                imageFileOS2.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Mat1 = new Mat();
            Mat2 = new Mat();
            Utils.bitmapToMat(bmp1, Mat1);
            Utils.bitmapToMat(bmp2, Mat2);
            //-- Step 1: Detect the keypoints using SURF Detector
            //  int minHessian = 400;
            matches = new MatOfDMatch();
            gm = new MatOfDMatch();
            good_matches = new LinkedList<DMatch>();
            objList = new LinkedList<Point>();
            sceneList = new LinkedList<Point>();
            keypoints_object = new MatOfKeyPoint();
            keypoints_scene = new MatOfKeyPoint();
            descriptors_object = new Mat();
            descriptors_scene = new Mat();
            obj = new MatOfPoint2f();
            scene = new MatOfPoint2f();

            fd =  FeatureDetector.create(ORB);



            fd.detect( Mat1, keypoints_object );
            fd.detect( Mat2, keypoints_scene );
            int con = 3;
            //-- Step 2: Calculate descriptors (feature vectors)
            extractor = DescriptorExtractor.create(con);



            extractor.compute( Mat1, keypoints_object, descriptors_object );
            extractor.compute( Mat2, keypoints_scene, descriptors_scene );
            con = 1;
            //-- Step 3: Matching descriptor vectors using FLANN matcher
            matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            matcher.match( descriptors_object, descriptors_scene, matches);

            double max_dist = 0; double min_dist = 100;
            List<DMatch> matchesList = matches.toList();

            //-- Quick calculation of max and min distances between keypoints
            for( int i = 0; i < descriptors_object.rows(); i++ )
            {
                Double dist = (double) matchesList.get(i).distance;
                if( dist < min_dist ) min_dist = dist;
                if( dist > max_dist ) max_dist = dist;
            }

            for(int i = 0; i < descriptors_object.rows(); i++){
                if(matchesList.get(i).distance < 3*min_dist){
                    good_matches.addLast(matchesList.get(i));
                }
            }

            gm.fromList(good_matches);


            List<KeyPoint> keypoints_objectList = keypoints_object.toList();
            List<KeyPoint> keypoints_sceneList = keypoints_scene.toList();

            for(int i = 0; i<good_matches.size(); i++){
                objList.addLast(keypoints_objectList.get(good_matches.get(i).queryIdx).pt);
                sceneList.addLast(keypoints_sceneList.get(good_matches.get(i).trainIdx).pt);
            }


            obj.fromList(objList);

            scene.fromList(sceneList);

            Mat H = Calib3d.findHomography(obj, scene);

            Mat warpimg = Mat1.clone();
            org.opencv.core.Size ims = new org.opencv.core.Size(Mat1.cols(),Mat1.rows());
            Imgproc.warpPerspective(Mat1, warpimg,H, ims);

            bmpfin = Bitmap.createBitmap(bmp1.getWidth(),bmp1.getHeight(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(warpimg, bmpfin);
            Uri imageFileUri2 =  getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
            OutputStream imageFileOS1;
            try {
                imageFileOS1 = getContentResolver().openOutputStream(imageFileUri2);
                bmpfin.compress(Bitmap.CompressFormat.JPEG, 90, imageFileOS1);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        camera.startPreview();

    }

    @Override
    public void onShutter() {
        // TODO Auto-generated method stub

    }

}
