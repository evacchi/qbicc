package org.qbicc.plugin.llvm;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.LoadedTypeDefinition;

public class LLVMCompileStage implements Consumer<CompilationContext> {

    private final LLVMCompiler.Factory compilerFactory;
    private final boolean isPie;

    public LLVMCompileStage(LLVMCompiler.Factory factory, boolean isPie) {
        this.compilerFactory = factory;
        this.isPie = isPie;
    }

    public void accept(final CompilationContext context) {
        LLVMState llvmState = context.getAttachment(LLVMState.KEY);
        if (llvmState == null) {
            context.note("No LLVM compilation units detected");
            return;
        }

        Iterator<Map.Entry<LoadedTypeDefinition, Path>> iterator = llvmState.getModulePaths().entrySet().iterator();
        context.runParallelTask(ctxt -> {
            LLVMCompiler compiler = compilerFactory.create(context, isPie);
            for (;;) {
                Map.Entry<LoadedTypeDefinition, Path> entry;
                synchronized (iterator) {
                    if (! iterator.hasNext()) {
                        return;
                    }
                    entry = iterator.next();
                }
                LoadedTypeDefinition typeDefinition = entry.getKey();
                Path modulePath = entry.getValue();
                compiler.compileModule(ctxt, typeDefinition, modulePath);
            }
        });
    }
}
