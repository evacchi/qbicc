package org.qbicc.tests;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.CNative;
import org.qbicc.runtime.CNative.export;
import org.qbicc.runtime.CNative.extern;
import org.qbicc.runtime.Hidden;
import org.qbicc.tests.snippets.ArithmeticCompare;
import org.qbicc.tests.snippets.ArithmeticNegation;
import org.qbicc.tests.snippets.Arrays;
import org.qbicc.tests.snippets.ClassInit;
import org.qbicc.tests.snippets.ClassLiteralTests;
import org.qbicc.tests.snippets.DynamicTypeTests;
import org.qbicc.tests.snippets.InvokeInterface;
import org.qbicc.tests.snippets.InvokeVirtual;
import org.qbicc.tests.snippets.MathMinMax;
import org.qbicc.tests.snippets.MethodHandle;
import org.qbicc.tests.snippets.Synchronized;
import org.qbicc.tests.snippets.TryCatch;

import java.util.List;

import static org.qbicc.runtime.CNative.word;

/**
 * The main test coordinator.
 */
public final class TestRunner {
    private TestRunner() {}

    @export
    public static int proxy_abi_version_0_2_0(int memory_size) {
        return 0;
    }

    @export
    public static void proxy_on_memory_allocate() {

    }

    public static void main(String[] args) throws Throwable {
        if (args.length == 0) {
            System.err.println("Expected an argument to indicate which test to run");
            System.exit(1);
            return; // unreachable
        }
        String test = args[0];
        System.out.println(List.of(args));
        String[] testArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
        System.out.println(List.of(testArgs));
        switch (test) {
            case "snippet-ArithmeticCompare" -> ArithmeticCompare.main(testArgs);
            case "snippet-ArithmeticNegation" -> ArithmeticNegation.main(testArgs);
            case "snippet-Arrays" -> Arrays.main(testArgs);
            case "snippet-ClassInit" -> ClassInit.main(testArgs);
            case "snippet-DynamicTypeTests" -> DynamicTypeTests.main(testArgs);
            case "snippet-InvokeInterface" -> InvokeInterface.main(testArgs);
            case "snippet-InvokeVirtual" -> InvokeVirtual.main(testArgs);
            case "snippet-MathMinMax" -> MathMinMax.main(testArgs);
            case "snippet-MethodHandle" -> MethodHandle.main(testArgs);
            case "snippet-TryCatch" -> TryCatch.main(testArgs);
            case "snippet-ClassLiteralTests" -> ClassLiteralTests.main(testArgs);
            case "snippet-Synchronized" -> Synchronized.main(testArgs);
            default -> {
                System.err.printf("Unknown test name \"%s\"%n", test);
                System.exit(1);
            }
        }
    }
}
