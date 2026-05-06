#include "io_github_dsheirer_dsp_filter_fir_real_NativeRealFIRFilter.h"
#include <jni.h>
#include <volk/volk.h>

extern "C" {

JNIEXPORT void JNICALL Java_io_github_dsheirer_dsp_filter_fir_real_NativeRealFIRFilter_nativeFilter
  (JNIEnv *env, jobject obj, jobject buffer, jobject coefficients, jobject filtered, jint sampleLength, jint coefficientLength) {

    float* pBuffer = (float*)env->GetDirectBufferAddress(buffer);
    float* pCoefficients = (float*)env->GetDirectBufferAddress(coefficients);
    float* pFiltered = (float*)env->GetDirectBufferAddress(filtered);

    for(int i = 0; i < sampleLength; i++) {
        volk_32f_x2_dot_prod_32f(&pFiltered[i], &pBuffer[i], pCoefficients, coefficientLength);
    }
}

}
