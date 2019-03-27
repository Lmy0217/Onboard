#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;

extern "C" JNIEXPORT jstring JNICALL
Java_cn_myluo_ob1_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {

    Mat image = Mat::zeros(100, 100, CV_8SC1);

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
