#include <jni.h>
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_polymath_local_NativeLlm_generate(JNIEnv * env, jobject, jint, jbyteArray, jint) {
    env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Local AI requires a supported 64-bit device.");
    return nullptr;
}
