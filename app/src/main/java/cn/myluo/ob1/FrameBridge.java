package cn.myluo.ob1;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

public class FrameBridge {

    private byte[] data = null;
    private int height = 0;
    private int width = 0;
    private boolean lock = false;

    private Object result = null;
    private boolean next = false;
    private boolean finish = false;

    private int startId = -1;
    private boolean stop = false;

    private String func = "face";

    private FaceDetect faceDetect;

    private FrameBridge() {
        this("/sdcard/ZeuseesFaceTracking/models");
    }

    private FrameBridge(String path) {
        faceDetect = new FaceDetect(path);
    }

    public static FrameBridge getInstance() {
        return FrameBridgeHolder.instance;
    }

    private static class FrameBridgeHolder {
        private static FrameBridge instance = new FrameBridge();
    }

    public void startService(Context context) {
        next = true;
        if (startId == -1 || finish)
            context.startService(new Intent(context, FrameService.class));
    }

    public void pause() {
        next = false;
    }

    public void stop() {
        if (startId != -1)
            stop = true;
    }

    public void startId(int startId) {
        this.startId = startId;
        finish = false;
    }

    public boolean isStop() {
        return stop;
    }

    public boolean isNext() {
        return next;
    }

    public boolean isNull() {
        return data == null;
    }

    public String getFunc() {
        return func;
    }

    public void finish() {
        finish = true;
    }

    public void stopService(Service service) {
        service.stopSelf(startId);
        startId = -1;
    }

    public synchronized void setLock(boolean lock) {
        this.lock = lock;
    }

    public boolean getLock() {
        return lock;
    }

    public void setData(byte[] data, int height, int width) {
        this.data = data;
        this.height = height;
        this.width = width;
        setLock(false);
    }

    public byte[] getData() {
        setLock(true);
        return data;
    }

    public int height() {
        return height;
    }

    public int width() {
        return width;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

    public FaceDetect getFaceDetect() {
        return faceDetect;
    }
}
