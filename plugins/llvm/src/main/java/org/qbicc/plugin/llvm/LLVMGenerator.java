package org.qbicc.plugin.llvm;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.object.ProgramModule;

/**
 *
 */
public class LLVMGenerator implements Consumer<CompilationContext>, ValueVisitor<CompilationContext, LLValue> {
    private final int picLevel;
    private final int pieLevel;
    private int referenceAddressSpace;

    public LLVMGenerator(final int picLevel, final int pieLevel, final int referenceAddressSpace) {
        this.picLevel = picLevel;
        this.pieLevel = pieLevel;
        this.referenceAddressSpace = referenceAddressSpace;
    }

    public void accept(final CompilationContext compilationContext) {
        LLVMModuleGenerator generator = new LLVMModuleGenerator(compilationContext, picLevel, pieLevel, referenceAddressSpace);
        List<ProgramModule> allProgramModules = compilationContext.getAllProgramModules();
        Iterator<ProgramModule> iterator = allProgramModules.iterator();
        compilationContext.runParallelTask(ctxt -> {
            for (;;) {
                ProgramModule programModule;
                synchronized (iterator) {
                    if (! iterator.hasNext()) {
                        return;
                    }
                    programModule = iterator.next();
                }
                Path outputFile = generator.processProgramModule(programModule);
                LLVMState llvmState = ctxt.computeAttachmentIfAbsent(LLVMState.KEY, LLVMState::new);
                llvmState.addModulePath(programModule.getTypeDefinition().load(), outputFile);
            }
        });
    }
}
