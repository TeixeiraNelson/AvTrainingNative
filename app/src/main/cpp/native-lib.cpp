#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <android/bitmap.h>

jdouble calculateSNR(std::vector<uint8_t> vector1);

extern "C"
JNIEXPORT _jdoubleArray * JNICALL
Java_com_example_avtrainingnative_ArgbAnalyzer_analyzeImageOpenCV(JNIEnv *env, jobject thiz,
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

extern "C"
JNIEXPORT _jdoubleArray * JNICALL
Java_com_example_avtrainingnative_ArgbAnalyzer_analyzeImageCpp(JNIEnv *env, jobject thiz,
                                                                  jintArray argb_data, jint image_width,
                                                                  jint image_height, jint area_width,
                                                                  jint area_height, jint area_center_x,
                                                                  jint area_center_y) {
    // Obtain a pointer to the native array in Java
    jint *argb_data_ptr = env->GetIntArrayElements(argb_data, 0);

    // Calculate the coordinates for cropping and make sure that
    // the x cannot be below 0 (same for Y), making sure that it is not out of the image
    int x = std::max(0, area_center_x - area_width / 2);
    int y = std::max(0, area_center_y - area_height / 2);

    // Ensure that the crop area does not exceed the image boundaries (on the right side of the image)
    // this ensures that if the crop center is misplaced, code won't crash
    // If it is in the image but too much on the right (meaning that there is not enough pixels in the image to do the
    // entire cropWidth, it will get the pixels available only
    int cropWidth = std::min(area_width, image_width - x);
    int cropHeight = std::min(area_height, image_height - y);

    // Create a new array to hold the cropped data
    jintArray croppedData = env->NewIntArray(cropWidth * cropHeight);

    // Check if array creation is successful
    if (croppedData == nullptr) {
        // Handle error
        return nullptr;
    }

    // Create a pointer to the cropped data
    jint *croppedDataPtr = env->GetIntArrayElements(croppedData, nullptr);
    for(int w = 0; w < cropWidth; w ++) {
        for(int h = 0; h < cropHeight; h ++){
            int cropArrayIndex = cropWidth * h + w;
            int originalImageIndex = image_width * (y + h) + (x + w);
            croppedDataPtr[cropArrayIndex] = argb_data_ptr[originalImageIndex];
        }
    }

    uint sumRed = 0;
    uint sumGreen = 0;
    uint sumBlue = 0;
    uint sumAlpha = 0;

    // now iterate through cropped data to get mean value of ARGB values
    for(int i = 0; i < cropWidth * cropHeight; i++){
        uint pixelValue = croppedDataPtr[i];

        // Extracting individual bytes
        uint alpha = (pixelValue >> 24) & 0xFF;
        uint blue = (pixelValue >> 16) & 0xFF;
        uint green = (pixelValue >> 8) & 0xFF;
        uint red = pixelValue & 0xFF; // <<< ask Yves why ABGR ???

        sumRed += red;
        sumGreen += green;
        sumBlue += blue;
        sumAlpha += alpha;
    }

    double meanRed = sumRed / (cropWidth * cropHeight);
    double meanGreen = sumGreen / (cropWidth * cropHeight);
    double meanBlue = sumBlue / (cropWidth * cropHeight);
    double meanAlpha = sumAlpha / (cropWidth * cropHeight);

    // Release the arrays
    env->ReleaseIntArrayElements(argb_data, argb_data_ptr, JNI_ABORT);
    env->ReleaseIntArrayElements(croppedData, croppedDataPtr, 0);

    // Create a double array to store mean values
    jdoubleArray resultArray = env->NewDoubleArray(4);
    if (resultArray != nullptr) {

        jdouble meanValuesArray[] = {meanAlpha, meanRed, meanGreen, meanBlue};

        // Set the values in the result array
        env->SetDoubleArrayRegion(resultArray, 0, 4, meanValuesArray);
    }

    return resultArray;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_avtrainingnative_ImageAnalyzer_cropImage(JNIEnv *env, jobject thiz,
                                                               jbyteArray rawImage, jint image_width,
                                                               jint image_height, jint area_width,
                                                               jint area_height, jint area_center_x,
                                                               jint area_center_y) {
    // Obtain a pointer to the native array in Java
    jbyte *raw_data = env->GetByteArrayElements(rawImage, 0);

    // Calculate the coordinates for cropping and make sure that
    // the x cannot be below 0 (same for Y), making sure that it is not out of the image
    int x = std::max(0, area_center_x - area_width / 2);
    int y = std::max(0, area_center_y - area_height / 2);

    // Ensure that the crop area does not exceed the image boundaries (on the right side of the image)
    // this ensures that if the crop center is misplaced, code won't crash
    // If it is in the image but too much on the right (meaning that there is not enough pixels in the image to do the
    // entire cropWidth, it will get the pixels available only
    int cropWidth = std::min(area_width, image_width - x);
    int cropHeight = std::min(area_height, image_height - y);

    // Create a new array to hold the cropped data
    jbyteArray croppedData = env->NewByteArray(cropWidth * cropHeight);

    // Check if array creation is successful
    if (croppedData == nullptr) {
        // Handle error
        return nullptr;
    }

    // Create a pointer to the cropped data
    jbyte *croppedDataPtr = env->GetByteArrayElements(croppedData, nullptr);

    for(int w = 0; w < cropWidth; w ++) {
        for(int h = 0; h < cropHeight; h ++){
            int cropArrayIndex = cropWidth * h + w;
            int originalImageIndex = image_width * (y + h) + (x + w);
            croppedDataPtr[cropArrayIndex] = raw_data[originalImageIndex];
        }
    }

    // Release the arrays
    env->ReleaseByteArrayElements(rawImage, raw_data, JNI_ABORT);
    env->ReleaseByteArrayElements(croppedData, croppedDataPtr, 0);

    return croppedData;
}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_avtrainingnative_ImageAnalyzer_crossCorr(JNIEnv *env, jobject thiz,
                                                         jbyteArray source_image,
                                                          jbyteArray template_image,
                                                         jint image_width,
                                                         jint image_height) {
    // Obtain a pointer to the native array in Java
    // Get the array elements as signed bytes
    jbyte *source_data = env->GetByteArrayElements(source_image, nullptr);
    jbyte *template_data = env->GetByteArrayElements(template_image, nullptr);
    // Convert jbyte arrays to uint8_t arrays
    uint8_t *source_uint8 = reinterpret_cast<uint8_t*>(source_data);
    uint8_t *template_uint8 = reinterpret_cast<uint8_t*>(template_data);
    // Create a new array to hold the cropped data

    jbyteArray resultArray = env->NewByteArray(image_height * image_width);
    jbyte* resultData = env->GetByteArrayElements(resultArray, nullptr);

    // Perform cross-correlation and populate resultDataPtr
    double min = std::numeric_limits<double>::max();
    double max = std::numeric_limits<double>::min();
    double mean = 0.0;

    int resultWidth = image_width;
    int resultHeight = image_height;

    int templateHeight = image_height;
    int templateWidth = image_width;

    double* resultValues = new double[image_height * image_width];

    for (int Ry = 0; Ry < resultHeight; Ry ++){
        for (int Rx = 0; Rx < resultWidth; Rx ++){

            int resultIndex = resultWidth * Ry + Rx;
            double crossCorr = 0;

            for (int Ty = 0; Ty < templateHeight; Ty ++){
                for(int Tx = 0; Tx < templateWidth; Tx ++){

                    int templateIndex = templateWidth * Ty + Tx;

                    int imageY = (Ty + Ry);
                    int imageX = (Tx + Rx);

                    if(imageX >= image_width){
                        imageX = imageX % image_width;
                    }
                    if(imageY >= image_height){
                        imageY = imageY % image_height;
                    }

                    int imageIndex = templateWidth * imageY + imageX;

                    crossCorr += template_uint8[templateIndex] * source_uint8[imageIndex];

                }
            }
            max = std::max(max, crossCorr);
            min = std::min(min, crossCorr);
            mean += crossCorr;
            resultValues[resultIndex] = crossCorr;
        }
    }

    mean /= (resultWidth * resultHeight);
    // Calculate SNR
    double snr = 20 * std::log10((max - min) / (mean - min));

    // Normalize resultDataPtr between 0 and 255
    for (int i = 0; i < resultWidth * resultHeight; i++) {
        // Perform the normalization and cast to uint8_t
        resultData[i] = static_cast<jbyte>(static_cast<jint>((resultValues[i] - min) / (max - min) * 255.0));
    }
    // Create a new instance of ImageAnalysisResult and set its fields
    jclass resultClass = env->FindClass("com/example/avtrainingnative/ImageAnalysisResult");
    jmethodID resultConstructor = env->GetMethodID(resultClass, "<init>", "([BD)V");

    jbyteArray resultArrayCopy = env->NewByteArray(image_height * image_width);
    env->SetByteArrayRegion(resultArrayCopy, 0, image_height * image_width, resultData);

    jobject resultObject = env->NewObject(resultClass, resultConstructor, resultArrayCopy, snr);

    // Release the native arrays
    env->ReleaseByteArrayElements(source_image, source_data, JNI_ABORT);
    env->ReleaseByteArrayElements(template_image, template_data, JNI_ABORT);

    return resultObject;
}