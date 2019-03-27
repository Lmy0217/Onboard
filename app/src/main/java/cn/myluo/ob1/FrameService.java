package cn.myluo.ob1;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.List;

import zeusees.tracking.Face;

public class FrameService extends Service {

    private static final String TAG = "Onboard::FrameService";

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String mName;
    private boolean mRedelivery;
    private FrameBridge mFrameBridge;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
        }
    }

    public FrameService() {
        this("");
    }

    public FrameService(String name) {
        super();
        mName = name;
        mFrameBridge = FrameBridge.getInstance();
    }

    public void setIntentRedelivery(boolean enabled) {
        mRedelivery = enabled;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("FrameService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
//        mServiceHandler.removeCallbacksAndMessages(null);
        mFrameBridge.startId(startId);
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @WorkerThread
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!mFrameBridge.isStop()) {
            while (mFrameBridge.isNext()) {
                if (!mFrameBridge.isNull() && !mFrameBridge.getLock()) {
                    if (mFrameBridge.getFunc().equals("gaze")) {
//                        FrameBridge.result = gaze(FrameBridge.frame);
                    } else if (mFrameBridge.getFunc().equals("face")) {
                        mFrameBridge.setResult(face(mFrameBridge.getData(),
                                mFrameBridge.height(), mFrameBridge.width()));
                    }
                }
            }
        }
        mFrameBridge.finish();
        if (mFrameBridge.isStop()) {
            mFrameBridge.stopService(this);
        }
    }

//    private Object gaze(Mat frame) {
//        return facedetect(frame);
//    }
//
//    static {
//        System.loadLibrary("facedetection");
//    }
//
//    public static int[] facedetect(Mat frame) {
//        int size = (int)(frame.total() * frame.elemSize());
//        byte[] bytebuffer = new byte[size];
//        frame.get(0, 0, bytebuffer);
//        return facedetectcnn(bytebuffer, frame.cols(), frame.rows(), (int)(frame.step1() * frame.elemSize1()));
//    }
//
//    public static native int[] facedetectcnn(byte[] image, int cols, int rows, int step);

    private List<Face> face(byte[] data, int height, int width) {
        return mFrameBridge.getFaceDetect() != null ?
                mFrameBridge.getFaceDetect().zeusees(data, height, width) : null;
    }
}
