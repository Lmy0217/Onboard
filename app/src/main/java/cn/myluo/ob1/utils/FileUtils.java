package cn.myluo.ob1.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    public static String getSDCardPath(String path) {
        return Environment.getExternalStorageDirectory() + File.separator + path;
    }

    public static void copyFilesFromAssets(Context context, String assetPath,
                                           String sdcardPath) {
        try {
            String[] fileNames = context.getAssets().list(assetPath);
            if (fileNames != null) {
                if (fileNames.length > 0) {
                    File file = new File(sdcardPath);
                    if (!file.mkdir()) {
                        Log.d("mkdir","can't make folder");
                    }
                    for (String fileName : fileNames) {
                        copyFilesFromAssets(context,
                                assetPath + File.separator + fileName,
                                sdcardPath + File.separator + fileName);
                    }
                } else {
                    InputStream is = context.getAssets().open(assetPath);
                    FileOutputStream fos = new FileOutputStream(new File(sdcardPath));
                    byte[] buffer = new byte[1024];
                    int byteCount;
                    while ((byteCount = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, byteCount);
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
