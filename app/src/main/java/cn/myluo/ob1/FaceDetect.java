package cn.myluo.ob1;

import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.List;

import zeusees.tracking.Face;
import zeusees.tracking.FaceTracking;

public class FaceDetect {

    private static final String TAG = "FaceDetect";

    private FaceTracking mMultiTrack106 = null;
    private int frameIndex;

    public FaceDetect() {}

    public FaceDetect(String path) {
        mMultiTrack106 = new FaceTracking(path);
    }

    public List<Face> zeusees(byte[] data, int height, int width) {
        Log.w(TAG, "zeusees");
        if (frameIndex == 0) {
            mMultiTrack106.FaceTrackingInit(data, height, width);
        } else {
            mMultiTrack106.Update(data, height, width);
        }
        frameIndex += 1;

        List<Face> faces = mMultiTrack106.getTrackingInfo();
        return faces;
    }
}
