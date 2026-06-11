#include <jni.h>
extern "C" {

JNIEXPORT void JNICALL Java_io_github_dsheirer_dsp_filter_fir_real_NativeRealFIRFilter_nativeFilter
  (JNIEnv *env, jobject obj, jobject buffer, jobject coefficients, jobject filtered, jint sampleLength, jint coefficientLength) {

    float* pBuffer = (float*)env->GetDirectBufferAddress(buffer);
    float* pCoefficients = (float*)env->GetDirectBufferAddress(coefficients);
    float* pFiltered = (float*)env->GetDirectBufferAddress(filtered);

    for(int i = 0; i < sampleLength; i++) {
        float sum = 0.0f;
        for(int j = 0; j < coefficientLength; j++) {
            sum += pBuffer[i + j] * pCoefficients[j];
        }
        pFiltered[i] = sum;
    }
}

}
