#include "local_llm.h"
#include <jni.h>
#include <exception>
#include <stdexcept>
#include <memory>
#include <unistd.h>

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_polymath_local_NativeLlm_generate(JNIEnv * env, jobject, jint fd, jbyteArray input, jint threads) {
    try {
        const auto length = env->GetArrayLength(input);
        if (length > 24000) throw std::runtime_error("Local prompt is too large.");
        std::string prompt(length, '\0');
        env->GetByteArrayRegion(input, 0, length, reinterpret_cast<jbyte *>(prompt.data()));
        // Binder grants use of this descriptor; reopening its path would require the
        // permissionless worker to have the app UID's filesystem access.
        const int model_fd = dup(fd);
        if (model_fd < 0) throw std::runtime_error("Could not duplicate the model descriptor.");
        FILE * raw = fdopen(model_fd, "rb");
        if (!raw) { close(model_fd); throw std::runtime_error("Could not read the model descriptor."); }
        std::unique_ptr<FILE, decltype(&fclose)> file(raw, fclose);
        auto result = polymath_generate(file.get(), prompt, threads);
        auto output = env->NewByteArray(static_cast<jsize>(result.size()));
        if (output) env->SetByteArrayRegion(output, 0, static_cast<jsize>(result.size()), reinterpret_cast<const jbyte *>(result.data()));
        return output;
    } catch (const std::exception & error) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return nullptr;
    }
}
