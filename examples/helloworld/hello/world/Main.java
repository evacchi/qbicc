// Basic helloworld example with the commands to execute it. 
//
// Compile the example with jbang (0.65.1+):
// $ jbang build --java=17 examples/helloworld/hello/world/Main.java
//
// Build the native executable in /tmp/output with:
// $ jbang org.qbicc:qbicc-main:0.3.0-SNAPSHOT --boot-path-append-file $(jbang info classpath examples/helloworld/hello/world/Main.java) --output-path /tmp/output hello.world.Main
//
// Run the executable
// $ /tmp/output/a.out
//
package hello.world;
import static org.qbicc.runtime.CNative.*;

/**
 * The classic Hello World program for Java.
 */
public class Main {
//    @extern
//    public static native int putchar(int arg);
//    @export
//    public static int main() {
//        putchar('h');
//        putchar('e');
//        putchar('l');
//        putchar('l');
//        putchar('o');
//        putchar(' ');
//        putchar('E');
//        putchar('n');
//        putchar('v');
//        putchar('o');
//        putchar('y');
//        putchar('\n');
//        return 0;
//    }
//
//    @export public static void proxy_abi_version_0_2_0() {}
//    @export public static int proxy_on_memory_allocate(int memory_size) { return 0; }
//    @export public static boolean proxy_on_vm_start(int root_context_id, int vm_configuration_size) {
//        return true;
//    }
//
    public static void main(String[] args) {
        System.out.println("hello world");
    }

}
