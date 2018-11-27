#include <iostream>
#include <fstream>
#include <string>

int main(int argc, char **argv) {

  if (argc < 3) {
    std::cerr << "usage: " << argv[0] << " inputfastq outputprefix\n";
    return 1;
  }
  
  std::ifstream f(argv[1]);
  std::ofstream r1(std::string(argv[2]) + ".R1.fastq");
  std::ofstream r2(std::string(argv[2]) + ".R2.fastq");
  std::string line;
  
  while (std::getline(f, line)) {

    /* determine which end this is and choose the appropriate output stream */
    std::ofstream &r = (std::string::npos != line.find("1:N:") ? r1 : r2);
    r << line << '\n';

    /* there are three associated data lines per read */
    for (auto i = 0; i < 3; ++i) {
      if (!std::getline(f, line)) {
	std::cerr << "Unexpected EOF: a read did not have three associated data lines. The number of lines in "
		  << argv[1] << " should be a multiple of 4.\n";
	return 1;
      }
      r << line << '\n';
    }
    
  }

  return 0;
  
}
