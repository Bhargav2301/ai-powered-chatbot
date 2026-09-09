#pragma once
#include <string>
// One call owns and frees all model/context/sampler allocations. Never called on the UI thread.
std::string polymath_generate(const std::string & path, const std::string & prompt, int threads);
