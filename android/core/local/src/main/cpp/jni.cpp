#include "local_llm.h"
#include <jni.h>
#include <exception>
#include <stdexcept>

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_polymath_local_NativeLlm_generate(JNIEnv * env, jobject, jint fd, jbyteArray input, jint threads) {
    try {
        const auto length = env->GetArrayLength(input);
        if (length > 24000) throw std::runtime_error("Local prompt is too large.");
        std::string prompt(length, '\0');
        env->GetByteArrayRegion(input, 0, length, reinterpret_cast<jbyte *>(prompt.data()));
        auto result = polymath_generate("/proc/self/fd/" + std::to_string(fd), prompt, threads);
        auto output = env->NewByteArray(static_cast<jsize>(result.size()));
        if (output) env->SetByteArrayRegion(output, 0, static_cast<jsize>(result.size()), reinterpret_cast<const jbyte *>(result.data()));
        return output;
    } catch (const std::exception & error) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return nullptr;
    }
}
