/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.helloar.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer.BlendMode;
import com.google.ar.core.examples.java.helloar.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer, SensorEventListener {
    private static final String TAG = HelloArActivity.class.getSimpleName();

    public static final float THRESHOLD_DISTANCE = 0.1f;
    public static final float SPHERE_RADIUS = 1f;
    public static final int ANDROID_CNT = 3;
    public static final long ANDROID_PERIOD = 5000;
    private static final float DEFAULT_TARGET_SCALE = 1f;
    private static final float TRIGGER_TARGET_SCALE = 5f;

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private Button toggleBtn;
    private TextView coordTxt;

    private boolean installRequested;

    private Session session;
    private GestureDetector gestureDetector;
    private DisplayRotationHelper displayRotationHelper;

    private SensorManager sensorManager;
    private final float[] sensorQuaternion = new float[4];
    private final float[] arQuaternion = new float[4];

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloud = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] anchorMatrix = new float[16];

    private Anchor cameraAnchor = null;
    private boolean needShow = false;

    private List<Anchor> randomAnchors = new ArrayList<>(ANDROID_CNT);
    private List<Float> targetScales = new ArrayList<>(ANDROID_CNT);
    private int anchorCnt = 0;

    private boolean needAddVirtualObject = false;
    private Anchor sphereOrigin = null;

    private Anchor gpsAnchor = null;
    private boolean needRecalculate = false;

    private LocationListener onSelfLocationChangeListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            Log.e("INFO", String.valueOf(location.getLatitude()) + " " + location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        for (int i = 0; i != ANDROID_CNT; ++i) {
            randomAnchors.add(null);
            targetScales.add(DEFAULT_TARGET_SCALE);
        }

        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

//        toggleBtn = findViewById(R.id.toggle_btn);
//        toggleBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                needShow = !needShow;
//            }
//        });
        coordTxt = findViewById(R.id.coord_txt);

        // Set up tap listener.
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                onSingleTap(e);
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });

        surfaceView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return gestureDetector.onTouchEvent(event);
                    }
                });

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        installRequested = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_UI
        );

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (PermissionHelper.hasPermissions(this)) {
            if (session != null) {
                // Note that order matters - see the note in onPause(), the reverse applies here.
                session.resume();
            }
            surfaceView.onResume();
            displayRotationHelper.onResume();
            startLocationTracking();
        } else {
            PermissionHelper.requestPermissions(this);
        }

        if (session == null) {
            try {
                configureSession();
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage());
                return;
            }
        }

        // start generating random androids around you
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                needAddVirtualObject = true;
            }
        }, 0, ANDROID_PERIOD);

        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                needRecalculate = true;
            }
        }, 0, 10000);

        // Note that order matters - see the note in onPause(), the reverse applies here.
        session.resume();
        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);

        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!PermissionHelper.hasPermissions(this)) {
            Toast.makeText(this, "Camera and location permissions are needed to run this application", Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        // TODO add here some sensible code
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        backgroundRenderer.createOnGlThread(/*context=*/ this);

        // Prepare the other rendering objects.
        try {
            virtualObject.createOnGlThread(/*context=*/ this, "andy.obj", "andy.png");
            virtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            virtualObjectShadow.createOnGlThread(/*context=*/ this, "andy_shadow.obj", "andy_shadow.png");
            virtualObjectShadow.setBlendMode(BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        try {
            planeRenderer.createOnGlThread(/*context=*/ this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        pointCloud.createOnGlThread(/*context=*/ this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            final Camera camera = frame.getCamera();

            if (camera.getTrackingState() == TrackingState.TRACKING) {
                camera.getPose().getRotationQuaternion(arQuaternion, 0);

                final Pose poseByGlobalOffset = getPoseByGlobalOffset(camera, new float[]{1, 0, 0});
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        writeCoordInfo(camera.getPose(), poseByGlobalOffset);
                    }
                });

                if (gpsAnchor == null || needRecalculate) {
                    gpsAnchor = session.createAnchor(poseByGlobalOffset);
                    needRecalculate = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HelloArActivity.this, "Recalculated", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                checkAllCollisions();
                if (cameraAnchor != null) {
                    cameraAnchor.detach();
                }
                cameraAnchor = getCameraFloatingPoint(session, frame);

                if (needAddVirtualObject) {
                    if (sphereOrigin == null) {
                        sphereOrigin = session.createAnchor(camera.getPose());
                    }

                    Pose pose = sphereOrigin.getPose().compose(getRandomOffsetPose()).extractTranslation();
                    setNextAnchor(session.createAnchor(pose));
                    needAddVirtualObject = false;
                }
            }

            // Draw background.
            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

//            // Visualize anchors.
//            for (int i = 0; i != randomAnchors.size(); ++i) {
//                Anchor anchor = randomAnchors.get(i);
//                if (anchor == null || anchor.getTrackingState() != TrackingState.TRACKING) {
//                    continue;
//                }
//                // Get the current pose of an Anchor in world space. The Anchor pose is updated
//                // during calls to session.update() as ARCore refines its estimate of the world.
//                anchor.getPose().toMatrix(anchorMatrix, 0);
//
//                // Update and draw the model and its shadow.
//                virtualObject.updateModelMatrix(anchorMatrix, targetScales.get(i));
//                virtualObjectShadow.updateModelMatrix(anchorMatrix, targetScales.get(i));
//                virtualObject.draw(viewmtx, projmtx, lightIntensity);
//                virtualObjectShadow.draw(viewmtx, projmtx, lightIntensity);
//            }

            float scaleFactor = 1.0f;
            if (cameraAnchor != null && needShow) {
                cameraAnchor.getPose().toMatrix(anchorMatrix, 0);

                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
                virtualObject.draw(viewmtx, projmtx, lightIntensity);
                virtualObjectShadow.draw(viewmtx, projmtx, lightIntensity);
            }

            if (gpsAnchor != null) {
                gpsAnchor.getPose().toMatrix(anchorMatrix, 0);

                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
                virtualObject.draw(viewmtx, projmtx, lightIntensity);
                virtualObjectShadow.draw(viewmtx, projmtx, lightIntensity);
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            System.arraycopy(
                    event.values, 0,
                    sensorQuaternion, 0,
                    sensorQuaternion.length
            );
        }

        Log.w("QUATERNION", Arrays.toString(sensorQuaternion) + Arrays.toString(arQuaternion));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void setNextAnchor(Anchor anchor) {
        int index = anchorCnt++ % ANDROID_CNT;
        Anchor oldAnchor = randomAnchors.get(index);
        if (oldAnchor != null) {
            oldAnchor.detach();
        }
        randomAnchors.set(index, anchor);
        targetScales.set(index, DEFAULT_TARGET_SCALE);
    }

    private void checkAllCollisions() {
        if (!needShow) {
            return;
        }
        for (int i = 0; i != randomAnchors.size(); i++) {
            Anchor anchor = randomAnchors.get(i);
            if (anchor == null) {
                return;
            }

            if (detectCollision(cameraAnchor, anchor)) {
                targetScales.set(i, TRIGGER_TARGET_SCALE);
            }
        }
    }

    private boolean detectCollision(Anchor anchor1, Anchor anchor2) {
        float distance = getDistance(anchor1, anchor2);
        return distance < THRESHOLD_DISTANCE;
    }

    private float getDistance(Anchor anchor1, Anchor anchor2) {
        float[] v1 = anchor1.getPose().transformPoint(new float[]{0, 0, 0});
        float[] v2 = anchor2.getPose().transformPoint(new float[]{0 ,0, 0});

        float dist = 0;
        for (int i = 0; i != v1.length; ++i) {
            float d = v2[i] - v1[i];
            dist += d * d;
        }
        return (float) Math.sqrt(dist);
    }

    private Anchor getCameraFloatingPoint(Session session, Frame frame) {
        Camera camera = frame.getCamera();
        Pose pose = camera.getPose().compose(Pose.makeTranslation(0f, 0f, -1f)).extractTranslation();
        return session.createAnchor(pose);
    }

    private Pose getRandomOffsetPose() {
        return Pose.makeTranslation(
                SPHERE_RADIUS * (float) (Math.random() - 0.5),
                SPHERE_RADIUS * (float) (Math.random() - 0.5),
                SPHERE_RADIUS * (float) (Math.random() - 0.5)
        );
    }

    @SuppressLint("MissingPermission")
    private void startLocationTracking() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, onSelfLocationChangeListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, onSelfLocationChangeListener);
    }

    private Pose getPoseByGlobalOffset(final Camera camera, float[] globalOffset) {
        final float[] localTranslation = new float[3];
        camera.getPose().inverse().rotateVector(globalOffset, 0, localTranslation, 0);
        return camera.getDisplayOrientedPose().compose(Pose.makeTranslation(localTranslation));
    }

    private void configureSession() throws Exception {
        Exception exception = null;
        String message = null;
        try {
            switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                case INSTALL_REQUESTED:
                    installRequested = true;
                    return;
                case INSTALLED:
                    break;
            }

            // ARCore requires camera permissions to operate. If we did not yet obtain runtime
            // permission on Android M and above, now is a good time to ask the user for it.
            if (!PermissionHelper.hasPermissions(this)) {
                PermissionHelper.requestPermissions(this);
                return;
            }

            session = new Session(/* context= */ this);
        } catch (UnavailableArcoreNotInstalledException
                | UnavailableUserDeclinedInstallationException e) {
            message = "Please install ARCore";
            exception = e;
        } catch (UnavailableApkTooOldException e) {
            message = "Please update ARCore";
            exception = e;
        } catch (UnavailableSdkTooOldException e) {
            message = "Please update this app";
            exception = e;
        } catch (Exception e) {
            message = "This device does not support AR";
            exception = e;
        }

        if (message != null) {
            Log.e(TAG, "Exception creating session", exception);
            throw exception;
        }

        // Create default config and check if supported.
        Config config = new Config(session);
        if (!session.isSupported(config)) {
            message = "This device does not support AR";
            throw new Exception(message);
        }
        session.configure(config);
    }

    private void writeCoordInfo(Pose cameraPose, Pose androidPose) {
        String msg = cameraPose.toString() + "\n" + androidPose.toString();
        coordTxt.setText(msg);
    }
}
