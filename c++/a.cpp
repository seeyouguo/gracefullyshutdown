#include <iostream>
#include <cstdlib> // Needed to use the exit function
using namespace std;

// Function prototype
void someFunction();
int main ()
{
    someFunction ();
    return 0;
}

void someFunction()
{
    cout << "This program terminates with the exit function.\n";
    cout << "Bye!\n";
    std::exit (0);
    cout << "This message will never be displayed\n";
    cout << "because the program has already terminated.\n";
}
