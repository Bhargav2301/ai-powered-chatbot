#pragma once
#include <string>
#include <cstdio>
// One call owns and frees all model/context/sampler allocations. Never called on the UI thread.
// The caller owns the open model file for the entire call; no path is reopened in the isolated UID.
std::string polymath_generate(FILE * file, const std::string & prompt, int threads);
