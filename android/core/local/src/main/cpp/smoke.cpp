#include "local_llm.h"
#include <fstream>
#include <iostream>
#include <iterator>
int main(int argc, char ** argv) {
    if (argc != 3) return 2;
    try {
        std::ifstream input(argv[2]);
        std::string prompt((std::istreambuf_iterator<char>(input)), std::istreambuf_iterator<char>());
        std::cout << polymath_generate(argv[1], prompt, 2) << '\n';
        return 0;
    } catch (const std::exception & error) { std::cerr << error.what() << '\n'; return 1; }
}
