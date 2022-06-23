package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;
import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.Platform;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.nio.file.Path;
import java.util.function.Function;

public interface LLVMCompiler {
    interface Factory {
        LLVMCompiler create(CompilationContext context, boolean isPie);
    }
    Function<Platform, Factory> FACTORY = (platform) -> {
        if (platform.getCpu() == Cpu.WASM32) {
            return LLVMEmscriptenCompilerImpl::new;
        } else {
            return LLVMCompilerImpl::new;
        }
    };

    void compileModule(CompilationContext context, LoadedTypeDefinition typeDefinition, Path modulePath);
}
