#include "local_llm.h"
#include <fstream>
#include <iostream>
#include <iterator>
#include <memory>
#include <stdexcept>
int main(int argc, char ** argv) {
    if (argc != 3) return 2;
    try {
        std::ifstream input(argv[2]);
        std::string prompt((std::istreambuf_iterator<char>(input)), std::istreambuf_iterator<char>());
        std::unique_ptr<FILE, decltype(&fclose)> model(fopen(argv[1], "rb"), fclose);
        if (!model) throw std::runtime_error("Could not open the model file.");
        std::cout << polymath_generate(model.get(), prompt, 2) << '\n';
        return 0;
    } catch (const std::exception & error) { std::cerr << error.what() << '\n'; return 1; }
}
