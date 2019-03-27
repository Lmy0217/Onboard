package cn.myluo.ob1.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

public class DrawUtils {

    public static void drawFaceRect(Canvas canvas, Rect rect, int width, int height,
                                    boolean frontCamera) {
        if(canvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.rgb(255, 0, 127));
            int strokeWidth = Math.max(width / 240, 2);
            paint.setStrokeWidth((float)strokeWidth);
            if(frontCamera) {
                int left = rect.left;
                rect.left = width - rect.right;
                rect.right = width - left;
            }
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rect, paint);
        }
    }

    public static void drawPoints(Canvas canvas, Paint paint, PointF[] points, float[] visibles,
                                  int width, int height, boolean frontCamera) {
        if(canvas != null) {
            int strokeWidth = Math.max(width / 240, 2);
            for(int i = 0; i < points.length; ++i) {
                PointF p = points[i];
                if(frontCamera) {
                    p.x = (float)width - p.x;
                }
                if((double)visibles[i] < 0.5D) {
                    paint.setColor(Color.rgb(255, 20, 20));
                } else {
                    paint.setColor(Color.rgb(57, 168, 243));
                }
                canvas.drawCircle(p.x, p.y, (float)strokeWidth, paint);
            }
            paint.setColor(Color.rgb(57, 138, 243));
        }
    }
}
