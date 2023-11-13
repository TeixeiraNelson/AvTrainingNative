#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>

extern "C"
JNIEXPORT _jdoubleArray * JNICALL
Java_com_example_avtrainingnative_ArgbAnalyzer_analyzeImage(JNIEnv *env, jobject thiz,
                                                            jintArray argb_data, jint image_width,
                                                            jint image_height, jint area_width,
                                                            jint area_height, jint area_center_x,
                                                            jint area_center_y) {
    // Obtain the native array from Java
    jint *argb_data_ptr = env->GetIntArrayElements(argb_data, 0);

    // Create a cv::Mat from the ARGB data
    cv::Mat inputImage(image_height, image_width, CV_8UC4, reinterpret_cast<void *>(argb_data_ptr));

    // Calculate the coordinates for cropping and makes sure that
    // the x cannot be below 0 (same for Y)
    int x = std::max(0, area_center_x - area_width / 2);
    int y = std::max(0, area_center_y - area_height / 2);

    // Ensure that the crop area does not exceed the image boundaries (on the right side of the image)
    // this ensures that if the crop center is misplaced, code won't crash
    // If it is in the image but too much on the right (meaning that there is not enough pixels in the image to do the
    // entire cropWidth, it will get the pixels available only
    int cropWidth = std::min(area_width, image_width - x);
    int cropHeight = std::min(area_height, image_height - y);

    // Creates a Rect based on the coordinates we calculated and width/height
    cv::Rect cropRect(x, y, cropWidth, cropHeight);

    // creates a new cv::Mat based on cropRect which is used to extract a sub-matrix from input image
    cv::Mat croppedImage = inputImage(cropRect);

    // Reorder the channels from ARGB to BGRA
    // openCV expects BGRA, and android uses ARGB the channels are mixed
    cv::cvtColor(croppedImage, croppedImage, cv::COLOR_RGBA2BGRA);

    // Release the array
    env->ReleaseIntArrayElements(argb_data, argb_data_ptr, 0);

    // Calculate the mean values for each channel in the cropped image
    cv::Scalar meanValues = cv::mean(croppedImage);

    // Create a double array to store mean values
    jdoubleArray resultArray = env->NewDoubleArray(4);
    if (resultArray != nullptr) {
        jdouble meanAlpha = meanValues[3];  // Alpha channel
        jdouble meanRed = meanValues[2];    // Red channel
        jdouble meanGreen = meanValues[1];  // Green channel
        jdouble meanBlue = meanValues[0];   // Blue channel

        jdouble meanValuesArray[] = {meanAlpha, meanRed, meanGreen, meanBlue};

        // Set the values in the result array
        env->SetDoubleArrayRegion(resultArray, 0, 4, meanValuesArray);
    }

    return resultArray;
}