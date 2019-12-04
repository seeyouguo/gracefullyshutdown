/* for Mac sysctl kernel.coredump=1 */
/* ulimit -c unlimited */
/* dump path /cores/ */
/* lldb -core core.83877 */
int main(int argc, char* argv[])
{
    int i = 10;
    printf("hello word\n");
    abort();
    return 0;
}
