package cn.myluo.ob1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.myluo.ob1.utils.DrawUtils;
import cn.myluo.ob1.utils.FileUtils;
import cn.myluo.ob1.utils.ImageUtils;
import cn.myluo.ob1.utils.PermissionUtils;
import zeusees.tracking.Face;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private String mCameraId;
    private Size mPreviewSize;
    private Size mCaptureSize;
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private FrameBridge mFrameBridge;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        //全屏无状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setZOrderOnTop(true);
        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mSurfaceHolder = mSurfaceView.getHolder();
    }

    @Override
    protected void onStart() {
        Log.w(TAG, "onStart");
        super.onStart();
        PermissionUtils.checkPermission(this);
    }

    @Override
    protected void onResume() {
        Log.w(TAG, "onResume");
        super.onResume();
        if (PermissionUtils.isPermissionGranted(this)) {
            initModelFiles();
            mFrameBridge = FrameBridge.getInstance();
            startCameraThread();
            if (!mTextureView.isAvailable()) {
                mTextureView.setSurfaceTextureListener(mTextureListener);
            } else {
                openCamera();
            }
            mFrameBridge.startService(this);
        }
    }

    @Override
    protected void onPause() {
        Log.w(TAG, "onPause");
        super.onPause();
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        mFrameBridge.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFrameBridge.stop();
    }

    private void initModelFiles() {
        String assetPath = "ZeuseesFaceTracking";
        String sdcardPath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        FileUtils.copyFilesFromAssets(this, assetPath, sdcardPath);
    }

    private void startCameraThread() {
        Log.w(TAG, "startCameraThread");
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.w(TAG, "onSurfaceTextureAvailable");
            //当SurefaceTexture可用的时候，设置相机参数并打开相机
            setupCamera(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.w(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.w(TAG, "onSurfaceTextureDestroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            Log.w(TAG, "onSurfaceTextureUpdated");
//            Log.w(TAG,"Pid: " + android.os.Process.myPid() + " Tid: " + android.os.Process.myTid() + " name: " + Thread.currentThread().getName());
        }
    };

    private void setupCamera(int width, int height) {
        Log.w(TAG, "setupCamera");
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍历所有摄像头
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //此处默认打开前置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK)
                    continue;
                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                //根据TextureView的尺寸设置预览尺寸
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                //获取相机支持的最大拍照尺寸
                mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                    }
                });
                //此ImageReader用于拍照所需
                setupImageReader();
                mCameraId = cameraId;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //选择sizeMap中大于并且最接近width和height的size
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        Log.w(TAG, "getOptimalSize");
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private void openCamera() {
        Log.w(TAG, "openCamera");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.w(TAG, "onOpened");
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.w(TAG, "onDisconnected");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.w(TAG, "onError");
            camera.close();
            mCameraDevice = null;
        }
    };

    private void startPreview() {
        Log.w(TAG, "startPreview");
        SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(mSurfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.w(TAG, "onConfigured");
                    try {
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.w(TAG, "onConfigureFailed");
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupImageReader() {
        Log.w(TAG, "setupImageReader");
        //2代表ImageReader中最多可以获取两帧图像流
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(),
                ImageFormat.YUV_420_888, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.w(TAG, "onImageAvailable");
//                Log.w(TAG,"Pid: " + android.os.Process.myPid() + " Tid: " + android.os.Process.myTid() + " name: " + Thread.currentThread().getName());
                Image image = reader.acquireLatestImage();

                mFrameBridge.setData(ImageUtils.getDataFromImage(image,
                        ImageUtils.COLOR_FormatNV21), image.getHeight(), image.getWidth());

                List<Face> result = (List<Face>) mFrameBridge.getResult();
                if (result != null) {
                    Paint paint = new Paint();

                    Canvas canvas = mSurfaceHolder.lockCanvas();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    for (Face r : result) {
                        Rect rect = new Rect(mPreviewSize.getHeight() - r.left, r.top,
                                mPreviewSize.getHeight() - r.right, r.bottom);

                        PointF[] points = new PointF[106];
                        for(int i = 0; i < 106; i++) {
                            points[i] = new PointF(r.landmarks[i * 2], r.landmarks[i * 2 + 1]);
                        }

                        float[] visibles =  new float[106];
                        for (int i = 0; i < points.length; i++) {
                            visibles[i] = 1.0f;
                            if (true) {
                                points[i].x = mPreviewSize.getHeight() - points[i].x;
                            }
                        }

                        DrawUtils.drawFaceRect(canvas, rect, mPreviewSize.getHeight(),
                                mPreviewSize.getWidth(), true);
                        DrawUtils.drawPoints(canvas, paint, points, visibles, mPreviewSize.getHeight(),
                                mPreviewSize.getWidth(), true);

                    }
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
                image.close();
            }
        }, mCameraHandler);
    }

//    public void takePicture(View view) {
//        Log.w(TAG, "takePicture");
//        lockFocus();
//    }
//
//    private void lockFocus() {
//        Log.w(TAG, "lockFocus");
//        try {
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback, mCameraHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
//        @Override
//        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
//            Log.w(TAG, "onCaptureProgressed");
//        }
//
//        @Override
//        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
//            Log.w(TAG, "onCaptureCompleted");
//            capture();
//        }
//    };
//
//    private void capture() {
//        Log.w(TAG, "capture");
//        try {
//            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            mCaptureBuilder.addTarget(mImageReader.getSurface());
//            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
//            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
//                @Override
//                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
//                    Log.w(TAG, "onCaptureCompleted");
//                    Toast.makeText(getApplicationContext(), "Image Saved!", Toast.LENGTH_SHORT).show();
//                    unLockFocus();
//                }
//            };
//            mCameraCaptureSession.stopRepeating();
//            mCameraCaptureSession.capture(mCaptureBuilder.build(), CaptureCallback, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void unLockFocus() {
//        Log.w(TAG, "unLockFocus");
//        try {
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//            //mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), null, mCameraHandler);
//            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
}
