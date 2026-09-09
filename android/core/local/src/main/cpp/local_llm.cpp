#include "local_llm.h"
#include "llama.h"
#include <algorithm>
#include <chrono>
#include <memory>
#include <stdexcept>
#include <vector>

namespace {
using Clock = std::chrono::steady_clock;
bool expired(void * p) { return Clock::now() >= *static_cast<Clock::time_point *>(p); }
bool loading(float, void * p) { return !expired(p); }
void quiet_log(ggml_log_level, const char *, void *) {} // Never log model prompts or source text.
constexpr const char * grammar = R"GBNF(
root ::= "{" ws "\"answer\"" ws ":" ws string ws "," ws "\"citation_ids\"" ws ":" ws "[" ws (id (ws "," ws id)*)? ws "]" ws "}"
id ::= "\"S" [1-3] "\""
string ::= "\"" char* "\""
char ::= [^"\\\x00-\x1F] | "\\" (["\\/bfnrt] | "u" [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F])
ws ::= [ \t\n\r]*
)GBNF";
}

std::string polymath_generate(const std::string & path, const std::string & prompt, int threads) {
    if (prompt.empty() || prompt.size() > 24000) throw std::runtime_error("Local prompt exceeds its byte budget.");
    llama_log_set(quiet_log, nullptr);
    llama_backend_init();
    const auto deadline_value = Clock::now() + std::chrono::seconds(120);
    auto deadline = deadline_value;
    auto mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    mp.load_mode = LLAMA_LOAD_MODE_MMAP;
    mp.progress_callback = loading;
    mp.progress_callback_user_data = &deadline;
    using Model = std::unique_ptr<llama_model, decltype(&llama_model_free)>;
    Model model(llama_model_load_from_file(path.c_str(), mp), llama_model_free);
    if (!model) throw std::runtime_error("Could not load the local model. Check free memory and the model pack.");
    const auto * vocab = llama_model_get_vocab(model.get());
    const int n = -llama_tokenize(vocab, prompt.data(), static_cast<int>(prompt.size()), nullptr, 0, true, true);
    if (n <= 0 || n > 1536) throw std::runtime_error("The selected evidence exceeds the local token budget. Ask a shorter question or use a smaller source.");
    std::vector<llama_token> tokens(n);
    if (llama_tokenize(vocab, prompt.data(), static_cast<int>(prompt.size()), tokens.data(), n, true, true) != n)
        throw std::runtime_error("Could not tokenize the local prompt.");
    auto cp = llama_context_default_params();
    cp.n_ctx = 2048;
    cp.n_batch = 128;
    cp.n_ubatch = 64;
    cp.n_seq_max = 1;
    cp.n_threads = cp.n_threads_batch = std::clamp(threads, 1, 2);
    cp.type_k = cp.type_v = GGML_TYPE_F16;
    cp.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_DISABLED;
    cp.abort_callback = expired;
    cp.abort_callback_data = &deadline;
    cp.no_perf = true;
    using Context = std::unique_ptr<llama_context, decltype(&llama_free)>;
    Context ctx(llama_init_from_model(model.get(), cp), llama_free);
    if (!ctx) throw std::runtime_error("Could not allocate the local inference context.");
    auto sp = llama_sampler_chain_default_params(); sp.no_perf = true;
    using Sampler = std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)>;
    Sampler sampler(llama_sampler_chain_init(sp), llama_sampler_free);
    auto * constrained = llama_sampler_init_grammar(vocab, grammar, "root");
    if (!constrained) throw std::runtime_error("Could not initialize the response grammar.");
    llama_sampler_chain_add(sampler.get(), constrained);
    llama_sampler_chain_add(sampler.get(), llama_sampler_init_greedy());
    for (int at = 0; at < n; at += 128) {
        auto batch = llama_batch_get_one(tokens.data() + at, std::min(128, n - at));
        if (expired(&deadline) || llama_decode(ctx.get(), batch) != 0)
            throw std::runtime_error("Local inference stopped or ran out of memory during prompt processing.");
    }
    std::string answer;
    for (int generated = 0; generated < 256; ++generated) {
        if (expired(&deadline)) throw std::runtime_error("Local inference reached its two-minute limit.");
        auto token = llama_sampler_sample(sampler.get(), ctx.get(), -1);
        if (llama_vocab_is_eog(vocab, token)) return answer;
        std::vector<char> piece(256);
        int size = llama_token_to_piece(vocab, token, piece.data(), static_cast<int>(piece.size()), 0, false);
        if (size < 0) {
            piece.resize(-size);
            size = llama_token_to_piece(vocab, token, piece.data(), static_cast<int>(piece.size()), 0, false);
        }
        if (size < 0) throw std::runtime_error("Could not decode a local token.");
        answer.append(piece.data(), size);
        if (answer.size() > 12000) throw std::runtime_error("Local answer exceeds the output budget.");
        auto batch = llama_batch_get_one(&token, 1);
        if (llama_decode(ctx.get(), batch) != 0) throw std::runtime_error("Local inference stopped during generation.");
    }
    // The caller validates JSON; incomplete output must never masquerade as a cited answer.
    return answer;
}
